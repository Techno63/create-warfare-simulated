package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.network.MushroomCapPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;

public final class MushroomCapEffect {

    private static final double DISPLAY_DISTANCE = 1024.0;
    private static final double DISPLAY_DISTANCE_SQUARED =
            DISPLAY_DISTANCE * DISPLAY_DISTANCE;

    private MushroomCapEffect() {
    }

    public static void start(
            ServerLevel level,
            Vec3 pos,
            float height,
            float pillarRadius,
            float capRadius
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");

        MushroomCapPayload payload =
                new MushroomCapPayload(
                        pos.x,
                        pos.y,
                        pos.z,
                        height,
                        pillarRadius,
                        capRadius
                );

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(
                    pos.x,
                    pos.y,
                    pos.z
            ) <= DISPLAY_DISTANCE_SQUARED) {
                PacketDistributor.sendToPlayer(
                        player,
                        payload
                );
            }
        }
    }

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float height,
            float pillarRadius,
            float capRadius
    ) {
        start(
                level,
                Vec3.atCenterOf(pos),
                height,
                pillarRadius,
                capRadius
        );
    }
}