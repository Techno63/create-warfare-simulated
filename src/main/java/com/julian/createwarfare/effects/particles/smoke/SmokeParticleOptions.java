package com.julian.createwarfare.effects.particles.smoke;

import com.julian.createwarfare.registry.CWParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class SmokeParticleOptions implements ParticleOptions {

    public static final StreamCodec<RegistryFriendlyByteBuf, SmokeParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    SmokeParticleOptions::getRiseSpeed,
                    ByteBufCodecs.INT,
                    SmokeParticleOptions::getLifetime,
                    SmokeParticleOptions::new
            );

    private final float riseSpeed;
    private final int lifetime;

    public SmokeParticleOptions(float riseSpeed, int lifetime) {
        this.riseSpeed = riseSpeed;
        this.lifetime = lifetime;
    }

    public float getRiseSpeed() {
        return riseSpeed;
    }

    public int getLifetime() {
        return lifetime;
    }

    @Override
    public ParticleType<?> getType() {
        return CWParticles.SMOKE.get();
    }
}