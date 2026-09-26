package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.network.FireballPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FireballEffect {

    private FireballEffect() {
    }

    public static void start(
            ServerLevel level,
            Vec3 pos,
            float speed,
            float radius
    ) {
        speed = Math.max(speed, 0.01f);
        radius = Math.max(radius, 0.01f);

        FireballPayload payload = new FireballPayload(
                pos.x,
                pos.y,
                pos.z,
                speed,
                radius
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
            float radius
    ) {
        start(
                level,
                Vec3.atCenterOf(pos),
                speed,
                radius
        );
    }
}