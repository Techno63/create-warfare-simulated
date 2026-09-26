package com.julian.createwarfare.effects.client;

import com.julian.createwarfare.CreateWarfare;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = CreateWarfare.MODID, value = Dist.CLIENT)
public class ScreenVignetteHandler {

    private static final ResourceLocation VIGNETTE =
            ResourceLocation.fromNamespaceAndPath(
                    CreateWarfare.MODID,
                    "textures/gui/vignette.png"
            );

    private static float remaining = 0.0f;
    private static float duration = 0.0f;
    private static float intensity = 0.0f;

    public static void vignette(float strength, int ticks) {
        intensity = Math.max(intensity, strength);
        remaining = Math.max(remaining, (float) ticks);
        duration = Math.max(duration, (float) ticks);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remaining > 0.0f) {
            remaining -= 1.0f;

            if (remaining < 0.0f) {
                remaining = 0.0f;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (remaining <= 0.0f) {
            intensity = 0.0f;
            duration = 0.0f;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();

        float progress = remaining / duration;
        float alpha;

        if (progress > 0.5f) {
            alpha = intensity;
        } else {
            alpha = intensity * (progress / 0.5f);
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        graphics.blit(
                VIGNETTE,
                0,
                0,
                0,
                0,
                width,
                height,
                width,
                height
        );

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
