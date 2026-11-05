package com.julian.createwarfare.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.Map;

public record CWGuiSyncPayload(
        String guiId,
        String actionId,
        Map<String, Integer> values
) implements CustomPacketPayload {

    public static final Type<CWGuiSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    "createwarfare", "gui_sync"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, CWGuiSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeUtf(payload.guiId(), 128);
                        buffer.writeUtf(payload.actionId(), 128);
                        buffer.writeUtf(toJson(payload.values()), 4096);
                    },
                    buffer -> new CWGuiSyncPayload(
                            buffer.readUtf(128),
                            buffer.readUtf(128),
                            fromJson(buffer.readUtf(4096))
                    )
            );

    public static void handle(CWGuiSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CWGuiBuilder.handleSubmit(player, payload);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static String toJson(Map<String, Integer> values) {
        JsonObject object = new JsonObject();

        if (values != null) {
            values.forEach((key, value) -> {
                if (key != null && value != null) {
                    object.addProperty(key, value);
                }
            });
        }

        return object.toString();
    }

    private static Map<String, Integer> fromJson(String json) {
        Map<String, Integer> values = new LinkedHashMap<>();

        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();

            object.entrySet().forEach(entry -> {
                if (entry.getKey().length() <= 128
                        && entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    values.put(entry.getKey(), entry.getValue().getAsInt());
                }
            });
        } catch (Exception ignored) {
        }

        return values;
    }
}