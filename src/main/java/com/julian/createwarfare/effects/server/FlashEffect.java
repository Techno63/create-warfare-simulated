package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.network.ClientEffectPayload;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

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

        Position rawFlashPosition =
                Vec3.atCenterOf(pos);

        Vec3 flashPosition =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawFlashPosition
                );

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
                                (int) (
                                        duration *
                                                falloffAmount
                                )
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

                    float normalizedDirection =
                            (float)
                                    ((dot + 1.0) *
                                            0.5);

                    float intensityFactor =
                            0.5f +
                                    0.5f *
                                            normalizedDirection;

                    float durationFactor =
                            0.2f +
                                    0.8f *
                                            normalizedDirection;

                    scaledStrength *=
                            intensityFactor;

                    scaledDuration =
                            Math.max(
                                    1,
                                    Math.round(
                                            scaledDuration *
                                                    durationFactor
                                    )
                            );
                }
            }

            PacketDistributor.sendToPlayer(
                    player,
                    new ClientEffectPayload(
                            ClientEffectPayload.FLASH,
                            0.0,
                            0.0,
                            0.0,
                            scaledStrength,
                            0.0f,
                            color,
                            scaledDuration
                    )
            );
        }
    }
}