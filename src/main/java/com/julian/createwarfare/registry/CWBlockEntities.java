package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.blocks.CentrifugeBlockEntity;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.neoforged.bus.api.IEventBus;

public class CWBlockEntities {

    public static final CreateRegistrate REGISTRATE =
            CreateWarfare.REGISTRATE;

    public static final BlockEntityEntry<CentrifugeBlockEntity> CENTRIFUGE =
            REGISTRATE
                    .blockEntity(
                            "centrifuge",
                            CentrifugeBlockEntity::new
                    )
                    .visual(
                            () -> SingleAxisRotatingVisual::shaft,
                            false
                    )
                    .validBlocks(CWBlocks.CENTRIFUGE)
                    .renderer(() -> ShaftRenderer::new)
                    .register();

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(
                CentrifugeBlockEntity::registerCapabilities
        );
    }
}