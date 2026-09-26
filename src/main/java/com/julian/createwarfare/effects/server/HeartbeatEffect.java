package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.sounds.HeartbeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class HeartbeatEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float radius,
            int duration,
            float intensity,
            boolean falloff
    ) {
        double radiusSqr = radius * radius;

        intensity = Math.max(0.0F, Math.min(1.0F, intensity));

        int minInterval = 5;
        int maxInterval = 40;

        int intervalTicks = Math.round(
                maxInterval - (maxInterval - minInterval) * intensity
        );

        for (ServerPlayer player : level.players()) {
            double distanceSqr = player.distanceToSqr(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
            );

            if (distanceSqr > radiusSqr) {
                continue;
            }

            int scaledDuration = duration;

            if (falloff) {
                double distance = Math.sqrt(distanceSqr);
                double falloffAmount = 1.0 - (distance / radius) * 0.75;
                scaledDuration = Math.max(1, (int) (duration * falloffAmount));
            }

            int cycleCount = Math.max(1, scaledDuration / intervalTicks);

            HeartbeatHandler.start(
                    intervalTicks,
                    cycleCount
            );
        }
    }
}