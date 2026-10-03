package com.julian.createwarfare.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CWNetwork {

    private CWNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CWNetwork::registerPayloads);
    }

    private static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {
        event.registrar("1")
                .playToClient(
                        WavePayload.TYPE,
                        WavePayload.STREAM_CODEC,
                        WavePayload::handle
                )
                .playToClient(
                        MushroomCapPayload.TYPE,
                        MushroomCapPayload.STREAM_CODEC,
                        MushroomCapPayload::handle
                ).playToClient(
                        ClientEffectPayload.TYPE,
                        ClientEffectPayload.STREAM_CODEC,
                        (payload, context) ->
                                context.enqueueWork(() ->
                                        ClientEffectPayloadHandler.handle(
                                                payload
                                        )
                                )
                );
    }
}