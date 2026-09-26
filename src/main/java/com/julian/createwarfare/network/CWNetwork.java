package com.julian.createwarfare.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CWNetwork {

    private CWNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CWNetwork::registerPayloads);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                MushroomCapPayload.TYPE,
                MushroomCapPayload.STREAM_CODEC,
                MushroomCapPayload::handle
        );

        event.registrar("1").playToClient(
                FireballPayload.TYPE,
                FireballPayload.STREAM_CODEC,
                FireballPayload::handle
        );
    }
}
