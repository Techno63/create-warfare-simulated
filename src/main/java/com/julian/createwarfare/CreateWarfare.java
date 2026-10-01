package com.julian.createwarfare;

import com.julian.createwarfare.network.CWNetwork;
import com.julian.createwarfare.registry.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

@Mod(CreateWarfare.MODID)
public class CreateWarfare {

    public static final String MODID = "createwarfare";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);

    public CreateWarfare(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {
        REGISTRATE.registerEventListeners(modEventBus);

        CWCreativeModeTabs.register();
        CWSoundEvents.register();
        CWParticles.register();
        CWBlocks.register();
        CWBlockEntities.register(modEventBus);
        CWItems.register();
        CWRecipes.register();
        CWNetwork.register(modEventBus);

        NeoForge.EVENT_BUS.register(CWCommands.class);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                MODID,
                path
        );
    }
}