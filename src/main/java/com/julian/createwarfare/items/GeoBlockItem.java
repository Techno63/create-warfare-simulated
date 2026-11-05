package com.julian.createwarfare.items;

import com.julian.createwarfare.CreateWarfare;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class GeoBlockItem extends BlockItem implements GeoItem {

    private final String modelPath;
    private final String texturePath;
    private final float scale;
    private final float modelHeight;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GeoBlockItem(
            Block block,
            Item.Properties properties,
            String modelPath,
            String texturePath,
            float scale,
            float modelHeight
    ) {
        super(block, properties);
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.scale = scale;
        this.modelHeight = modelHeight;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<GeoBlockItem> renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new GeoItemRenderer<GeoBlockItem>(new GeoModel<>() {
                        @Override
                        public ResourceLocation getModelResource(GeoBlockItem item) {
                            return CreateWarfare.asResource(item.modelPath);
                        }

                        @Override
                        public ResourceLocation getTextureResource(GeoBlockItem item) {
                            return CreateWarfare.asResource(item.texturePath);
                        }

                        @Override
                        public ResourceLocation getAnimationResource(GeoBlockItem item) {
                            return CreateWarfare.asResource("animations/block_models.animation.json");
                        }
                    }) {
                        @Override
                        public void preRender(
                                PoseStack poseStack,
                                GeoBlockItem item,
                                BakedGeoModel model,
                                @Nullable MultiBufferSource bufferSource,
                                @Nullable VertexConsumer buffer,
                                boolean isReRender,
                                float partialTick,
                                int packedLight,
                                int packedOverlay,
                                int colour
                        ) {
                            super.preRender(
                                    poseStack,
                                    item,
                                    model,
                                    bufferSource,
                                    buffer,
                                    isReRender,
                                    partialTick,
                                    packedLight,
                                    packedOverlay,
                                    colour
                            );

                            if (!isReRender) {
                                poseStack.translate(0, -item.modelHeight / 32.0, 0);
                            }
                        }
                    }.withScale(scale);
                }

                return renderer;
            }
        });
    }
}
