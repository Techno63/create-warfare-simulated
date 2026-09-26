package com.julian.createwarfare.effects.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class VisualEffectsRenderer {
    private VisualEffectsRenderer() { }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) return;

        Vec3 cameraPosition = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        try {
            VisualEffectsEngine.manager().render(poseStack, event.getCamera(), bufferSource,
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
            bufferSource.endBatch();
        } finally {
            poseStack.popPose();
        }
    }
}
