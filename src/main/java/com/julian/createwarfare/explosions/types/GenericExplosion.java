package com.julian.createwarfare.explosions.types;

import com.julian.createwarfare.explosions.CustomExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class GenericExplosion {

    public static void trigger(
            Level level,
            BlockPos pos,
            float power,
            float radius,
            float damage
    ) {
        CustomExplosion.explode(
                level,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                power,
                radius,
                damage
        );
    }
}