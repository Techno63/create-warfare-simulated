package com.julian.createwarfare.effects.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class FocusedExplosionEffect {

    public static void explode(
            Level level,
            BlockPos pos,
            float power,
            float radius,
            float range,
            Direction direction
    ) {
        int r = (int) radius;

        for (int distance = 1; distance <= range; distance++) {
            BlockPos center = pos.relative(direction, distance);

            for (int a = -r; a <= r; a++) {
                for (int b = -r; b <= r; b++) {

                    BlockPos blockPos;

                    if (direction == Direction.NORTH || direction == Direction.SOUTH) {
                        blockPos = center.offset(a, b, 0);
                    } else if (direction == Direction.EAST || direction == Direction.WEST) {
                        blockPos = center.offset(0, b, a);
                    } else {
                        blockPos = center.offset(a, 0, b);
                    }

                    float resistance = level.getBlockState(blockPos)
                            .getExplosionResistance(level, blockPos, null) / 10.0F;

                    float falloff = (Math.abs(a) + Math.abs(b)) / 2.0F + (distance - 1);

                    if (resistance <= power - falloff) {
                        level.destroyBlock(blockPos, false);
                    }
                }
            }
        }
    }
}