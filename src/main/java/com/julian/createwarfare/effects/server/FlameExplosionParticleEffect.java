package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.particles.explosions.FlameExplosionParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

public class FlameExplosionParticleEffect {

    private static final double SEND_DISTANCE = 512.0;
    private static final double SEND_DISTANCE_SQUARED =
            SEND_DISTANCE * SEND_DISTANCE;

    public static void spawn(
            ServerLevel level,
            BlockPos pos,
            float radius
    ) {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        RandomSource random = level.random;

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(
                    centerX,
                    centerY,
                    centerZ
            ) > SEND_DISTANCE_SQUARED) {
                continue;
            }

            int particleCount = 35;

            for (int i = 0; i < particleCount; i++) {
                float size =
                        2.0f
                                + random.nextFloat() * 6.0f;

                float sizeProgress =
                        (size - 2.0f) / 6.0f;

                float travel =
                        radius
                                * (1.0f - sizeProgress);

                travel *=
                        0.6f
                                + random.nextFloat() * 0.4f;

                double angle =
                        random.nextDouble()
                                * Math.PI * 2.0;

                double verticalAngle =
                        (random.nextDouble() - 0.5)
                                * Math.PI * 0.6;

                double horizontal =
                        Math.cos(verticalAngle);

                double directionX =
                        Math.cos(angle)
                                * horizontal;

                double directionY =
                        Math.sin(verticalAngle);

                double directionZ =
                        Math.sin(angle)
                                * horizontal;

                double speed =
                        travel / 14.0;

                double spawnOffset =
                        random.nextDouble() * 0.5;

                double x =
                        centerX
                                + directionX
                                * spawnOffset;

                double y =
                        centerY
                                + directionY
                                * spawnOffset;

                double z =
                        centerZ
                                + directionZ
                                * spawnOffset;

                double xd =
                        directionX
                                * speed;

                double yd =
                        directionY
                                * speed;

                double zd =
                        directionZ
                                * speed;

                int delay =
                        random.nextInt(6);

                FlameExplosionParticleOptions options =
                        new FlameExplosionParticleOptions(
                                size,
                                14,
                                delay
                        );

                level.sendParticles(
                        player,
                        options,
                        true,
                        x,
                        y,
                        z,
                        1,
                        xd,
                        yd,
                        zd,
                        0.0
                );
            }
        }
    }
}