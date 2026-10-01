package com.julian.createwarfare.explosions.post;

import com.julian.createwarfare.effects.particles.smoke.SmokeParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class MushroomCapPost {

    private static final int LIFETIME = 1800;

    private static final int PILLAR_PARTICLES = 20;
    private static final int CAP_PARTICLES = 140;

    private static final int PILLAR_DELAY = 2;
    private static final int CAP_DELAY = 2;

    private static final double VIEW_DISTANCE = 1500.0;
    private static final double VIEW_DISTANCE_SQUARED =
            VIEW_DISTANCE * VIEW_DISTANCE;

    private static ServerLevel level;

    private static double x;
    private static double y;
    private static double z;

    private static float height;
    private static float mushroomSize;

    private static int age;
    private static int nextPillarLayer;
    private static int nextCapParticle;

    private static boolean active;

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float height,
            float mushroomSize
    ) {
        MushroomCapPost.level = level;

        x = pos.getX() + 0.5;
        y = pos.getY();
        z = pos.getZ() + 0.5;

        MushroomCapPost.height = height;
        MushroomCapPost.mushroomSize = mushroomSize;

        age = 0;
        nextPillarLayer = 0;
        nextCapParticle = 0;

        active = true;
    }

    public static void tick() {
        if (!active || level == null) {
            return;
        }

        age++;

        spawnPillar();

        if (nextPillarLayer >= PILLAR_PARTICLES) {
            spawnCap();
        }

        if (age >= LIFETIME) {
            active = false;
        }
    }

    private static void spawnPillar() {
        if (nextPillarLayer >= PILLAR_PARTICLES) {
            return;
        }

        if (age % PILLAR_DELAY != 0) {
            return;
        }

        float progress =
                nextPillarLayer /
                        (float) (PILLAR_PARTICLES - 1);

        double py =
                y + progress * height;

        send(
                x,
                py,
                z,
                height / 6.0f
        );

        nextPillarLayer++;
    }

    private static void spawnCap() {
        if (nextCapParticle >= CAP_PARTICLES) {
            return;
        }

        if (age % CAP_DELAY != 0) {
            return;
        }

        float progress =
                nextCapParticle /
                        (float) (CAP_PARTICLES - 1);

        double angle =
                nextCapParticle * 2.399963229728653;

        double radiusProgress =
                Math.sqrt(progress);

        double radius =
                mushroomSize *
                        radiusProgress;

        double px =
                x +
                        Math.cos(angle) *
                                radius;

        double pz =
                z +
                        Math.sin(angle) *
                                radius;

        double edge =
                radiusProgress;

        double dome =
                Math.sqrt(
                        Math.max(
                                0.0,
                                1.0 - edge * edge
                        )
                );

        double py =
                y +
                        height +
                        dome *
                                mushroomSize *
                                0.30;

        send(
                px,
                py,
                pz,
                mushroomSize / 6.0f
        );

        nextCapParticle++;
    }

    private static void send(
            double px,
            double py,
            double pz,
            float particleSize
    ) {
        if (level == null) {
            return;
        }

        SmokeParticleOptions particle =
                new SmokeParticleOptions(
                        particleSize,
                        LIFETIME
                );

        ClientboundLevelParticlesPacket packet =
                new ClientboundLevelParticlesPacket(
                        particle,
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

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(px, py, pz)
                    <= VIEW_DISTANCE_SQUARED) {
                player.connection.send(packet);
            }
        }
    }
}