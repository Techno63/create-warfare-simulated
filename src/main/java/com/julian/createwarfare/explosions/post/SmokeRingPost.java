package com.julian.createwarfare.explosions.post;

import com.julian.createwarfare.effects.particles.smoke.SmokeParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SmokeRingPost {

    private static final float PARTICLE_SPACING = 3.5f;
    private static final int MIN_PARTICLES = 16;
    private static final int MAX_PARTICLES_PER_RING = 700;

    private static final float BASE_SIZE = 2.2f;
    private static final float SIZE_FALLOFF = 0.7f;

    private static final int BASE_LIFETIME = 10;
    private static final double RADIAL_JITTER = 1.0;

    private static final double VIEW_DISTANCE = 3000.0;
    private static final double VIEW_DISTANCE_SQUARED =
            VIEW_DISTANCE * VIEW_DISTANCE;

    private static final List<Ring> RINGS = new ArrayList<>();

    private SmokeRingPost() {}

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float speed,
            float maxRadius,
            int holdTicks
    ) {
        if (level == null || speed <= 0.0f || maxRadius <= 0.0f) {
            return;
        }

        int duration =
                Math.max(
                        2,
                        Mth.ceil(maxRadius / speed)
                );

        RINGS.add(
                new Ring(
                        level,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        maxRadius,
                        duration,
                        Math.max(0, holdTicks)
                )
        );
    }

    public static void tick() {
        if (RINGS.isEmpty()) {
            return;
        }

        Iterator<Ring> it = RINGS.iterator();

        while (it.hasNext()) {
            if (it.next().tick()) {
                it.remove();
            }
        }
    }

    public static void clear() {
        RINGS.clear();
    }

    private static final class Ring {

        private final ServerLevel level;
        private final double x;
        private final double y;
        private final double z;
        private final float maxRadius;
        private final int duration;
        private final int holdTicks;
        private final RandomSource random;

        private int age;

        Ring(
                ServerLevel level,
                double x,
                double y,
                double z,
                float maxRadius,
                int duration,
                int holdTicks
        ) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
            this.maxRadius = maxRadius;
            this.duration = duration;
            this.holdTicks = holdTicks;
            this.random = RandomSource.create();
        }

        boolean tick() {
            age++;

            if (age <= duration) {
                float t =
                        Math.min(
                                1.0f,
                                (float) age / duration
                        );

                float eased =
                        1.0f -
                                (1.0f - t) *
                                        (1.0f - t);

                float radius =
                        maxRadius * eased;

                spawnRing(radius, t, false);

                return false;
            }

            if (age == duration + 1) {
                spawnRing(maxRadius, 1.0f, true);
            }

            return age > duration + holdTicks;
        }

        private void spawnRing(
                float radius,
                float t,
                boolean finalRing
        ) {
            List<ServerPlayer> nearby =
                    getNearbyPlayers(radius);

            if (nearby.isEmpty()) {
                return;
            }

            float circumference =
                    (float) (
                            Math.PI *
                                    2.0 *
                                    radius
                    );

            int count =
                    Mth.clamp(
                            Mth.ceil(
                                    circumference /
                                            PARTICLE_SPACING
                            ),
                            MIN_PARTICLES,
                            MAX_PARTICLES_PER_RING
                    );

            float size =
                    BASE_SIZE *
                            (
                                    1.0f +
                                            SIZE_FALLOFF *
                                                    (1.0f - t)
                            );

            int lifetime =
                    finalRing
                            ? BASE_LIFETIME + holdTicks
                            : BASE_LIFETIME;

            SmokeParticleOptions options =
                    new SmokeParticleOptions(
                            size,
                            lifetime
                    );

            double rotation =
                    random.nextDouble() *
                            Math.PI *
                            2.0;

            for (int i = 0; i < count; i++) {
                double angle =
                        rotation +
                                Math.PI *
                                        2.0 *
                                        i /
                                        count;

                double r =
                        radius +
                                (
                                        random.nextDouble() -
                                                0.5
                                ) *
                                        2.0 *
                                        RADIAL_JITTER;

                double px =
                        x +
                                Math.cos(angle) *
                                        r;

                double pz =
                        z +
                                Math.sin(angle) *
                                        r;

                send(
                        options,
                        px,
                        y,
                        pz,
                        nearby
                );
            }
        }

        private void send(
                SmokeParticleOptions options,
                double px,
                double py,
                double pz,
                List<ServerPlayer> nearby
        ) {
            ClientboundLevelParticlesPacket packet =
                    new ClientboundLevelParticlesPacket(
                            options,
                            true,
                            px,
                            py,
                            pz,
                            0.0f,
                            0.0f,
                            0.0f,
                            0.0f,
                            1
                    );

            for (ServerPlayer player : nearby) {
                if (player.distanceToSqr(
                        px,
                        py,
                        pz
                ) <= VIEW_DISTANCE_SQUARED) {
                    player.connection.send(packet);
                }
            }
        }

        private List<ServerPlayer> getNearbyPlayers(
                float radius
        ) {
            List<ServerPlayer> result =
                    new ArrayList<>();

            for (ServerPlayer player : level.players()) {
                double dx =
                        player.getX() - x;

                double dz =
                        player.getZ() - z;

                double horizontal =
                        Math.sqrt(
                                dx * dx +
                                        dz * dz
                        );

                double toRing =
                        horizontal - radius;

                if (toRing * toRing <= VIEW_DISTANCE_SQUARED) {
                    result.add(player);
                }
            }

            return result;
        }
    }
}