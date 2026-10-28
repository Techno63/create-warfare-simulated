package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.server.*;
import com.julian.createwarfare.effects.server.particles.GroundExplosionParticleEffect;
import com.julian.createwarfare.explosions.post.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = CreateWarfare.MODID)
public class CWServerTicker {

    private static final List<ScheduledTask> SCHEDULED_TASKS = new ArrayList<>();

    public static void schedule(
            int delay,
            Runnable task
    ) {
        if (delay <= 0) {
            task.run();
            return;
        }

        SCHEDULED_TASKS.add(
                new ScheduledTask(
                        delay,
                        task
                )
        );
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        for (int i = SCHEDULED_TASKS.size() - 1; i >= 0; i--) {
            ScheduledTask scheduledTask = SCHEDULED_TASKS.get(i);

            scheduledTask.ticks--;

            if (scheduledTask.ticks <= 0) {
                scheduledTask.task.run();
                SCHEDULED_TASKS.remove(i);
            }
        }

        PressureWavePost.tick();
        SoundWaveEffect.tick();
        FragmentationPost.tick();
        IncinerationWavePost.tick();
        GroundExplosionParticleEffect.tick();

    }

    private static class ScheduledTask {

        int ticks;
        final Runnable task;

        ScheduledTask(
                int ticks,
                Runnable task
        ) {
            this.ticks = ticks;
            this.task = task;
        }
    }
}