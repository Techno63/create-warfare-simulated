package com.julian.createwarfare.effects.server;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SubLevelInclusiveLevelEntityGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class FragmentationEffect {

    private static final List<Fragmentation> EFFECTS =
            new ArrayList<>();

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float speed,
            float radius,
            int fragments,
            float power,
            float damage,
            boolean falloff
    ) {
        Vec3 origin =
                new Vec3(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5
                );

        SubLevel subLevel =
                Sable.HELPER.getContaining(
                        level,
                        pos
                );

        if (subLevel != null) {
            origin =
                    subLevel
                            .logicalPose()
                            .transformPosition(
                                    origin
                            );
        }

        EFFECTS.add(
                new Fragmentation(
                        level,
                        origin,
                        speed,
                        radius,
                        fragments,
                        power,
                        damage,
                        falloff
                )
        );
    }

    public static void tick() {
        EFFECTS.removeIf(
                Fragmentation::tick
        );
    }

    private static class Fragmentation {

        private final ServerLevel level;
        private final Vec3 origin;
        private final float baseSpeed;
        private final float radius;
        private final float basePower;
        private final float baseDamage;
        private final boolean falloff;
        private final List<Fragment> fragments;

        private Fragmentation(
                ServerLevel level,
                Vec3 origin,
                float speed,
                float radius,
                int fragmentCount,
                float power,
                float damage,
                boolean falloff
        ) {
            this.level = level;
            this.origin = origin;
            this.baseSpeed = speed;
            this.radius = radius;
            this.basePower = power;
            this.baseDamage = damage;
            this.falloff = falloff;

            this.fragments =
                    new ArrayList<>(
                            Math.max(
                                    0,
                                    fragmentCount
                            )
                    );

            Random random =
                    new Random();

            for (int i = 0; i < fragmentCount; i++) {
                fragments.add(
                        new Fragment(
                                randomDirection(
                                        random
                                )
                        )
                );
            }
        }

        private boolean tick() {
            boolean allDead = true;

            for (Fragment fragment : fragments) {
                if (fragment.dead) {
                    continue;
                }

                allDead = false;

                float multiplier =
                        getFalloff(
                                fragment.distance
                        );

                float currentSpeed =
                        baseSpeed * multiplier;

                float nextDistance =
                        fragment.distance + currentSpeed;

                if (nextDistance > radius) {
                    nextDistance = radius;
                }

                Vec3 from =
                        origin.add(
                                fragment.direction.scale(
                                        fragment.distance
                                )
                        );

                Vec3 to =
                        origin.add(
                                fragment.direction.scale(
                                        nextDistance
                                )
                        );

                processSegment(
                        fragment,
                        from,
                        to,
                        nextDistance
                );

                fragment.distance =
                        nextDistance;

                if (fragment.distance >= radius) {
                    fragment.dead = true;
                }
            }

            return allDead;
        }

        private void processSegment(
                Fragment fragment,
                Vec3 from,
                Vec3 to,
                float distance
        ) {
            ClipContext context =
                    new ClipContext(
                            from,
                            to,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            CollisionContext.empty()
                    );

            SubLevel subLevel =
                    Sable.HELPER.getContaining(
                            level,
                            from
                    );

            if (subLevel != null) {
                ((dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension) context)
                        .sable$setIgnoredSubLevel(
                                subLevel
                        );
            }

            BlockHitResult blockHit =
                    level.clip(
                            context
                    );

            Vec3 blockLocation =
                    blockHit.getType() ==
                            HitResult.Type.BLOCK
                            ? blockHit.getLocation()
                            : to;

            boolean blockWasHit =
                    blockHit.getType() ==
                            HitResult.Type.BLOCK;

            damageEntities(
                    fragment,
                    from,
                    blockLocation,
                    distance
            );

            if (!blockWasHit) {
                return;
            }

            BlockPos blockPos =
                    blockHit.getBlockPos();

            net.minecraft.world.level.block.state.BlockState state =
                    level.getBlockState(
                            blockPos
                    );

            float hardness =
                    state.getDestroySpeed(
                            level,
                            blockPos
                    );

            spawnImpactParticles(
                    blockLocation
            );

            if (hardness < 0.0f) {
                fragment.dead = true;
                return;
            }

            float multiplier =
                    falloff
                            ? getFalloff(distance)
                            : 1.0f;

            float currentPower =
                    basePower * multiplier;

            if (hardness > currentPower) {
                fragment.dead = true;
                return;
            }

            level.setBlock(
                    blockPos,
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                    3
            );
        }

        @SuppressWarnings("unchecked")
        private void damageEntities(
                Fragment fragment,
                Vec3 from,
                Vec3 to,
                float distance
        ) {
            double minX =
                    Math.min(
                            from.x,
                            to.x
                    ) - 0.75;

            double minY =
                    Math.min(
                            from.y,
                            to.y
                    ) - 0.75;

            double minZ =
                    Math.min(
                            from.z,
                            to.z
                    ) - 0.75;

            double maxX =
                    Math.max(
                            from.x,
                            to.x
                    ) + 0.75;

            double maxY =
                    Math.max(
                            from.y,
                            to.y
                    ) + 0.75;

            double maxZ =
                    Math.max(
                            from.z,
                            to.z
                    ) + 0.75;

            AABB area =
                    new AABB(
                            minX,
                            minY,
                            minZ,
                            maxX,
                            maxY,
                            maxZ
                    );

            SubLevelInclusiveLevelEntityGetter<Entity> getter =
                    (SubLevelInclusiveLevelEntityGetter<Entity>)
                            (Object) level.getEntities();

            List<LivingEntity> entities =
                    new ArrayList<>();

            getter.getIgnoringSubLevels(
                    area,
                    entity -> {
                        if (entity instanceof LivingEntity livingEntity) {
                            entities.add(
                                    livingEntity
                            );
                        }
                    }
            );

            for (LivingEntity entity : entities) {
                damageEntity(
                        fragment,
                        entity,
                        from,
                        to,
                        distance
                );
            }
        }

        private void damageEntity(
                Fragment fragment,
                LivingEntity entity,
                Vec3 from,
                Vec3 to,
                float distance
        ) {
            UUID id =
                    entity.getUUID();

            if (fragment.hitEntities.contains(id)) {
                return;
            }

            Vec3 hitPoint =
                    getClosestPointOnSegment(
                            entity.position(),
                            from,
                            to
                    );

            double distanceToRay =
                    entity.position()
                            .distanceTo(
                                    hitPoint
                            );

            if (distanceToRay > 1.0) {
                return;
            }

            fragment.hitEntities.add(
                    id
            );

            float multiplier =
                    falloff
                            ? getFalloff(distance)
                            : 1.0f;

            entity.hurt(
                    level.damageSources().generic(),
                    baseDamage * multiplier
            );

            spawnImpactParticles(
                    hitPoint
            );
        }

        private Vec3 getClosestPointOnSegment(
                Vec3 point,
                Vec3 start,
                Vec3 end
        ) {
            Vec3 segment =
                    end.subtract(
                            start
                    );

            double lengthSquared =
                    segment.lengthSqr();

            if (lengthSquared <= 0.000001) {
                return start;
            }

            double t =
                    point
                            .subtract(start)
                            .dot(segment)
                            / lengthSquared;

            t =
                    Mth.clamp(
                            t,
                            0.0,
                            1.0
                    );

            return start.add(
                    segment.scale(
                            t
                    )
            );
        }

        private void spawnImpactParticles(
                Vec3 position
        ) {
            level.sendParticles(
                    ParticleTypes.CRIT,
                    position.x,
                    position.y,
                    position.z,
                    12,
                    0.08,
                    0.08,
                    0.08,
                    0.12
            );
        }

        private float getFalloff(
                float distance
        ) {
            if (!falloff) {
                return 1.0f;
            }

            return Mth.clamp(
                    1.0f
                            - (distance / radius)
                            * 0.5f,
                    0.5f,
                    1.0f
            );
        }

        private Vec3 randomDirection(
                Random random
        ) {
            double x =
                    random.nextDouble() * 2.0 - 1.0;

            double y =
                    random.nextDouble() * 2.0 - 1.0;

            double z =
                    random.nextDouble() * 2.0 - 1.0;

            Vec3 direction =
                    new Vec3(
                            x,
                            y,
                            z
                    );

            if (direction.lengthSqr() < 0.0001) {
                return new Vec3(
                        0.0,
                        1.0,
                        0.0
                );
            }

            return direction.normalize();
        }
    }

    private static class Fragment {

        private final Vec3 direction;

        private final Set<UUID> hitEntities =
                new HashSet<>();

        private float distance;
        private boolean dead;

        private Fragment(
                Vec3 direction
        ) {
            this.direction = direction;
        }
    }
}