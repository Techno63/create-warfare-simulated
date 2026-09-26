package com.julian.createwarfare.effects.particles.explosions;

import com.julian.createwarfare.registry.CWParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class FlameExplosionParticleOptions implements ParticleOptions {

    public static final StreamCodec<RegistryFriendlyByteBuf, FlameExplosionParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    FlameExplosionParticleOptions::getSize,
                    ByteBufCodecs.INT,
                    FlameExplosionParticleOptions::getLifetime,
                    ByteBufCodecs.INT,
                    FlameExplosionParticleOptions::getDelay,
                    FlameExplosionParticleOptions::new
            );

    private final float size;
    private final int lifetime;
    private final int delay;

    public FlameExplosionParticleOptions(
            float size,
            int lifetime,
            int delay
    ) {
        this.size = size;
        this.lifetime = lifetime;
        this.delay = delay;
    }

    public float getSize() {
        return size;
    }

    public int getLifetime() {
        return lifetime;
    }

    public int getDelay() {
        return delay;
    }

    @Override
    public ParticleType<?> getType() {
        return CWParticles.FLAME_EXPLOSION.get();
    }
}