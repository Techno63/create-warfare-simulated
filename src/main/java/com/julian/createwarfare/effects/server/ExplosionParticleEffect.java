package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.particles.explosions.SmokeExplosionParticleOptions;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ExplosionParticleEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float scale
    ) {
        Vec3 particlePos =
                new Vec3(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5
                );

        SubLevel subLevel =
                Sable.HELPER.getContaining(
                        level,
                        pos
                );

        if (subLevel != null) {
            particlePos =
                    subLevel
                            .logicalPose()
                            .transformPosition(
                                    particlePos
                            );
        }

        for (ServerPlayer player : level.players()) {
            level.sendParticles(
                    player,
                    new SmokeExplosionParticleOptions(
                            5f * scale
                    ),
                    true,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    20,
                    1,
                    1,
                    1,
                    0.15
            );

            level.sendParticles(
                    player,
                    new SmokeExplosionParticleOptions(
                            scale
                    ),
                    true,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    60,
                    1,
                    1,
                    1,
                    0.3
            );
        }
    }
}