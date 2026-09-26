package com.julian.createwarfare.explosions.post;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShockwavePost {

    private static final List<Shockwave> ACTIVE = new ArrayList<>();

    private static final float AFTERMATH_CHANCE = 0.35f;
    private static final float AFTERMATH_DESTRUCTION_CHANCE = 0.2f;
    private static final float FIRE_CHANCE = 0.8f;

    public static void create(
            ServerLevel level,
            Vec3 center,
            float expandSpeed,
            float radius,
            float power,
            float entityDamage,
            int verticalLayers,
            boolean fire
    ) {
        Vector3dc rawCenterJoml =
                JOMLConversion.toJOML(center);

        Vector3dc globalCenterJoml =
                SableCompanion.INSTANCE.projectOutOfSubLevel(level, rawCenterJoml, new Vector3d());

        Vec3 globalCenter =
                new Vec3(globalCenterJoml.x(), globalCenterJoml.y(), globalCenterJoml.z());

        ACTIVE.add(
                new Shockwave(
                        level,
                        globalCenter,
                        expandSpeed,
                        radius,
                        power,
                        entityDamage,
                        verticalLayers,
                        fire
                )
        );
    }

    public static void tick() {
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            if (ACTIVE.get(i).tick()) {
                ACTIVE.remove(i);
            }
        }
    }

    private static class Shockwave {

        private final ServerLevel level;
        private final Vec3 center;
        private final float expandSpeed;
        private final float radius;
        private final float power;
        private final float entityDamage;
        private final int verticalLayers;
        private final boolean fire;

        private final Set<BlockPos> destroyedBlocks =
                new HashSet<>();

        private final Set<BlockPos> processedAftermath =
                new HashSet<>();

        private final Set<LivingEntity> damagedEntities =
                new HashSet<>();

        private float currentRadius;

        private Shockwave(
                ServerLevel level,
                Vec3 center,
                float expandSpeed,
                float radius,
                float power,
                float entityDamage,
                int verticalLayers,
                boolean fire
        ) {
            this.level = level;
            this.center = center;
            this.expandSpeed = expandSpeed;
            this.radius = radius;
            this.power = power;
            this.entityDamage = entityDamage;
            this.verticalLayers = Math.max(
                    0,
                    verticalLayers
            );
            this.fire = fire;
        }

        private boolean tick() {
            float previousRadius =
                    currentRadius;

            currentRadius += expandSpeed;

            if (currentRadius > radius) {
                currentRadius = radius;
            }

            Set<BlockPos> newlyDestroyed =
                    new HashSet<>();

            breakBlocks(
                    previousRadius,
                    currentRadius,
                    newlyDestroyed
            );

            processAftermath(
                    newlyDestroyed
            );

            damageEntities(
                    previousRadius,
                    currentRadius
            );

            return currentRadius >= radius;
        }

        private void breakBlocks(
                float previousRadius,
                float currentRadius,
                Set<BlockPos> newlyDestroyed
        ) {
            int rayCount =
                    Math.max(
                            64,
                            (int) (currentRadius * 20.0f)
                    );

            double angleStep =
                    Math.PI * 2.0 / rayCount;

            for (int i = 0; i < rayCount; i++) {
                double angle =
                        angleStep * i;

                double dx =
                        Math.cos(angle);

                double dz =
                        Math.sin(angle);

                float variation =
                        1.0f
                                + (float) Math.sin(i * 2.731) * 0.025f
                                + (float) Math.sin(i * 0.417) * 0.015f;

                castRay(
                        dx,
                        dz,
                        previousRadius,
                        currentRadius * variation,
                        newlyDestroyed
                );
            }
        }

        private void castRay(
                double dx,
                double dz,
                float previousRadius,
                float currentRadius,
                Set<BlockPos> newlyDestroyed
        ) {
            float distance =
                    Math.max(
                            previousRadius,
                            0.0f
                    );

            boolean[] blocked =
                    new boolean[
                            verticalLayers * 2 + 1
                            ];

            while (distance < currentRadius) {
                int x =
                        (int) Math.floor(
                                center.x + dx * distance
                        );

                int z =
                        (int) Math.floor(
                                center.z + dz * distance
                        );

                for (
                        int offset = -verticalLayers;
                        offset <= verticalLayers;
                        offset++
                ) {
                    int index =
                            offset + verticalLayers;

                    if (blocked[index]) {
                        continue;
                    }

                    int y =
                            (int) Math.floor(center.y)
                                    + offset;

                    BlockPos worldPos =
                            new BlockPos(
                                    x,
                                    y,
                                    z
                            );

                    BlockTarget target =
                            getBlockTarget(
                                    worldPos
                            );

                    if (target == null) {
                        continue;
                    }

                    float resistance =
                            target.state()
                                    .getBlock()
                                    .getExplosionResistance();

                    if (resistance >= power) {
                        blocked[index] = true;
                        continue;
                    }

                    level.removeBlock(
                            target.pos(),
                            false
                    );

                    BlockPos immutable =
                            target.pos().immutable();

                    destroyedBlocks.add(
                            immutable
                    );

                    newlyDestroyed.add(
                            immutable
                    );

                    if (
                            fire
                                    && level.random.nextFloat() < FIRE_CHANCE
                                    && Blocks.FIRE.defaultBlockState().canSurvive(
                                    level,
                                    immutable
                            )
                    ) {
                        level.setBlock(
                                immutable,
                                Blocks.FIRE.defaultBlockState(),
                                3
                        );
                    }
                }

                distance += 1.0f;
            }
        }

        private BlockTarget getBlockTarget(
                BlockPos worldPos
        ) {
            return Sable.HELPER.runIncludingSubLevels(
                    level,
                    worldPos.getCenter(),
                    true,
                    null,
                    (access, pos) -> {
                        BlockState state =
                                level.getBlockState(pos);

                        if (state.isAir()) {
                            return null;
                        }

                        return new BlockTarget(
                                pos,
                                state
                        );
                    }
            );
        }

        private void processAftermath(
                Set<BlockPos> newlyDestroyed
        ) {
            for (BlockPos destroyed : newlyDestroyed) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (
                                    dx == 0
                                            && dy == 0
                                            && dz == 0
                            ) {
                                continue;
                            }

                            BlockPos pos =
                                    destroyed.offset(
                                            dx,
                                            dy,
                                            dz
                                    );

                            processAftermathBlock(
                                    pos
                            );
                        }
                    }
                }
            }
        }

        private void processAftermathBlock(
                BlockPos pos
        ) {
            if (!processedAftermath.add(pos)) {
                return;
            }

            if (destroyedBlocks.contains(pos)) {
                return;
            }

            BlockState state =
                    level.getBlockState(pos);

            if (state.isAir()) {
                return;
            }

            BlockState aftermath =
                    getAftermathState(
                            state,
                            pos
                    );

            if (aftermath == null) {
                return;
            }

            if (
                    level.random.nextFloat()
                            < AFTERMATH_DESTRUCTION_CHANCE
            ) {
                level.removeBlock(
                        pos,
                        false
                );

                destroyedBlocks.add(
                        pos.immutable()
                );

                return;
            }

            if (
                    level.random.nextFloat()
                            >= AFTERMATH_CHANCE
            ) {
                return;
            }

            level.setBlock(
                    pos,
                    aftermath,
                    3
            );
        }

        private BlockState getAftermathState(
                BlockState state,
                BlockPos pos
        ) {
            if (state.is(Blocks.GRASS_BLOCK)) {
                return Blocks.PODZOL.defaultBlockState();
            }

            if (state.is(Blocks.DIRT)) {
                if (level.random.nextBoolean()) {
                    return Blocks.PODZOL.defaultBlockState();
                }

                return Blocks.COARSE_DIRT.defaultBlockState();
            }

            if (state.is(Blocks.STONE)) {
                if (level.random.nextBoolean()) {
                    return Blocks.TUFF.defaultBlockState();
                }

                return Blocks.DEEPSLATE.defaultBlockState();
            }

            return null;
        }

        private void damageEntities(
                float previousRadius,
                float currentRadius
        ) {
            if (entityDamage <= 0.0f) {
                return;
            }

            AABB bounds =
                    new AABB(
                            center.x - currentRadius,
                            center.y - verticalLayers - 2,
                            center.z - currentRadius,
                            center.x + currentRadius,
                            center.y + verticalLayers + 2,
                            center.z + currentRadius
                    );

            for (
                    LivingEntity entity :
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            bounds
                    )
            ) {
                if (entity.isSpectator()) {
                    continue;
                }

                if (damagedEntities.contains(entity)) {
                    continue;
                }

                Vec3 rawEntityCenter =
                        entity.getBoundingBox().getCenter();

                Vector3dc rawEntityCenterJoml =
                        JOMLConversion.toJOML(rawEntityCenter);

                Vector3dc globalEntityCenterJoml =
                        SableCompanion.INSTANCE.projectOutOfSubLevel(
                                level,
                                rawEntityCenterJoml,
                                new Vector3d()
                        );

                double dx =
                        globalEntityCenterJoml.x() - center.x;

                double dz =
                        globalEntityCenterJoml.z() - center.z;

                double horizontalDistance =
                        Math.sqrt(
                                dx * dx
                                        + dz * dz
                        );

                if (
                        horizontalDistance < previousRadius
                                || horizontalDistance >= currentRadius
                ) {
                    continue;
                }

                if (horizontalDistance >= radius) {
                    continue;
                }

                double falloff =
                        1.0
                                - horizontalDistance / radius;

                float damage =
                        (float)
                                (entityDamage * falloff);

                if (damage <= 0.0f) {
                    continue;
                }

                entity.hurt(
                        level.damageSources().generic(),
                        damage
                );

                damagedEntities.add(entity);
            }
        }

        private record BlockTarget(
                BlockPos pos,
                BlockState state
        ) {
        }
    }
}