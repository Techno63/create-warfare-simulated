package com.julian.createwarfare.network;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.client.engines.MushroomCapEngine;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MushroomCapPayload(
        double x,
        double y,
        double z,
        float height,
        float pillarRadius,
        float capRadius
) implements CustomPacketPayload {

    public static final Type<MushroomCapPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CreateWarfare.MODID,
                            "mushroom_cap"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            MushroomCapPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE,
                    MushroomCapPayload::x,
                    ByteBufCodecs.DOUBLE,
                    MushroomCapPayload::y,
                    ByteBufCodecs.DOUBLE,
                    MushroomCapPayload::z,
                    ByteBufCodecs.FLOAT,
                    MushroomCapPayload::height,
                    ByteBufCodecs.FLOAT,
                    MushroomCapPayload::pillarRadius,
                    ByteBufCodecs.FLOAT,
                    MushroomCapPayload::capRadius,
                    MushroomCapPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(
            MushroomCapPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            MushroomCapEngine.spawn(
                    new Vec3(
                            payload.x(),
                            payload.y(),
                            payload.z()
                    ),
                    payload.height(),
                    payload.pillarRadius(),
                    payload.capRadius()
            );
        });
    }
}