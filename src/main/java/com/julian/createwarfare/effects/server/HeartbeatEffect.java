package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.network.ClientEffectPayload;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class HeartbeatEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float radius,
            int duration,
            float intensity,
            boolean falloff
    ) {
        Position rawHeartbeatPosition =
                pos.getCenter();

        Vec3 heartbeatPosition =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawHeartbeatPosition
                );

        double radiusSqr =
                radius * radius;

        intensity =
                Math.max(
                        0.0f,
                        Math.min(
                                1.0f,
                                intensity
                        )
                );

        int minInterval =
                5;

        int maxInterval =
                40;

        int intervalTicks =
                Math.round(
                        maxInterval -
                                (maxInterval - minInterval) *
                                        intensity
                );

        for (ServerPlayer player :
                level.players()) {

            double distanceSqr =
                    player.distanceToSqr(
                            heartbeatPosition.x,
                            heartbeatPosition.y,
                            heartbeatPosition.z
                    );

            if (distanceSqr > radiusSqr) {
                continue;
            }

            int scaledDuration =
                    duration;

            if (falloff) {
                double distance =
                        Math.sqrt(
                                distanceSqr
                        );

                double falloffAmount =
                        1.0 -
                                (distance / radius) *
                                        0.75;

                scaledDuration =
                        Math.max(
                                1,
                                (int) (
                                        duration *
                                                falloffAmount
                                )
                        );
            }

            int cycleCount =
                    Math.max(
                            1,
                            scaledDuration /
                                    intervalTicks
                    );

            PacketDistributor.sendToPlayer(
                    player,
                    new ClientEffectPayload(
                            ClientEffectPayload.HEARTBEAT,
                            0.0,
                            0.0,
                            0.0,
                            0.0f,
                            0.0f,
                            intervalTicks,
                            cycleCount
                    )
            );
        }
    }
}