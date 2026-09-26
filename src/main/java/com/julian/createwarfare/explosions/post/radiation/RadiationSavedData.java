package com.julian.createwarfare.explosions.post.radiation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RadiationSavedData extends SavedData {

    public static final String DATA_NAME =
            "createwarfare_radiation";

    public float getRadiationRate(
            ChunkPos pos
    ) {
        RadiationData data =
                chunks.get(pos);

        if (data == null) {
            return 0.0f;
        }

        return data.radiation;
    }

    public void setRadiationRate(
            ChunkPos pos,
            float radiation
    ) {
        if (radiation <= 0.0f) {
            chunks.remove(pos);
            setDirty();
            return;
        }

        RadiationData data =
                chunks.get(pos);

        if (data == null) {
            data = new RadiationData(
                    radiation,
                    0L
            );

            chunks.put(
                    pos,
                    data
            );
        } else {
            data.radiation =
                    radiation;
        }

        setDirty();
    }

    public void addRadiationRate(
            ChunkPos pos,
            float radiation
    ) {
        setRadiationRate(
                pos,
                getRadiationRate(pos) +
                        radiation
        );
    }

    public float getContamination(
            UUID uuid
    ) {
        return contamination.getOrDefault(
                uuid,
                0.0f
        );
    }

    public void setContamination(
            UUID uuid,
            float value
    ) {
        if (value <= 0.0f) {
            contamination.remove(uuid);
            setDirty();
            return;
        }

        contamination.put(
                uuid,
                value
        );

        setDirty();
    }

    public void addContamination(
            UUID uuid,
            float value
    ) {
        setContamination(
                uuid,
                getContamination(uuid) +
                        value
        );
    }

    public float getRads(
            UUID uuid
    ) {
        return rads.getOrDefault(
                uuid,
                0.0f
        );
    }

    public void setRads(
            UUID uuid,
            float value
    ) {
        if (value <= 0.0f) {
            rads.remove(uuid);
            setDirty();
            return;
        }

        rads.put(
                uuid,
                value
        );

        setDirty();
    }

    public void addRads(
            UUID uuid,
            float value
    ) {
        setRads(
                uuid,
                getRads(uuid) +
                        value
        );
    }

    public final Map<ChunkPos, RadiationData> chunks =
            new HashMap<>();

    public final Map<UUID, Float> contamination =
            new HashMap<>();

    public final Map<UUID, Float> rads =
            new HashMap<>();

    public static final Factory<RadiationSavedData> FACTORY =
            new Factory<>(
                    RadiationSavedData::new,
                    RadiationSavedData::load,
                    null
            );

    public static RadiationSavedData get(
            ServerLevel level
    ) {
        return level.getDataStorage()
                .computeIfAbsent(
                        FACTORY,
                        DATA_NAME
                );
    }

    public static RadiationSavedData load(
            CompoundTag tag,
            HolderLookup.Provider lookup
    ) {
        RadiationSavedData data =
                new RadiationSavedData();

        ListTag chunks =
                tag.getList(
                        "Chunks",
                        Tag.TAG_COMPOUND
                );

        for (int i = 0; i < chunks.size(); i++) {
            CompoundTag nbt =
                    chunks.getCompound(i);

            ChunkPos pos =
                    new ChunkPos(
                            nbt.getInt("X"),
                            nbt.getInt("Z")
                    );

            data.chunks.put(
                    pos,
                    new RadiationData(
                            nbt.getFloat(
                                    "Radiation"
                            ),
                            nbt.getLong(
                                    "StartTime"
                            )
                    )
            );
        }

        ListTag contamination =
                tag.getList(
                        "Contamination",
                        Tag.TAG_COMPOUND
                );

        for (
                int i = 0;
                i < contamination.size();
                i++
        ) {
            CompoundTag nbt =
                    contamination.getCompound(i);

            try {
                UUID uuid =
                        UUID.fromString(
                                nbt.getString(
                                        "UUID"
                                )
                        );

                data.contamination.put(
                        uuid,
                        nbt.getFloat(
                                "Value"
                        )
                );

            } catch (IllegalArgumentException ignored) {
            }
        }

        ListTag rads =
                tag.getList(
                        "Rads",
                        Tag.TAG_COMPOUND
                );

        for (int i = 0; i < rads.size(); i++) {
            CompoundTag nbt =
                    rads.getCompound(i);

            try {
                UUID uuid =
                        UUID.fromString(
                                nbt.getString(
                                        "UUID"
                                )
                        );

                data.rads.put(
                        uuid,
                        nbt.getFloat(
                                "Value"
                        )
                );

            } catch (IllegalArgumentException ignored) {
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider lookup
    ) {
        ListTag chunks =
                new ListTag();

        for (
                Map.Entry<ChunkPos, RadiationData> entry :
                this.chunks.entrySet()
        ) {
            CompoundTag nbt =
                    new CompoundTag();

            nbt.putInt(
                    "X",
                    entry.getKey().x
            );

            nbt.putInt(
                    "Z",
                    entry.getKey().z
            );

            nbt.putFloat(
                    "Radiation",
                    entry.getValue().radiation
            );

            nbt.putLong(
                    "StartTime",
                    entry.getValue().startTime
            );

            chunks.add(
                    nbt
            );
        }

        tag.put(
                "Chunks",
                chunks
        );

        ListTag contamination =
                new ListTag();

        for (
                Map.Entry<UUID, Float> entry :
                this.contamination.entrySet()
        ) {
            CompoundTag nbt =
                    new CompoundTag();

            nbt.putString(
                    "UUID",
                    entry.getKey().toString()
            );

            nbt.putFloat(
                    "Value",
                    entry.getValue()
            );

            contamination.add(
                    nbt
            );
        }

        tag.put(
                "Contamination",
                contamination
        );

        ListTag rads =
                new ListTag();

        for (
                Map.Entry<UUID, Float> entry :
                this.rads.entrySet()
        ) {
            CompoundTag nbt =
                    new CompoundTag();

            nbt.putString(
                    "UUID",
                    entry.getKey().toString()
            );

            nbt.putFloat(
                    "Value",
                    entry.getValue()
            );

            rads.add(
                    nbt
            );
        }

        tag.put(
                "Rads",
                rads
        );

        return tag;
    }

    public static class RadiationData {

        public float radiation;
        public long startTime;

        public RadiationData(
                float radiation,
                long startTime
        ) {
            this.radiation =
                    radiation;

            this.startTime =
                    startTime;
        }
    }
}