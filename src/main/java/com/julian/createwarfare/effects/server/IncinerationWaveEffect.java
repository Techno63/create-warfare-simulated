package com.julian.createwarfare.effects.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.ryanhcode.sable.companion.SableCompanion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IncinerationWaveEffect {

    private static final float NANOS_PER_TICK = 50_000_000.0f;

    private static final List<IncinerationWave> WAVES =
            new ArrayList<>();

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float speed,
            float radius,
            float damage,
            int fireSeconds,
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
                new IncinerationWave(
                        level,
                        center,
                        Math.max(speed, 0.01f),
                        Math.max(radius, 0.01f),
                        Math.max(damage, 0.0f),
                        Math.max(fireSeconds, 0),
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

    private static class IncinerationWave {

        private final ServerLevel level;
        private final Vec3 center;
        private final float speed;
        private final float radius;
        private final float damage;
        private final int fireSeconds;
        private final boolean falloff;

        private final Set<Integer> affectedEntities =
                new HashSet<>();

        private final Set<Long> ignitedBlocks =
                new HashSet<>();

        private final long startNanos =
                System.nanoTime();

        private IncinerationWave(
                ServerLevel level,
                Vec3 center,
                float speed,
                float radius,
                float damage,
                int fireSeconds,
                boolean falloff
        ) {
            this.level = level;
            this.center = center;
            this.speed = speed;
            this.radius = radius;
            this.damage = damage;
            this.fireSeconds = fireSeconds;
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

            AABB area = new AABB(
                    center.x - travelled,
                    center.y - travelled,
                    center.z - travelled,
                    center.x + travelled,
                    center.y + travelled,
                    center.z + travelled
            );

            igniteBlocks(area, travelled);

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
                        entity.distanceToSqr(center);

                if (distanceSqr >
                        travelledSqr) {
                    continue;
                }

                double distance =
                        Math.sqrt(distanceSqr);

                affectedEntities.add(
                        entity.getId()
                );

                applyEntityEffect(
                        entity,
                        distance
                );
            }

            return travelled < radius;
        }

        private void igniteBlocks(
                AABB area,
                float travelled
        ) {
            int minX = Mth.floor(area.minX);
            int minY = Mth.floor(area.minY);
            int minZ = Mth.floor(area.minZ);

            int maxX = Mth.floor(area.maxX);
            int maxY = Mth.floor(area.maxY);
            int maxZ = Mth.floor(area.maxZ);

            BlockPos.MutableBlockPos pos =
                    new BlockPos.MutableBlockPos();

            double travelledSqr =
                    (double) travelled *
                            travelled;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (y < level.getMinBuildHeight() ||
                            y >= level.getMaxBuildHeight()) {
                        continue;
                    }

                    for (int z = minZ; z <= maxZ; z++) {
                        double dx =
                                x + 0.5 -
                                        center.x;

                        double dy =
                                y + 0.5 -
                                        center.y;

                        double dz =
                                z + 0.5 -
                                        center.z;

                        double distanceSqr =
                                dx * dx +
                                        dy * dy +
                                        dz * dz;

                        if (distanceSqr >
                                travelledSqr) {
                            continue;
                        }

                        pos.set(x, y, z);

                        long blockKey =
                                pos.asLong();

                        if (ignitedBlocks.contains(
                                blockKey
                        )) {
                            continue;
                        }

                        BlockState state =
                                level.getBlockState(pos);

                        if (!state.isFlammable(
                                level,
                                pos,
                                net.minecraft.core.Direction.UP
                        )) {
                            continue;
                        }

                        BlockPos firePos =
                                pos.above();

                        if (!level.isEmptyBlock(
                                firePos
                        )) {
                            continue;
                        }

                        if (!BaseFireBlock.canBePlacedAt(
                                level,
                                firePos,
                                net.minecraft.core.Direction.UP
                        )) {
                            continue;
                        }

                        level.setBlockAndUpdate(
                                firePos,
                                BaseFireBlock.getState(
                                        level,
                                        firePos
                                )
                        );

                        ignitedBlocks.add(
                                blockKey
                        );
                    }
                }
            }
        }

        private void applyEntityEffect(
                LivingEntity entity,
                double distance
        ) {
            float multiplier =
                    getDamageMultiplier(distance);

            float effectiveDamage =
                    damage * multiplier;

            if (effectiveDamage > 0.0f) {
                entity.hurt(
                        level.damageSources().inFire(),
                        effectiveDamage
                );
            }

            if (fireSeconds > 0) {
                entity.setRemainingFireTicks(
                        fireSeconds * 20
                );
            }
        }

        private float getDamageMultiplier(
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
                    0.3f,
                    1.0f
            );
        }
    }
}