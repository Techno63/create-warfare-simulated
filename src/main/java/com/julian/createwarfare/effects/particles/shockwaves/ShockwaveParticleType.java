package com.julian.createwarfare.effects.particles.shockwaves;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ShockwaveParticleType extends ParticleType<ShockwaveParticleOptions> {

    public ShockwaveParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<ShockwaveParticleOptions> codec() {
        return ShockwaveParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ShockwaveParticleOptions> streamCodec() {
        return ShockwaveParticleOptions.STREAM_CODEC;
    }
}
