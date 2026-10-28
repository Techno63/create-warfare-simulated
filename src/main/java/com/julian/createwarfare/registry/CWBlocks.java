package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.blocks.*;
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

    public static final BlockEntry<SmallBombBlock> SMALL_BOMB = REGISTRATE
            .block("small_bomb", SmallBombBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties
                    .noOcclusion()
                    .forceSolidOff()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
            )
            .simpleItem()
            .tag(BlockTags.NEEDS_STONE_TOOL)
            .blockstate((ctx, prov) ->
                    prov.directionalBlock(
                            ctx.getEntry(),
                            state -> prov.models().getExistingFile(prov.modLoc("block/small_bomb")),
                            180
                    )
            )
            .lang("Small Bomb")
            .register();

    public static final BlockEntry<MediumBombBlock> MEDIUM_BOMB = REGISTRATE
            .block("medium_bomb", MediumBombBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties
                    .noOcclusion()
                    .forceSolidOff()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
            )
            .simpleItem()
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .blockstate((ctx, prov) ->
                    prov.directionalBlock(
                            ctx.getEntry(),
                            state -> prov.models().getExistingFile(prov.modLoc("block/medium_bomb")),
                            180
                    )
            )
            .lang("Medium Bomb")
            .register();

    public static final BlockEntry<LargeBombBlock> LARGE_BOMB = REGISTRATE
            .block("large_bomb", LargeBombBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties
                    .noOcclusion()
                    .forceSolidOff()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
            )
            .simpleItem()
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .blockstate((ctx, prov) ->
                    prov.directionalBlock(
                            ctx.getEntry(),
                            state -> prov.models().getExistingFile(prov.modLoc("block/large_bomb")),
                            180
                    )
            )
            .lang("Large Bomb")
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
            .tag(BlockTags.NEEDS_IRON_TOOL)
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

    public static void register() {

    }

}
