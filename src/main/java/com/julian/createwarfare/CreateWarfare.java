package com.julian.createwarfare;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(CreateWarfare.MODID)
public class CreateWarfare {
    public static final String MODID = "createwarfare";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateWarfare(IEventBus modEventBus, ModContainer modContainer) {
    }
}
