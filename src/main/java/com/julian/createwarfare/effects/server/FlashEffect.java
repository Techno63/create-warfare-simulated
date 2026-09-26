package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.client.ScreenFlashHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class FlashEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float radius,
            float strength,
            int duration,
            int color,
            boolean falloff,
            boolean reduceByDirection
    ) {
        double radiusSqr =
                radius * radius;

        Vec3 flashPosition =
                Vec3.atCenterOf(pos);

        for (ServerPlayer player :
                level.players()) {

            double distanceSqr =
                    player.distanceToSqr(
                            flashPosition.x,
                            flashPosition.y,
                            flashPosition.z
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
                        1.0 -
                                (distance / radius) *
                                        0.75;

                scaledStrength =
                        strength *
                                (float) falloffAmount;

                scaledDuration =
                        Math.max(
                                1,
                                (int)
                                        (duration *
                                                falloffAmount)
                        );
            }

            if (reduceByDirection) {
                Vec3 directionToFlash =
                        flashPosition.subtract(
                                player.getEyePosition()
                        );

                if (directionToFlash.lengthSqr() >
                        0.0001) {

                    directionToFlash =
                            directionToFlash.normalize();

                    Vec3 lookDirection =
                            player.getLookAngle()
                                    .normalize();

                    double dot =
                            lookDirection.dot(
                                    directionToFlash
                            );

                    float directionFactor =
                            0.15f +
                                    0.85f *
                                            (float)
                                                    ((dot + 1.0) *
                                                            0.5);

                    scaledStrength *=
                            directionFactor;

                    scaledDuration =
                            Math.max(
                                    1,
                                    Math.round(
                                            scaledDuration *
                                                    directionFactor
                                    )
                            );
                }
            }

            ScreenFlashHandler.flash(
                    scaledStrength,
                    scaledDuration,
                    color
            );
        }
    }
}