package com.julian.createwarfare.commands;

import com.julian.createwarfare.explosions.post.radiation.RadiationChunks;
import com.julian.createwarfare.explosions.post.radiation.RadiationExposure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class RadiationCommand {

    public static void clear(
            CommandSourceStack source
    ) {
        ServerLevel level =
                source.getLevel();

        RadiationChunks.clear(level);
        RadiationExposure.clear(level);

        source.sendSuccess(
                () -> Component.literal(
                        "Cleared radiation in this dimension."
                ),
                true
        );
    }
}