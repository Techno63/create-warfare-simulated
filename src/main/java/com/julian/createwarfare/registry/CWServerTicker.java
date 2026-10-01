package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.effects.server.*;
import com.julian.createwarfare.explosions.post.radiation.RadiationExposure;
import com.julian.createwarfare.explosions.post.radiation.RadiationChunks;
import com.julian.createwarfare.explosions.post.*;
import com.julian.createwarfare.items.GeigerCounterItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CreateWarfare.MODID)
public class CWServerTicker {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        //Effects
        PressureWaveEffect.tick();
        SoundWaveEffect.tick();
        IncinerationWaveEffect.tick();

        //Post
        ShockwavePost.tick();
        MushroomCapPost.tick();
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
}
