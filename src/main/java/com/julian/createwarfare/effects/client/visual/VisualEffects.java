package com.julian.createwarfare.effects.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

public interface VisualEffects {
    void tick();

    void render(PoseStack poseStack, Camera camera, MultiBufferSource.BufferSource bufferSource, float partialTick);

    boolean isAlive();
}
