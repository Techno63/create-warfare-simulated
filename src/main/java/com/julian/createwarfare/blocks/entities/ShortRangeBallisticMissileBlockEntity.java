package com.julian.createwarfare.blocks.entities;

import com.julian.createwarfare.blocks.ShortRangeBallisticMissileBlock;
import com.julian.createwarfare.effects.particles.smoke.SmokeParticleOptions;
import com.julian.createwarfare.registry.CWFuelRegistry;
import com.julian.createwarfare.registry.CWBlockEntities;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@EventBusSubscriber(modid = "createwarfare")
public class ShortRangeBallisticMissileBlockEntity extends BlockEntity implements BlockEntitySubLevelActor, GeoBlockEntity {

    public static final int HEIGHT = ShortRangeBallisticMissileBlock.HEIGHT;
    public static final int RANGE = 350;
    public static final int FUEL_CAPACITY_MB = 8000;

    private static final double FUEL_CONSUMPTION_MB_PER_SECOND = 500.0D;

    private static final double GRAVITY = 11.0D;
    private static final double THRUST_ACCELERATION = 42.0D;
    private static final double MAX_SPEED = 190.0D;

    private static final double MINIMUM_ARC_HEIGHT = 35.0D;
    private static final double MAXIMUM_ARC_HEIGHT = 135.0D;
    private static final double ARC_HEIGHT_FACTOR = 0.42D;

    private static final double ORIENTATION_RESPONSE = 1.5D;
    private static final double GUIDANCE_LOOKAHEAD = 0.075D;
    private static final double GUIDANCE_DELAY = 0.15D;

    private static final double SIMULATION_STEP = 0.025D;
    private static final double SIMULATION_MAX_TIME = 90.0D;
    private static final double INITIAL_DRAG_ESTIMATE = 0.09D;
    private static final double DRAG_ESTIMATE_RESPONSE = 0.15D;
    private static final double DRAG_ESTIMATE_MAXIMUM = 1.5D;

    private static final double CUTOFF_DISTANCE_MARGIN = 3.0D;

    private static final double PAYLOAD_RELEASE_PROGRESS = 0.80D;

    private static final double SEPARATION_SPEED = 1.5D;
    private static final int PAYLOAD_CHUNK_LOAD_TICKS = 1800;
    private static final int CHUNK_TICKET_RADIUS = 1;

    private static final double EXHAUST_INTERVAL = 0.025D;
    private static final double EXHAUST_OFFSET = 2.5D;
    private static final float EXHAUST_PARTICLE_SIZE = 0.75F;

    private static final int TRAIL_PARTICLE_COUNT = 5;
    private static final int TRAIL_PARTICLE_LIFETIME = 75;

    private static final double CONTRAIL_INTERVAL = 0.025D;
    private static final int CONTRAIL_PARTICLE_LIFETIME = 500;
    private static final float CONTRAIL_PARTICLE_SIZE = 0.65F;

    private static final int DETACH_RING_PARTICLES = 48;
    private static final double DETACH_RING_RADIUS = 2.6D;
    private static final double DETACH_RING_PARTICLE_SPEED = 0.12D;

    private static final double PARTICLE_VIEW_DISTANCE = 300.0D;
    private static final double PARTICLE_VIEW_DISTANCE_SQUARED =
            PARTICLE_VIEW_DISTANCE * PARTICLE_VIEW_DISTANCE;

    private static final Vector3d MISSILE_FORWARD = new Vector3d(0.0D, 1.0D, 0.0D);

    private static final int[] EXHAUST_COLORS = {
            0xFFFF7028,
            0xFFFF7D2D,
            0xFFFF8B36,
            0xFFFF9B3B,
            0xFFFFAA43,
            0xFFFFBA4D
    };

    private static final TicketController CHUNK_TICKETS = new TicketController(
            ResourceLocation.fromNamespaceAndPath("createwarfare", "short_range_ballistic_missile")
    );

    private static final ConcurrentLinkedQueue<PendingPayloadSeparation> PENDING_SEPARATIONS =
            new ConcurrentLinkedQueue<>();

    private static final ConcurrentLinkedQueue<PayloadChunkTicket> PAYLOAD_CHUNK_TICKETS =
            new ConcurrentLinkedQueue<>();

    private record PendingPayloadSeparation(
            ServerLevel level,
            BlockPos position,
            Vector3d separationDirection
    ) {
    }

    private static final class PayloadChunkTicket {
        private final ServerLevel level;
        private final BlockPos owner;
        private final ServerSubLevel subLevel;
        private final long releaseAt;
        private final Set<Long> chunks = new HashSet<>();

        private PayloadChunkTicket(
                ServerLevel level,
                BlockPos owner,
                ServerSubLevel subLevel,
                long releaseAt
        ) {
            this.level = level;
            this.owner = owner;
            this.subLevel = subLevel;
            this.releaseAt = releaseAt;
        }
    }

    private final FluidTank fuelTank = new FluidTank(
            FUEL_CAPACITY_MB,
            stack -> CWFuelRegistry.isFuel(stack.getFluid())
    ) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private BlockPos target = BlockPos.ZERO;
    private final Set<Long> subLevelChunkTickets = new HashSet<>();

    private boolean launched;
    private boolean launchPending;
    private boolean armed;
    private boolean lastRedstonePowered;
    private boolean flightEnded;
    private boolean initialVelocityApplied;
    private boolean poweredAscent;
    private boolean payloadPresent;
    private boolean payloadDetached;
    private boolean hasPreviousVelocity;

    private int launchDelay;
    private int flightTicks;

    private double flightTime;
    private double fuelConsumptionRemainder;
    private double exhaustTimer;
    private double contrailTimer;
    private double boostAltitude;
    private double targetGroundY;
    private double estimatedDragCoefficient = INITIAL_DRAG_ESTIMATE;
    private double previousPhysicsStep;

    private final Vector3d launchPosition = new Vector3d();
    private final Vector3d previousVelocity = new Vector3d();
    private final Vector3d previousAppliedAcceleration = new Vector3d();

    private final AnimatableInstanceCache animatableInstanceCache =
            GeckoLibUtil.createInstanceCache(this);

    public ShortRangeBallisticMissileBlockEntity(
            BlockEntityType<ShortRangeBallisticMissileBlockEntity> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CWBlockEntities.SHORT_RANGE_BALLISTIC_MISSILE.get(),
                (missile, side) -> missile.getFluidHandler()
        );
    }

    @SubscribeEvent
    public static void registerChunkTickets(RegisterTicketControllersEvent event) {
        event.register(CHUNK_TICKETS);
    }

    @SubscribeEvent
    public static void processPendingPayloadSeparations(ServerTickEvent.Post event) {
        for (PayloadChunkTicket ticket : PAYLOAD_CHUNK_TICKETS) {
            if (ticket.level.getGameTime() >= ticket.releaseAt) {
                releasePayloadChunkTickets(ticket);
                PAYLOAD_CHUNK_TICKETS.remove(ticket);
                continue;
            }

            updatePayloadChunkTickets(ticket);
        }

        PendingPayloadSeparation pending;

        while ((pending = PENDING_SEPARATIONS.poll()) != null) {
            ServerLevel serverLevel = pending.level();
            BlockPos payloadPos = pending.position();

            BlockState state = serverLevel.getBlockState(payloadPos);

            if (state.isAir()) {
                continue;
            }

            BoundingBox3i bounds = new BoundingBox3i(
                    payloadPos.getX(),
                    payloadPos.getY(),
                    payloadPos.getZ(),
                    payloadPos.getX(),
                    payloadPos.getY(),
                    payloadPos.getZ()
            );

            ServerSubLevel payloadSubLevel = SubLevelAssemblyHelper.assembleBlocks(
                    serverLevel,
                    payloadPos,
                    List.of(payloadPos),
                    bounds
            );

            PayloadChunkTicket ticket = new PayloadChunkTicket(
                    serverLevel,
                    payloadPos.immutable(),
                    payloadSubLevel,
                    serverLevel.getGameTime() + PAYLOAD_CHUNK_LOAD_TICKS
            );

            updatePayloadChunkTickets(ticket);
            PAYLOAD_CHUNK_TICKETS.add(ticket);

            RigidBodyHandle payloadHandle = RigidBodyHandle.of(payloadSubLevel);

            if (payloadHandle != null && payloadHandle.isValid()) {
                Vector3d separationVelocity = new Vector3d(
                        pending.separationDirection()
                ).mul(SEPARATION_SPEED);

                payloadHandle.addLinearAndAngularVelocity(
                        separationVelocity,
                        new Vector3d()
                );
            }
        }
    }

    private static void updatePayloadChunkTickets(PayloadChunkTicket ticket) {
        BlockPos localCenter = ticket.subLevel.getPlot()
                .getCenterBlock()
                .immutable();

        Vector3d worldCenter = new Vector3d(
                localCenter.getX() + 0.5D,
                localCenter.getY() + 0.5D,
                localCenter.getZ() + 0.5D
        );

        ticket.subLevel.logicalPose().transformPosition(worldCenter);

        int centerChunkX = Mth.floor(worldCenter.x) >> 4;
        int centerChunkZ = Mth.floor(worldCenter.z) >> 4;

        Set<Long> desired = new HashSet<>();

        for (int x = -CHUNK_TICKET_RADIUS; x <= CHUNK_TICKET_RADIUS; x++) {
            for (int z = -CHUNK_TICKET_RADIUS; z <= CHUNK_TICKET_RADIUS; z++) {
                int chunkX = centerChunkX + x;
                int chunkZ = centerChunkZ + z;
                long key = chunkKey(chunkX, chunkZ);

                desired.add(key);

                if (!ticket.chunks.contains(key)) {
                    CHUNK_TICKETS.forceChunk(
                            ticket.level,
                            ticket.owner,
                            chunkX,
                            chunkZ,
                            true,
                            true
                    );
                }
            }
        }

        for (long key : ticket.chunks) {
            if (!desired.contains(key)) {
                CHUNK_TICKETS.forceChunk(
                        ticket.level,
                        ticket.owner,
                        chunkX(key),
                        chunkZ(key),
                        false,
                        true
                );
            }
        }

        ticket.chunks.clear();
        ticket.chunks.addAll(desired);
    }

    private static void releasePayloadChunkTickets(PayloadChunkTicket ticket) {
        for (long key : ticket.chunks) {
            CHUNK_TICKETS.forceChunk(
                    ticket.level,
                    ticket.owner,
                    chunkX(key),
                    chunkZ(key),
                    false,
                    true
            );
        }

        ticket.chunks.clear();
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
    }

    private static int chunkX(long key) {
        return (int) (key >> 32);
    }

    private static int chunkZ(long key) {
        return (int) key;
    }

    private static int randomExhaustColor(RandomSource random) {
        return EXHAUST_COLORS[random.nextInt(EXHAUST_COLORS.length)];
    }

    @Override
    public void onLoad() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        forceChunk(serverLevel, worldPosition, true);
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            releaseChunkTickets(serverLevel);
        }

        super.setRemoved();
    }

    private void forceChunk(ServerLevel serverLevel, BlockPos pos, boolean force) {
        CHUNK_TICKETS.forceChunk(
                serverLevel,
                worldPosition,
                pos.getX() >> 4,
                pos.getZ() >> 4,
                force,
                true
        );
    }

    private void releaseChunkTickets(ServerLevel serverLevel) {
        forceChunk(serverLevel, worldPosition, false);

        for (long key : subLevelChunkTickets) {
            CHUNK_TICKETS.forceChunk(
                    serverLevel,
                    worldPosition,
                    chunkX(key),
                    chunkZ(key),
                    false,
                    true
            );
        }

        subLevelChunkTickets.clear();
    }

    private void ensureSubLevelChunkLoaded(ServerSubLevel subLevel) {
        ServerLevel serverLevel = subLevel.getLevel();

        BlockPos localCenter = subLevel.getPlot()
                .getCenterBlock()
                .immutable();

        Vector3d worldCenter = new Vector3d(
                localCenter.getX() + 0.5D,
                localCenter.getY() + 0.5D,
                localCenter.getZ() + 0.5D
        );

        subLevel.logicalPose().transformPosition(worldCenter);

        int centerChunkX = Mth.floor(worldCenter.x) >> 4;
        int centerChunkZ = Mth.floor(worldCenter.z) >> 4;

        Set<Long> desired = new HashSet<>();

        for (int x = -CHUNK_TICKET_RADIUS; x <= CHUNK_TICKET_RADIUS; x++) {
            for (int z = -CHUNK_TICKET_RADIUS; z <= CHUNK_TICKET_RADIUS; z++) {
                int chunkX = centerChunkX + x;
                int chunkZ = centerChunkZ + z;

                if ((worldPosition.getX() >> 4) == chunkX
                        && (worldPosition.getZ() >> 4) == chunkZ) {
                    continue;
                }

                long key = chunkKey(chunkX, chunkZ);
                desired.add(key);

                if (!subLevelChunkTickets.contains(key)) {
                    CHUNK_TICKETS.forceChunk(
                            serverLevel,
                            worldPosition,
                            chunkX,
                            chunkZ,
                            true,
                            true
                    );
                }
            }
        }

        for (long key : subLevelChunkTickets) {
            if (!desired.contains(key)) {
                CHUNK_TICKETS.forceChunk(
                        serverLevel,
                        worldPosition,
                        chunkX(key),
                        chunkZ(key),
                        false,
                        true
                );
            }
        }

        subLevelChunkTickets.clear();
        subLevelChunkTickets.addAll(desired);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    public IFluidHandler getFluidHandler() {
        return fuelTank;
    }

    public int getFuelAmount() {
        return fuelTank.getFluidAmount();
    }

    public int getFuelCapacity() {
        return FUEL_CAPACITY_MB;
    }

    public boolean hasFuel() {
        return !fuelTank.isEmpty();
    }

    public void setTarget(BlockPos targetPos) {
        if (targetPos == null || isLaunched()) return;

        target = targetPos.immutable();

        setChanged();
        sync();
    }

    public BlockPos getTarget() {
        return target;
    }

    public boolean isTargetWithinRange(BlockPos targetPos) {
        if (targetPos == null || level == null) return false;

        Vec3 missilePosition = new Vec3(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + HEIGHT * 0.5D,
                worldPosition.getZ() + 0.5D
        );

        Vec3 normalPosition = Sable.HELPER.projectOutOfSubLevel(
                level,
                missilePosition
        );

        double dx = targetPos.getX() + 0.5D - normalPosition.x;
        double dy = targetPos.getY() + 0.5D - normalPosition.y;
        double dz = targetPos.getZ() + 0.5D - normalPosition.z;

        return dx * dx + dy * dy + dz * dz <= (double) RANGE * RANGE;
    }

    public void setArmed(boolean armed, boolean redstonePowered) {
        if (isLaunched()) return;

        this.armed = armed;
        this.lastRedstonePowered = redstonePowered;

        setChanged();
        sync();
    }

    public boolean isArmed() {
        return armed;
    }

    public boolean isLaunchPending() {
        return launchPending;
    }

    public void setLaunchDelay(int seconds) {
        if (isLaunched()) return;

        launchDelay = Mth.clamp(seconds, 0, 120);

        setChanged();
        sync();
    }

    public int getLaunchDelay() {
        return launchDelay;
    }

    public boolean isLaunched() {
        return launched || launchPending;
    }

    public void handleRedstoneSignal(boolean powered) {
        if (isLaunched()) return;

        boolean risingEdge = powered && !lastRedstonePowered;

        if (powered != lastRedstonePowered) {
            lastRedstonePowered = powered;
            setChanged();
        }

        if (risingEdge && armed) {
            launch(target);
        }
    }

    public boolean launch(BlockPos targetPos) {
        if (!armed || isLaunched() || !hasFuel()
                || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        BlockPos launchTarget = targetPos == null ? target : targetPos.immutable();

        if (!isTargetWithinRange(launchTarget)) {
            return false;
        }

        BlockPos payloadPos = worldPosition.above(HEIGHT);
        BlockState payloadState = serverLevel.getBlockState(payloadPos);

        if (!payloadState.isAir()
                && !isMovablePayload(serverLevel, payloadPos, payloadState)) {
            return false;
        }

        target = launchTarget;
        armed = false;
        lastRedstonePowered = false;

        if (launchDelay > 0) {
            launchPending = true;

            setChanged();
            sync();

            serverLevel.scheduleTick(
                    worldPosition,
                    getBlockState().getBlock(),
                    launchDelay * 20
            );

            return true;
        }

        return launchNow(serverLevel);
    }

    private boolean isMovablePayload(ServerLevel serverLevel, BlockPos pos, BlockState state) {
        return !state.isAir()
                && !state.is(Blocks.BEDROCK)
                && state.getDestroySpeed(serverLevel, pos) >= 0.0F;
    }

    public void completeDelayedLaunch() {
        if (!launchPending || launched || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState state = serverLevel.getBlockState(worldPosition);

        if (!(state.getBlock() instanceof ShortRangeBallisticMissileBlock)
                || state.getValue(ShortRangeBallisticMissileBlock.PART) != 0) {
            launchPending = false;
            setChanged();
            return;
        }

        launchPending = false;
        setChanged();

        if (!hasFuel() || !isTargetWithinRange(target)) {
            sync();
            return;
        }

        launchNow(serverLevel);
    }

    private boolean isValidGroundBlock(ServerLevel serverLevel, BlockPos pos, BlockState state) {
        return !state.isAir()
                && !state.getCollisionShape(serverLevel, pos).isEmpty()
                && !state.is(BlockTags.LEAVES)
                && !state.is(BlockTags.LOGS);
    }

    private double findGroundHeight(ServerLevel serverLevel, BlockPos targetPos) {
        int startY = Math.min(
                serverLevel.getMaxBuildHeight() - 1,
                targetPos.getY() + 32
        );

        for (int y = startY; y >= serverLevel.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(targetPos.getX(), y, targetPos.getZ());

            if (isValidGroundBlock(
                    serverLevel,
                    checkPos,
                    serverLevel.getBlockState(checkPos)
            )) {
                return y + 1.0D;
            }
        }

        return targetPos.getY() + 0.5D;
    }

    private boolean launchNow(ServerLevel serverLevel) {
        if (launched || level == null || !hasFuel()) return false;
        if (!isTargetWithinRange(target)) return false;

        BlockPos root = worldPosition.immutable();
        BlockPos payloadPos = root.above(HEIGHT);

        forceChunk(serverLevel, root, true);

        List<BlockPos> blocks = new ArrayList<>(HEIGHT + 1);

        for (int i = 0; i < HEIGHT; i++) {
            BlockPos pos = root.above(i);
            BlockState state = serverLevel.getBlockState(pos);

            if (!(state.getBlock() instanceof ShortRangeBallisticMissileBlock)
                    || state.getValue(ShortRangeBallisticMissileBlock.PART) != i) {
                return false;
            }

            blocks.add(pos);
        }

        BlockState payloadState = serverLevel.getBlockState(payloadPos);

        payloadPresent = !payloadState.isAir();

        if (payloadPresent) {
            if (!isMovablePayload(serverLevel, payloadPos, payloadState)) {
                return false;
            }

            blocks.add(payloadPos);
        }

        int maxY = payloadPresent
                ? payloadPos.getY()
                : root.getY() + HEIGHT - 1;

        BoundingBox3i bounds = new BoundingBox3i(
                root.getX(),
                root.getY(),
                root.getZ(),
                root.getX(),
                maxY,
                root.getZ()
        );

        targetGroundY = findGroundHeight(serverLevel, target);

        launchPosition.set(
                root.getX() + 0.5D,
                root.getY() + HEIGHT * 0.5D,
                root.getZ() + 0.5D
        );

        double horizontalRange = horizontalDistance(
                launchPosition,
                getGroundTargetPosition()
        );

        boostAltitude = Mth.clamp(
                horizontalRange * ARC_HEIGHT_FACTOR,
                MINIMUM_ARC_HEIGHT,
                MAXIMUM_ARC_HEIGHT
        );

        boostAltitude = Math.min(
                boostAltitude,
                Math.max(
                        MINIMUM_ARC_HEIGHT,
                        serverLevel.getMaxBuildHeight() - launchPosition.y - 32.0D
                )
        );

        launched = true;
        launchPending = false;
        armed = false;
        flightEnded = false;
        initialVelocityApplied = false;
        poweredAscent = true;
        payloadDetached = false;

        flightTicks = 0;
        flightTime = 0.0D;

        exhaustTimer = 0.0D;
        contrailTimer = 0.0D;
        fuelConsumptionRemainder = 0.0D;

        estimatedDragCoefficient = INITIAL_DRAG_ESTIMATE;
        previousPhysicsStep = 0.0D;
        hasPreviousVelocity = false;

        previousVelocity.zero();
        previousAppliedAcceleration.zero();

        setChanged();

        SubLevelAssemblyHelper.assembleBlocks(
                serverLevel,
                root,
                blocks,
                bounds
        );

        return true;
    }

    private Vector3d getGroundTargetPosition() {
        return new Vector3d(
                target.getX() + 0.5D,
                targetGroundY,
                target.getZ() + 0.5D
        );
    }

    private double horizontalDistance(Vector3d first, Vector3d second) {
        double dx = second.x - first.x;
        double dz = second.z - first.z;

        return Math.sqrt(dx * dx + dz * dz);
    }

    private double getHorizontalRouteProgress(Vector3d position) {
        Vector3d targetPosition = getGroundTargetPosition();

        double routeX = targetPosition.x - launchPosition.x;
        double routeZ = targetPosition.z - launchPosition.z;

        double routeLengthSquared = routeX * routeX + routeZ * routeZ;

        if (routeLengthSquared < 1.0D) {
            return 1.0D;
        }

        double travelledX = position.x - launchPosition.x;
        double travelledZ = position.z - launchPosition.z;

        return (travelledX * routeX + travelledZ * routeZ)
                / routeLengthSquared;
    }

    private Vector3d getDesiredFlightDirection(Vector3d position) {
        Vector3d targetPosition = getGroundTargetPosition();

        double totalHorizontalDistance = Math.max(
                1.0D,
                horizontalDistance(launchPosition, targetPosition)
        );

        double remainingHorizontalDistance = horizontalDistance(
                position,
                targetPosition
        );

        double progress = Mth.clamp(
                1.0D - remainingHorizontalDistance / totalHorizontalDistance,
                0.0D,
                1.0D
        );

        double futureProgress = Math.min(
                1.0D,
                progress + GUIDANCE_LOOKAHEAD
        );

        double altitudeProfile = Math.sin(Math.PI * futureProgress);
        double waypointY = launchPosition.y + boostAltitude * altitudeProfile;

        Vector3d waypoint = new Vector3d(
                Mth.lerp(futureProgress, launchPosition.x, targetPosition.x),
                waypointY,
                Mth.lerp(futureProgress, launchPosition.z, targetPosition.z)
        );

        Vector3d direction = waypoint.sub(position);

        if (direction.lengthSquared() < 0.000001D) {
            direction.set(
                    targetPosition.x - position.x,
                    targetPosition.y - position.y,
                    targetPosition.z - position.z
            );
        }

        if (direction.lengthSquared() < 0.000001D) {
            direction.set(0.0D, -1.0D, 0.0D);
        }

        return direction.normalize();
    }

    private void updateFlight(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        if (!handle.isValid() || timeStep <= 0.0D || flightEnded) return;

        Vector3d position = new Vector3d(subLevel.logicalPose().position());
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());

        updateDragEstimate(velocity);

        if (!initialVelocityApplied) {
            initialVelocityApplied = true;
            exhaustTimer = 0.0D;

            if (poweredAscent && hasFuel()) {
                spawnLaunchIgnition(subLevel, position);
            }

            setChanged();
        }

        flightTicks++;
        flightTime += timeStep;

        previousAppliedAcceleration.zero();

        if (poweredAscent) {
            updatePoweredFlight(
                    subLevel,
                    handle,
                    position,
                    velocity,
                    timeStep
            );
        }

        if (!poweredAscent && !payloadDetached) {
            contrailTimer += timeStep;

            while (contrailTimer >= CONTRAIL_INTERVAL) {
                contrailTimer -= CONTRAIL_INTERVAL;

                Vector3d forward = new Vector3d(MISSILE_FORWARD);
                subLevel.logicalPose().orientation().transform(forward);
                forward.normalize();

                spawnContrail(subLevel, position, forward);
            }
        } else {
            contrailTimer = 0.0D;
        }

        previousVelocity.set(velocity);
        previousPhysicsStep = timeStep;
        hasPreviousVelocity = true;
    }

    private void updateDragEstimate(Vector3d currentVelocity) {
        if (!hasPreviousVelocity || previousPhysicsStep <= 0.0D) {
            return;
        }

        double previousSpeedSquared = previousVelocity.lengthSquared();

        if (previousSpeedSquared < 0.01D) {
            return;
        }

        Vector3d expectedVelocity = new Vector3d(previousVelocity)
                .fma(previousPhysicsStep, previousAppliedAcceleration);

        expectedVelocity.y -= GRAVITY * previousPhysicsStep;

        Vector3d velocityDifference = new Vector3d(currentVelocity)
                .sub(expectedVelocity);

        double measuredDrag = -velocityDifference.dot(previousVelocity)
                / (previousSpeedSquared * previousPhysicsStep);

        if (!Double.isFinite(measuredDrag) || measuredDrag < 0.0D) {
            return;
        }

        measuredDrag = Mth.clamp(
                measuredDrag,
                0.0D,
                DRAG_ESTIMATE_MAXIMUM
        );

        estimatedDragCoefficient = Mth.clamp(
                Mth.lerp(
                        DRAG_ESTIMATE_RESPONSE,
                        estimatedDragCoefficient,
                        measuredDrag
                ),
                0.0D,
                DRAG_ESTIMATE_MAXIMUM
        );
    }

    private void updateOrientation(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            Vector3d position,
            Vector3d desiredDirection,
            double timeStep
    ) {
        if (desiredDirection.lengthSquared() < 0.000001D) return;

        Quaterniond currentOrientation = new Quaterniond(
                subLevel.logicalPose().orientation()
        );

        Quaterniond desiredOrientation = new Quaterniond()
                .rotationTo(MISSILE_FORWARD, desiredDirection);

        double response = 1.0D - Math.exp(
                -ORIENTATION_RESPONSE * timeStep
        );

        currentOrientation.slerp(desiredOrientation, response);

        handle.teleport(position, currentOrientation);

        Vector3d angularVelocity = handle.getAngularVelocity(new Vector3d());

        if (angularVelocity.lengthSquared() > 0.000001D) {
            handle.addLinearAndAngularVelocity(
                    new Vector3d(),
                    angularVelocity.negate()
            );
        }
    }

    private void updatePoweredFlight(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            Vector3d position,
            Vector3d velocity,
            double timeStep
    ) {
        if (!hasFuel()) {
            poweredAscent = false;
            setChanged();
            return;
        }

        if (shouldCutOffEngine(position, velocity)) {
            poweredAscent = false;
            setChanged();
            return;
        }

        if (flightTime >= GUIDANCE_DELAY) {
            Vector3d desiredDirection = getDesiredFlightDirection(position);

            updateOrientation(
                    subLevel,
                    handle,
                    position,
                    desiredDirection,
                    timeStep
            );
        }

        double fuelFraction = consumeFuel(timeStep);

        if (fuelFraction <= 0.0D) {
            poweredAscent = false;
            setChanged();
            return;
        }

        Vector3d forward = new Vector3d(MISSILE_FORWARD);
        subLevel.logicalPose().orientation().transform(forward);
        forward.normalize();

        double forwardSpeed = velocity.dot(forward);

        if (forwardSpeed < MAX_SPEED) {
            double availableAcceleration = Math.min(
                    THRUST_ACCELERATION,
                    (MAX_SPEED - forwardSpeed) / Math.max(timeStep, 0.0001D)
            );

            double actualAcceleration = availableAcceleration * fuelFraction;

            Vector3d thrust = new Vector3d(forward)
                    .mul(actualAcceleration * timeStep);

            handle.addLinearAndAngularVelocity(
                    thrust,
                    new Vector3d()
            );

            previousAppliedAcceleration.set(forward)
                    .mul(actualAcceleration);
        }

        exhaustTimer += timeStep;

        while (exhaustTimer >= EXHAUST_INTERVAL) {
            exhaustTimer -= EXHAUST_INTERVAL;
            spawnExhaust(subLevel, position, forward);
        }

        if (!hasFuel()) {
            poweredAscent = false;
        }

        setChanged();
    }

    private boolean shouldCutOffEngine(
            Vector3d position,
            Vector3d velocity
    ) {
        Vector3d predictedImpact = predictImpactPosition(
                position,
                velocity,
                getGroundTargetPosition()
        );

        if (predictedImpact == null) {
            return false;
        }

        Vector3d targetPosition = getGroundTargetPosition();

        double targetDx = targetPosition.x - launchPosition.x;
        double targetDz = targetPosition.z - launchPosition.z;

        double targetRange = Math.sqrt(
                targetDx * targetDx + targetDz * targetDz
        );

        if (targetRange < 1.0D) {
            return true;
        }

        double directionX = targetDx / targetRange;
        double directionZ = targetDz / targetRange;

        double predictedDx = predictedImpact.x - launchPosition.x;
        double predictedDz = predictedImpact.z - launchPosition.z;

        double predictedAlongTrack = predictedDx * directionX
                + predictedDz * directionZ;

        return predictedAlongTrack >= targetRange - CUTOFF_DISTANCE_MARGIN;
    }

    private double consumeFuel(double timeStep) {
        if (fuelTank.isEmpty() || timeStep <= 0.0D) {
            return 0.0D;
        }

        double exactDemand = FUEL_CONSUMPTION_MB_PER_SECOND * timeStep
                + fuelConsumptionRemainder;

        if (exactDemand <= 0.0D) {
            return 1.0D;
        }

        int available = fuelTank.getFluidAmount();

        if (exactDemand >= available) {
            fuelTank.drain(
                    available,
                    IFluidHandler.FluidAction.EXECUTE
            );

            fuelConsumptionRemainder = 0.0D;

            return Mth.clamp(
                    available / exactDemand,
                    0.0D,
                    1.0D
            );
        }

        int wholeAmount = (int) exactDemand;

        fuelConsumptionRemainder = exactDemand - wholeAmount;

        if (wholeAmount > 0) {
            fuelTank.drain(
                    wholeAmount,
                    IFluidHandler.FluidAction.EXECUTE
            );
        }

        return 1.0D;
    }

    private Vector3d predictImpactPosition(
            Vector3d startingPosition,
            Vector3d startingVelocity,
            Vector3d targetPosition
    ) {
        Vector3d position = new Vector3d(startingPosition);
        Vector3d velocity = new Vector3d(startingVelocity);

        double time = 0.0D;
        double step = SIMULATION_STEP;

        double drag = estimatedDragCoefficient;

        while (time < SIMULATION_MAX_TIME) {
            double dragFactor = Math.max(
                    0.0D,
                    1.0D - drag * step
            );

            velocity.mul(dragFactor);
            velocity.y -= GRAVITY * step;

            Vector3d previousPosition = new Vector3d(position);
            position.fma(step, velocity);

            time += step;

            if (position.y <= targetPosition.y) {
                double fraction = previousPosition.y == position.y
                        ? 1.0D
                        : Mth.clamp(
                        (previousPosition.y - targetPosition.y)
                        / (previousPosition.y - position.y),
                        0.0D,
                        1.0D
                );

                return new Vector3d(
                        Mth.lerp(fraction, previousPosition.x, position.x),
                        targetPosition.y,
                        Mth.lerp(fraction, previousPosition.z, position.z)
                );
            }

            if (position.y < targetPosition.y - 100.0D) {
                return null;
            }
        }

        return null;
    }

    private double getPredictedImpactError(
            Vector3d position,
            Vector3d velocity,
            Vector3d targetPosition
    ) {
        Vector3d impact = predictImpactPosition(
                position,
                velocity,
                targetPosition
        );

        if (impact == null) {
            return Double.MAX_VALUE;
        }

        double dx = targetPosition.x - impact.x;
        double dz = targetPosition.z - impact.z;

        return Math.sqrt(dx * dx + dz * dz);
    }

    private double findGroundBelow(ServerLevel serverLevel, Vector3d position) {
        int x = Mth.floor(position.x);
        int z = Mth.floor(position.z);

        int startY = Math.min(
                Mth.floor(position.y) - 1,
                serverLevel.getMaxBuildHeight() - 1
        );

        for (int y = startY; y >= serverLevel.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState state = serverLevel.getBlockState(checkPos);

            if (isValidGroundBlock(serverLevel, checkPos, state)) {
                return y + 1.0D;
            }
        }

        return serverLevel.getMinBuildHeight();
    }

    private <T extends ParticleOptions> void sendParticles(
            ServerLevel serverLevel,
            T particle,
            double x,
            double y,
            double z,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed
    ) {
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(x, y, z) > PARTICLE_VIEW_DISTANCE_SQUARED) {
                continue;
            }

            serverLevel.sendParticles(
                    player,
                    particle,
                    true,
                    x,
                    y,
                    z,
                    count,
                    offsetX,
                    offsetY,
                    offsetZ,
                    speed
            );
        }
    }

    private void spawnLaunchIgnition(ServerSubLevel subLevel, Vector3d position) {
        ServerLevel serverLevel = subLevel.getLevel();
        RandomSource random = RandomSource.create();

        Vector3d forward = new Vector3d(MISSILE_FORWARD);
        subLevel.logicalPose().orientation().transform(forward);
        forward.normalize();

        Vector3d exhaustPosition = new Vector3d(position)
                .fma(-EXHAUST_OFFSET, forward);

        for (int i = 0; i < 22; i++) {
            Vec3 motion = new Vec3(
                    -forward.x * (0.35D + random.nextDouble() * 0.55D) + (random.nextDouble() - 0.5D) * 0.22D,
                    -forward.y * (0.35D + random.nextDouble() * 0.55D) + (random.nextDouble() - 0.5D) * 0.22D,
                    -forward.z * (0.35D + random.nextDouble() * 0.55D) + (random.nextDouble() - 0.5D) * 0.22D
            );

            int color = randomExhaustColor(random);

            sendParticles(
                    serverLevel,
                    new SmokeParticleOptions(
                            1.1F + random.nextFloat() * 0.9F,
                            color,
                            color,
                            false,
                            35,
                            motion,
                            45
                    ),
                    exhaustPosition.x + (random.nextDouble() - 0.5D) * 0.8D,
                    exhaustPosition.y + (random.nextDouble() - 0.5D) * 0.8D,
                    exhaustPosition.z + (random.nextDouble() - 0.5D) * 0.8D,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private void spawnExhaust(
            ServerSubLevel subLevel,
            Vector3d position,
            Vector3d forward
    ) {
        ServerLevel serverLevel = subLevel.getLevel();
        RandomSource random = RandomSource.create();

        Vector3d exhaustPosition = new Vector3d(position)
                .fma(-EXHAUST_OFFSET, forward);

        for (int i = 0; i < TRAIL_PARTICLE_COUNT; i++) {
            int color = randomExhaustColor(random);

            Vec3 exhaustMotion = new Vec3(
                    -forward.x * (0.35D + random.nextDouble() * 0.25D) + (random.nextDouble() - 0.5D) * 0.10D,
                    -forward.y * (0.35D + random.nextDouble() * 0.25D) + (random.nextDouble() - 0.5D) * 0.10D,
                    -forward.z * (0.35D + random.nextDouble() * 0.25D) + (random.nextDouble() - 0.5D) * 0.10D
            );

            sendParticles(
                    serverLevel,
                    new SmokeParticleOptions(
                            EXHAUST_PARTICLE_SIZE * (1.0F + random.nextFloat() * 0.65F),
                            color,
                            color,
                            false,
                            TRAIL_PARTICLE_LIFETIME,
                            exhaustMotion,
                            40
                    ),
                    exhaustPosition.x,
                    exhaustPosition.y,
                    exhaustPosition.z,
                    1,
                    0.20D,
                    0.20D,
                    0.20D,
                    0.0D
            );
        }
    }

    private void spawnContrail(
            ServerSubLevel subLevel,
            Vector3d position,
            Vector3d forward
    ) {
        Vector3d trailPosition = new Vector3d(position)
                .fma(-EXHAUST_OFFSET, forward);

        sendParticles(
                subLevel.getLevel(),
                new SmokeParticleOptions(
                        CONTRAIL_PARTICLE_SIZE,
                        0xFFF5F5F5,
                        0xFFDADADA,
                        false,
                        CONTRAIL_PARTICLE_LIFETIME,
                        new Vec3(0.0D, 0.008D, 0.0D),
                        40
                ),
                trailPosition.x,
                trailPosition.y,
                trailPosition.z,
                1,
                0.08D,
                0.08D,
                0.08D,
                0.0D
        );
    }

    private void spawnDetachRing(ServerSubLevel subLevel, BlockPos payloadPos) {
        ServerLevel serverLevel = subLevel.getLevel();
        RandomSource random = RandomSource.create();

        Quaterniond orientation = new Quaterniond(
                subLevel.logicalPose().orientation()
        );

        Vector3d forward = new Vector3d(MISSILE_FORWARD);
        orientation.transform(forward);
        forward.normalize();

        Vector3d reference = Math.abs(
                forward.dot(new Vector3d(0.0D, 1.0D, 0.0D))
        ) > 0.95D
                ? new Vector3d(1.0D, 0.0D, 0.0D)
                : new Vector3d(0.0D, 1.0D, 0.0D);

        Vector3d right = forward.cross(
                reference,
                new Vector3d()
        ).normalize();

        Vector3d up = right.cross(
                forward,
                new Vector3d()
        ).normalize();

        Vector3d localCenter = new Vector3d(
                payloadPos.getX() + 0.5D,
                payloadPos.getY() + 0.5D,
                payloadPos.getZ() + 0.5D
        );

        subLevel.logicalPose().transformPosition(localCenter);

        for (int i = 0; i < DETACH_RING_PARTICLES; i++) {
            double angle = Math.PI * 2.0D * i / DETACH_RING_PARTICLES;

            Vector3d radial = new Vector3d(right)
                    .mul(Math.cos(angle))
                    .fma(Math.sin(angle), up);

            Vector3d particlePosition = new Vector3d(localCenter)
                    .fma(DETACH_RING_RADIUS, radial);

            Vec3 particleMotion = new Vec3(
                    radial.x * DETACH_RING_PARTICLE_SPEED + forward.x * 0.04D,
                    radial.y * DETACH_RING_PARTICLE_SPEED + forward.y * 0.04D,
                    radial.z * DETACH_RING_PARTICLE_SPEED + forward.z * 0.04D
            );

            sendParticles(
                    serverLevel,
                    new SmokeParticleOptions(
                            0.9F + random.nextFloat() * 0.35F,
                            0xFFE6E6E6,
                            0xFFE6E6E6,
                            false,
                            45,
                            particleMotion,
                            30
                    ),
                    particlePosition.x,
                    particlePosition.y,
                    particlePosition.z,
                    1,
                    0.035D,
                    0.035D,
                    0.035D,
                    0.0D
            );
        }
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        if (!launched || flightEnded || !handle.isValid()) return;

        ensureSubLevelChunkLoaded(subLevel);
        updateFlight(subLevel, handle, timeStep);
    }

    private Vector3d getPayloadWorldPosition(ServerSubLevel subLevel) {
        BlockPos payloadPos = subLevel.getPlot()
                .getCenterBlock()
                .above(HEIGHT);

        Vector3d position = new Vector3d(
                payloadPos.getX() + 0.5D,
                payloadPos.getY() + 0.5D,
                payloadPos.getZ() + 0.5D
        );

        subLevel.logicalPose().transformPosition(position);

        return position;
    }

    private void beginPayloadRelease(ServerSubLevel subLevel) {
        if (payloadDetached) return;

        poweredAscent = false;
        fuelConsumptionRemainder = 0.0D;
        exhaustTimer = 0.0D;
        contrailTimer = 0.0D;

        if (payloadPresent) {
            BlockPos payloadPos = subLevel.getPlot()
                    .getCenterBlock()
                    .above(HEIGHT);

            Vector3d forward = new Vector3d(MISSILE_FORWARD);
            subLevel.logicalPose().orientation().transform(forward);
            forward.normalize();

            spawnDetachRing(subLevel, payloadPos);

            PENDING_SEPARATIONS.add(
                    new PendingPayloadSeparation(
                            subLevel.getLevel(),
                            payloadPos.immutable(),
                            new Vector3d(forward)
                    )
            );
        }

        payloadDetached = true;
        setChanged();
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        if (!launched || flightEnded || !initialVelocityApplied || payloadDetached) {
            return;
        }

        ensureSubLevelChunkLoaded(subLevel);

        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);

        if (handle == null || !handle.isValid()) {
            return;
        }

        Vector3d payloadPosition = getPayloadWorldPosition(subLevel);
        double routeProgress = getHorizontalRouteProgress(payloadPosition);

        if (routeProgress >= PAYLOAD_RELEASE_PROGRESS) {
            beginPayloadRelease(subLevel);
        }
    }

    private void sync() {
        if (level == null) return;

        level.sendBlockUpdated(
                worldPosition,
                getBlockState(),
                getBlockState(),
                Block.UPDATE_CLIENTS
        );
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.putInt("TargetX", target.getX());
        tag.putInt("TargetY", target.getY());
        tag.putInt("TargetZ", target.getZ());

        tag.putBoolean("Launched", launched);
        tag.putBoolean("LaunchPending", launchPending);
        tag.putBoolean("Armed", armed);
        tag.putBoolean("LastRedstonePowered", lastRedstonePowered);
        tag.putBoolean("FlightEnded", flightEnded);
        tag.putBoolean("InitialVelocityApplied", initialVelocityApplied);
        tag.putBoolean("PoweredAscent", poweredAscent);
        tag.putBoolean("PayloadPresent", payloadPresent);
        tag.putBoolean("PayloadDetached", payloadDetached);

        tag.putBoolean("HasPreviousVelocity", hasPreviousVelocity);

        tag.putInt("LaunchDelay", launchDelay);
        tag.putInt("FlightTicks", flightTicks);

        tag.putDouble("FlightTime", flightTime);
        tag.putDouble("BoostAltitude", boostAltitude);
        tag.putDouble("TargetGroundY", targetGroundY);
        tag.putDouble("FuelConsumptionRemainder", fuelConsumptionRemainder);
        tag.putDouble("ExhaustTimer", exhaustTimer);
        tag.putDouble("ContrailTimer", contrailTimer);
        tag.putDouble("EstimatedDragCoefficient", estimatedDragCoefficient);
        tag.putDouble("PreviousPhysicsStep", previousPhysicsStep);

        tag.putDouble("LaunchPositionX", launchPosition.x);
        tag.putDouble("LaunchPositionY", launchPosition.y);
        tag.putDouble("LaunchPositionZ", launchPosition.z);

        tag.putDouble("PreviousVelocityX", previousVelocity.x);
        tag.putDouble("PreviousVelocityY", previousVelocity.y);
        tag.putDouble("PreviousVelocityZ", previousVelocity.z);

        tag.putDouble("PreviousAccelerationX", previousAppliedAcceleration.x);
        tag.putDouble("PreviousAccelerationY", previousAppliedAcceleration.y);
        tag.putDouble("PreviousAccelerationZ", previousAppliedAcceleration.z);

        tag.put("FuelTank", fuelTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        target = new BlockPos(
                tag.getInt("TargetX"),
                tag.getInt("TargetY"),
                tag.getInt("TargetZ")
        );

        launched = tag.getBoolean("Launched");
        launchPending = tag.getBoolean("LaunchPending");
        armed = tag.getBoolean("Armed");
        lastRedstonePowered = tag.getBoolean("LastRedstonePowered");
        flightEnded = tag.getBoolean("FlightEnded");
        initialVelocityApplied = tag.getBoolean("InitialVelocityApplied");
        poweredAscent = tag.getBoolean("PoweredAscent");
        payloadPresent = tag.getBoolean("PayloadPresent");
        payloadDetached = tag.getBoolean("PayloadDetached");

        hasPreviousVelocity = tag.getBoolean("HasPreviousVelocity");

        launchDelay = Mth.clamp(tag.getInt("LaunchDelay"), 0, 120);
        flightTicks = tag.getInt("FlightTicks");

        flightTime = tag.getDouble("FlightTime");
        boostAltitude = tag.getDouble("BoostAltitude");
        targetGroundY = tag.getDouble("TargetGroundY");
        fuelConsumptionRemainder = tag.getDouble("FuelConsumptionRemainder");
        exhaustTimer = tag.getDouble("ExhaustTimer");
        contrailTimer = tag.getDouble("ContrailTimer");

        estimatedDragCoefficient = Mth.clamp(
                tag.getDouble("EstimatedDragCoefficient"),
                0.0D,
                DRAG_ESTIMATE_MAXIMUM
        );

        previousPhysicsStep = tag.getDouble("PreviousPhysicsStep");

        launchPosition.set(
                tag.getDouble("LaunchPositionX"),
                tag.getDouble("LaunchPositionY"),
                tag.getDouble("LaunchPositionZ")
        );

        previousVelocity.set(
                tag.getDouble("PreviousVelocityX"),
                tag.getDouble("PreviousVelocityY"),
                tag.getDouble("PreviousVelocityZ")
        );

        previousAppliedAcceleration.set(
                tag.getDouble("PreviousAccelerationX"),
                tag.getDouble("PreviousAccelerationY"),
                tag.getDouble("PreviousAccelerationZ")
        );

        if (tag.contains("FuelTank")) {
            fuelTank.readFromNBT(registries, tag.getCompound("FuelTank"));
        }
    }
}