package com.julian.createwarfare.effects.particles.shockwaves;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

public class ShockwaveParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float maxRadius;
    private final float speed;

    protected ShockwaveParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            float maxRadius,
            float speed,
            int lifetime,
            SpriteSet sprites
    ) {
        super(level, x, y, z);

        this.sprites = sprites;
        this.maxRadius = maxRadius;
        this.speed = speed;

        this.lifetime = Math.max(1, lifetime);
        this.quadSize = 0.01f;

        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.gravity = 0.0f;

        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        this.alpha = 1.0f;

        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();

        float radius =
                Math.min(
                        maxRadius,
                        this.age * speed
                );

        this.quadSize = radius;

        int fadeTicks = Math.min(30, this.lifetime);

        if (this.age >= this.lifetime - fadeTicks) {
            float fadeProgress =
                    (float) (this.age - (this.lifetime - fadeTicks))
                            / (float) fadeTicks;

            this.alpha =
                    1.0f -
                            Mth.clamp(
                                    fadeProgress,
                                    0.0f,
                                    1.0f
                            );
        } else {
            this.alpha = 1.0f;
        }

        this.setSpriteFromAge(sprites);
    }

    @Override
    public void render(
            VertexConsumer buffer,
            Camera camera,
            float partialTick
    ) {
        double px = Mth.lerp(partialTick, this.xo, this.x);
        double py = Mth.lerp(partialTick, this.yo, this.y);
        double pz = Mth.lerp(partialTick, this.zo, this.z);

        float x = (float) (px - camera.getPosition().x());
        float y = (float) (py - camera.getPosition().y());
        float z = (float) (pz - camera.getPosition().z());

        float radius =
                Math.min(
                        maxRadius,
                        (this.age + partialTick) * speed
                );

        float minX = x - radius;
        float maxX = x + radius;
        float minZ = z - radius;
        float maxZ = z + radius;

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        int light = this.getLightColor(partialTick);
        int alpha = (int) (this.alpha * 255.0f);

        buffer.addVertex(minX, y, minZ)
                .setUv(u0, v0)
                .setColor(255, 255, 255, alpha)
                .setLight(light);

        buffer.addVertex(minX, y, maxZ)
                .setUv(u0, v1)
                .setColor(255, 255, 255, alpha)
                .setLight(light);

        buffer.addVertex(maxX, y, maxZ)
                .setUv(u1, v1)
                .setColor(255, 255, 255, alpha)
                .setLight(light);

        buffer.addVertex(maxX, y, minZ)
                .setUv(u1, v0)
                .setColor(255, 255, 255, alpha)
                .setLight(light);

        buffer.addVertex(minX, y, minZ)
                .setUv(u0, v0)
                .setColor(255, 255, 255, alpha)
                .setLight(light);

        buffer.addVertex(maxX, y, minZ)
                .setUv(u1, v0)
                .setColor(255, 255, 255, alpha)
                .setLight(light);

        buffer.addVertex(maxX, y, maxZ)
                .setUv(u1, v1)
                .setColor(255, 255, 255, alpha)
                .setLight(light);

        buffer.addVertex(minX, y, maxZ)
                .setUv(u0, v1)
                .setColor(255, 255, 255, alpha)
                .setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public boolean shouldCull() {
        return false;
    }

    public static class Provider
            implements ParticleProvider<ShockwaveParticleOptions> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                ShockwaveParticleOptions options,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new ShockwaveParticle(
                    level,
                    x,
                    y,
                    z,
                    options.getRadius(),
                    options.getSpeed(),
                    options.getDuration(),
                    sprites
            );
        }
    }
}