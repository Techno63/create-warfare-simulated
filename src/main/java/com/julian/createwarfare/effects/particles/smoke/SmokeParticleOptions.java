package com.julian.createwarfare.effects.particles.smoke;

import com.julian.createwarfare.registry.CWParticles;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class SmokeParticleOptions implements ParticleOptions {

    public static final MapCodec<SmokeParticleOptions> CODEC = MapCodec.unit(
            new SmokeParticleOptions(1.0f, 20)
    );

    public static final StreamCodec<ByteBuf, SmokeParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    options -> options.size,
                    ByteBufCodecs.VAR_INT,
                    options -> options.duration,
                    SmokeParticleOptions::new
            );

    private final float size;
    private final int duration;

    public SmokeParticleOptions(float size, int duration) {
        this.size = Math.max(size, 0.01f);
        this.duration = Math.max(duration, 1);
    }

    public float getSize() {
        return size;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public ParticleType<?> getType() {
        return CWParticles.SMOKE.get();
    }
}