package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.blocks.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.julian.createwarfare.blocks.TestBlock;


import net.minecraft.world.level.block.Blocks;

public class CWBlocks {

    public static final CreateRegistrate REGISTRATE = CreateWarfare.REGISTRATE;

    public static final BlockEntry<TestBlock> TEST_BLOCK = REGISTRATE
            .block("test_block", TestBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .lang("Test Block")
            .register();

    public static void register() {}
}