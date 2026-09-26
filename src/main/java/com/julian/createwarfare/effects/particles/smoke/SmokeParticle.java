package com.julian.createwarfare.effects.particles.smoke;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;

public class SmokeParticle extends TextureSheetParticle {

    private final double riseSpeed;

    protected SmokeParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            SpriteSet sprites,
            double riseSpeed,
            int lifetime
    ) {
        super(level, x, y, z);

        this.riseSpeed = riseSpeed;
        this.quadSize = 2.0f + this.random.nextFloat() * 2.0f;
        this.yd = riseSpeed;
        this.pickSprite(sprites);
        this.lifetime = Math.max(
                1,
                Math.round(lifetime * (1f + this.random.nextFloat() * 0.5f))
        );
    }

    @Override
    public void tick() {
        super.tick();

        this.yd = riseSpeed;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SmokeParticleOptions> {

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
                double xd,
                double yd,
                double zd
        ) {
            return new SmokeParticle(
                    level,
                    x,
                    y,
                    z,
                    sprites,
                    options.getRiseSpeed(),
                    options.getLifetime()
            );
        }
    }
}