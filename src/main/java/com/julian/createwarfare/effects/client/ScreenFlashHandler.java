package com.julian.createwarfare.effects.client;

import com.julian.createwarfare.CreateWarfare;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = CreateWarfare.MODID, value = Dist.CLIENT)
public class ScreenFlashHandler {

    private static float intensity = 0.0f;
    private static float remaining = 0.0f;
    private static float duration = 0.0f;
    private static int color = 0xFFFFFF;

    public static void flash(
            float strength,
            int ticks,
            int hexColor
    ) {
        intensity = Math.max(intensity, strength);
        remaining = Math.max(remaining, ticks);
        duration = Math.max(duration, ticks);
        color = hexColor;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remaining > 0.0f) {
            remaining--;

            if (remaining <= 0.0f) {
                intensity = 0.0f;
                duration = 0.0f;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (remaining <= 0.0f) {
            return;
        }

        float half = duration / 2.0f;
        float currentAlpha;

        if (remaining > half) {
            currentAlpha = intensity;
        } else {
            float fadeProgress = remaining / half;
            currentAlpha = intensity * fadeProgress;
        }

        int alphaByte =
                (int) (currentAlpha * 255.0f) << 24;

        int argb =
                alphaByte | (color & 0xFFFFFF);

        GuiGraphics graphics =
                event.getGuiGraphics();

        Minecraft mc =
                Minecraft.getInstance();

        graphics.fill(
                0,
                0,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),
                argb
        );
    }
}
