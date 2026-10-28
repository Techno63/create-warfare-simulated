package com.julian.createwarfare.effects.server.particles;

import com.julian.createwarfare.effects.particles.smoke_explosion.SmokeExplosionParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class GenericExplosionParticleEffect {
    private static final int WAVES = 3;
    private static final int PARTICLES_PER_WAVE = 55;
    private static final int PARTICLE_LIFETIME = 40;

    private static final double MAX_VIEW_DISTANCE = 512.0D;
    private static final double MAX_VIEW_DISTANCE_SQR =
            MAX_VIEW_DISTANCE * MAX_VIEW_DISTANCE;

    private static final float MAX_SCALE = 4.0F;

    private static final float BIG_MIN_SIZE = 1.4F;
    private static final float BIG_MAX_SIZE = 2.2F;

    private static final float MEDIUM_MIN_SIZE = 0.85F;
    private static final float MEDIUM_MAX_SIZE = 1.3F;

    private static final float SMALL_MIN_SIZE = 0.4F;
    private static final float SMALL_MAX_SIZE = 0.75F;

    private static final double BIG_MIN_SPEED = 0.12D;
    private static final double BIG_MAX_SPEED = 0.3D;

    private static final double MEDIUM_MIN_SPEED = 0.45D;
    private static final double MEDIUM_MAX_SPEED = 0.7D;

    private static final double SMALL_MIN_SPEED = 0.85D;
    private static final double SMALL_MAX_SPEED = 1.2D;

    private static final double BIG_SPREAD = 0.35D;
    private static final double MEDIUM_SPREAD = 0.7D;
    private static final double SMALL_SPREAD = 1.0D;

    private GenericExplosionParticleEffect() {
    }

    public static void start(ServerLevel level, BlockPos pos, float scale) {
        if (pos == null) {
            return;
        }

        start(level, Vec3.atCenterOf(pos), scale);
    }

    public static void start(ServerLevel level, Vec3 pos, float scale) {
        if (level == null || pos == null || level.isClientSide()
                || !Float.isFinite(scale)) {
            return;
        }

        float safeScale = Math.max(0.1F, Math.min(MAX_SCALE, scale));
        RandomSource random = level.random;

        emitWave(
                level,
                pos,
                safeScale,
                random,
                BIG_MIN_SIZE,
                BIG_MAX_SIZE,
                BIG_MIN_SPEED,
                BIG_MAX_SPEED,
                BIG_SPREAD
        );

        emitWave(
                level,
                pos,
                safeScale,
                random,
                MEDIUM_MIN_SIZE,
                MEDIUM_MAX_SIZE,
                MEDIUM_MIN_SPEED,
                MEDIUM_MAX_SPEED,
                MEDIUM_SPREAD
        );

        emitWave(
                level,
                pos,
                safeScale,
                random,
                SMALL_MIN_SIZE,
                SMALL_MAX_SIZE,
                SMALL_MIN_SPEED,
                SMALL_MAX_SPEED,
                SMALL_SPREAD
        );
    }

    private static void emitWave(
            ServerLevel level,
            Vec3 origin,
            float scale,
            RandomSource random,
            float minSize,
            float maxSize,
            double minSpeed,
            double maxSpeed,
            double spread
    ) {
        int particleCount = Math.max(
                1,
                Math.round(PARTICLES_PER_WAVE * scale)
        );

        for (int i = 0; i < particleCount; i++) {
            double x = random.nextGaussian();
            double y = random.nextGaussian();
            double z = random.nextGaussian();

            Vec3 direction = new Vec3(x, y, z);

            if (direction.lengthSqr() < 1.0E-6D) {
                direction = new Vec3(0.0D, 1.0D, 0.0D);
            } else {
                direction = direction.normalize();
            }

            double speed = minSpeed
                    + random.nextDouble() * (maxSpeed - minSpeed);

            Vec3 velocity = direction.scale(speed);

            double offset = spread * scale;

            Vec3 particlePos = origin.add(
                    random.nextGaussian() * offset * 0.35D,
                    random.nextGaussian() * offset * 0.35D,
                    random.nextGaussian() * offset * 0.35D
            );

            float particleScale = (
                    minSize + random.nextFloat() * (maxSize - minSize)
            ) * Math.min(scale, 1.8F);

            SmokeExplosionParticleOptions particle =
                    new SmokeExplosionParticleOptions(
                            particleScale,
                            PARTICLE_LIFETIME
                    );

            for (ServerPlayer player : level.players()) {
                if (player.isRemoved()
                        || player.serverLevel() != level
                        || player.distanceToSqr(
                        particlePos.x,
                        particlePos.y,
                        particlePos.z
                ) > MAX_VIEW_DISTANCE_SQR) {
                    continue;
                }

                level.sendParticles(
                        player,
                        particle,
                        true,
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        0,
                        velocity.x,
                        velocity.y,
                        velocity.z,
                        1.0D
                );
            }
        }
    }
}