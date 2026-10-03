package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.particles.shockwaves.ShockwaveParticleOptions;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class ShockwaveParticleEffect {

    private ShockwaveParticleEffect() {
    }

    public static void start(
            ServerLevel level,
            Vec3 pos,
            float speed,
            float radius
    ) {
        speed =
                Math.max(
                        speed,
                        0.01f
                );

        radius =
                Math.max(
                        radius,
                        0.01f
                );

        Position rawPosition =
                pos;

        Vec3 worldPosition =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawPosition
                );

        int duration =
                Math.max(
                        1,
                        (int) Math.ceil(
                                radius / speed
                        )
                );

        ShockwaveParticleOptions options =
                new ShockwaveParticleOptions(
                        radius,
                        speed,
                        duration
                );

        for (ServerPlayer player :
                level.players()) {

            level.sendParticles(
                    player,
                    options,
                    true,
                    worldPosition.x,
                    worldPosition.y,
                    worldPosition.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
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