package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.blocks.entities.*;
import com.julian.createwarfare.blocks.renderers.CentrifugeRenderer;
import com.julian.createwarfare.blocks.renderers.ShortRangeBallisticMissileRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.neoforged.bus.api.IEventBus;

public class CWBlockEntities {

    public static final CreateRegistrate REGISTRATE =
            CreateWarfare.REGISTRATE;

    public static final BlockEntityEntry<CentrifugeBlockEntity> CENTRIFUGE = REGISTRATE
                    .blockEntity(
                            "centrifuge",
                            CentrifugeBlockEntity::new
                    )
                    .visual(
                            () -> SingleAxisRotatingVisual::shaft,
                            false
                    )
                    .validBlocks(CWBlocks.CENTRIFUGE)
                    .renderer(() -> CentrifugeRenderer::new)
                    .register();

    //------------------------------------------------------------------------------------------------------------------------------------------------------

    public static final BlockEntityEntry<ShortRangeBallisticMissileBlockEntity> SHORT_RANGE_BALLISTIC_MISSILE = REGISTRATE
            .blockEntity(
                    "short_range_ballistic_missile",
                    ShortRangeBallisticMissileBlockEntity::new
            )
            .validBlock(CWBlocks.SHORT_RANGE_BALLISTIC_MISSILE)
            .renderer(() -> ShortRangeBallisticMissileRenderer::new)
            .register();

    //------------------------------------------------------------------------------------------------------------------------------------------------------

    public static final BlockEntityEntry<HEWarheadBlockEntity> HE_WARHEAD = REGISTRATE
            .blockEntity(
                    "he_warhead",
                    HEWarheadBlockEntity::new
            )
            .validBlock(CWBlocks.HE_WARHEAD)
            .register();

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(
                CentrifugeBlockEntity::registerCapabilities
        );
    }
}