package com.julian.createwarfare.items;

import com.julian.createwarfare.explosions.post.radiation.RadiationExposure;
import com.julian.createwarfare.registry.CWSoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class GeigerCounterItem extends Item {

    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    public static void tick(ServerPlayer player) {
        ItemStack mainHand =
                player.getMainHandItem();

        ItemStack offHand =
                player.getOffhandItem();

        if (!(mainHand.getItem() instanceof GeigerCounterItem)
                && !(offHand.getItem() instanceof GeigerCounterItem)) {
            return;
        }

        float svPerHour =
                RadiationExposure.getRadiationRate(player);

        if (svPerHour <= 0.0f) {
            return;
        }

        int clicks = Math.max(2, (int) (svPerHour / 4));

        double chance = Math.min(1.0, 0.2 + 0.2 * Math.log1p(svPerHour));

        if (player.serverLevel().random.nextDouble() < chance) {

            for (int i=0; i<clicks; i++) {

                float pitch = 1.2f + player.serverLevel().random.nextFloat()* 0.8f;

                player.playNotifySound(
                        CWSoundEvents.GEIGER_COUNTER_CLICK.getMainEvent(),
                        SoundSource.PLAYERS,
                        1.0f,
                        pitch
                );
            }
        }
    }
}