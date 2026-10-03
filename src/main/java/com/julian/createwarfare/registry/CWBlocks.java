package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.blocks.CentrifugeBlock;
import com.julian.createwarfare.blocks.TestBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

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

    public static final BlockEntry<Block> URANIUM_ORE = REGISTRATE
            .block("uranium_ore", Block::new)
            .initialProperties(() -> Blocks.STONE)
            .properties(p -> p
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()
            )
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .loot((lt, block) -> lt.dropOther(block, CWItems.NATURAL_URANIUM.get()))
            .simpleItem()
            .register();

    public static final BlockEntry<Block> DEEPSLATE_URANIUM_ORE = REGISTRATE
            .block("deepslate_uranium_ore", Block::new)
            .initialProperties(() -> Blocks.DEEPSLATE)
            .properties(p -> p
                    .strength(4.5f, 3.0f)
                    .requiresCorrectToolForDrops()
            )
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .loot((lt, block) -> lt.dropOther(block, CWItems.NATURAL_URANIUM.get()))
            .simpleItem()
            .register();


    public static void register() {}
}