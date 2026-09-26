package com.julian.createwarfare.effects.sounds;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.registry.CWSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CreateWarfare.MODID, value = Dist.CLIENT)
public class TinnitusHandler {

    private static TinnitusSoundInstance currentSound;

    public static void start(int ticks) {
        stop();

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            return;
        }

        currentSound = new TinnitusSoundInstance(ticks);
        mc.getSoundManager().play(currentSound);
    }

    public static void stop() {
        if (currentSound != null) {
            Minecraft.getInstance().getSoundManager().stop(currentSound);
            currentSound = null;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (currentSound != null && currentSound.isStopped()) {
            currentSound = null;
        }
    }

    private static class TinnitusSoundInstance extends AbstractTickableSoundInstance {

        private final int duration;
        private int ticks;

        private TinnitusSoundInstance(int duration) {
            super(
                    CWSoundEvents.TINNITUS.getMainEvent(),
                    SoundSource.MASTER,
                    RandomSource.create()
            );

            this.duration = duration;
            this.ticks = 0;

            this.looping = false;
            this.relative = true;
            this.volume = 1.0f;
            this.pitch = 1.0f;
        }

        @Override
        public void tick() {
            ticks++;

            float fadeStart = duration * 0.25f;

            if (ticks <= fadeStart) {
                this.volume = 1.0f;
            } else {
                float fadeProgress = (ticks - fadeStart) / (duration * 0.75f);
                this.volume = 1.0f - fadeProgress * fadeProgress;
            }

            if (this.volume < 0.0f) {
                this.volume = 0.0f;
            }
        }

        @Override
        public boolean isStopped() {
            return ticks >= duration;
        }
    }
}