package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.blocks.*;
import com.julian.createwarfare.blocks.entities.*;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
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
                    .renderer(() -> ShaftRenderer::new)
                    .register();

    public static final BlockEntityEntry<SmallBombBlockEntity> SMALL_BOMB = REGISTRATE
            .blockEntity(
                    "mk81",
                    SmallBombBlockEntity::new
            )
            .validBlocks(CWBlocks.SMALL_BOMB)
            .register();

    public static final BlockEntityEntry<MediumBombBlockEntity> MEDIUM_BOMB = REGISTRATE
            .blockEntity(
                    "mk82",
                    MediumBombBlockEntity::new
            )
            .validBlocks(CWBlocks.MEDIUM_BOMB)
            .register();

    public static final BlockEntityEntry<LargeBombBlockEntity> LARGE_BOMB = REGISTRATE
            .blockEntity(
                    "mk83",
                    LargeBombBlockEntity::new
            )
            .validBlocks(CWBlocks.LARGE_BOMB)
            .register();

    public static final BlockEntityEntry<AirToAirMissileBlockEntity> AIR_TO_AIR_MISSILE = REGISTRATE
            .blockEntity(
                    "air_to_air_missile",
                    AirToAirMissileBlockEntity::new
            )
            .validBlocks(CWBlocks.AIR_TO_AIR_MISSILE)
            .register();

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(
                CentrifugeBlockEntity::registerCapabilities
        );
    }
}