package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.client.ScreenBlurHandler;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class BlurEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float radius,
            float strength,
            int duration,
            boolean falloff
    ) {
        Position rawCenter =
                new Vec3(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5
                );

        Vec3 center =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawCenter
                );

        double radiusSqr =
                radius * radius;

        for (ServerPlayer player : level.players()) {
            double distanceSqr =
                    player.distanceToSqr(
                            center.x,
                            center.y,
                            center.z
                    );

            if (distanceSqr > radiusSqr) {
                continue;
            }

            float scaledStrength =
                    strength;

            int scaledDuration =
                    duration;

            if (falloff) {
                double distance =
                        Math.sqrt(distanceSqr);

                double falloffAmount =
                        1.0
                                - (distance / radius) * 0.75;

                scaledStrength =
                        strength
                                * (float) falloffAmount;

                scaledDuration =
                        Math.max(
                                1,
                                (int) (
                                        duration
                                                * falloffAmount
                                )
                        );
            }

            ScreenBlurHandler.blur(
                    scaledStrength,
                    scaledDuration
            );
        }
    }
}