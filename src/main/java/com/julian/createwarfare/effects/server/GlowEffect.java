package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.client.ScreenGlowHandler;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class GlowEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float radius,
            int color,
            float intensity,
            int duration,
            boolean falloff
    ) {
        double radiusSqr =
                radius * radius;

        Position rawGlowPosition =
                Vec3.atCenterOf(pos);

        Vec3 glowPosition =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawGlowPosition
                );

        for (ServerPlayer player :
                level.players()) {

            double distanceSqr =
                    player.distanceToSqr(
                            glowPosition.x,
                            glowPosition.y,
                            glowPosition.z
                    );

            if (distanceSqr > radiusSqr) {
                continue;
            }

            float scaledIntensity =
                    intensity;

            int scaledDuration =
                    duration;

            if (falloff) {
                double distance =
                        Math.sqrt(distanceSqr);

                double falloffAmount =
                        1.0 -
                                (distance / radius) *
                                        0.75;

                scaledIntensity =
                        intensity *
                                (float) falloffAmount;

                scaledDuration =
                        Math.max(
                                1,
                                (int) (
                                        duration *
                                                falloffAmount
                                )
                        );
            }

            ScreenGlowHandler.glow(
                    glowPosition,
                    radius,
                    color,
                    scaledIntensity,
                    scaledDuration
            );
        }
    }
}