package com.julian.createwarfare.effects.particles.explosions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class FlameExplosionParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final int delay;

    private int frame;
    private int frameTimer;
    private final int frameDuration;

    protected FlameExplosionParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            float size,
            int lifetime,
            int delay,
            double xd,
            double yd,
            double zd,
            SpriteSet sprites
    ) {
        super(level, x, y, z);

        this.sprites = sprites;
        this.quadSize = size;
        this.lifetime = Math.max(1, lifetime);
        this.delay = Math.max(0, delay);

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.gravity = 0.0f;
        this.friction = 0.96f;
        this.alpha = 1.0f;

        this.frame = 0;
        this.frameTimer = 0;

        this.frameDuration =
                Math.max(
                        1,
                        this.lifetime / 7
                );

        this.setSprite(
                this.sprites.get(0, 7)
        );
    }

    @Override
    public void tick() {
        if (this.age < this.delay) {
            this.age++;

            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;

            return;
        }

        super.tick();

        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        this.frameTimer++;

        if (this.frameTimer >= this.frameDuration) {
            this.frameTimer = 0;

            if (this.frame < 6) {
                this.frame++;
            }
        }

        this.setSprite(
                this.sprites.get(
                        this.frame,
                        7
                )
        );

        this.alpha = 1.0f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider
            implements ParticleProvider<FlameExplosionParticleOptions> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                FlameExplosionParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new FlameExplosionParticle(
                    level,
                    x,
                    y,
                    z,
                    options.getSize(),
                    options.getLifetime(),
                    options.getDelay(),
                    xd,
                    yd,
                    zd,
                    this.sprites
            );
        }
    }
}