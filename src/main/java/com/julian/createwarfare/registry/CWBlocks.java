package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.blocks.CentrifugeBlock;
import com.julian.createwarfare.blocks.TestBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Blocks;

public class CWBlocks {
    public static final CreateRegistrate REGISTRATE = CreateWarfare.REGISTRATE;

    public static final BlockEntry<TestBlock> TEST_BLOCK = REGISTRATE
            .block("test_block", TestBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .lang("Test Block")
            .register();

    public static final BlockEntry<CentrifugeBlock> CENTRIFUGE = REGISTRATE
            .block("centrifuge", CentrifugeBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties
                    .noOcclusion()
                    .forceSolidOff()
            )
            .blockstate((context, provider) ->
                    provider.simpleBlock(
                            context.getEntry(),
                            provider.models().getExistingFile(
                                    provider.modLoc("block/centrifuge")
                            )
                    )
            )
            .simpleItem()
            .lang("Centrifuge")
            .register();


    public static void register() {}
}