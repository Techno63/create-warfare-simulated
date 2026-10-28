package com.julian.createwarfare.effects.particles.smoke_explosion;

import com.julian.createwarfare.registry.CWParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SmokeExplosionParticleOptions(float size, int lifetime)
        implements ParticleOptions {

    public static final MapCodec<SmokeExplosionParticleOptions> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance -> instance.group(
                            Codec.FLOAT
                                    .fieldOf("size")
                                    .forGetter(SmokeExplosionParticleOptions::size),
                            Codec.INT
                                    .fieldOf("lifetime")
                                    .forGetter(SmokeExplosionParticleOptions::lifetime)
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
                    ByteBufCodecs.VAR_INT,
                    SmokeExplosionParticleOptions::lifetime,
                    SmokeExplosionParticleOptions::new
            );

    public SmokeExplosionParticleOptions {
        size = Float.isFinite(size) ? Math.max(0.1f, size) : 0.1f;
        lifetime = Math.max(12, lifetime);
    }

    public SmokeExplosionParticleOptions(float size) {
        this(size, 70);
    }

    @Override
    public ParticleType<?> getType() {
        return CWParticles.SMOKE_EXPLOSION.get();
    }
}