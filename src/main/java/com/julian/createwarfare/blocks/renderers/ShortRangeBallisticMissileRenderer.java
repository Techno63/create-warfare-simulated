package com.julian.createwarfare.blocks.renderers;

import com.julian.createwarfare.blocks.entities.ShortRangeBallisticMissileBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class ShortRangeBallisticMissileRenderer extends GeoBlockModelRenderer<ShortRangeBallisticMissileBlockEntity> {

    public ShortRangeBallisticMissileRenderer(BlockEntityRendererProvider.Context context) {
        super(
                context,
                "geo/short_range_ballistic_missile.geo.json",
                "textures/block/short_range_ballistic_missile.png",
                false,
                ShortRangeBallisticMissileBlockEntity.HEIGHT
        );
    }

    @Override
    public boolean shouldRenderOffScreen(ShortRangeBallisticMissileBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 512;
    }
}