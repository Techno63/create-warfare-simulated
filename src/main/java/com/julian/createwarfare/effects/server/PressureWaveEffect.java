package com.julian.createwarfare.effects.server;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PressureWaveEffect {

    private static final List<PressureWave> WAVES = new ArrayList<>();

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float speed,
            float radius,
            float strength,
            boolean falloff
    ) {
        WAVES.add(
                new PressureWave(
                        level,
                        pos.getCenter(),
                        speed,
                        radius,
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

        private final Set<Integer> affectedEntities = new HashSet<>();
        private final Set<Object> affectedSubLevels = new HashSet<>();

        private int ticks;

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
            ticks++;

            double radiusSqr = radius * radius;

            AABB area = new AABB(
                    center.x - radius,
                    center.y - radius,
                    center.z - radius,
                    center.x + radius,
                    center.y + radius,
                    center.z + radius
            );

            for (LivingEntity entity : level.getEntitiesOfClass(
                    LivingEntity.class,
                    area
            )) {
                if (!entity.isAlive() || entity.isSpectator()) {
                    continue;
                }

                if (affectedEntities.contains(entity.getId())) {
                    continue;
                }

                double distanceSqr = entity.distanceToSqr(center);

                if (distanceSqr > radiusSqr) {
                    continue;
                }

                double distance = Math.sqrt(distanceSqr);

                int delay = Math.max(
                        1,
                        (int) Math.ceil(distance / speed)
                );

                if (ticks < delay) {
                    continue;
                }

                affectedEntities.add(entity.getId());

                applyEntityEffect(entity, distance);
            }

            BoundingBox3d sableArea = new BoundingBox3d(
                    area.minX,
                    area.minY,
                    area.minZ,
                    area.maxX,
                    area.maxY,
                    area.maxZ
            );

            applySableEffect(sableArea, radiusSqr);

            return ticks < Math.ceil(radius / speed);
        }

        private void applyEntityEffect(
                LivingEntity entity,
                double distance
        ) {
            float strengthMultiplier =
                    getStrengthMultiplier(distance);

            float effectiveStrength =
                    strength * strengthMultiplier;

            Vec3 direction =
                    entity.position().subtract(center);

            if (direction.lengthSqr() < 0.0001) {
                direction =
                        new Vec3(1.0, 0.0, 0.0);
            } else {
                direction = direction.normalize();
            }

            double knockback =
                    effectiveStrength * 0.12;

            double upwardForce =
                    effectiveStrength * 0.08;

            Vec3 velocity =
                    entity.getDeltaMovement();

            entity.setDeltaMovement(
                    velocity.x + direction.x * knockback,
                    velocity.y + direction.y * knockback + upwardForce,
                    velocity.z + direction.z * knockback
            );

            entity.hurtMarked = true;
        }

        private void applySableEffect(
                BoundingBox3d area,
                double radiusSqr
        ) {
            for (SubLevelAccess subLevel :
                    SableCompanion.INSTANCE.getAllIntersecting(level, area)) {

                if (!(subLevel instanceof ServerSubLevel serverSubLevel)) {
                    continue;
                }

                Object id = subLevel.getUniqueId();

                if (affectedSubLevels.contains(id)) {
                    continue;
                }

                BoundingBox3dc boundingBox =
                        subLevel.boundingBox();

                double closestX = Math.max(
                        boundingBox.minX(),
                        Math.min(center.x, boundingBox.maxX())
                );

                double closestY = Math.max(
                        boundingBox.minY(),
                        Math.min(center.y, boundingBox.maxY())
                );

                double closestZ = Math.max(
                        boundingBox.minZ(),
                        Math.min(center.z, boundingBox.maxZ())
                );

                Vec3 closestPoint = new Vec3(
                        closestX,
                        closestY,
                        closestZ
                );

                double distanceSqr =
                        closestPoint.distanceToSqr(center);

                if (distanceSqr > radiusSqr) {
                    continue;
                }

                double distance =
                        Math.sqrt(distanceSqr);

                int delay = Math.max(
                        1,
                        (int) Math.ceil(distance / speed)
                );

                if (ticks < delay) {
                    continue;
                }

                RigidBodyHandle handle =
                        RigidBodyHandle.of(serverSubLevel);

                if (handle == null || !handle.isValid()) {
                    continue;
                }

                affectedSubLevels.add(id);

                applySableImpulse(
                        serverSubLevel,
                        handle,
                        closestPoint,
                        distance
                );
            }
        }

        private void applySableImpulse(
                ServerSubLevel subLevel,
                RigidBodyHandle handle,
                Vec3 point,
                double distance
        ) {
            float strengthMultiplier =
                    getStrengthMultiplier(distance);

            double effectiveStrength =
                    strength * strengthMultiplier;

            Vec3 direction =
                    point.subtract(center);

            if (direction.lengthSqr() < 0.0001) {
                direction =
                        new Vec3(1.0, 0.0, 0.0);
            } else {
                direction = direction.normalize();
            }

            Vector3d localPosition =
                    subLevel.logicalPose().transformPositionInverse(
                            new Vector3d(
                                    center.x,
                                    center.y,
                                    center.z
                            )
                    );

            Vector3d localForce =
                    subLevel.logicalPose().transformNormalInverse(
                            new Vector3d(
                                    direction.x,
                                    direction.y,
                                    direction.z
                            )
                    );

            double impulseStrength =
                    effectiveStrength * 16.0;

            localForce.mul(impulseStrength);

            localForce.y += effectiveStrength * 0.02;

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

            float strengthMultiplier =
                    1.0f - (float) (distance / radius) * 0.5f;

            return Math.max(
                    0.5f,
                    Math.min(
                            1.0f,
                            strengthMultiplier
                    )
            );
        }
    }
}