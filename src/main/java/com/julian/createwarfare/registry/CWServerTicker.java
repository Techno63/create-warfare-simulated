package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.server.PressureWaveEffect;
import com.julian.createwarfare.effects.server.SoundWaveEffect;
import com.julian.createwarfare.explosions.post.*;
import com.julian.createwarfare.explosions.post.radiation.RadiationChunks;
import com.julian.createwarfare.explosions.post.radiation.RadiationExposure;
import com.julian.createwarfare.items.GeigerCounterItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

        PressureWaveEffect.tick();
        SoundWaveEffect.tick();

        ShockwavePost.tick();
        SmokeRingPost.tick();

        for (ServerLevel level :
                event.getServer().getAllLevels()) {

            RadiationChunks.tick(level);

            for (ServerPlayer player : level.players()) {
                RadiationExposure.tick(player);
                GeigerCounterItem.tick(player);
            }
        }
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