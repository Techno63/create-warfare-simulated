package com.julian.createwarfare.effects.sounds;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.registry.CWSoundEvents;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CreateWarfare.MODID, value = Dist.CLIENT)
public class HeartbeatHandler {

    private static int timer = 0;
    private static int interval = 0;
    private static int cycles = 0;
    private static int totalCycles = 0;

    public static void start(int intervalTicks, int cycleCount) {
        interval = intervalTicks;
        cycles = cycleCount;
        totalCycles = cycleCount;
        timer = 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (cycles <= 0) {
            return;
        }

        if (timer > 0) {
            timer--;
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            return;
        }

        int elapsedCycles = totalCycles - cycles;
        float progress = (float) elapsedCycles / totalCycles;

        float volume;

        if (progress < 0.75f) {
            volume = 1.0f;
        } else {
            float fadeProgress = (progress - 0.75f) / 0.25f;
            volume = 1.0f - fadeProgress;
        }

        mc.player.playSound(
                CWSoundEvents.HEARTBEAT.getMainEvent(),
                volume,
                1.0f
        );

        cycles--;

        if (cycles > 0) {
            timer = interval;
        }
    }
}