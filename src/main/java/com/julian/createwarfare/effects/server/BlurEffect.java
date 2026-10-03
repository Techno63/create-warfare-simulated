package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.network.ClientEffectPayload;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class BlurEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float radius,
            float strength,
            int duration,
            boolean falloff
    ) {
        double radiusSqr =
                radius * radius;

        Position rawCenter =
                Vec3.atCenterOf(pos);

        Vec3 center =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawCenter
                );

        for (ServerPlayer player :
                level.players()) {

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

            PacketDistributor.sendToPlayer(
                    player,
                    new ClientEffectPayload(
                            ClientEffectPayload.BLUR,
                            0.0,
                            0.0,
                            0.0,
                            scaledStrength,
                            0.0f,
                            0,
                            scaledDuration
                    )
            );
        }
    }
}