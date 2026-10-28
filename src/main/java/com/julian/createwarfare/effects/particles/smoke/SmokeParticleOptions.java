package com.julian.createwarfare.effects.particles.smoke;

import com.julian.createwarfare.registry.CWParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record SmokeParticleOptions(
        float size,
        int fromColor,
        int toColor,
        boolean shrink,
        int lifetime,
        float motionX,
        float motionY,
        float motionZ,
        int transitionTime
) implements ParticleOptions {

    public static final MapCodec<SmokeParticleOptions> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            Codec.FLOAT.fieldOf("size").forGetter(SmokeParticleOptions::size),
                            Codec.INT.fieldOf("from_color").forGetter(SmokeParticleOptions::fromColor),
                            Codec.INT.fieldOf("to_color").forGetter(SmokeParticleOptions::toColor),
                            Codec.BOOL.fieldOf("shrink").forGetter(SmokeParticleOptions::shrink),
                            Codec.INT.fieldOf("lifetime").forGetter(SmokeParticleOptions::lifetime),
                            Codec.FLOAT.fieldOf("motion_x").forGetter(SmokeParticleOptions::motionX),
                            Codec.FLOAT.fieldOf("motion_y").forGetter(SmokeParticleOptions::motionY),
                            Codec.FLOAT.fieldOf("motion_z").forGetter(SmokeParticleOptions::motionZ),
                            Codec.INT.fieldOf("transition_time").forGetter(SmokeParticleOptions::transitionTime)
                    ).apply(instance, SmokeParticleOptions::new)
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, SmokeParticleOptions> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, options) -> {
                        buffer.writeFloat(options.size());
                        buffer.writeInt(options.fromColor());
                        buffer.writeInt(options.toColor());
                        buffer.writeBoolean(options.shrink());
                        buffer.writeVarInt(options.lifetime());
                        buffer.writeFloat(options.motionX());
                        buffer.writeFloat(options.motionY());
                        buffer.writeFloat(options.motionZ());
                        buffer.writeVarInt(options.transitionTime());
                    },
                    buffer -> new SmokeParticleOptions(
                            buffer.readFloat(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readBoolean(),
                            buffer.readVarInt(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readVarInt()
                    )
            );

    public SmokeParticleOptions {
        size = Float.isFinite(size) ? Math.max(0.1f, Math.min(size, 64.0f)) : 1.0f;
        fromColor &= 0xFFFFFF;
        toColor &= 0xFFFFFF;
        lifetime = Math.max(1, Math.min(lifetime, 12000));
        motionX = clampMotion(motionX);
        motionY = clampMotion(motionY);
        motionZ = clampMotion(motionZ);
        transitionTime = Math.max(0, Math.min(transitionTime, 12000));
    }

    public SmokeParticleOptions(
            float size,
            int fromColor,
            int toColor,
            boolean shrink,
            int lifetime
    ) {
        this(size, fromColor, toColor, shrink, lifetime, 0.0f, 0.0f, 0.0f, lifetime);
    }

    public SmokeParticleOptions(
            float size,
            int fromColor,
            int toColor,
            boolean shrink,
            int lifetime,
            Vec3 motion,
            int transitionTime
    ) {
        this(
                size,
                fromColor,
                toColor,
                shrink,
                lifetime,
                (float) motion.x,
                (float) motion.y,
                (float) motion.z,
                transitionTime
        );
    }

    private static float clampMotion(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }

        return Math.max(-10.0f, Math.min(value, 10.0f));
    }

    @Override
    public ParticleType<?> getType() {
        return CWParticles.SMOKE.get();
    }
}