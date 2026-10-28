package com.julian.createwarfare.network;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.client.engines.TracerEngine;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TracerPayload(
        double x,
        double y,
        double z,
        float width,
        float speed,
        int tracerId
) implements CustomPacketPayload {

    public static final Type<TracerPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateWarfare.MODID,
                            "tracer"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, TracerPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public TracerPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new TracerPayload(
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readInt()
                    );
                }

                @Override
                public void encode(
                        RegistryFriendlyByteBuf buffer,
                        TracerPayload payload
                ) {
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeFloat(payload.width());
                    buffer.writeFloat(payload.speed());
                    buffer.writeInt(payload.tracerId());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            TracerPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> TracerEngine.accept(
                new Vec3(
                        payload.x(),
                        payload.y(),
                        payload.z()
                ),
                payload.width(),
                payload.speed(),
                payload.tracerId()
        ));
    }
}