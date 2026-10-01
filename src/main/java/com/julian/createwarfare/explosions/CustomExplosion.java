package com.julian.createwarfare.explosions;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class CustomExplosion {

    private static final float AFTERMATH_CHANCE = 0.35f;

    private static final ObjectOpenHashSet<BlockTarget> affectedBlocks =
            new ObjectOpenHashSet<>();

    private static final ObjectOpenHashSet<BlockTarget> aftermathBlocks =
            new ObjectOpenHashSet<>();

    private static final Object2FloatOpenHashMap<BlockTarget> resistanceCache =
            new Object2FloatOpenHashMap<>();

    public static void explode(
            Level level,
            double x,
            double y,
            double z,
            float power,
            float radius,
            float entityDamage
    ) {
        affectedBlocks.clear();
        aftermathBlocks.clear();
        resistanceCache.clear();

        Vec3 rawCenter =
                new Vec3(x, y, z);

        Vector3dc rawCenterJoml =
                JOMLConversion.toJOML(rawCenter);

        Vector3dc centerJoml =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawCenterJoml,
                        new Vector3d()
                );

        Vec3 center =
                new Vec3(
                        centerJoml.x(),
                        centerJoml.y(),
                        centerJoml.z()
                );

        castAllRays(
                level,
                center,
                power,
                radius
        );

        damageEntities(
                level,
                center,
                entityDamage,
                radius
        );

        destroyBlocks(level);
        createAftermath(level);
    }

    private static void castAllRays(
            Level level,
            Vec3 center,
            float power,
            float radius
    ) {
        int rayCount =
                Math.clamp(
                        (int) (radius * radius * 20.0f),
                        4000,
                        30000
                );

        double goldenAngle =
                Math.PI * (3.0 - Math.sqrt(5.0));

        for (int i = 0; i < rayCount; i++) {
            double y =
                    1.0 - (2.0 * i + 1.0) / rayCount;

            double ringRadius =
                    Math.sqrt(1.0 - y * y);

            double theta =
                    goldenAngle * i;

            double dx =
                    Math.cos(theta) * ringRadius;

            double dy =
                    y;

            double dz =
                    Math.sin(theta) * ringRadius;

            double variation =
                    1.0
                            + Math.sin(theta * 3.0 + y * 5.0) * 0.02
                            + Math.sin(theta * 7.0 - y * 4.0) * 0.015;

            castRay(
                    level,
                    center,
                    new Vec3(
                            dx,
                            dy,
                            dz
                    ),
                    power,
                    radius * variation
            );
        }
    }

    private static void castRay(
            Level level,
            Vec3 center,
            Vec3 direction,
            float power,
            double radius
    ) {
        BlockPos start =
                BlockPos.containing(center);

        int blockX =
                start.getX();

        int blockY =
                start.getY();

        int blockZ =
                start.getZ();

        int stepX =
                direction.x > 0.0
                        ? 1
                        : -1;

        int stepY =
                direction.y > 0.0
                        ? 1
                        : -1;

        int stepZ =
                direction.z > 0.0
                        ? 1
                        : -1;

        double tMaxX =
                direction.x > 0.0
                        ? (blockX + 1.0 - center.x) / direction.x
                        : direction.x < 0.0
                          ? (center.x - blockX) / -direction.x
                          : Double.MAX_VALUE;

        double tMaxY =
                direction.y > 0.0
                        ? (blockY + 1.0 - center.y) / direction.y
                        : direction.y < 0.0
                          ? (center.y - blockY) / -direction.y
                          : Double.MAX_VALUE;

        double tMaxZ =
                direction.z > 0.0
                        ? (blockZ + 1.0 - center.z) / direction.z
                        : direction.z < 0.0
                          ? (center.z - blockZ) / -direction.z
                          : Double.MAX_VALUE;

        double tDeltaX =
                direction.x == 0.0
                        ? Double.MAX_VALUE
                        : Math.abs(1.0 / direction.x);

        double tDeltaY =
                direction.y == 0.0
                        ? Double.MAX_VALUE
                        : Math.abs(1.0 / direction.y);

        double tDeltaZ =
                direction.z == 0.0
                        ? Double.MAX_VALUE
                        : Math.abs(1.0 / direction.z);

        double distance = 0.0;

        BlockTarget lastSolidBlock = null;

        while (
                distance < radius
                        && power > 0.0f
        ) {
            BlockPos worldPos =
                    new BlockPos(
                            blockX,
                            blockY,
                            blockZ
                    );

            BlockTarget target =
                    getBlockTarget(
                            level,
                            worldPos
                    );

            if (target != null) {
                lastSolidBlock =
                        target;

                float resistance =
                        getResistance(
                                target
                        );

                if (power > resistance) {
                    power -= resistance;

                    if (!aftermathBlocks.contains(target)) {
                        affectedBlocks.add(target);
                    }
                } else {
                    break;
                }
            }

            double nextDistance =
                    Math.min(
                            tMaxX,
                            Math.min(
                                    tMaxY,
                                    tMaxZ
                            )
                    );

            if (nextDistance >= radius) {
                break;
            }

            distance =
                    nextDistance;

            if (tMaxX <= nextDistance) {
                blockX += stepX;
                tMaxX += tDeltaX;
            }

            if (tMaxY <= nextDistance) {
                blockY += stepY;
                tMaxY += tDeltaY;
            }

            if (tMaxZ <= nextDistance) {
                blockZ += stepZ;
                tMaxZ += tDeltaZ;
            }
        }

        if (lastSolidBlock != null) {
            affectedBlocks.remove(
                    lastSolidBlock
            );

            if (shouldCreateAftermath(
                    lastSolidBlock
            )) {
                aftermathBlocks.add(
                        lastSolidBlock
                );
            }
        }
    }

    private static void damageEntities(
            Level level,
            Vec3 center,
            float entityDamage,
            float radius
    ) {
        AABB bounds =
                new AABB(
                        center.x - radius,
                        center.y - radius,
                        center.z - radius,
                        center.x + radius,
                        center.y + radius,
                        center.z + radius
                );

        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                bounds
        )) {
            if (entity.isSpectator()) {
                continue;
            }

            Vec3 entityCenter =
                    entity.getBoundingBox().getCenter();

            double distance =
                    center.distanceTo(entityCenter);

            if (distance >= radius) {
                continue;
            }

            double falloff =
                    1.0 - distance / radius;

            float damage =
                    (float) (entityDamage * falloff);

            if (damage <= 0.0f) {
                continue;
            }

            entity.hurt(
                    level.damageSources().explosion(null, null),
                    damage
            );
        }
    }

    private static BlockTarget getBlockTarget(
            Level level,
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
                            access,
                            pos,
                            state
                    );
                }
        );
    }

    private static float getResistance(
            BlockTarget target
    ) {
        if (resistanceCache.containsKey(target)) {
            return resistanceCache.getFloat(
                    target
            );
        }

        float resistance =
                target.state()
                        .getBlock()
                        .getExplosionResistance();

        resistanceCache.put(
                target,
                resistance
        );

        return resistance;
    }

    private static boolean shouldCreateAftermath(
            BlockTarget target
    ) {
        BlockState state =
                target.state();

        if (!isAftermathBlock(state)) {
            return false;
        }

        long seed =
                target.pos().asLong()
                        * 341873128712L
                        + 132897987541L;

        seed ^=
                seed >> 13;

        seed *=
                1274126177L;

        seed ^=
                seed >> 16;

        float value =
                (seed & 0x7fffffffL)
                        / (float) Integer.MAX_VALUE;

        return value < AFTERMATH_CHANCE;
    }

    private static boolean isAftermathBlock(
            BlockState state
    ) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT);
    }

    private static void destroyBlocks(
            Level level
    ) {
        for (BlockTarget target : affectedBlocks) {
            removeBlock(
                    level,
                    target
            );
        }
    }

    private static void createAftermath(
            Level level
    ) {
        for (BlockTarget target : aftermathBlocks) {
            BlockState current =
                    getBlockState(
                            level,
                            target
                    );

            if (current.isAir()) {
                continue;
            }

            BlockState aftermath =
                    getAftermathState(
                            target
                    );

            if (aftermath != null) {
                setBlock(
                        level,
                        target,
                        aftermath
                );
            }
        }
    }

    private static BlockState getAftermathState(
            BlockTarget target
    ) {
        BlockState state =
                target.state();

        BlockPos pos =
                target.pos();

        if (state.is(Blocks.STONE)) {
            double largeNoise =
                    Math.sin(
                            pos.getX() * 0.11
                                    + pos.getY() * 0.073
                                    + pos.getZ() * 0.097
                    );

            double smallNoise =
                    Math.sin(
                            pos.getX() * 0.31
                                    + pos.getY() * 0.23
                                    + pos.getZ() * 0.37
                    );

            double noise =
                    largeNoise * 0.75
                            + smallNoise * 0.25;

            if (noise > 0.12) {
                return Blocks.TUFF.defaultBlockState();
            }

            if (noise < -0.12) {
                return Blocks.DEEPSLATE.defaultBlockState();
            }

            return null;
        }

        if (state.is(Blocks.DIORITE)) {
            return Blocks.ANDESITE.defaultBlockState();
        }

        if (state.is(Blocks.ANDESITE)) {
            return Blocks.GRANITE.defaultBlockState();
        }

        if (state.is(Blocks.GRANITE)) {
            return Blocks.TUFF.defaultBlockState();
        }

        if (state.is(Blocks.GRASS_BLOCK)) {
            return Blocks.PODZOL.defaultBlockState();
        }

        if (state.is(Blocks.DIRT)) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }

        return null;
    }

    private static BlockState getBlockState(
            Level level,
            BlockTarget target
    ) {
        return level.getBlockState(
                target.pos()
        );
    }

    private static void removeBlock(
            Level level,
            BlockTarget target
    ) {
        level.removeBlock(
                target.pos(),
                false
        );
    }

    private static void setBlock(
            Level level,
            BlockTarget target,
            BlockState state
    ) {
        level.setBlock(
                target.pos(),
                state,
                3
        );
    }

    private record BlockTarget(
            Object access,
            BlockPos pos,
            BlockState state
    ) {
    }
}