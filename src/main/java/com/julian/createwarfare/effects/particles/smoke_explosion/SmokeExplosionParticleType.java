package com.julian.createwarfare.effects.particles.smoke_explosion;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class SmokeExplosionParticleType
        extends ParticleType<SmokeExplosionParticleOptions> {

    public SmokeExplosionParticleType() {
        super(false);
    }

    @Override
    public MapCodec<SmokeExplosionParticleOptions> codec() {
        return SmokeExplosionParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, SmokeExplosionParticleOptions> streamCodec() {
        return SmokeExplosionParticleOptions.STREAM_CODEC;
    }
}