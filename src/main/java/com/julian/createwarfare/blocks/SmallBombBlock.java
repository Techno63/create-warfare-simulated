package com.julian.createwarfare.blocks;

import com.julian.createwarfare.blocks.entities.SmallBombBlockEntity;
import com.julian.createwarfare.registry.CWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SmallBombBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty UPPER = BooleanProperty.create("upper");

    private static final VoxelShape NS_SHAPE = Block.box(5, 5, 0, 11, 11, 16);
    private static final VoxelShape NS_HALF_NORTH_SHAPE = Block.box(5, 5, 8, 11, 11, 16);
    private static final VoxelShape NS_HALF_SOUTH_SHAPE = Block.box(5, 5, 0, 11, 11, 8);

    private static final VoxelShape EW_SHAPE = Block.box(0, 5, 5, 16, 11, 11);
    private static final VoxelShape EW_HALF_EAST_SHAPE = Block.box(0, 5, 5, 8, 11, 11);
    private static final VoxelShape EW_HALF_WEST_SHAPE = Block.box(8, 5, 5, 16, 11, 11);

    private static final VoxelShape UD_SHAPE = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape UD_HALF_UP_SHAPE = Block.box(5, 8, 5, 11, 16, 11);
    private static final VoxelShape UD_HALF_DOWN_SHAPE = Block.box(5, 0, 5, 11, 8, 11);

    public SmallBombBlock(BlockBehaviour.Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(POWERED, false)
                        .setValue(UPPER, false)
        );
    }

    public static BlockPos partnerPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);

        return state.getValue(UPPER)
                ? pos.relative(facing.getOpposite())
                : pos.relative(facing);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(UPPER)) {
            return null;
        }

        return new SmallBombBlockEntity(
                CWBlockEntities.SMALL_BOMB.get(),
                pos,
                state
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(UPPER)
                ? RenderShape.INVISIBLE
                : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        BlockPos pos = context.getClickedPos();
        BlockPos partner = pos.relative(facing);
        Level level = context.getLevel();

        if (
                level.isOutsideBuildHeight(partner)
                        || !level.getBlockState(partner).canBeReplaced(context)
        ) {
            return null;
        }

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(UPPER, false);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        level.setBlock(
                partnerPos(pos, state),
                state.setValue(UPPER, true),
                3
        );
    }

    private static VoxelShape shapeFor(BlockState state) {
        Direction facing = state.getValue(FACING);
        boolean upper = state.getValue(UPPER);

        if (!upper) {
            return switch (facing.getAxis()) {
                case X -> EW_SHAPE;
                case Z -> NS_SHAPE;
                case Y -> UD_SHAPE;
            };
        }

        return switch (facing) {
            case NORTH -> NS_HALF_NORTH_SHAPE;
            case SOUTH -> NS_HALF_SOUTH_SHAPE;
            case EAST -> EW_HALF_EAST_SHAPE;
            case WEST -> EW_HALF_WEST_SHAPE;
            case UP -> UD_HALF_DOWN_SHAPE;
            case DOWN -> UD_HALF_UP_SHAPE;
        };
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeFor(state);
    }

    @Override
    protected List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        return List.of();
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        boolean upper = state.getValue(UPPER);
        Direction facing = state.getValue(FACING);
        Direction partnerDir = upper
                ? facing.getOpposite()
                : facing;

        if (
                direction == partnerDir
                        && (
                        !neighborState.is(this)
                                || neighborState.getValue(UPPER) == upper
                                || neighborState.getValue(FACING) != facing
                )
        ) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(
                state,
                direction,
                neighborState,
                level,
                pos,
                neighborPos
        );
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide) {
            boolean upper = state.getValue(UPPER);
            BlockPos lowerPos = upper
                    ? partnerPos(pos, state)
                    : pos;

            if (!player.isCreative()) {
                Block.popResource(
                        level,
                        lowerPos,
                        new ItemStack(this)
                );
            }

            if (upper) {
                BlockState lower = level.getBlockState(lowerPos);

                if (
                        lower.is(this)
                                && !lower.getValue(UPPER)
                ) {
                    level.setBlock(
                            lowerPos,
                            Blocks.AIR.defaultBlockState(),
                            35
                    );
                }
            }
        }

        return super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
    }

    public static void activate(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof SmallBombBlockEntity mk81) {
            mk81.activate();
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        if (
                level.isClientSide
                        || state.getValue(UPPER)
        ) {
            return;
        }

        boolean powered =
                level.hasNeighborSignal(pos)
                        || level.hasNeighborSignal(
                        partnerPos(pos, state)
                );

        boolean wasPowered =
                state.getValue(POWERED);

        if (powered == wasPowered) {
            return;
        }

        level.setBlock(
                pos,
                state.setValue(
                        POWERED,
                        powered
                ),
                3
        );

        if (powered) {
            activate(
                    (ServerLevel) level,
                    pos
            );
        }
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                POWERED,
                UPPER
        );
    }

    @Override
    protected boolean useShapeForLightOcclusion(
            BlockState state
    ) {
        return true;
    }

    @Override
    protected boolean propagatesSkylightDown(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return true;
    }

    @Override
    protected float getShadeBrightness(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return 1.0f;
    }
}