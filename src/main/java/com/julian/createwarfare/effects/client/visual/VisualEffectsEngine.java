package com.julian.createwarfare.effects.client.visual;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Objects;

public final class VisualEffectsEngine {
    private static final VisualEffectsManager MANAGER = new VisualEffectsManager();
    private static boolean initialized;

    private VisualEffectsEngine() { }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        NeoForge.EVENT_BUS.addListener(VisualEffectsEngine::onClientTick);
        NeoForge.EVENT_BUS.addListener(VisualEffectsRenderer::onRenderLevel);
    }

    public static void add(VisualEffects effect) {
        initialize();
        MANAGER.add(Objects.requireNonNull(effect, "effect"));
    }

    public static void clear() { MANAGER.clear(); }
    static VisualEffectsManager manager() { return MANAGER; }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            MANAGER.clear();
            return;
        }
        MANAGER.tick();
    }
}
