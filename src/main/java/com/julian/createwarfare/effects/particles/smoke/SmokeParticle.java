package com.julian.createwarfare.effects.particles.smoke;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

public final class SmokeParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;
    private final float startRed;
    private final float startGreen;
    private final float startBlue;
    private final float endRed;
    private final float endGreen;
    private final float endBlue;
    private final float initialSize;
    private final boolean shrink;
    private final int transitionTime;

    private SmokeParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            SmokeParticleOptions options,
            SpriteSet sprites
    ) {
        super(level, x, y, z, vx, vy, vz);

        this.spriteSet = sprites;

        this.xd = vx + options.motionX();
        this.yd = vy + options.motionY();
        this.zd = vz + options.motionZ();

        this.startRed = ((options.fromColor() >> 16) & 255) / 255.0f;
        this.startGreen = ((options.fromColor() >> 8) & 255) / 255.0f;
        this.startBlue = (options.fromColor() & 255) / 255.0f;

        this.endRed = ((options.toColor() >> 16) & 255) / 255.0f;
        this.endGreen = ((options.toColor() >> 8) & 255) / 255.0f;
        this.endBlue = (options.toColor() & 255) / 255.0f;

        this.initialSize = options.size();
        this.quadSize = this.initialSize;
        this.shrink = options.shrink();
        this.transitionTime = options.transitionTime();

        this.lifetime = options.lifetime();
        this.gravity = 0.0f;
        this.friction = 0.97f;
        this.hasPhysics = false;

        this.roll = this.random.nextFloat() * (Mth.PI / 4.0f);
        this.oRoll = this.roll;

        this.setSpriteFromAge(this.spriteSet);
        this.setColor(this.startRed, this.startGreen, this.startBlue);
        this.setAlpha(1.0f);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;

        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        float progress = this.transitionTime == 0
                ? 1.0f
                : Mth.clamp(this.age / (float) this.transitionTime, 0.0f, 1.0f);

        this.setColor(
                Mth.lerp(progress, this.startRed, this.endRed),
                Mth.lerp(progress, this.startGreen, this.endGreen),
                Mth.lerp(progress, this.startBlue, this.endBlue)
        );

        float lifeProgress = Mth.clamp(
                this.age / (float) this.lifetime,
                0.0f,
                1.0f
        );

        if (this.shrink) {
            this.quadSize = this.initialSize * (1.0f - lifeProgress);
            this.setAlpha(1.0f);
        } else {
            this.quadSize = this.initialSize;

            float fadeStart = 0.68f;
            float alpha = lifeProgress < fadeStart
                    ? 1.0f
                    : 1.0f - (lifeProgress - fadeStart) / (1.0f - fadeStart);

            this.setAlpha(Mth.clamp(alpha, 0.0f, 1.0f));
        }

        this.setSpriteFromAge(this.spriteSet);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SmokeParticleOptions> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SmokeParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double vx,
                double vy,
                double vz
        ) {
            return new SmokeParticle(
                    level,
                    x,
                    y,
                    z,
                    vx,
                    vy,
                    vz,
                    options,
                    this.sprites
            );
        }
    }
}