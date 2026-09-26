package com.julian.createwarfare.effects.client;

import com.julian.createwarfare.CreateWarfare;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = CreateWarfare.MODID, value = Dist.CLIENT)
public class ScreenShakeHandler {

    private static float intensity = 0.0f;
    private static float remaining = 0.0f;
    private static float duration = 0.0f;

    public static void shake(float strength, int ticks) {
        intensity = Math.max(intensity, strength);
        remaining = Math.max(remaining, ticks);
        duration = Math.max(duration, ticks);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remaining <= 0.0f) {
            return;
        }

        remaining--;

        if (remaining <= 0.0f) {
            intensity = 0.0f;
            duration = 0.0f;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (remaining <= 0.0f) {
            return;
        }

        float progress = remaining / duration;
        float currentIntensity = intensity * progress;

        event.setYaw(event.getYaw() +
                (float) (Math.random() - 0.5) * 2.0f * currentIntensity);

        event.setPitch(event.getPitch() +
                (float) (Math.random() - 0.5) * 2.0f * currentIntensity);

        event.setRoll(event.getRoll() +
                (float) (Math.random() - 0.5) * 2.0f * currentIntensity);
    }
}
