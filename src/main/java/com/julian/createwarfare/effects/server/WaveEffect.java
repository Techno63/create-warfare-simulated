package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.network.WavePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WaveEffect {

    private WaveEffect() {
    }

    public static void start(
            ServerLevel level,
            Vec3 pos,
            float speed,
            float radius,
            int color,
            float transparency
    ) {
        speed = Math.max(speed, 0.01f);
        radius = Math.max(radius, 0.01f);
        transparency = Mth.clamp(
                transparency,
                0.0f,
                1.0f
        );

        WavePayload payload =
                new WavePayload(
                        pos.x,
                        pos.y,
                        pos.z,
                        speed,
                        radius,
                        color,
                        transparency
                );

        for (ServerPlayer player : level.players()) {
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
            float radius,
            int color,
            float transparency
    ) {
        start(
                level,
                Vec3.atCenterOf(pos),
                speed,
                radius,
                color,
                transparency
        );
    }
}