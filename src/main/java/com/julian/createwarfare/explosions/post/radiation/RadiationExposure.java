package com.julian.createwarfare.explosions.post.radiation;

import com.julian.createwarfare.CreateWarfare;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.UUID;

import static java.lang.Math.ceil;

@EventBusSubscriber(modid = CreateWarfare.MODID)
public class RadiationExposure {

    //Tick
    public static void tick(ServerPlayer player) {

        applyRads(player);
        applyEffects(player);
        contaminateChunks(player);

    }

    public static void applyRads(ServerPlayer player) {

        float radsPerTick = getRadiationRate(player) / 10;

        addRads(player, radsPerTick);

        if (radsPerTick == 0) {
            addRads(player, -0.01f);
        }

        player.displayClientMessage(
                Component.literal("Rads: " + ceil(getRads(player)) + " rads" + " | Radiation Rate: " + getRadiationRate(player) + " Sv/h" + " | Contamination: " + getContamination(player) + " mSv/h"),
                true
        );
    }

    public static void applyEffects(ServerPlayer player) {

        float rads = getRads(player);

        if (rads > 25) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
        }

        if (rads > 75) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 0));
        }

        if (rads > 150) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 5));
        }

        if (rads > 200) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 0));
        }

        if (rads > 300) {
            player.setRemainingFireTicks(40);
        }

        if (rads > 500) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200000, 0));
        }
    }

    public static void contaminateChunks(ServerPlayer player) {

        setRadiationRate(player, Math.max(getContamination(player) / 1000, getRadiationRate(player)));

    }

    //Radiation Rate
    public static float setRadiationRate(ServerPlayer player, float rate) {
        RadiationSavedData data =
                RadiationSavedData.get(player.serverLevel());

        ChunkPos chunkPos =
                new ChunkPos(player.blockPosition());

        data.setRadiationRate(
                chunkPos,
                rate
        );

        return data.getRadiationRate(chunkPos);

    }

    public static float getRadiationRate(ServerPlayer player) {
        RadiationSavedData data =
                RadiationSavedData.get(player.serverLevel());

        ChunkPos chunkPos =
                new ChunkPos(player.blockPosition());

        return data.getRadiationRate(chunkPos);
    }

    //Rads
    public static float addRads(ServerPlayer player, float rads) {
        RadiationSavedData data =
                RadiationSavedData.get(player.serverLevel());

        UUID uuid =
                player.getUUID();

        data.addRads(
                uuid,
                rads
        );

        return data.getRads(uuid);
    }

    public static float getRads(ServerPlayer player) {
        RadiationSavedData data =
                RadiationSavedData.get(player.serverLevel());

        return data.getRads(player.getUUID());
    }

    //Contamination
    public static float getContamination(ServerPlayer player) {
        RadiationSavedData data =
                RadiationSavedData.get(player.serverLevel());

        return data.getContamination(player.getUUID());
    }

    public static void clear(ServerLevel level) {
        RadiationSavedData data =
                RadiationSavedData.get(level);

        data.rads.clear();
        data.contamination.clear();
        data.setDirty();
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        RadiationSavedData data =
                RadiationSavedData.get(player.serverLevel());

        data.setRads(
                player.getUUID(),
                0.0f
        );
    }
}