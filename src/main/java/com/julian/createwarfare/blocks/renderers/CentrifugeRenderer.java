package com.julian.createwarfare.blocks.renderers;

import com.julian.createwarfare.blocks.entities.CentrifugeBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;

public class CentrifugeRenderer extends GeoBlockModelRenderer<CentrifugeBlockEntity> {

    private final ShaftRenderer<CentrifugeBlockEntity> shaftRenderer;

    public CentrifugeRenderer(BlockEntityRendererProvider.Context context) {
        super(context, "geo/centrifuge.geo.json", "textures/block/centrifuge.png", false, 1);
        shaftRenderer = new ShaftRenderer<>(context);
    }

    @Override
    public void render(
            CentrifugeBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        shaftRenderer.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
