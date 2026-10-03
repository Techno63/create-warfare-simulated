package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.network.WavePayload;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WaveEffect {

    public static void start(
            ServerLevel level,
            Vec3 pos,
            float speed,
            float maxRadius,
            int color,
            float alpha,
            int stayTicks,
            boolean glow
    ) {
        speed =
                Math.max(
                        speed,
                        0.01f
                );

        maxRadius =
                Math.max(
                        maxRadius,
                        0.01f
                );

        alpha =
                Math.clamp(
                        alpha,
                        0.0f,
                        1.0f
                );

        stayTicks =
                Math.max(
                        stayTicks,
                        0
                );

        Position rawPosition =
                pos;

        Vec3 worldPosition =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawPosition
                );

        WavePayload payload =
                new WavePayload(
                        worldPosition.x,
                        worldPosition.y,
                        worldPosition.z,
                        speed,
                        maxRadius,
                        color,
                        alpha,
                        stayTicks,
                        glow
                );

        for (
                ServerPlayer player :
                level.players()
        ) {
            PacketDistributor.sendToPlayer(
                    player,
                    payload
            );
        }
    }

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float speed,
            float maxRadius,
            int color,
            float alpha,
            int stayTicks,
            boolean glow
    ) {
        start(
                level,
                Vec3.atCenterOf(pos),
                speed,
                maxRadius,
                color,
                alpha,
                stayTicks,
                glow
        );
    }
}