package com.julian.createwarfare.network;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.client.engines.WaveEngine;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WavePayload(
        double x,
        double y,
        double z,
        float speed,
        float maxRadius,
        int color,
        float transparency
) implements CustomPacketPayload {

    public static final Type<WavePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateWarfare.MODID,
                            "fireball"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            WavePayload
            > STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public WavePayload decode(
                        RegistryFriendlyByteBuf buffer
                ) {
                    return new WavePayload(
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readInt(),
                            buffer.readFloat()
                    );
                }

                @Override
                public void encode(
                        RegistryFriendlyByteBuf buffer,
                        WavePayload payload
                ) {
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeFloat(payload.speed());
                    buffer.writeFloat(payload.maxRadius());
                    buffer.writeInt(payload.color());
                    buffer.writeFloat(payload.transparency());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            WavePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            WaveEngine.spawn(
                    new Vec3(
                            payload.x(),
                            payload.y(),
                            payload.z()
                    ),
                    payload.speed(),
                    payload.maxRadius(),
                    payload.color(),
                    payload.transparency()
            );
        });
    }
}