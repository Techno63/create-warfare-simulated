package com.julian.createwarfare.network;

import com.julian.createwarfare.CreateWarfare;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientEffectPayload(
        int effect,
        double x,
        double y,
        double z,
        float value1,
        float value2,
        int value3,
        int value4
) implements CustomPacketPayload {

    public static final int GLOW = 0;
    public static final int FLASH = 1;
    public static final int BLUR = 2;
    public static final int SHAKE = 3;
    public static final int VIGNETTE = 4;
    public static final int HEARTBEAT = 5;
    public static final int TINNITUS = 6;

    public static final Type<ClientEffectPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateWarfare.MODID,
                            "client_effect"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientEffectPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ClientEffectPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new ClientEffectPayload(
                            buffer.readInt(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readInt(),
                            buffer.readInt()
                    );
                }

                @Override
                public void encode(
                        RegistryFriendlyByteBuf buffer,
                        ClientEffectPayload payload
                ) {
                    buffer.writeInt(payload.effect());
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeFloat(payload.value1());
                    buffer.writeFloat(payload.value2());
                    buffer.writeInt(payload.value3());
                    buffer.writeInt(payload.value4());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            ClientEffectPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> ClientEffectPayloadHandler.handle(payload)
        );
    }
}
