package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.blocks.*;
import com.julian.createwarfare.blocks.ShortRangeBallisticMissileBlock;
import com.julian.createwarfare.items.GeoBlockItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;
import static net.neoforged.neoforge.client.model.generators.ConfiguredModel.builder;

public class CWBlocks {
    public static final CreateRegistrate REGISTRATE = CreateWarfare.REGISTRATE;

    public static final BlockEntry<TestBlock> TEST_BLOCK = REGISTRATE
            .block("test_block", TestBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .simpleItem()
            .lang("Test Block")
            .register();

    //------------------------------------------------------------------------------------------------------------------------------------------------------

    public static final BlockEntry<ShortRangeBallisticMissileBlock> SHORT_RANGE_BALLISTIC_MISSILE = REGISTRATE
            .block("short_range_ballistic_missile", ShortRangeBallisticMissileBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties
                    .noOcclusion()
                    .forceSolidOff()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
            )
            .item((block, properties) -> new GeoBlockItem(
                    block,
                    properties,
                    "geo/short_range_ballistic_missile.geo.json",
                    "textures/block/short_range_ballistic_missile.png",
                    1.0f,
                    0
            ))
            .model((context, provider) ->
                    provider.withExistingParent(
                            context.getName(),
                            provider.modLoc("block/short_range_ballistic_missile")
                    )
            )
            .build()
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .blockstate((ctx, prov) ->
                    prov.getVariantBuilder(ctx.getEntry()).forAllStates(state ->
                            builder()
                                    .modelFile(prov.models().getExistingFile(
                                            prov.modLoc("block/short_range_ballistic_missile")
                                    ))
                                    .build()
                    )
            )
            .lang("Short-Range Ballistic Missile")
            .register();

    //------------------------------------------------------------------------------------------------------------------------------------------------------

    public static final BlockEntry<HEWarheadBlock> HE_WARHEAD = REGISTRATE
            .block("he_warhead", HEWarheadBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties
                    .noOcclusion()
                    .forceSolidOff()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
            )
            .blockstate((context, provider) ->
                    provider.simpleBlock(
                            context.getEntry(),
                            provider.models().cubeAll(
                                    context.getName(),
                                    provider.modLoc("block/he_warhead")
                            )
                    )
            )
            .simpleItem()
            .lang("HE Warhead")
            .register();

    //------------------------------------------------------------------------------------------------------------------------------------------------------

    public static final BlockEntry<CentrifugeBlock> CENTRIFUGE = REGISTRATE
            .block("centrifuge", CentrifugeBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(properties -> properties
                    .noOcclusion()
                    .forceSolidOff()
            )
            .blockstate((context, provider) ->
                    provider.getVariantBuilder(context.getEntry()).forAllStates(state ->
                            builder()
                                    .modelFile(provider.models().getExistingFile(
                                            ResourceLocation.fromNamespaceAndPath("minecraft", "block/air")
                                    ))
                                    .build()
                    )
            )
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .item((block, properties) -> new GeoBlockItem(
                    block,
                    properties,
                    "geo/centrifuge.geo.json",
                    "textures/block/centrifuge.png",
                    0.6f,
                    16
            ))
            .model((context, provider) -> {})
            .build()
            .lang("Centrifuge")
            .register();

    //------------------------------------------------------------------------------------------------------------------------------------------------------

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

    //------------------------------------------------------------------------------------------------------------------------------------------------------

    public static void register() {

    }

}
