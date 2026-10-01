package com.julian.createwarfare.effects.server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SoundWaveEffect {

    private static final float NANOS_PER_TICK = 50_000_000.0f;

    private static final List<SoundWave> WAVES = new ArrayList<>();

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float speed,
            float radius,
            SoundEvent sound,
            boolean falloff
    ) {
        WAVES.add(new SoundWave(
                level,
                pos.getCenter(),
                Math.max(speed, 0.01f),
                Math.max(radius, 0.01f),
                sound,
                falloff
        ));
    }

    public static void tick() {
        for (int i = WAVES.size() - 1; i >= 0; i--) {
            if (!WAVES.get(i).tick()) {
                WAVES.remove(i);
            }
        }
    }

    private static class SoundWave {

        private final ServerLevel level;
        private final Vec3 center;
        private final float speed;
        private final float radius;
        private final SoundEvent sound;
        private final boolean falloff;

        private final Set<Integer> affectedPlayers =
                new HashSet<>();

        private final long startNanos =
                System.nanoTime();

        private SoundWave(
                ServerLevel level,
                Vec3 center,
                float speed,
                float radius,
                SoundEvent sound,
                boolean falloff
        ) {
            this.level = level;
            this.center = center;
            this.speed = speed;
            this.radius = radius;
            this.sound = sound;
            this.falloff = falloff;
        }

        private boolean tick() {
            float elapsedTicks =
                    (System.nanoTime() - startNanos) /
                            NANOS_PER_TICK;

            float travelled =
                    Math.min(
                            elapsedTicks * speed,
                            radius
                    );

            double travelledSqr =
                    (double) travelled *
                            travelled;

            AABB area = new AABB(
                    center.x - travelled,
                    center.y - travelled,
                    center.z - travelled,
                    center.x + travelled,
                    center.y + travelled,
                    center.z + travelled
            );

            for (ServerPlayer player :
                    level.getEntitiesOfClass(
                            ServerPlayer.class,
                            area
                    )) {

                if (player.isSpectator()) {
                    continue;
                }

                if (affectedPlayers.contains(
                        player.getId()
                )) {
                    continue;
                }

                double distanceSqr =
                        player.distanceToSqr(center);

                if (distanceSqr > travelledSqr) {
                    continue;
                }

                affectedPlayers.add(
                        player.getId()
                );

                applyPlayerEffect(
                        player,
                        Math.sqrt(distanceSqr)
                );
            }

            return travelled < radius;
        }

        private void applyPlayerEffect(
                ServerPlayer player,
                double distance
        ) {
            float volume =
                    getVolume(distance);

            player.playNotifySound(
                    sound,
                    SoundSource.MASTER,
                    volume,
                    1.0f
            );
        }

        private float getVolume(
                double distance
        ) {
            if (!falloff) {
                return 1.0f;
            }

            return Mth.clamp(
                    1.0f -
                            (float) (distance / radius) *
                                    0.5f,
                    0.3f,
                    1.0f
            );
        }
    }
}