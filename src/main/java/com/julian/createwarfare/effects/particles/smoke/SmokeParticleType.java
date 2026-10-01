package com.julian.createwarfare.effects.particles.smoke;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.mojang.serialization.MapCodec;

public class SmokeParticleType extends ParticleType<SmokeParticleOptions> {

    public SmokeParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<SmokeParticleOptions> codec() {
        return SmokeParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, SmokeParticleOptions> streamCodec() {
        return SmokeParticleOptions.STREAM_CODEC.cast();
    }
}