package com.julian.createwarfare.effects.particles.explosions;

import com.julian.createwarfare.registry.CWParticles;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class ExplosionParticleOptions implements ParticleOptions {

    public static final MapCodec<ExplosionParticleOptions> CODEC = MapCodec.unit(
            new ExplosionParticleOptions(1.0f, 20)
    );

    public static final StreamCodec<ByteBuf, ExplosionParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    options -> options.size,
                    ByteBufCodecs.VAR_INT,
                    options -> options.lifetime,
                    ExplosionParticleOptions::new
            );

    private final float size;
    private final int lifetime;

    public ExplosionParticleOptions(float size, int lifetime) {
        this.size = Math.max(size, 0.01f);
        this.lifetime = Math.max(lifetime, 1);
    }

    public float getSize() {
        return size;
    }

    public int getLifetime() {
        return lifetime;
    }

    @Override
    public ParticleType<?> getType() {
        return CWParticles.EXPLOSION.get();
    }
}