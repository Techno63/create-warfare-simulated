package com.julian.createwarfare.explosions.post.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class RadiationChunks {

    private static final double DECAY_DAYS =
            7.0;

    private static final double SPATIAL_FALLOFF =
            6.0;

    public static void addRadiation(
            ServerLevel level,
            BlockPos center,
            int radius,
            float peakSvPerHour
    ) {
        if (peakSvPerHour <= 0.0f) {
            return;
        }

        RadiationSavedData data =
                RadiationSavedData.get(level);

        ChunkPos centerChunk =
                new ChunkPos(center);

        long now =
                level.getGameTime();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                double distance =
                        Math.sqrt(
                                x * x +
                                        z * z
                        );

                if (distance > radius) {
                    continue;
                }

                double normalized =
                        radius == 0
                                ? 0.0
                                : distance / radius;

                double falloff =
                        normalized >= 1.0
                                ? 0.0
                                : Math.exp(
                                -SPATIAL_FALLOFF *
                                normalized
                        );

                ChunkPos chunkPos =
                        new ChunkPos(
                                centerChunk.x + x,
                                centerChunk.z + z
                        );

                float radiation =
                        (float)
                                (peakSvPerHour * falloff);

                if (radiation <= 0.0f) {
                    continue;
                }

                RadiationSavedData.RadiationData existing =
                        data.chunks.get(chunkPos);

                if (existing == null) {
                    data.chunks.put(
                            chunkPos,
                            new RadiationSavedData.RadiationData(
                                    radiation,
                                    now
                            )
                    );
                } else {
                    existing.radiation += radiation;
                    existing.startTime = now;
                }

                for (ServerPlayer player :
                        level.players()) {

                    ChunkPos playerChunk =
                            new ChunkPos(
                                    player.blockPosition()
                            );

                    if (!playerChunk.equals(chunkPos)) {
                        continue;
                    }

                    data.addContamination(
                            player.getUUID(),
                            radiation / 16.0f
                    );
                }

                data.setDirty();
            }
        }
    }

    public static void clear(
            ServerLevel level
    ) {
        RadiationSavedData data =
                RadiationSavedData.get(level);

        data.chunks.clear();
        data.setDirty();
    }

    public static void tick(
            ServerLevel level
    ) {
        decay(level);
    }

    public static void decay(
            ServerLevel level
    ) {
        RadiationSavedData data =
                RadiationSavedData.get(level);

        long now =
                level.getGameTime();

        for (var entry :
                data.chunks.entrySet()) {

            RadiationSavedData.RadiationData radiation =
                    entry.getValue();

            long elapsedTicks =
                    Math.max(
                            0L,
                            now - radiation.startTime
                    );

            if (elapsedTicks <= 0L) {
                continue;
            }

            double elapsedDays =
                    elapsedTicks /
                            (20.0 * 60.0 * 60.0 * 24.0);

            double decay =
                    Math.pow(
                            10.0,
                            -elapsedDays / DECAY_DAYS
                    );

            radiation.radiation =
                    (float)
                            (radiation.radiation * decay);

            radiation.startTime =
                    now;

            data.setDirty();
        }
    }
}