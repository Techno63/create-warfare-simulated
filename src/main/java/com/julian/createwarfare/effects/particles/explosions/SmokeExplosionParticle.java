package com.julian.createwarfare.effects.particles.explosions;

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
    private final int frameTime;

    private int frame;
    private int frameAge;
    private int frameDuration;

    private int fadeStart;
    private int lastFrameDuration;

    protected SmokeExplosionParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            float size,
            SpriteSet sprites
    ) {
        super(
                level,
                x,
                y,
                z,
                xd,
                yd,
                zd
        );

        this.sprites = sprites;
        this.size = Math.max(0.1f, size);

        this.frameTime =
                Math.max(
                        1,
                        Mth.ceil(
                                (float) Math.sqrt(this.size)
                        )
                );

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.gravity = 0f;
        this.friction = 0.96f;

        this.quadSize = this.size;

        this.frame = 1;
        this.frameAge = 0;

        this.frameDuration =
                getAnimationDuration(level);

        this.fadeStart = 0;
        this.lastFrameDuration = 5;

        this.alpha = 1f;
        this.lifetime = Integer.MAX_VALUE;

        setSprite(
                sprites.get(
                        0,
                        10
                )
        );
    }

    private int getAnimationDuration(ClientLevel level) {
        return Math.max(
                1,
                frameTime
                        + level.random.nextInt(3)
                        - 1
        );
    }

    private int getHoldDuration(ClientLevel level) {
        float variation =
                0.85f
                        + level.random.nextFloat() * 0.3f;

        return Math.max(
                lastFrameDuration + 1,
                Math.round(
                        frameTime
                                * 60
                                * variation
                )
        );
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;

        if (frame == 10 || frame == 11) {
            yd += 0.003f;
        }

        move(
                xd,
                yd,
                zd
        );

        xd *= friction;
        yd *= 0.98;
        zd *= friction;

        if (frame == 11) {
            frameAge++;

            if (frameAge >= lastFrameDuration) {
                remove();
            }

            return;
        }

        frameAge++;

        if (frame == 10) {
            if (frameAge > fadeStart) {
                int fadeAge =
                        frameAge - fadeStart;

                int fadeDuration =
                        frameDuration - fadeStart;

                float fade =
                        1f
                                - fadeAge
                                / (float) fadeDuration;

                alpha =
                        Mth.clamp(
                                fade,
                                0f,
                                1f
                        );
            }

            if (frameAge >= frameDuration - lastFrameDuration) {
                frame = 11;
                frameAge = 0;

                setSprite(
                        sprites.get(
                                10,
                                10
                        )
                );

                return;
            }

            return;
        }

        if (frameAge >= frameDuration) {
            frameAge = 0;

            if (frame < 9) {
                frame++;

                frameDuration =
                        getAnimationDuration(level);

                setSprite(
                        sprites.get(
                                frame - 1,
                                10
                        )
                );
            } else {
                frame = 10;

                frameDuration =
                        getHoldDuration(level);

                fadeStart =
                        frameDuration / 2;

                alpha = 1f;

                setSprite(
                        sprites.get(
                                9,
                                10
                        )
                );
            }
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
                    sprites
            );
        }
    }
}