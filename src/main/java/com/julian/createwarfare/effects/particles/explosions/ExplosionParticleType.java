package com.julian.createwarfare.effects.particles.explosions;

import com.julian.createwarfare.effects.particles.explosions.ExplosionParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ExplosionParticleType extends ParticleType<ExplosionParticleOptions> {

    public ExplosionParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<ExplosionParticleOptions> codec() {
        return ExplosionParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ExplosionParticleOptions> streamCodec() {
        return ExplosionParticleOptions.STREAM_CODEC;
    }
}