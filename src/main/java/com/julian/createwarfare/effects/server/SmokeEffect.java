package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.particles.smoke.SmokeParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class SmokeEffect {

    private static final List<SmokeCloud> CLOUDS = new ArrayList<>();

    private static final double SEND_DISTANCE = 512.0;
    private static final double SEND_DISTANCE_SQUARED =
            SEND_DISTANCE * SEND_DISTANCE;

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float radius,
            int duration,
            float density,
            float riseSpeed,
            int lifetime
    ) {
        CLOUDS.add(new SmokeCloud(
                level,
                Vec3.atCenterOf(pos),
                radius,
                duration,
                density,
                riseSpeed,
                lifetime
        ));
    }

    public static void tick() {
        for (int i = CLOUDS.size() - 1; i >= 0; i--) {
            if (!CLOUDS.get(i).tick()) {
                CLOUDS.remove(i);
            }
        }
    }

    private static class SmokeCloud {

        private final ServerLevel level;
        private final Vec3 center;
        private final float radius;
        private final int duration;
        private final float density;
        private final float riseSpeed;
        private final int lifetime;

        private int age;
        private float spawnAccumulator;

        private SmokeCloud(
                ServerLevel level,
                Vec3 center,
                float radius,
                int duration,
                float density,
                float riseSpeed,
                int lifetime
        ) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.duration = duration;
            this.density = Math.max(0.0f, density);
            this.riseSpeed = riseSpeed;
            this.lifetime = lifetime;
        }

        private boolean tick() {
            if (age >= duration) {
                return false;
            }

            float progress = Math.min(
                    1.0f,
                    (float) age / duration
            );

            float expansion = Math.min(
                    1.0f,
                    progress / 0.25f
            );

            expansion =
                    expansion
                            * expansion
                            * (3.0f - 2.0f * expansion);

            float currentRadius = radius * expansion;

            spawnAccumulator += density / 20.0f;

            int particleCount = (int) spawnAccumulator;

            spawnAccumulator -= particleCount;

            for (int i = 0; i < particleCount; i++) {
                sendParticle(
                        center.add(
                                randomOffset(currentRadius)
                        )
                );
            }

            age++;

            return true;
        }

        private Vec3 randomOffset(float currentRadius) {
            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double distance =
                    Math.sqrt(level.random.nextDouble())
                            * currentRadius;

            double x =
                    Math.cos(angle)
                            * distance;

            double z =
                    Math.sin(angle)
                            * distance;

            return new Vec3(
                    x,
                    0.0,
                    z
            );
        }

        private void sendParticle(Vec3 position) {
            SmokeParticleOptions options =
                    new SmokeParticleOptions(
                            riseSpeed,
                            lifetime
                    );

            ClientboundLevelParticlesPacket packet =
                    new ClientboundLevelParticlesPacket(
                            options,
                            true,
                            position.x,
                            position.y,
                            position.z,
                            0.0f,
                            0.0f,
                            0.0f,
                            0.0f,
                            1
                    );

            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(
                        position.x,
                        position.y,
                        position.z
                ) <= SEND_DISTANCE_SQUARED) {
                    player.connection.send(packet);
                }
            }
        }
    }
}