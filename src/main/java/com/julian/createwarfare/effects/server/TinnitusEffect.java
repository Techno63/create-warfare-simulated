package com.julian.createwarfare.effects.server;

import com.julian.createwarfare.effects.sounds.TinnitusHandler;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class TinnitusEffect {

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float radius,
            int duration,
            boolean falloff
    ) {
        double radiusSqr =
                radius * radius;

        Position rawCenter =
                pos.getCenter();

        Vec3 center =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level,
                        rawCenter
                );

        for (ServerPlayer player :
                level.players()) {

            double distanceSqr =
                    player.distanceToSqr(
                            center.x,
                            center.y,
                            center.z
                    );

            if (distanceSqr > radiusSqr) {
                continue;
            }

            int scaledDuration =
                    duration;

            if (falloff) {
                double distance =
                        Math.sqrt(distanceSqr);

                double falloffAmount =
                        1.0 -
                                (distance / radius) *
                                        0.75;

                scaledDuration =
                        Math.max(
                                1,
                                (int) (
                                        duration *
                                                falloffAmount
                                )
                        );
            }

            TinnitusHandler.start(
                    scaledDuration
            );
        }
    }
}