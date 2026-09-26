package com.julian.createwarfare.effects.particles.explosions;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class FlameExplosionParticleType extends ParticleType<FlameExplosionParticleOptions> {

    public FlameExplosionParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<FlameExplosionParticleOptions> codec() {
        return MapCodec.unit(
                new FlameExplosionParticleOptions(
                        8.0f,
                        14,
                        0
                )
        );
    }
    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, FlameExplosionParticleOptions> streamCodec() {
        return FlameExplosionParticleOptions.STREAM_CODEC;
    }
}