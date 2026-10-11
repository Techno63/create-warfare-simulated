package com.julian.createwarfare.blocks;

import com.julian.createwarfare.blocks.entities.LargeBombBlockEntity;
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

public class LargeBombBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING =
            BlockStateProperties.FACING;

    public static final IntegerProperty PART =
            IntegerProperty.create(
                    "part",
                    0,
                    2
            );

    private static final VoxelShape NS_SHAPE =
            Block.box(
                    3,
                    3,
                    0,
                    13,
                    13,
                    16
            );

    private static final VoxelShape NS_HALF_NORTH =
            Block.box(
                    3,
                    3,
                    8,
                    13,
                    13,
                    16
            );

    private static final VoxelShape NS_HALF_SOUTH =
            Block.box(
                    3,
                    3,
                    0,
                    13,
                    13,
                    8
            );

    private static final VoxelShape EW_SHAPE =
            Block.box(
                    0,
                    3,
                    3,
                    16,
                    13,
                    13
            );

    private static final VoxelShape EW_HALF_EAST =
            Block.box(
                    0,
                    3,
                    3,
                    8,
                    13,
                    13
            );

    private static final VoxelShape EW_HALF_WEST =
            Block.box(
                    8,
                    3,
                    3,
                    16,
                    13,
                    13
            );

    private static final VoxelShape UD_SHAPE =
            Block.box(
                    3,
                    0,
                    3,
                    13,
                    16,
                    13
            );

    private static final VoxelShape UD_HALF_UP =
            Block.box(
                    3,
                    0,
                    3,
                    13,
                    8,
                    13
            );

    private static final VoxelShape UD_HALF_DOWN =
            Block.box(
                    3,
                    8,
                    3,
                    13,
                    16,
                    13
            );

    public LargeBombBlock(
            BlockBehaviour.Properties properties
    ) {
        super(
                properties
        );

        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(
                                FACING,
                                Direction.NORTH
                        )
                        .setValue(
                                PART,
                                0
                        )
        );
    }

    public static BlockPos getRootPos(
            BlockPos pos,
            BlockState state
    ) {
        Direction facing =
                state.getValue(
                        FACING
                );

        int part =
                state.getValue(
                        PART
                );

        return pos.relative(
                facing.getOpposite(),
                part
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        if (
                state.getValue(
                        PART
                ) != 0
        ) {
            return null;
        }

        return new LargeBombBlockEntity(
                CWBlockEntities.LARGE_BOMB.get(),
                pos,
                state
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == 1
                ? RenderShape.MODEL
                : RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Direction facing =
                context
                        .getNearestLookingDirection()
                        .getOpposite();

        BlockPos pos =
                context.getClickedPos();

        Level level =
                context.getLevel();

        BlockPos second =
                pos.relative(
                        facing
                );

        BlockPos third =
                pos.relative(
                        facing,
                        2
                );

        if (
                level.isOutsideBuildHeight(
                        second
                )
                        || level.isOutsideBuildHeight(
                        third
                )
        ) {
            return null;
        }

        if (
                !level
                        .getBlockState(
                                second
                        )
                        .canBeReplaced(
                                context
                        )
                        || !level
                        .getBlockState(
                                third
                        )
                        .canBeReplaced(
                                context
                        )
        ) {
            return null;
        }

        return defaultBlockState()
                .setValue(
                        FACING,
                        facing
                )
                .setValue(
                        PART,
                        0
                );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        Direction facing =
                state.getValue(
                        FACING
                );

        level.setBlock(
                pos.relative(
                        facing
                ),
                state.setValue(
                        PART,
                        1
                ),
                3
        );

        level.setBlock(
                pos.relative(
                        facing,
                        2
                ),
                state.setValue(
                        PART,
                        2
                ),
                3
        );
    }

    private static VoxelShape shapeFor(
            BlockState state
    ) {
        Direction facing =
                state.getValue(
                        FACING
                );

        int part =
                state.getValue(
                        PART
                );

        if (
                part < 2
        ) {
            return switch (
                    facing.getAxis()
                    ) {
                case X -> EW_SHAPE;
                case Z -> NS_SHAPE;
                case Y -> UD_SHAPE;
            };
        }

        return switch (
                facing
                ) {
            case NORTH -> NS_HALF_NORTH;
            case SOUTH -> NS_HALF_SOUTH;
            case EAST -> EW_HALF_EAST;
            case WEST -> EW_HALF_WEST;
            case UP -> UD_HALF_UP;
            case DOWN -> UD_HALF_DOWN;
        };
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeFor(
                state
        );
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeFor(
                state
        );
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
        Direction facing =
                state.getValue(
                        FACING
                );

        int part =
                state.getValue(
                        PART
                );

        if (
                part == 0
                        && direction == facing
        ) {
            if (
                    !neighborState.is(
                            this
                    )
                            || neighborState.getValue(
                            FACING
                    ) != facing
                            || neighborState.getValue(
                            PART
                    ) != 1
            ) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        if (
                part == 1
        ) {
            if (
                    direction == facing.getOpposite()
                            && (
                            !neighborState.is(
                                    this
                            )
                                    || neighborState.getValue(
                                    FACING
                            ) != facing
                                    || neighborState.getValue(
                                    PART
                            ) != 0
                    )
            ) {
                return Blocks.AIR.defaultBlockState();
            }

            if (
                    direction == facing
                            && (
                            !neighborState.is(
                                    this
                            )
                                    || neighborState.getValue(
                                    FACING
                            ) != facing
                                    || neighborState.getValue(
                                    PART
                            ) != 2
                    )
            ) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        if (
                part == 2
                        && direction == facing.getOpposite()
        ) {
            if (
                    !neighborState.is(
                            this
                    )
                            || neighborState.getValue(
                            FACING
                    ) != facing
                            || neighborState.getValue(
                            PART
                    ) != 1
            ) {
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
            BlockPos rootPos =
                    getRootPos(
                            pos,
                            state
                    );

            BlockState rootState =
                    level.getBlockState(
                            rootPos
                    );

            if (!player.isCreative()) {
                Block.popResource(
                        level,
                        rootPos,
                        new ItemStack(
                                this
                        )
                );
            }

            if (
                    rootState.is(
                            this
                    )
            ) {
                Direction facing =
                        rootState.getValue(
                                FACING
                        );

                level.setBlock(
                        rootPos.relative(
                                facing
                        ),
                        Blocks.AIR.defaultBlockState(),
                        35
                );

                level.setBlock(
                        rootPos.relative(
                                facing,
                                2
                        ),
                        Blocks.AIR.defaultBlockState(),
                        35
                );
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
        BlockState state = level.getBlockState(pos);

        if (!state.is(CWBlocks.LARGE_BOMB.get())) {
            return;
        }

        BlockPos rootPos = getRootPos(pos, state);

        BlockEntity blockEntity = level.getBlockEntity(rootPos);

        if (blockEntity instanceof LargeBombBlockEntity bomb) {
            bomb.activate();
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
        ) {
            return;
        }

        BlockPos rootPos =
                getRootPos(
                        pos,
                        state
                );

        BlockState rootState =
                level.getBlockState(
                        rootPos
                );

        if (
                !rootState.is(
                        this
                )
                        || rootState.getValue(
                        PART
                ) != 0
        ) {
            return;
        }

        Direction facing =
                rootState.getValue(
                        FACING
                );

        boolean powered =
                level.hasNeighborSignal(
                        rootPos
                )
                        || level.hasNeighborSignal(
                        rootPos.relative(
                                facing
                        )
                )
                        || level.hasNeighborSignal(
                        rootPos.relative(
                                facing,
                                2
                        )
                );

        if (powered) {
            activate(
                    (ServerLevel) level,
                    rootPos
            );
        }
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                PART
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