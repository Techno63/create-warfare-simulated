package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.simibubi.create.AllSoundEvents.SoundEntry;
import com.simibubi.create.AllSoundEvents.SoundEntryBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public class CWSoundEvents {

    public static final SoundEntry HEARTBEAT = create("heartbeat")
            .noSubtitle()
            .category(SoundSource.PLAYERS)
            .build();

    public static final SoundEntry TINNITUS = create("tinnitus")
            .noSubtitle()
            .category(SoundSource.PLAYERS)
            .build();

    public static final SoundEntry GEIGER_COUNTER_CLICK = create("geiger_counter_click")
            .noSubtitle()
            .category(SoundSource.PLAYERS)
            .build();

    public static final SoundEntry EXPLOSION = create("explosion")
            .noSubtitle()
            .attenuationDistance(512)
            .category(SoundSource.PLAYERS)
            .build();

    private static SoundEntryBuilder create(String id) {
        return new SoundEntryBuilder(ResourceLocation.fromNamespaceAndPath(CreateWarfare.MODID, id));
    }

    public static void register() {}
}