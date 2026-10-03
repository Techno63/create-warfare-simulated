package com.julian.createwarfare.network;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.client.engines.MushroomCapEngine;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MushroomCapPayload(
        double x,
        double y,
        double z,
        float baseSmokeRadius,
        float baseSmokeHeight,
        float pillarHeight,
        float pillarRadius,
        float capHeight,
        float capRadius,
        int lifetime
) implements CustomPacketPayload {

    public static final Type<MushroomCapPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateWarfare.MODID,
                            "mushroom_cap"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, MushroomCapPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public MushroomCapPayload decode(
                        RegistryFriendlyByteBuf buffer
                ) {
                    return new MushroomCapPayload(
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readVarInt()
                    );
                }

                @Override
                public void encode(
                        RegistryFriendlyByteBuf buffer,
                        MushroomCapPayload payload
                ) {
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeFloat(payload.baseSmokeRadius());
                    buffer.writeFloat(payload.baseSmokeHeight());
                    buffer.writeFloat(payload.pillarHeight());
                    buffer.writeFloat(payload.pillarRadius());
                    buffer.writeFloat(payload.capHeight());
                    buffer.writeFloat(payload.capRadius());
                    buffer.writeVarInt(payload.lifetime());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            MushroomCapPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() ->
                MushroomCapEngine.spawn(
                        new Vec3(
                                payload.x(),
                                payload.y(),
                                payload.z()
                        ),
                        payload.baseSmokeRadius(),
                        payload.baseSmokeHeight(),
                        payload.pillarHeight(),
                        payload.pillarRadius(),
                        payload.capHeight(),
                        payload.capRadius(),
                        payload.lifetime()
                )
        );
    }
}