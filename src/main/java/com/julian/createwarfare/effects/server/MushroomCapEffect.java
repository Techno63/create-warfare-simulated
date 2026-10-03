package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.network.MushroomCapPayload;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MushroomCapEffect {

    private MushroomCapEffect() {
    }

    public static void start(
            ServerLevel level,
            Vec3 pos,
            float baseSmokeRadius,
            float baseSmokeHeight,
            float pillarHeight,
            float pillarRadius,
            float capHeight,
            float capRadius,
            int lifetime
    ) {
        baseSmokeRadius =
                Math.max(0.1f, baseSmokeRadius);

        baseSmokeHeight =
                Math.max(0.1f, baseSmokeHeight);

        pillarHeight =
                Math.max(0.0f, pillarHeight);

        pillarRadius =
                Math.max(0.0f, pillarRadius);

        capHeight =
                Math.max(0.0f, capHeight);

        capRadius =
                Math.max(0.0f, capRadius);

        lifetime =
                Math.max(1, lifetime);

        Position rawPosition =
                pos;

        Vec3 worldPosition =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawPosition
                );

        MushroomCapPayload payload =
                new MushroomCapPayload(
                        worldPosition.x,
                        worldPosition.y,
                        worldPosition.z,
                        baseSmokeRadius,
                        baseSmokeHeight,
                        pillarHeight,
                        pillarRadius,
                        capHeight,
                        capRadius,
                        lifetime
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
            float baseSmokeRadius,
            float baseSmokeHeight,
            float pillarHeight,
            float pillarRadius,
            float capHeight,
            float capRadius,
            int lifetime
    ) {
        start(
                level,
                Vec3.atCenterOf(pos),
                baseSmokeRadius,
                baseSmokeHeight,
                pillarHeight,
                pillarRadius,
                capHeight,
                capRadius,
                lifetime
        );
    }
}