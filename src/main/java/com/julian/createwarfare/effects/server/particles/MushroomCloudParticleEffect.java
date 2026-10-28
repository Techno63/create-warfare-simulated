
package com.julian.createwarfare.effects.server.particles;

import com.julian.createwarfare.effects.particles.smoke.SmokeParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

@EventBusSubscriber(modid = "createwarfare")
public final class MushroomCloudParticleEffect {

    private static final Map<ServerLevel, List<Cloud>> CLOUDS = new HashMap<>();

    private static final int BASE_BUILD_TICKS = 27;
    private static final int PILLAR_BUILD_TICKS = 27;
    private static final int CAP_BUILD_TICKS = 27;
    private static final int CAP_START_TICK = Math.max(BASE_BUILD_TICKS, PILLAR_BUILD_TICKS);
    private static final int BUILD_TICKS = CAP_START_TICK + CAP_BUILD_TICKS;
    private static final int HOLD_TICKS = 366;
    private static final int FADE_TICKS = 160;
    private static final int FADE_START = BUILD_TICKS + HOLD_TICKS;
    private static final int FADE_PARTICLE_LIFETIME = 35;
    private static final int BOTTOM_TO_TOP_FADE_TICKS = 160;
    private static final int TOTAL_TICKS = FADE_START + BOTTOM_TO_TOP_FADE_TICKS
            + FADE_TICKS + FADE_PARTICLE_LIFETIME + 10;

    private static final int HOLD_EMISSION_INTERVAL = 5;
    private static final int MIN_PARTICLE_LIFETIME = 100;
    private static final int PARTICLE_LIFETIME_VARIATION = 45;

    private static final int BASE_PARTICLES_PER_TICK = 7;
    private static final int PILLAR_PARTICLES_PER_TICK = 5;
    private static final int CAP_PARTICLES_PER_TICK = 9;

    private static final int RING_START_TICK = CAP_START_TICK;
    private static final int RING_BUILD_TICKS = CAP_BUILD_TICKS;
    private static final int RING_PARTICLES = 30;

    private static final int BASE_TRANSITION_TICKS = 12;
    private static final int PILLAR_TRANSITION_TICKS = 15;
    private static final int CAP_TRANSITION_TICKS = 17;
    private static final int RING_TRANSITION_TICKS = 12;

    private static final double BASE_RADIUS = 12.67;
    private static final double BASE_HEIGHT = 1.66;
    private static final double PILLAR_HEIGHT = 32.5;
    private static final double PILLAR_MIN_WIDTH = 2.0;
    private static final double PILLAR_MAX_WIDTH = 6.5;
    private static final double CAP_RADIUS = 19.0;
    private static final double CAP_HEIGHT = 17.0;

    private static final double MAX_VIEW_DISTANCE = 512.0;
    private static final double MAX_VIEW_DISTANCE_SQR = MAX_VIEW_DISTANCE * MAX_VIEW_DISTANCE;
    private static final int MAX_ACTIVE_CLOUDS = 4;

    private static final double TAU = Math.PI * 2.0;

    private MushroomCloudParticleEffect() {
    }

    public static void start(ServerLevel level, BlockPos pos, float scale) {
        if (level == null || pos == null) {
            return;
        }

        List<Cloud> clouds = CLOUDS.computeIfAbsent(level, ignored -> new ArrayList<>());

        if (clouds.size() >= MAX_ACTIVE_CLOUDS) {
            return;
        }

        clouds.add(new Cloud(
                pos.immutable(),
                Mth.clamp(scale, 0.1f, 8.0f),
                level.random.nextLong()
        ));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        List<Cloud> clouds = CLOUDS.get(level);

        if (clouds == null || clouds.isEmpty()) {
            CLOUDS.remove(level);
            return;
        }

        Iterator<Cloud> iterator = clouds.iterator();

        while (iterator.hasNext()) {
            Cloud cloud = iterator.next();

            if (cloud.age >= TOTAL_TICKS) {
                iterator.remove();
                continue;
            }

            cloud.tick(level);
            cloud.age++;
        }

        if (clouds.isEmpty()) {
            CLOUDS.remove(level);
        }
    }

    private static final class Cloud {

        private final BlockPos pos;
        private final float scale;
        private final Random random;
        private final List<ServerPlayer> recipients = new ArrayList<>();

        private int age;

        private Cloud(BlockPos pos, float scale, long seed) {
            this.pos = pos;
            this.scale = scale;
            this.random = new Random(seed);
        }

        private void tick(ServerLevel level) {
            updateRecipients(level);

            if (recipients.isEmpty()) {
                return;
            }

            if (age < BUILD_TICKS) {
                emitBase(level, BASE_PARTICLES_PER_TICK);
                emitPillar(level, PILLAR_PARTICLES_PER_TICK);

                if (age >= CAP_START_TICK) {
                    emitCap(level, CAP_PARTICLES_PER_TICK);
                    emitRing(level);
                }

                return;
            }

            if (age < FADE_START && age % HOLD_EMISSION_INTERVAL == 0) {
                emitBase(level, 3);
                emitPillar(level, 3);
                emitCap(level, 4);
            }
        }

        private void updateRecipients(ServerLevel level) {
            recipients.clear();

            double x = centerX();
            double y = pos.getY() + 0.5;
            double z = centerZ();

            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(x, y, z) <= MAX_VIEW_DISTANCE_SQR) {
                    recipients.add(player);
                }
            }
        }

        private void emitBase(ServerLevel level, int count) {
            double progress = easeOut(Math.min(1.0, (double) age / BASE_BUILD_TICKS));
            double radius = BASE_RADIUS * scale * progress;
            double height = BASE_HEIGHT * scale;
            double expansionSpeed = BASE_RADIUS * scale / BASE_BUILD_TICKS;

            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * TAU;
                double radial = Math.sqrt(random.nextDouble());
                double distance = radius * radial;

                double x = centerX() + Math.cos(angle) * distance;
                double z = centerZ() + Math.sin(angle) * distance;
                double y = pos.getY() + 0.2 + random.nextDouble() * height;

                double radialSpeed = expansionSpeed * (0.35 + radial * 0.65)
                        + 0.012 * scale;
                double swirl = (random.nextDouble() - 0.5) * 0.018 * scale;
                double upwardSpeed = (0.012 + random.nextDouble() * 0.018) * scale;

                Vec3 motion = new Vec3(
                        Math.cos(angle) * radialSpeed - Math.sin(angle) * swirl,
                        upwardSpeed,
                        Math.sin(angle) * radialSpeed + Math.cos(angle) * swirl
                );

                float size = (float) ((4.5 + random.nextDouble() * 2.8) * scale);

                sendCloudSmoke(
                        level, x, y, z, size,
                        motion, BASE_TRANSITION_TICKS
                );
            }
        }

        private void emitPillar(ServerLevel level, int count) {
            double progress = easeOut(Math.min(1.0, (double) age / PILLAR_BUILD_TICKS));
            double height = PILLAR_HEIGHT * scale * progress;

            for (int i = 0; i < count; i++) {
                double yProgress = random.nextDouble();
                double y = pos.getY() + 0.5 + yProgress * height;

                double localProgress = Mth.clamp(
                        (y - pos.getY() - 0.5) / Math.max(0.001, PILLAR_HEIGHT * scale),
                        0.0,
                        1.0
                );

                double width = pillarWidth(localProgress) * scale;
                double angle = random.nextDouble() * TAU;
                double radial = Math.sqrt(random.nextDouble());
                double distance = width * radial;

                double x = centerX() + Math.cos(angle) * distance;
                double z = centerZ() + Math.sin(angle) * distance;

                double upwardSpeed = (0.045 + random.nextDouble() * 0.035) * scale;
                double radialSpeed = (0.006 + random.nextDouble() * 0.012) * scale;
                double swirl = (random.nextDouble() - 0.5) * 0.018 * scale;

                Vec3 motion = new Vec3(
                        Math.cos(angle) * radialSpeed - Math.sin(angle) * swirl,
                        upwardSpeed,
                        Math.sin(angle) * radialSpeed + Math.cos(angle) * swirl
                );

                float size = (float) ((4.0 + random.nextDouble() * 2.8) * scale);

                sendCloudSmoke(
                        level, x, y, z, size,
                        motion, PILLAR_TRANSITION_TICKS
                );
            }
        }

        private void emitCap(ServerLevel level, int count) {
            double capAge = age - CAP_START_TICK;
            double progress = easeOut(Mth.clamp(capAge / CAP_BUILD_TICKS, 0.0, 1.0));

            double radius = CAP_RADIUS * scale * progress;
            double height = CAP_HEIGHT * scale;
            double centerY = pos.getY() + 0.5 + PILLAR_HEIGHT * scale;
            double expansionSpeed = CAP_RADIUS * scale / CAP_BUILD_TICKS;

            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * TAU;
                double radial = Math.sqrt(random.nextDouble());
                double distance = radius * radial;

                double domeHeight = height * Math.sqrt(
                        Math.max(0.0, 1.0 - radial * radial)
                );

                double x = centerX() + Math.cos(angle) * distance;
                double z = centerZ() + Math.sin(angle) * distance;
                double y = centerY + domeHeight * random.nextDouble();

                double radialSpeed = expansionSpeed * (0.3 + radial * 0.7)
                        + (0.01 + random.nextDouble() * 0.018) * scale;

                double upwardSpeed = (
                        0.006 + (1.0 - radial) * 0.024
                                + random.nextDouble() * 0.01
                ) * scale;

                double swirl = (random.nextDouble() - 0.5) * 0.016 * scale;

                Vec3 motion = new Vec3(
                        Math.cos(angle) * radialSpeed - Math.sin(angle) * swirl,
                        upwardSpeed,
                        Math.sin(angle) * radialSpeed + Math.cos(angle) * swirl
                );

                float size = (float) ((4.5 + random.nextDouble() * 3.0) * scale);

                sendCloudSmoke(
                        level, x, y, z, size,
                        motion, CAP_TRANSITION_TICKS
                );
            }
        }

        private void emitRing(ServerLevel level) {
            int ringAge = age - RING_START_TICK;

            if (ringAge != 0) {
                return;
            }

            double finalRadius = 16.0 * scale;
            double y = pos.getY() + 0.5 + PILLAR_HEIGHT * scale * 0.52;
            double startRadius = 0.2 * scale;
            double outwardSpeed = finalRadius / RING_BUILD_TICKS;

            for (int i = 0; i < RING_PARTICLES; i++) {
                double angle = TAU * i / RING_PARTICLES
                        + (random.nextDouble() - 0.5) * 0.025;
                double distance = startRadius * (0.5 + random.nextDouble());

                double x = centerX() + Math.cos(angle) * distance;
                double z = centerZ() + Math.sin(angle) * distance;

                double radialSpeed = outwardSpeed * (0.9 + random.nextDouble() * 0.2);
                double tangentialSpeed = (random.nextDouble() - 0.5) * 0.004 * scale;
                double upwardSpeed = (random.nextDouble() - 0.5) * 0.004 * scale;

                Vec3 motion = new Vec3(
                        Math.cos(angle) * radialSpeed - Math.sin(angle) * tangentialSpeed,
                        upwardSpeed,
                        Math.sin(angle) * radialSpeed + Math.cos(angle) * tangentialSpeed
                );

                float size = (float) ((2.0 + random.nextDouble() * 0.5) * scale);

                sendCloudSmoke(
                        level,
                        x,
                        y + (random.nextDouble() - 0.5) * 0.1 * scale,
                        z,
                        size,
                        motion,
                        RING_TRANSITION_TICKS
                );
            }
        }

        private void sendCloudSmoke(
                ServerLevel level,
                double x,
                double y,
                double z,
                float size,
                Vec3 motion,
                int transitionTime
        ) {
            double fadeHeight = (PILLAR_HEIGHT + CAP_HEIGHT) * scale;
            double relativeHeight = Math.max(0.0, y - pos.getY());

            double heightProgress = Mth.clamp(
                    relativeHeight / Math.max(0.001, fadeHeight),
                    0.0,
                    1.0
            );

            int fadeDelay = (int) (
                    (1.0 - heightProgress) * BOTTOM_TO_TOP_FADE_TICKS
            );

            int fadeEnd = FADE_START + fadeDelay + FADE_TICKS
                    + FADE_PARTICLE_LIFETIME;

            int lifetime = Math.max(
                    particleLifetime(),
                    fadeEnd - age
            );

            sendSmoke(
                    level,
                    x,
                    y,
                    z,
                    size,
                    lifetime,
                    motion,
                    transitionTime
            );
        }

        private void sendSmoke(
                ServerLevel level,
                double x,
                double y,
                double z,
                float size,
                int lifetime,
                Vec3 motion,
                int transitionTime
        ) {
            SmokeParticleOptions options = new SmokeParticleOptions(
                    size,
                    variedOrange(),
                    variedGray(),
                    false,
                    Math.max(1, lifetime),
                    motion,
                    Math.max(0, transitionTime)
            );

            for (ServerPlayer player : recipients) {
                level.sendParticles(
                        player,
                        options,
                        true,
                        x,
                        y,
                        z,
                        0,
                        0.0,
                        0.0,
                        0.0,
                        1.0
                );
            }
        }

        private double centerX() {
            return pos.getX() + 0.5;
        }

        private double centerZ() {
            return pos.getZ() + 0.5;
        }

        private int particleLifetime() {
            return MIN_PARTICLE_LIFETIME
                    + random.nextInt(PARTICLE_LIFETIME_VARIATION);
        }

        private double pillarWidth(double progress) {
            progress = Mth.clamp(progress, 0.0, 1.0);

            double lowerBulge = Math.exp(-Math.pow(progress / 0.23, 2.0));
            double upperBulge = Math.exp(-Math.pow((1.0 - progress) / 0.28, 2.0));
            double middleNarrowing = Math.sin(Math.PI * progress);

            return PILLAR_MIN_WIDTH
                    + (PILLAR_MAX_WIDTH - PILLAR_MIN_WIDTH)
                    * Math.max(
                    lowerBulge,
                    Math.max(upperBulge, 1.0 - middleNarrowing)
            );
        }

        private int variedOrange() {
            int red = 238 + random.nextInt(18);
            int green = 85 + random.nextInt(66);
            int blue = random.nextInt(24);

            return (red << 16) | (green << 8) | blue;
        }

        private int variedGray() {
            int gray = 85 + random.nextInt(48);
            int red = Math.min(255, gray + random.nextInt(9));
            int green = Math.min(255, gray + random.nextInt(7));
            int blue = Math.max(0, gray - random.nextInt(7));

            return (red << 16) | (green << 8) | blue;
        }

        private double easeOut(double value) {
            value = Mth.clamp(value, 0.0, 1.0);
            return 1.0 - (1.0 - value) * (1.0 - value);
        }
    }
}
