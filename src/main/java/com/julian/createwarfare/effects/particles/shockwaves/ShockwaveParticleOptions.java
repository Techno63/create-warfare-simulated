package com.julian.createwarfare.effects.particles.shockwaves;

import com.julian.createwarfare.registry.CWParticles;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ShockwaveParticleOptions implements ParticleOptions {

    public static final MapCodec<ShockwaveParticleOptions> CODEC =
            MapCodec.unit(new ShockwaveParticleOptions(20.0F, 100));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShockwaveParticleOptions> STREAM_CODEC =
            StreamCodec.of(
                    (buf, options) -> {
                        buf.writeFloat(options.size);
                        buf.writeInt(options.duration);
                    },
                    buf -> new ShockwaveParticleOptions(
                            buf.readFloat(),
                            buf.readInt()
                    )
            );

    private final float size;
    private final int duration;

    public ShockwaveParticleOptions(float size, int duration) {
        this.size = size;
        this.duration = duration;
    }

    public float getSize() {
        return size;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public ShockwaveParticleType getType() {
        return (ShockwaveParticleType) CWParticles.SHOCKWAVE.get();
    }
}
