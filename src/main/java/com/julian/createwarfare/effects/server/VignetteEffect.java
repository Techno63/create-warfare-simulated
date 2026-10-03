package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.network.ClientEffectPayload;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class VignetteEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            double radius,
            int duration,
            float strength
    ) {
        double radiusSquared =
                radius * radius;

        Position rawCenter =
                pos.getCenter();

        Vec3 center =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawCenter
                );

        for (ServerPlayer player :
                level.players()) {

            double distanceSquared =
                    player.distanceToSqr(
                            center.x,
                            center.y,
                            center.z
                    );

            if (distanceSquared > radiusSquared) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    new ClientEffectPayload(
                            ClientEffectPayload.VIGNETTE,
                            0.0,
                            0.0,
                            0.0,
                            strength,
                            0.0f,
                            0,
                            duration
                    )
            );
        }
    }
}