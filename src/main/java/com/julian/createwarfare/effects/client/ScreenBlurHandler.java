package com.julian.createwarfare.effects.client;

import com.julian.createwarfare.CreateWarfare;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.io.IOException;

@EventBusSubscriber(modid = CreateWarfare.MODID, value = Dist.CLIENT)
public class ScreenBlurHandler {

    private static final ResourceLocation BLUR_SHADER =
            ResourceLocation.fromNamespaceAndPath(
                    "minecraft",
                    "shaders/post/blur.json"
            );

    private static PostChain blurChain;

    private static float intensity = 0.0f;
    private static float remaining = 0.0f;
    private static float duration = 0.0f;

    public static void blur(float strength, int ticks) {
        intensity = Math.max(intensity, strength);
        remaining = Math.max(remaining, (float) ticks);
        duration = Math.max(duration, (float) ticks);

        Minecraft.getInstance().execute(() -> {
            if (blurChain == null) {
                loadChain();
            }
        });
    }

    private static void loadChain() {
        Minecraft mc = Minecraft.getInstance();

        try {
            blurChain = new PostChain(
                    mc.getTextureManager(),
                    mc.getResourceManager(),
                    mc.getMainRenderTarget(),
                    BLUR_SHADER
            );

            blurChain.resize(
                    mc.getMainRenderTarget().width,
                    mc.getMainRenderTarget().height
            );
        } catch (IOException e) {
            e.printStackTrace();
            blurChain = null;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remaining <= 0.0f) {
            return;
        }

        remaining--;

        if (remaining <= 0.0f) {
            remaining = 0.0f;
            intensity = 0.0f;
            duration = 0.0f;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (remaining <= 0.0f || blurChain == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        float progress = remaining / duration;
        float currentIntensity = intensity * progress * progress;

        blurChain.resize(
                mc.getMainRenderTarget().width,
                mc.getMainRenderTarget().height
        );

        blurChain.setUniform("Radius", currentIntensity);

        blurChain.process(
                event.getPartialTick().getGameTimeDeltaPartialTick(true)
        );
    }
}
