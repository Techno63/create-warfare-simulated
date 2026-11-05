package com.julian.createwarfare.network;

import com.julian.createwarfare.gui.CWGuiBuilder;
import com.julian.createwarfare.gui.CWGuiSyncPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CWNetwork {

    private CWNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CWNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(
                        WavePayload.TYPE,
                        WavePayload.STREAM_CODEC,
                        WavePayload::handle
                )
                .playToClient(
                        TracerPayload.TYPE,
                        TracerPayload.STREAM_CODEC,
                        TracerPayload::handle
                )
                .playToClient(
                        ClientEffectPayload.TYPE,
                        ClientEffectPayload.STREAM_CODEC,
                        ClientEffectPayload::handle
                )
                .playToServer(
                        CWGuiSyncPayload.TYPE,
                        CWGuiSyncPayload.STREAM_CODEC,
                        CWGuiSyncPayload::handle
                )
                .playToClient(
                        CWGuiBuilder.OpenPayload.TYPE,
                        CWGuiBuilder.OpenPayload.STREAM_CODEC,
                        CWGuiBuilder.OpenPayload::handle
                );
    }
}