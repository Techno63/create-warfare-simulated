
package com.julian.createwarfare.blocks;

import com.julian.createwarfare.blocks.entities.AirToAirMissileBlockEntity;
import com.julian.createwarfare.registry.CWBlockEntities;
import com.julian.createwarfare.registry.CWBlocks;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirToAirMissileBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);

    private static final VoxelShape NS_SHAPE = Block.box(3, 3, 0, 13, 13, 16);
    private static final VoxelShape EW_SHAPE = Block.box(0, 3, 3, 16, 13, 13);
    private static final VoxelShape UD_SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public AirToAirMissileBlock(BlockBehaviour.Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(PART, 0)
        );
    }

    public static BlockPos getRootPos(BlockPos pos, BlockState state) {
        return pos.relative(
                state.getValue(FACING).getOpposite(),
                state.getValue(PART)
        );
    }

    public static List<BlockPos> getAllPositions(BlockPos root, BlockState state) {
        Direction facing = state.getValue(FACING);

        return List.of(
                root,
                root.relative(facing),
                root.relative(facing, 2)
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART) != 0) {
            return null;
        }

        return new AirToAirMissileBlockEntity(
                CWBlockEntities.AIR_TO_AIR_MISSILE.get(),
                pos,
                state
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == 0
                ? RenderShape.MODEL
                : RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        BlockPos root = context.getClickedPos();
        Level level = context.getLevel();

        for (int part = 1; part <= 2; part++) {
            BlockPos partPos = root.relative(facing, part);

            if (level.isOutsideBuildHeight(partPos)
                    || !level.getBlockState(partPos).canBeReplaced(context)) {
                return null;
            }
        }

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, 0);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        Direction facing = state.getValue(FACING);

        for (int part = 1; part <= 2; part++) {
            level.setBlock(
                    pos.relative(facing, part),
                    state.setValue(PART, part),
                    3
            );
        }
    }

    private static VoxelShape shapeFor(BlockState state) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> EW_SHAPE;
            case Y -> UD_SHAPE;
            case Z -> NS_SHAPE;
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
        Direction facing = state.getValue(FACING);
        int part = state.getValue(PART);

        if (direction == facing.getOpposite() && part > 0) {
            if (!neighborState.is(this)
                    || neighborState.getValue(FACING) != facing
                    || neighborState.getValue(PART) != part - 1) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        if (direction == facing && part < 2) {
            if (!neighborState.is(this)
                    || neighborState.getValue(FACING) != facing
                    || neighborState.getValue(PART) != part + 1) {
                return Blocks.AIR.defaultBlockState();
            }
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
            BlockPos root = getRootPos(pos, state);
            BlockState rootState = level.getBlockState(root);

            if (!player.isCreative()) {
                Block.popResource(level, root, new ItemStack(this));
            }

            if (rootState.is(this)) {
                for (BlockPos partPos : getAllPositions(root, rootState)) {
                    if (!partPos.equals(pos)) {
                        level.setBlock(
                                partPos,
                                Blocks.AIR.defaultBlockState(),
                                35
                        );
                    }
                }
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    public static void activate(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!state.is(CWBlocks.AIR_TO_AIR_MISSILE.get())) {
            return;
        }

        BlockPos root = getRootPos(pos, state);
        BlockEntity blockEntity = level.getBlockEntity(root);

        if (blockEntity instanceof AirToAirMissileBlockEntity missile) {
            missile.activate();
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
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos root = getRootPos(pos, state);
        BlockState rootState = level.getBlockState(root);

        if (!rootState.is(this) || rootState.getValue(PART) != 0) {
            return;
        }

        for (BlockPos partPos : getAllPositions(root, rootState)) {
            if (level.hasNeighborSignal(partPos)) {
                activate(serverLevel, root);
                return;
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING, PART);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
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