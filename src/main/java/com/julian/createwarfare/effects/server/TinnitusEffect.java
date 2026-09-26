package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.sounds.TinnitusHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class TinnitusEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float radius,
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

            int scaledDuration = duration;

            if (falloff) {
                double distance = Math.sqrt(distanceSqr);
                double falloffAmount = 1.0 - (distance / radius) * 0.75;
                scaledDuration = Math.max(1, (int) (duration * falloffAmount));
            }

            TinnitusHandler.start(scaledDuration);
        }
    }
}