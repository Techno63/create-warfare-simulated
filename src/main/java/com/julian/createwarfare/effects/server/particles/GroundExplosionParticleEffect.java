package com.julian.createwarfare.effects.server.particles;

import com.julian.createwarfare.effects.particles.smoke_explosion.SmokeExplosionParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GroundExplosionParticleEffect {
    private static final List<Explosion> ACTIVE = new ArrayList<>();

    private static final int MAX_VIEW_DISTANCE = 512;
    private static final double MAX_VIEW_DISTANCE_SQR =
            (double) MAX_VIEW_DISTANCE * MAX_VIEW_DISTANCE;

    private static final int MAX_ACTIVE_EXPLOSIONS = 6;
    private static final float MAX_SCALE = 4.0F;

    private static final int MIN_EMISSION_TICKS = 6;
    private static final int MAX_EMISSION_TICKS = 30;
    private static final int PARTICLES_PER_TICK = 12;

    private static final int ARC_COUNT = 6;
    private static final int ARC_MIN_DURATION = 28;
    private static final int ARC_MAX_DURATION = 42;

    private static final double CONE_HEIGHT = 4.8D;
    private static final double CONE_RADIUS = 3.2D;

    private GroundExplosionParticleEffect() {
    }

    public static void start(ServerLevel level, BlockPos pos, float scale) {
        if (pos != null) {
            start(level, Vec3.atCenterOf(pos), scale);
        }
    }

    public static void start(ServerLevel level, Vec3 pos, float scale) {
        if (level == null || pos == null || level.isClientSide()
                || !Float.isFinite(scale)) {
            return;
        }

        float safeScale = Math.max(0.1F, Math.min(MAX_SCALE, scale));

        List<ServerPlayer> recipients = new ArrayList<>();

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.x, pos.y, pos.z)
                    <= MAX_VIEW_DISTANCE_SQR) {
                recipients.add(player);
            }
        }

        if (recipients.isEmpty()) {
            return;
        }

        while (ACTIVE.size() >= MAX_ACTIVE_EXPLOSIONS) {
            ACTIVE.remove(0);
        }

        ACTIVE.add(new Explosion(
                level,
                pos,
                safeScale,
                level.random,
                List.copyOf(recipients)
        ));
    }

    public static void tick() {
        Iterator<Explosion> iterator = ACTIVE.iterator();

        while (iterator.hasNext()) {
            if (!iterator.next().tick()) {
                iterator.remove();
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static final class Explosion {
        private final ServerLevel level;
        private final Vec3 origin;
        private final float scale;
        private final RandomSource random;
        private final List<ServerPlayer> recipients;
        private final int emissionTicks;
        private final List<Arc> arcs = new ArrayList<>();
        private final int arcDuration;

        private int age;

        private Explosion(
                ServerLevel level,
                Vec3 origin,
                float scale,
                RandomSource random,
                List<ServerPlayer> recipients
        ) {
            this.level = level;
            this.origin = origin;
            this.scale = scale;
            this.random = random;
            this.recipients = recipients;

            this.emissionTicks = Math.max(
                    MIN_EMISSION_TICKS,
                    Math.min(
                            MAX_EMISSION_TICKS,
                            Math.round(MIN_EMISSION_TICKS * scale)
                    )
            );

            this.arcDuration = Math.round(
                    randomBetween(ARC_MIN_DURATION, ARC_MAX_DURATION) * scale
                            * 0.75F
            );

            int arcCount = Math.max(
                    2,
                    Math.round(ARC_COUNT * Math.min(scale, 1.0F))
            );

            for (int i = 0; i < arcCount; i++) {
                double angle = (Math.PI * 2.0D * i / arcCount)
                        + randomBetween(-0.22D, 0.22D);

                double length = randomBetween(3.8D, 6.0D) * scale;
                double height = randomBetween(3.0D, 4.8D) * scale;
                double drop = randomBetween(0.35D, 0.9D) * scale;

                int duration = Math.max(
                        12,
                        Math.round(
                                arcDuration * randomBetween(0.85F, 1.2F)
                        )
                );

                arcs.add(new Arc(angle, length, height, drop, duration));
            }
        }

        private boolean tick() {
            if (age < emissionTicks) {
                emitConeParticles();
            }

            if (age < arcDuration) {
                emitArcs();
            }

            age++;

            return age < Math.max(emissionTicks, arcDuration);
        }

        private void emitConeParticles() {
            double layerProgress =
                    age / (double) Math.max(1, emissionTicks - 1);

            double layerHeight =
                    CONE_HEIGHT * scale * layerProgress;

            double layerRadius =
                    CONE_RADIUS * scale * layerProgress;

            int particleCount = Math.max(
                    1,
                    Math.round(
                            PARTICLES_PER_TICK
                                    * (1.0F - (float) layerProgress * 0.65F)
                    )
            );

            particleCount = Math.max(
                    1,
                    particleCount + random.nextInt(-1, 2)
            );

            for (int i = 0; i < particleCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;

                double radius = layerRadius
                        * Math.sqrt(random.nextDouble())
                        * randomBetween(0.97D, 1.03D);

                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                double y = layerHeight
                        + randomBetween(-0.045D, 0.045D) * scale;

                y = Math.max(0.035D * scale, y);

                Vec3 position = origin.add(x, y, z);

                double drift = randomBetween(0.008D, 0.025D) * scale;

                Vec3 velocity = new Vec3(
                        Math.cos(angle) * drift
                                + randomBetween(-0.008D, 0.008D) * scale,
                        randomBetween(-0.006D, 0.012D) * scale,
                        Math.sin(angle) * drift
                                + randomBetween(-0.008D, 0.008D) * scale
                );

                float layerSize = 1.0F - (float) layerProgress;

                float minSize = 0.72F + layerSize * 1.35F;
                float maxSize = 1.08F + layerSize * 1.85F;

                float particleScale =
                        randomBetween(minSize, maxSize)
                                * Math.min(scale, 1.8F)
                                * randomBetween(0.97F, 1.03F);

                int scaleTicks = Math.round(scale * 18.0F);

                int minLifetime =
                        35 + (int) (layerProgress * 90.0F) + scaleTicks;

                int maxLifetime =
                        55 + (int) (layerProgress * 120.0F) + scaleTicks;

                int lifetime =
                        random.nextInt(minLifetime, maxLifetime + 1);

                emit(
                        new SmokeExplosionParticleOptions(
                                particleScale,
                                lifetime
                        ),
                        position,
                        velocity
                );
            }
        }

        private void emitArcs() {
            for (Arc arc : arcs) {
                if (age >= arc.duration) {
                    continue;
                }

                double progress = age / (double) arc.duration;

                double distance = arc.length * progress;

                double arcHeight = arc.height
                        * Math.sin(Math.PI * progress);

                double verticalDrop = arc.drop * progress;

                double x = Math.cos(arc.angle) * distance;
                double z = Math.sin(arc.angle) * distance;
                double y = arcHeight - verticalDrop;

                Vec3 position = origin.add(
                        x + randomBetween(-0.035D, 0.035D) * scale,
                        y + randomBetween(-0.025D, 0.025D) * scale,
                        z + randomBetween(-0.035D, 0.035D) * scale
                );

                float arcParticleScale = Math.max(
                        0.1F,
                        randomBetween(0.25F, 0.4F)
                                * Math.min(scale, 1.8F)
                );

                SmokeExplosionParticleOptions arcParticle =
                        new SmokeExplosionParticleOptions(
                                arcParticleScale,
                                40
                        );

                Vec3 velocity = new Vec3(
                        Math.cos(arc.angle)
                                * randomBetween(0.006D, 0.018D) * scale,
                        randomBetween(0.012D, 0.028D) * scale,
                        Math.sin(arc.angle)
                                * randomBetween(0.006D, 0.018D) * scale
                );

                emit(arcParticle, position, velocity);
            }
        }

        private void emit(
                ParticleOptions particle,
                Vec3 position,
                Vec3 velocity
        ) {
            for (ServerPlayer player : recipients) {
                if (player.isRemoved()
                        || player.serverLevel() != level
                        || player.distanceToSqr(
                        position.x,
                        position.y,
                        position.z
                ) > MAX_VIEW_DISTANCE_SQR) {
                    continue;
                }

                level.sendParticles(
                        player,
                        particle,
                        true,
                        position.x,
                        position.y,
                        position.z,
                        0,
                        velocity.x,
                        velocity.y,
                        velocity.z,
                        1.0D
                );
            }
        }

        private double randomBetween(double min, double max) {
            return min + random.nextDouble() * (max - min);
        }

        private float randomBetween(float min, float max) {
            return min + random.nextFloat() * (max - min);
        }
    }

    private static final class Arc {
        private final double angle;
        private final double length;
        private final double height;
        private final double drop;
        private final int duration;

        private Arc(
                double angle,
                double length,
                double height,
                double drop,
                int duration
        ) {
            this.angle = angle;
            this.length = length;
            this.height = height;
            this.drop = drop;
            this.duration = duration;
        }
    }
}