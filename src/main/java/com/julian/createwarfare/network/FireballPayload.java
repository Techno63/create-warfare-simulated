package com.julian.createwarfare.network;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.client.engines.FireballEngine;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FireballPayload(
        double x,
        double y,
        double z,
        float speed,
        float maxRadius
) implements CustomPacketPayload {

    public static final Type<FireballPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateWarfare.MODID,
                            "fireball"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            FireballPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE,
                    FireballPayload::x,
                    ByteBufCodecs.DOUBLE,
                    FireballPayload::y,
                    ByteBufCodecs.DOUBLE,
                    FireballPayload::z,
                    ByteBufCodecs.FLOAT,
                    FireballPayload::speed,
                    ByteBufCodecs.FLOAT,
                    FireballPayload::maxRadius,
                    FireballPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            FireballPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            FireballEngine.spawn(
                    new Vec3(
                            payload.x(),
                            payload.y(),
                            payload.z()
                    ),
                    payload.speed(),
                    payload.maxRadius()
            );
        });
    }
}