package com.julian.createwarfare.effects.particles.smoke;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class SmokeParticleType extends ParticleType<SmokeParticleOptions> {

    public SmokeParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<SmokeParticleOptions> codec() {
        return MapCodec.unit(new SmokeParticleOptions(0.2f, 100));
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, SmokeParticleOptions> streamCodec() {
        return SmokeParticleOptions.STREAM_CODEC;
    }
}