package com.julian.createwarfare.effects.particles.explosions;

import com.julian.createwarfare.registry.CWParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SmokeExplosionParticleOptions(float size)
        implements ParticleOptions {

    public static final MapCodec<SmokeExplosionParticleOptions> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance -> instance.group(
                            Codec.FLOAT
                                    .fieldOf("size")
                                    .forGetter(
                                            SmokeExplosionParticleOptions::size
                                    )
                    ).apply(
                            instance,
                            SmokeExplosionParticleOptions::new
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SmokeExplosionParticleOptions
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    SmokeExplosionParticleOptions::size,
                    SmokeExplosionParticleOptions::new
            );

    @Override
    public ParticleType<?> getType() {
        return CWParticles.SMOKE_EXPLOSION.get();
    }
}