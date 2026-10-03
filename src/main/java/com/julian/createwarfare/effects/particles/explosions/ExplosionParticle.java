package com.julian.createwarfare.effects.particles.explosions;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class ExplosionParticle extends TextureSheetParticle {

    private final float baseSize;
    private final int maxLifetime;

    private ExplosionParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            ExplosionParticleOptions options,
            SpriteSet spriteSet,
            RandomSource random
    ) {
        super(level, x, y, z, 0.0, 0.0, 0.0);

        this.baseSize = options.getSize();
        this.maxLifetime = options.getLifetime();

        this.lifetime = maxLifetime;
        this.gravity = 0.0f;
        this.friction = 1.0f;
        this.hasPhysics = false;

        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;

        this.quadSize = baseSize;

        this.setSprite(spriteSet.get(random));
        this.setAlpha(1.0f);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;

        age++;

        if (age >= lifetime) {
            remove();
        }
    }

    @Override
    public void render(
            VertexConsumer vertexConsumer,
            Camera camera,
            float partialTick
    ) {
        float progress = Mth.clamp(
                (age + partialTick) / (float) maxLifetime,
                0.0f,
                1.0f
        );

        this.quadSize = baseSize * (1.0f - progress);

        super.render(
                vertexConsumer,
                camera,
                partialTick
        );
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<ExplosionParticleOptions> {

        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(
                ExplosionParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new ExplosionParticle(
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