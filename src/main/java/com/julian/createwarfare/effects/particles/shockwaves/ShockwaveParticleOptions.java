package com.julian.createwarfare.effects.particles.shockwaves;

import com.julian.createwarfare.registry.CWParticles;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ShockwaveParticleOptions implements ParticleOptions {

    public static final MapCodec<ShockwaveParticleOptions> CODEC =
            MapCodec.unit(
                    new ShockwaveParticleOptions(
                            20.0f,
                            1.0f,
                            20
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ShockwaveParticleOptions> STREAM_CODEC =
            StreamCodec.of(
                    (buf, options) -> {
                        buf.writeFloat(options.radius);
                        buf.writeFloat(options.speed);
                        buf.writeInt(options.duration);
                    },
                    buf -> new ShockwaveParticleOptions(
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readInt()
                    )
            );

    private final float radius;
    private final float speed;
    private final int duration;

    public ShockwaveParticleOptions(
            float radius,
            float speed,
            int duration
    ) {
        this.radius = radius;
        this.speed = speed;
        this.duration = duration;
    }

    public float getRadius() {
        return radius;
    }

    public float getSpeed() {
        return speed;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public ShockwaveParticleType getType() {
        return (ShockwaveParticleType) CWParticles.SHOCKWAVE.get();
    }
}