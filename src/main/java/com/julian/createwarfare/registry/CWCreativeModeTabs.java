package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CWCreativeModeTabs {
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> BASE_TAB = CreateWarfare.REGISTRATE
            .defaultCreativeTab("base_tab", builder -> builder
                    .icon(() -> new ItemStack(CWBlocks.TEST_BLOCK.get()))
                    .title(Component.literal("Create: Warfare Simulated")))
            .register();

    public static void register() {}
}
