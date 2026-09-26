package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.client.ScreenVignetteHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class VignetteEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            double radius,
            int duration,
            float strength
    ) {
        double radiusSquared = radius * radius;

        for (ServerPlayer player : level.players()) {
            double distanceSquared = player.distanceToSqr(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
            );

            if (distanceSquared > radiusSquared) {
                continue;
            }

            ScreenVignetteHandler.vignette(strength, duration);
        }
    }
}