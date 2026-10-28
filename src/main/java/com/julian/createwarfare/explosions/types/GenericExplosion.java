package com.julian.createwarfare.explosions.types;

import com.julian.createwarfare.explosions.CustomExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
                (ServerLevel) level,
                pos,
                power,
                radius,
                damage,
                true,
                true
        );
    }
}