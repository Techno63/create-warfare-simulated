package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.client.ScreenShakeHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ShakeEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float radius,
            float strength,
            int duration,
            boolean falloff
    ) {
        double radiusSqr = radius * radius;

        for (ServerPlayer player : level.players()) {
            double distanceSqr = player.distanceToSqr(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
            );

            if (distanceSqr > radiusSqr) {
                continue;
            }

            float scaledStrength = strength;
            int scaledDuration = duration;

            if (falloff) {
                double distance = Math.sqrt(distanceSqr);
                double falloffAmount = 1.0 - (distance / radius) * 0.75;

                scaledStrength = strength * (float) falloffAmount;
                scaledDuration = Math.max(1, (int) (duration * falloffAmount));
            }

            ScreenShakeHandler.shake(
                    scaledStrength,
                    scaledDuration
            );
        }
    }
}
