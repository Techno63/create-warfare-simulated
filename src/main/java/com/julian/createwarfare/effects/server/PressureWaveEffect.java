package com.julian.createwarfare.effects.server;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PressureWaveEffect {

    private static final float NANOS_PER_TICK =
            50_000_000.0f;

    private static final List<PressureWave> WAVES =
            new ArrayList<>();

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float speed,
            float radius,
            float strength,
            boolean falloff
    ) {
        Position rawCenter =
                pos.getCenter();

        Vec3 center =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawCenter
                );

        WAVES.add(
                new PressureWave(
                        level,
                        center,
                        Math.max(speed, 0.01f),
                        Math.max(radius, 0.01f),
                        strength,
                        falloff
                )
        );
    }

    public static void tick() {
        for (int i = WAVES.size() - 1; i >= 0; i--) {
            if (!WAVES.get(i).tick()) {
                WAVES.remove(i);
            }
        }
    }

    private static class PressureWave {

        private final ServerLevel level;
        private final Vec3 center;
        private final float speed;
        private final float radius;
        private final float strength;
        private final boolean falloff;
        private final long startNanos =
                System.nanoTime();

        private final Set<Integer> affectedEntities =
                new HashSet<>();

        private final Set<Object> affectedSubLevels =
                new HashSet<>();

        private PressureWave(
                ServerLevel level,
                Vec3 center,
                float speed,
                float radius,
                float strength,
                boolean falloff
        ) {
            this.level = level;
            this.center = center;
            this.speed = speed;
            this.radius = radius;
            this.strength = strength;
            this.falloff = falloff;
        }

        private boolean tick() {
            float elapsedTicks =
                    (System.nanoTime() - startNanos) /
                            NANOS_PER_TICK;

            float travelled =
                    Math.min(
                            elapsedTicks * speed,
                            radius
                    );

            double travelledSqr =
                    (double) travelled *
                            travelled;

            AABB area =
                    new AABB(
                            center.x - travelled,
                            center.y - travelled,
                            center.z - travelled,
                            center.x + travelled,
                            center.y + travelled,
                            center.z + travelled
                    );

            for (LivingEntity entity :
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            area
                    )) {

                if (!entity.isAlive() ||
                        entity.isSpectator()) {
                    continue;
                }

                if (affectedEntities.contains(
                        entity.getId()
                )) {
                    continue;
                }

                double distanceSqr =
                        entity.distanceToSqr(
                                center
                        );

                if (distanceSqr >
                        travelledSqr) {
                    continue;
                }

                affectedEntities.add(
                        entity.getId()
                );

                applyEntityEffect(
                        entity,
                        Math.sqrt(distanceSqr)
                );
            }

            applySableEffect(
                    new BoundingBox3d(
                            area.minX,
                            area.minY,
                            area.minZ,
                            area.maxX,
                            area.maxY,
                            area.maxZ
                    ),
                    travelledSqr
            );

            return travelled < radius;
        }

        private Vec3 directionFrom(
                Vec3 point
        ) {
            Vec3 direction =
                    point.subtract(
                            center
                    );

            if (direction.lengthSqr() <
                    0.0001) {
                return new Vec3(
                        1.0,
                        0.0,
                        0.0
                );
            }

            return direction.normalize();
        }

        private void applyEntityEffect(
                LivingEntity entity,
                double distance
        ) {
            float effectiveStrength =
                    strength *
                            getStrengthMultiplier(
                                    distance
                            );

            Vec3 direction =
                    directionFrom(
                            entity.position()
                    );

            double knockback =
                    effectiveStrength *
                            0.12;

            double upward =
                    effectiveStrength *
                            0.08;

            Vec3 velocity =
                    entity.getDeltaMovement();

            entity.setDeltaMovement(
                    velocity.x +
                            direction.x *
                                    knockback,
                    velocity.y +
                            direction.y *
                                    knockback +
                            upward,
                    velocity.z +
                            direction.z *
                                    knockback
            );

            entity.hurtMarked =
                    true;
        }

        private void applySableEffect(
                BoundingBox3d area,
                double travelledSqr
        ) {
            for (SubLevelAccess subLevel :
                    SableCompanion.INSTANCE.getAllIntersecting(
                            level,
                            area
                    )) {

                if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
                    continue;
                }

                Object id =
                        subLevel.getUniqueId();

                if (affectedSubLevels.contains(
                        id
                )) {
                    continue;
                }

                BoundingBox3dc box =
                        subLevel.boundingBox();

                Vec3 closest =
                        new Vec3(
                                Math.max(
                                        box.minX(),
                                        Math.min(
                                                center.x,
                                                box.maxX()
                                        )
                                ),
                                Math.max(
                                        box.minY(),
                                        Math.min(
                                                center.y,
                                                box.maxY()
                                        )
                                ),
                                Math.max(
                                        box.minZ(),
                                        Math.min(
                                                center.z,
                                                box.maxZ()
                                        )
                                )
                        );

                double distanceSqr =
                        closest.distanceToSqr(
                                center
                        );

                if (distanceSqr >
                        travelledSqr) {
                    continue;
                }

                RigidBodyHandle handle =
                        RigidBodyHandle.of(
                                serverSubLevel
                        );

                if (handle == null ||
                        !handle.isValid()) {
                    continue;
                }

                affectedSubLevels.add(
                        id
                );

                applySableImpulse(
                        serverSubLevel,
                        handle,
                        closest,
                        Math.sqrt(
                                distanceSqr
                        )
                );
            }
        }

        private void applySableImpulse(
                ServerSubLevel subLevel,
                RigidBodyHandle handle,
                Vec3 point,
                double distance
        ) {
            double effectiveStrength =
                    strength *
                            getStrengthMultiplier(
                                    distance
                            );

            Vec3 direction =
                    directionFrom(
                            point
                    );

            Vector3d localPosition =
                    subLevel.logicalPose()
                            .transformPositionInverse(
                                    new Vector3d(
                                            point.x,
                                            point.y,
                                            point.z
                                    )
                            );

            Vector3d localForce =
                    subLevel.logicalPose()
                            .transformNormalInverse(
                                    new Vector3d(
                                            direction.x,
                                            direction.y,
                                            direction.z
                                    )
                            );

            double sablePushMultiplier =
                    2.0;

            localForce.mul(
                    effectiveStrength *
                            sablePushMultiplier
            );

            localForce.y +=
                    effectiveStrength *
                            0.005;

            handle.applyImpulseAtPoint(
                    localPosition,
                    localForce
            );
        }

        private float getStrengthMultiplier(
                double distance
        ) {
            if (!falloff) {
                return 1.0f;
            }

            return Mth.clamp(
                    1.0f -
                            (float) (
                                    distance /
                                            radius
                            ) *
                                    0.5f,
                    0.5f,
                    1.0f
            );
        }
    }
}