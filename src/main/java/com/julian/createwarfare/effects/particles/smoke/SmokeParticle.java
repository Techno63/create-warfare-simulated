package com.julian.createwarfare.effects.particles.smoke;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class SmokeParticle extends TextureSheetParticle {

    private final float baseSize;
    private final int maxLifetime;

    private SmokeParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            SmokeParticleOptions options,
            SpriteSet spriteSet,
            RandomSource random
    ) {
        super(level, x, y, z, xd, yd, zd);

        this.baseSize = options.getSize();
        this.maxLifetime = options.getDuration();

        this.lifetime = maxLifetime;
        this.gravity = 0.0f;
        this.friction = 0.96f;
        this.hasPhysics = false;

        this.quadSize = baseSize;

        this.setSprite(spriteSet.get(random));
        this.setAlpha(1.0f);
    }

    @Override
    public void tick() {
        super.tick();

        float fadeStart = maxLifetime * 0.8f;

        if (age >= fadeStart) {
            float progress = (age - fadeStart) / (maxLifetime - fadeStart);
            setAlpha(1.0f - Mth.clamp(progress, 0.0f, 1.0f));
        } else {
            setAlpha(1.0f);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SmokeParticleOptions> {

        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(
                SmokeParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new SmokeParticle(
                    level,
                    x,
                    y,
                    z,
                    xd,
                    yd,
                    zd,
                    options,
                    spriteSet,
                    RandomSource.create()
            );
        }
    }
}