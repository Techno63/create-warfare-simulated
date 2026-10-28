package com.julian.createwarfare.effects.particles.smoke_explosion;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

public class SmokeExplosionParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float size;
    private final int particleLifetime;
    private final int animationDuration;
    private final int lastFrameDuration;
    private final int fadeStart;

    private int particleAge;
    private int frame;

    protected SmokeExplosionParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            float size,
            int lifetime,
            SpriteSet sprites
    ) {
        super(level, x, y, z, xd, yd, zd);

        this.sprites = sprites;
        this.size = Math.max(0.1f, size);
        this.particleLifetime = Math.max(12, lifetime);

        this.animationDuration = Math.max(
                1,
                Math.min(
                        this.particleLifetime - 1,
                        Math.round(this.particleLifetime * 0.30f)
                )
        );

        this.lastFrameDuration = Math.max(
                1,
                Math.min(5, this.particleLifetime / 10)
        );

        this.fadeStart = Math.min(
                this.particleLifetime - this.lastFrameDuration,
                Math.max(
                        this.animationDuration,
                        Math.round(this.particleLifetime * 0.65f)
                )
        );

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.gravity = 0f;
        this.friction = 0.96f;
        this.quadSize = this.size;
        this.lifetime = this.particleLifetime;
        this.alpha = 1f;

        this.particleAge = 0;
        this.frame = 0;

        setSprite(sprites.get(0, 10));
    }

    @Override
    public void tick() {
        if (removed) {
            return;
        }

        xo = x;
        yo = y;
        zo = z;

        if (particleAge >= particleLifetime) {
            remove();
            return;
        }

        if (particleAge >= animationDuration) {
            yd += 0.003f;
        }

        move(xd, yd, zd);

        xd *= friction;
        yd *= 0.98;
        zd *= friction;

        particleAge++;

        if (particleAge < animationDuration) {
            int nextFrame = Math.min(
                    9,
                    particleAge * 10 / animationDuration
            );

            if (nextFrame != frame) {
                frame = nextFrame;
                setSprite(sprites.get(frame, 10));
            }
        } else {
            if (frame != 9 && frame != 10) {
                frame = 9;
                setSprite(sprites.get(9, 10));
            }

            if (particleAge >= particleLifetime - lastFrameDuration) {
                if (frame != 10) {
                    frame = 10;
                    setSprite(sprites.get(10, 10));
                }
            }
        }

        if (particleAge >= fadeStart) {
            float fade = (particleLifetime - particleAge)
                    / (float) Math.max(1, particleLifetime - fadeStart);

            alpha = Mth.clamp(fade, 0f, 1f);
        } else {
            alpha = 1f;
        }

        if (particleAge >= particleLifetime) {
            remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xf00000;
    }

    public static class Provider
            implements ParticleProvider<SmokeExplosionParticleOptions> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SmokeExplosionParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new SmokeExplosionParticle(
                    level,
                    x,
                    y,
                    z,
                    xd,
                    yd,
                    zd,
                    options.size(),
                    options.lifetime(),
                    sprites
            );
        }
    }
}