package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.particles.explosions.ExplosionParticleOptions;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class ExplosionParticleEffect {

    private static final double VIEW_DISTANCE = 1500.0;
    private static final double VIEW_DISTANCE_SQUARED = VIEW_DISTANCE * VIEW_DISTANCE;

    private ExplosionParticleEffect() {}

    public static void spawn(
            ServerLevel level,
            BlockPos pos,
            float size,
            int lifetime
    ) {
        if (level == null || size <= 0.0f || lifetime <= 0) {
            return;
        }

        Position position =
                new Vec3(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5
                );

        Vec3 worldPosition =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        position
                );

        double x = worldPosition.x;
        double y = worldPosition.y;
        double z = worldPosition.z;

        ExplosionParticleOptions options =
                new ExplosionParticleOptions(
                        size,
                        lifetime
                );

        ClientboundLevelParticlesPacket packet =
                new ClientboundLevelParticlesPacket(
                        options,
                        true,
                        x,
                        y,
                        z,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        1
                );

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(
                    x,
                    y,
                    z
            ) <= VIEW_DISTANCE_SQUARED) {
                player.connection.send(packet);
            }
        }
    }
}