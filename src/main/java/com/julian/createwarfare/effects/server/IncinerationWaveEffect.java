package com.julian.createwarfare.effects.server;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class IncinerationWaveEffect {

    private static final List<IncinerationWave> WAVES =
            new ArrayList<>();

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float speed,
            float radius,
            float damage,
            int burnTime,
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

        WAVES.add(
                new IncinerationWave(
                        level,
                        origin,
                        speed,
                        radius,
                        damage,
                        burnTime,
                        falloff
                )
        );
    }

    public static void tick() {
        WAVES.removeIf(
                IncinerationWave::tick
        );
    }

    private static class IncinerationWave {

        private final ServerLevel level;
        private final Vec3 origin;
        private final float speed;
        private final float radius;
        private final float damage;
        private final int burnTime;
        private final boolean falloff;

        private final Set<UUID> affectedEntities =
                new HashSet<>();

        private float currentRadius;

        private IncinerationWave(
                ServerLevel level,
                Vec3 origin,
                float speed,
                float radius,
                float damage,
                int burnTime,
                boolean falloff
        ) {
            this.level = level;
            this.origin = origin;
            this.speed = speed;
            this.radius = radius;
            this.damage = damage;
            this.burnTime = burnTime;
            this.falloff = falloff;
        }

        private boolean tick() {
            float previousRadius =
                    currentRadius;

            currentRadius += speed;

            if (currentRadius > radius) {
                currentRadius = radius;
            }

            AABB area =
                    new AABB(
                            origin.x - currentRadius,
                            origin.y - currentRadius,
                            origin.z - currentRadius,
                            origin.x + currentRadius,
                            origin.y + currentRadius,
                            origin.z + currentRadius
                    );

            List<LivingEntity> entities =
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            area
                    );

            for (LivingEntity entity : entities) {
                UUID id =
                        entity.getUUID();

                if (affectedEntities.contains(id)) {
                    continue;
                }

                double distance =
                        entity.position()
                                .distanceTo(origin);

                if (distance <= previousRadius ||
                        distance > currentRadius) {
                    continue;
                }

                affectedEntities.add(id);

                float multiplier =
                        falloff
                                ? getFalloff(distance)
                                : 1.0f;

                float effectiveDamage =
                        damage * multiplier;

                int effectiveBurnTime =
                        Math.max(
                                1,
                                Math.round(
                                        burnTime * multiplier
                                )
                        );

                entity.hurt(
                        level.damageSources().onFire(),
                        effectiveDamage
                );

                entity.setRemainingFireTicks(
                        Math.max(
                                entity.getRemainingFireTicks(),
                                effectiveBurnTime * 20
                        )
                );
            }

            return currentRadius >= radius;
        }

        private float getFalloff(
                double distance
        ) {
            return Mth.clamp(
                    1.0f
                            - (float)
                            (distance / radius)
                            * 0.5f,
                    0.5f,
                    1.0f
            );
        }
    }
}