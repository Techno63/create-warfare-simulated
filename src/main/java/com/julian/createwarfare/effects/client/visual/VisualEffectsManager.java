package com.julian.createwarfare.effects.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VisualEffectsManager {
    private final List<VisualEffects> effects = new ArrayList<>();
    private final List<VisualEffects> pendingEffects = new ArrayList<>();
    private boolean iterating;

    public void add(VisualEffects effect) {
        Objects.requireNonNull(effect, "effect");
        if (iterating) pendingEffects.add(effect); else effects.add(effect);
    }

    public void tick() {
        iterating = true;
        try {
            for (int index = effects.size() - 1; index >= 0; index--) {
                VisualEffects effect = effects.get(index);
                effect.tick();
                if (!effect.isAlive()) effects.remove(index);
            }
        } finally {
            iterating = false;
            flushPendingEffects();
        }
    }

    public void render(PoseStack poseStack, Camera camera, MultiBufferSource.BufferSource bufferSource, float partialTick) {
        iterating = true;
        try {
            for (VisualEffects effect : effects) effect.render(poseStack, camera, bufferSource, partialTick);
        } finally {
            iterating = false;
            flushPendingEffects();
        }
    }

    public void clear() { effects.clear(); pendingEffects.clear(); }
    public boolean isEmpty() { return effects.isEmpty() && pendingEffects.isEmpty(); }

    private void flushPendingEffects() {
        if (!pendingEffects.isEmpty()) {
            effects.addAll(pendingEffects);
            pendingEffects.clear();
        }
    }
}
