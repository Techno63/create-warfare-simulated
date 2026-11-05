package com.julian.createwarfare.blocks;

import com.julian.createwarfare.blocks.entities.ShortRangeBallisticMissileBlockEntity;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.julian.createwarfare.gui.CWGuiBuilder;
import com.julian.createwarfare.registry.CWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShortRangeBallisticMissileBlock extends Block implements EntityBlock {

    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);

    public static final int HEIGHT = 4;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public ShortRangeBallisticMissileBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 0));
        BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> {
            if (!state.is(this) || direction.getAxis() != Direction.Axis.Y) {
                return BlockMovementChecks.CheckResult.PASS;
            }

            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (!neighbor.is(this)) {
                return BlockMovementChecks.CheckResult.PASS;
            }

            return BlockMovementChecks.CheckResult.of(
                    neighbor.getValue(PART) == state.getValue(PART) + direction.getStepY()
            );
        });
    }

    public static BlockPos rootPos(BlockPos pos, BlockState state) {
        return pos.below(state.getValue(PART));
    }

    public static BlockPos partPos(BlockPos root, int part) {
        return root.above(part);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of();
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(PART);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART) != 0) return null;

        return new ShortRangeBallisticMissileBlockEntity(
                CWBlockEntities.SHORT_RANGE_BALLISTIC_MISSILE.get(),
                pos,
                state
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos root = context.getClickedPos();

        for (int part = 0; part < HEIGHT; part++) {
            BlockPos checkPos = root.above(part);

            if (!level.getBlockState(checkPos).canBeReplaced(context)) {
                return null;
            }
        }

        return defaultBlockState().setValue(PART, 0);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide) return;

        for (int part = 1; part < HEIGHT; part++) {
            level.setBlock(
                    pos.above(part),
                    defaultBlockState().setValue(PART, part),
                    Block.UPDATE_ALL
            );
        }

        if (level.getBlockEntity(pos)
                instanceof ShortRangeBallisticMissileBlockEntity missile) {
            missile.setTarget(pos);
        }
    }

    @Override
    public void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {
        if (state.getValue(PART) != 0) return;

        if (level.getBlockEntity(pos)
                instanceof ShortRangeBallisticMissileBlockEntity missile) {
            missile.completeDelayedLaunch();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        BlockPos root = rootPos(pos, state);

        if (!(level.getBlockEntity(root)
                instanceof ShortRangeBallisticMissileBlockEntity missile)) {
            return InteractionResult.CONSUME;
        }

        if (missile.isLaunched()) {
            serverPlayer.displayClientMessage(
                    Component.literal(
                            missile.isLaunchPending()
                                    ? "LAUNCH SEQUENCE PENDING"
                                    : "MISSILE ALREADY LAUNCHED"
                    ).withStyle(style -> style.withColor(0xFFFF5555)),
                    true
            );

            return InteractionResult.CONSUME;
        }

        openControlPanel(serverPlayer, root, missile);

        return InteractionResult.CONSUME;
    }

    private void openControlPanel(
            ServerPlayer player,
            BlockPos root,
            ShortRangeBallisticMissileBlockEntity missile
    ) {
        BlockPos currentTarget = missile.getTarget();

        CWGuiBuilder.create(
                        "tactical_ballistic_missile_" + root.asLong(),
                        "TACTICAL BALLISTIC MISSILE"
                )
                .panelWidth(360)
                .panelHeight(260)
                .minimumPanelHeight(260)
                .padding(12)
                .headerHeight(30)
                .buttonHeight(26)

                .backgroundColors(0xD00B100D, 0xE00A0E0B)
                .backgroundDimEnabled(true)

                .panelColor(0xFF171F19)
                .panelHighlightColor(0xFF526B50)
                .panelShadowColor(0xFF090D0A)
                .headerColor(0xFF252F27)
                .accentColor(0xFFB18A4A)
                .stripeColor(0xFF303D32)
                .textColor(0xFFE1E5DF)
                .borderColor(0xFF0B0E0C)
                .shadowColor(0x99000000)
                .shadowSize(4)
                .shadowsEnabled(true)

                .inputColors(0xFF18221B, 0xFF263529, 0xFF304532)
                .inputBorderColor(0xFF604C2E)
                .inputHighlightColor(0xFFB18A4A)
                .cursorColor(0xFFE5C486)

                .buttonColor(0xFF354139)
                .buttonHoverColor(0xFF46564A)
                .buttonBorderColor(0xFF111711)
                .buttonHighlightColor(0xFFB18A4A)
                .buttonShadowColor(0xFF182019)
                .buttonTextColor(0xFFE1E5DF)

                .slotColors(0xFF604C2E, 0xFFB18A4A, 0xFF263329, 0xFF18221B)
                .checkerColors(0xFF263B2D, 0xFF1D3024)
                .checkerPixelSize(2)
                .checkerEnabled(false)
                .stripesEnabled(true)
                .sideBarsEnabled(false)

                .font("minecraft:default")
                .textScale(1.0F)
                .titleScale(1.15F)
                .textAlignment(1)

                .label("TARGET COORDINATES", 16, 43, 0xFFE5C486)
                .divider(16, 61, 328, 0xFF604C2E)

                .numberFieldAt(
                        "target_x",
                        "TARGET X",
                        currentTarget.getX(),
                        16,
                        72,
                        100
                )

                .numberFieldAt(
                        "target_y",
                        "TARGET Y",
                        currentTarget.getY(),
                        130,
                        72,
                        100
                )

                .numberFieldAt(
                        "target_z",
                        "TARGET Z",
                        currentTarget.getZ(),
                        244,
                        72,
                        100
                )

                .divider(16, 113, 328, 0xFF303D32)

                .label("LAUNCH DELAY (SECONDS)", 16, 124, 0xFFE5C486)

                .numberFieldAt(
                        "delay",
                        "DELAY TIME",
                        missile.getLaunchDelay(),
                        16,
                        145,
                        328
                )

                .divider(16, 176, 328, 0xFF303D32)

                .label(
                        "FUEL: " + missile.getFuelAmount()
                                + " / " + missile.getFuelCapacity() + " mB",
                        16,
                        188,
                        0xFFE5C486
                )

                .buttonShape(CWGuiBuilder.ButtonShape.ROUNDED, 5, 1)
                .buttonStateColors(
                        0xFF354B38,
                        0xFF49664C,
                        0xFF263A29,
                        0xFF303030,
                        0xFF607A55
                )
                .closeOnClick(true)
                .textButtonAt(
                        "SAVE",
                        "save",
                        16,
                        210,
                        100,
                        28,
                        "minecraft:ui.button.click"
                )

                .buttonShape(CWGuiBuilder.ButtonShape.ROUNDED, 5, 1)
                .buttonStateColors(
                        0xFF70542D,
                        0xFF96713C,
                        0xFF503C20,
                        0xFF303030,
                        0xFFE5C486
                )
                .closeOnClick(true)
                .textButtonAt(
                        missile.isArmed() ? "DISARM" : "ARM",
                        "toggle_arm",
                        130,
                        210,
                        100,
                        28,
                        "minecraft:ui.button.click"
                )

                .buttonShape(CWGuiBuilder.ButtonShape.ROUNDED, 5, 1)
                .buttonStateColors(
                        0xFF59332F,
                        0xFF82463F,
                        0xFF402421,
                        0xFF303030,
                        0xFF9B5A50
                )
                .closeOnClick(true)
                .textButtonAt(
                        "CLOSE",
                        "close",
                        244,
                        210,
                        100,
                        28,
                        "minecraft:ui.button.click"
                )

                .onAction((guiPlayer, actionId, values) -> {
                    if (guiPlayer.isRemoved()
                            || !(guiPlayer.level() instanceof ServerLevel serverLevel)
                            || !serverLevel.hasChunkAt(root)
                            || !serverLevel.getBlockState(root).is(this)
                            || guiPlayer.distanceToSqr(
                            root.getX() + 0.5D,
                            root.getY() + 0.5D,
                            root.getZ() + 0.5D
                    ) > 64.0D) {
                        return;
                    }

                    if (!(serverLevel.getBlockEntity(root)
                            instanceof ShortRangeBallisticMissileBlockEntity currentMissile)
                            || currentMissile.isLaunched()) {
                        return;
                    }

                    if ("close".equals(actionId)) {
                        return;
                    }

                    if ("toggle_arm".equals(actionId) && currentMissile.isArmed()) {
                        currentMissile.setArmed(
                                false,
                                hasRedstoneSignal(serverLevel, root)
                        );

                        guiPlayer.displayClientMessage(
                                Component.literal("MISSILE DISARMED")
                                        .withStyle(style -> style.withColor(0xFF80B875)),
                                true
                        );

                        return;
                    }

                    int x = values.getOrDefault(
                            "target_x",
                            currentMissile.getTarget().getX()
                    );

                    int y = values.getOrDefault(
                            "target_y",
                            currentMissile.getTarget().getY()
                    );

                    int z = values.getOrDefault(
                            "target_z",
                            currentMissile.getTarget().getZ()
                    );

                    int delay = values.getOrDefault(
                            "delay",
                            currentMissile.getLaunchDelay()
                    );

                    delay = Math.max(0, Math.min(120, delay));

                    BlockPos newTarget = new BlockPos(x, y, z);

                    if (!currentMissile.isTargetWithinRange(newTarget)) {
                        guiPlayer.displayClientMessage(
                                Component.literal("TARGET OUT OF RANGE")
                                        .withStyle(style -> style.withColor(0xFFFF3333)),
                                true
                        );

                        return;
                    }

                    switch (actionId) {
                        case "save" -> {
                            currentMissile.setTarget(newTarget);
                            currentMissile.setLaunchDelay(delay);

                            guiPlayer.displayClientMessage(
                                    Component.literal(
                                            "CONFIGURATION SAVED  |  DELAY: "
                                                    + delay + "s"
                                    ).withStyle(style -> style.withColor(0xFF80B875)),
                                    true
                            );
                        }

                        case "toggle_arm" -> {
                            currentMissile.setTarget(newTarget);
                            currentMissile.setLaunchDelay(delay);

                            boolean powered = hasRedstoneSignal(
                                    serverLevel,
                                    root
                            );

                            currentMissile.setArmed(true, powered);

                            guiPlayer.displayClientMessage(
                                    Component.literal(
                                            powered
                                                    ? "MISSILE ARMED  |  REDSTONE MUST RESET BEFORE LAUNCH"
                                                    : "MISSILE ARMED  |  WAITING FOR REDSTONE"
                                    ).withStyle(style -> style.withColor(0xFFE5C486)),
                                    true
                            );
                        }
                    }
                })
                .build()
                .openFor(player);
    }

    private static boolean hasRedstoneSignal(
            ServerLevel level,
            BlockPos root
    ) {
        for (int part = 0; part < HEIGHT; part++) {
            if (level.hasNeighborSignal(root.above(part))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        BlockPos root = rootPos(pos, state);

        if (!level.isClientSide && !player.isCreative()) {
            popResource(level, root, new ItemStack(this));
        }

        if (!level.isClientSide) {
            for (int part = 0; part < HEIGHT; part++) {
                BlockPos currentPart = root.above(part);

                if (!currentPart.equals(pos)
                        && level.getBlockState(currentPart).is(this)) {

                    level.setBlock(
                            currentPart,
                            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL
                    );
                }
            }
        }

        super.playerWillDestroy(level, pos, state, player);

        return state;
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        int part = state.getValue(PART);

        if (direction == Direction.DOWN && part > 0
                && (!neighborState.is(this)
                || neighborState.getValue(PART) != part - 1)) {

            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }

        if (direction == Direction.UP && part < HEIGHT - 1
                && (!neighborState.is(this)
                || neighborState.getValue(PART) != part + 1)) {

            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
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
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston
    ) {
        super.neighborChanged(
                state,
                level,
                pos,
                neighborBlock,
                neighborPos,
                movedByPiston
        );

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos root = rootPos(pos, state);

        if (!(serverLevel.getBlockEntity(root)
                instanceof ShortRangeBallisticMissileBlockEntity missile)) {
            return;
        }

        if (missile.isLaunched()) {
            return;
        }

        missile.handleRedstoneSignal(
                hasRedstoneSignal(serverLevel, root)
        );
    }

    public static void launch(
            ServerLevel level,
            BlockPos pos,
            BlockPos target
    ) {
        BlockState state = level.getBlockState(pos);

        BlockPos root = state.getBlock() instanceof ShortRangeBallisticMissileBlock
                ? rootPos(pos, state)
                : pos;

        if (level.getBlockEntity(root)
                instanceof ShortRangeBallisticMissileBlockEntity missile) {
            missile.launch(target);
        }
    }
}