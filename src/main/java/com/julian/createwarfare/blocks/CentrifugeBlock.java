package com.julian.createwarfare.blocks;

import com.julian.createwarfare.registry.CWBlockEntities;
import com.julian.createwarfare.registry.CWBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;

public class CentrifugeBlock extends KineticBlock implements IBE<CentrifugeBlockEntity> {

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public CentrifugeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader world,
            BlockPos pos,
            BlockState state,
            Direction face
    ) {
        return face.getAxis() == Direction.Axis.Y;
    }

    @Override
    public Class<CentrifugeBlockEntity> getBlockEntityClass() {
        return CentrifugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CentrifugeBlockEntity> getBlockEntityType() {
        return CWBlockEntities.CENTRIFUGE.get();
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide
                && !level.getBlockState(pos.below()).is(CWBlocks.CENTRIFUGE.get())) {

            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof CentrifugeBlockEntity centrifuge) {

                dropInventory(
                        level,
                        pos,
                        centrifuge.getInputInventory()
                );

                dropInventory(
                        level,
                        pos,
                        centrifuge.getPrimaryInventory()
                );

                dropInventory(
                        level,
                        pos,
                        centrifuge.getSecondaryInventory()
                );
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    private void dropInventory(
            Level level,
            BlockPos pos,
            net.neoforged.neoforge.items.ItemStackHandler inventory
    ) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);

            if (!stack.isEmpty()) {
                popResource(
                        level,
                        pos,
                        stack.copy()
                );
            }
        }
    }
}