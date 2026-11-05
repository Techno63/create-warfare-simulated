package com.julian.createwarfare.blocks.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.julian.createwarfare.CreateWarfare;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GeoBlockModelRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {

    private final boolean alignVerticalModelToFacing;
    private final int length;

    public GeoBlockModelRenderer(
            BlockEntityRendererProvider.Context context,
            String modelPath,
            String texturePath,
            boolean alignVerticalModelToFacing,
            int length
    ) {
        super(new GeoModel<>() {
            @Override
                public ResourceLocation getModelResource(T animatable) {
                return CreateWarfare.asResource(modelPath);
            }

            @Override
                public ResourceLocation getTextureResource(T animatable) {
                return CreateWarfare.asResource(texturePath);
            }

            @Override
                public ResourceLocation getAnimationResource(T animatable) {
                    return CreateWarfare.asResource("animations/block_models.animation.json");
            }
        });
        this.alignVerticalModelToFacing = alignVerticalModelToFacing;
        this.length = length;
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        if (!alignVerticalModelToFacing) {
            super.rotateBlock(facing, poseStack);
            return;
        }

        if (facing.getAxis().isVertical()) {
            if (facing == Direction.DOWN) {
                poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.PI, 1, 0, 0)));
            }
            return;
        }

        Vector3f facingVector = new Vector3f(
                facing.getStepX(),
                facing.getStepY(),
                facing.getStepZ()
        );
        Quaternionf alignment = new Quaternionf().rotationTo(
                0,
                1,
                0,
                facingVector.x,
                facingVector.y,
                facingVector.z
        );
        Vector3f modelUp = new Vector3f(0, 0, 1).rotate(alignment);
        Vector3f worldUp = new Vector3f(0, 1, 0);
        float roll = (float) Math.atan2(
                facingVector.dot(new Vector3f(modelUp).cross(worldUp)),
                modelUp.dot(worldUp)
        );
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(
                roll,
                facingVector.x,
                facingVector.y,
                facingVector.z
        )).mul(alignment));
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        var pos = blockEntity.getBlockPos();
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.hasProperty(DirectionalBlock.FACING)
                ? state.getValue(DirectionalBlock.FACING)
                : Direction.UP;
        var end = pos.relative(facing, length - 1);
        return new AABB(
                Math.min(pos.getX(), end.getX()),
                Math.min(pos.getY(), end.getY()),
                Math.min(pos.getZ(), end.getZ()),
                Math.max(pos.getX(), end.getX()) + 1,
                Math.max(pos.getY(), end.getY()) + 1,
                Math.max(pos.getZ(), end.getZ()) + 1
        ).inflate(0.5);
    }
}
