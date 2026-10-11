package com.julian.createwarfare.blocks.entities;

import com.julian.createwarfare.blocks.AirToAirMissileBlock;
import com.julian.createwarfare.effects.server.ExplosionParticleEffect;
import com.julian.createwarfare.effects.server.SoundWaveEffect;
import com.julian.createwarfare.explosions.types.GenericExplosion;
import com.julian.createwarfare.registry.CWSoundEvents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AirToAirMissileBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    private static final int MISSILE_PARTS = 3;

    private static final double ARM_DELAY = 0.4;
    private static final double MOTOR_BURN_DURATION = 4.0;
    private static final double MOTOR_ACCELERATION = 120.0;
    private static final double MAX_SPEED = 350.0;
    private static final double GRAVITY_COMPENSATION = 0.92;
    private static final double DOWNWARD_BIAS = 0.025;
    private static final double CONTACT_RADIUS = 0.12;
    private static final double SAMPLE_OFFSET = 0.3;

    private boolean activated;
    private boolean launched;
    private boolean armed;
    private boolean detonationQueued;

    private double armingTime;
    private double flightTime;

    private transient Vec3[] previousCollisionSamples;

    public AirToAirMissileBlockEntity(
            BlockEntityType<AirToAirMissileBlockEntity> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    public void activate() {
        if (!(level instanceof ServerLevel serverLevel) || activated) {
            return;
        }

        BlockState state = getBlockState();
        Direction facing = state.getValue(AirToAirMissileBlock.FACING);
        BlockPos root = getBlockPos();
        BlockPos end = root.relative(facing, MISSILE_PARTS - 1);

        BoundingBox3i bounds = new BoundingBox3i(
                Math.min(root.getX(), end.getX()),
                Math.min(root.getY(), end.getY()),
                Math.min(root.getZ(), end.getZ()),
                Math.max(root.getX(), end.getX()),
                Math.max(root.getY(), end.getY()),
                Math.max(root.getZ(), end.getZ())
        );

        activated = true;
        setChanged();
        previousCollisionSamples = null;

        SubLevelAssemblyHelper.assembleBlocks(
                serverLevel,
                root,
                AirToAirMissileBlock.getAllPositions(root, state),
                bounds
        );
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.putBoolean("Activated", activated);
        tag.putBoolean("Launched", launched);
        tag.putBoolean("Armed", armed);
        tag.putBoolean("DetonationQueued", detonationQueued);
        tag.putDouble("ArmingTime", armingTime);
        tag.putDouble("FlightTime", flightTime);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        activated = tag.getBoolean("Activated");
        launched = tag.getBoolean("Launched");
        armed = tag.getBoolean("Armed");
        detonationQueued = tag.getBoolean("DetonationQueued");
        armingTime = tag.getDouble("ArmingTime");
        flightTime = tag.getDouble("FlightTime");
        previousCollisionSamples = null;
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        if (!activated || detonationQueued || handle == null || !handle.isValid()) {
            return;
        }

        if (level == null || level.isClientSide) {
            return;
        }

        ServerLevel serverLevel = subLevel.getLevel();
        SubLevel containing = Sable.HELPER.getContaining(this);

        if (!(containing instanceof ServerSubLevel missileSubLevel)
                || missileSubLevel.isRemoved()) {
            return;
        }

        double dt = Math.min(Math.max(timeStep, 0.0), 0.1);

        if (dt <= 0.0) {
            return;
        }

        if (!launched) {
            launched = true;
            armingTime = 0.0;
            flightTime = 0.0;
            emitLaunchParticles(serverLevel, missileSubLevel);
            setChanged();
        }

        armingTime += dt;
        flightTime += dt;

        boolean justArmed = false;

        if (!armed && armingTime >= ARM_DELAY) {
            armed = true;
            justArmed = true;
            setChanged();
        }

        Direction facing = getBlockState().getValue(AirToAirMissileBlock.FACING);
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());

        Vec3[] currentSamples = buildCollisionSamples(missileSubLevel, facing);

        if (!armed || justArmed) {
            previousCollisionSamples = currentSamples;
        } else {
            CollisionResult collision = findSweptCollision(
                    serverLevel,
                    missileSubLevel,
                    currentSamples,
                    velocity,
                    dt,
                    facing
            );

            if (collision != null) {
                queueDetonation(
                        serverLevel,
                        missileSubLevel,
                        collision.position()
                );
                return;
            }
        }

        if (flightTime <= MOTOR_BURN_DURATION) {
            Vector3d forward = getWorldForward(missileSubLevel, facing);

            forward.y -= DOWNWARD_BIAS;
            forward.normalize();

            Vector3d velocityDelta = new Vector3d(forward)
                    .mul(MOTOR_ACCELERATION * dt);

            Vector3d predictedVelocity = new Vector3d(velocity)
                    .add(velocityDelta);

            if (predictedVelocity.length() > MAX_SPEED) {
                predictedVelocity.normalize().mul(MAX_SPEED);
                velocityDelta.set(predictedVelocity).sub(velocity);
            }

            velocityDelta.y += 9.81 * GRAVITY_COMPENSATION * dt;

            handle.addLinearAndAngularVelocity(
                    velocityDelta,
                    new Vector3d()
            );
        }

        if (flightTime <= MOTOR_BURN_DURATION) {
            emitFlightParticles(serverLevel, missileSubLevel, facing);
        }
    }

    private Vec3[] buildCollisionSamples(
            ServerSubLevel missileSubLevel,
            Direction facing
    ) {
        Vector3d sideA;
        Vector3d sideB;

        switch (facing.getAxis()) {
            case X -> {
                sideA = new Vector3d(0.0, 1.0, 0.0);
                sideB = new Vector3d(0.0, 0.0, 1.0);
            }
            case Y -> {
                sideA = new Vector3d(1.0, 0.0, 0.0);
                sideB = new Vector3d(0.0, 0.0, 1.0);
            }
            case Z -> {
                sideA = new Vector3d(1.0, 0.0, 0.0);
                sideB = new Vector3d(0.0, 1.0, 0.0);
            }
            default -> throw new IllegalStateException();
        }

        Quaterniondc orientation = missileSubLevel.logicalPose().orientation();
        orientation.transform(sideA);
        orientation.transform(sideB);
        sideA.normalize();
        sideB.normalize();

        List<Vec3> samples = new ArrayList<>(MISSILE_PARTS * 5);
        BlockPos root = getBlockPos();

        for (int part = 0; part < MISSILE_PARTS; part++) {
            BlockPos partPos = root.relative(facing, part);
            Vec3 localCenter = Vec3.atCenterOf(partPos);
            Vec3 center = missileSubLevel.logicalPose().transformPosition(localCenter);

            samples.add(center);
            samples.add(center.add(
                    sideA.x * SAMPLE_OFFSET,
                    sideA.y * SAMPLE_OFFSET,
                    sideA.z * SAMPLE_OFFSET
            ));
            samples.add(center.add(
                    -sideA.x * SAMPLE_OFFSET,
                    -sideA.y * SAMPLE_OFFSET,
                    -sideA.z * SAMPLE_OFFSET
            ));
            samples.add(center.add(
                    sideB.x * SAMPLE_OFFSET,
                    sideB.y * SAMPLE_OFFSET,
                    sideB.z * SAMPLE_OFFSET
            ));
            samples.add(center.add(
                    -sideB.x * SAMPLE_OFFSET,
                    -sideB.y * SAMPLE_OFFSET,
                    -sideB.z * SAMPLE_OFFSET
            ));
        }

        return samples.toArray(Vec3[]::new);
    }

    private CollisionResult findSweptCollision(
            ServerLevel serverLevel,
            ServerSubLevel missileSubLevel,
            Vec3[] currentSamples,
            Vector3d velocity,
            double dt,
            Direction facing
    ) {
        Vec3 displacement = new Vec3(
                velocity.x,
                velocity.y,
                velocity.z
        ).scale(dt);

        CollisionResult result = null;

        if (previousCollisionSamples != null
                && previousCollisionSamples.length == currentSamples.length) {
            for (int i = 0; i < currentSamples.length; i++) {
                result = earlierCollision(
                        result,
                        traceSegment(
                                serverLevel,
                                missileSubLevel,
                                previousCollisionSamples[i],
                                currentSamples[i],
                                facing
                        )
                );
            }
        }

        if (result == null) {
            for (Vec3 sample : currentSamples) {
                result = earlierCollision(
                        result,
                        traceSegment(
                                serverLevel,
                                missileSubLevel,
                                sample,
                                sample.add(displacement),
                                facing
                        )
                );
            }
        }

        previousCollisionSamples = currentSamples;
        return result;
    }

    private CollisionResult traceSegment(
            ServerLevel serverLevel,
            ServerSubLevel missileSubLevel,
            Vec3 start,
            Vec3 originalEnd,
            Direction facing
    ) {
        Vec3 end = originalEnd;

        if (start.distanceToSqr(end) < 1.0e-10) {
            Vector3d forward = getWorldForward(missileSubLevel, facing);
            end = start.add(
                    forward.x * 0.08,
                    forward.y * 0.08,
                    forward.z * 0.08
            );
        }

        double bestFraction = Double.POSITIVE_INFINITY;
        CollisionResult best = null;

        ClipContext context = new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                (Entity) null
        );

        ((ClipContextExtension) context)
                .sable$setIgnoredSubLevel(missileSubLevel);

        BlockHitResult blockHit = serverLevel.clip(context);

        if (blockHit.getType() != HitResult.Type.MISS) {
            double fraction = segmentFraction(start, end, blockHit.getLocation());
            bestFraction = fraction;
            best = new CollisionResult(blockHit.getLocation(), fraction);
        }

        AABB sweep = new AABB(
                Math.min(start.x, end.x),
                Math.min(start.y, end.y),
                Math.min(start.z, end.z),
                Math.max(start.x, end.x),
                Math.max(start.y, end.y),
                Math.max(start.z, end.z)
        ).inflate(CONTACT_RADIUS);

        for (Entity entity : serverLevel.getEntities(
                (Entity) null,
                sweep,
                entity -> entity.isAlive() && !entity.isSpectator()
        )) {
            AABB entityBox = entity.getBoundingBox().inflate(CONTACT_RADIUS);
            Vec3 hitPosition = null;

            if (entityBox.contains(start)) {
                hitPosition = start;
            } else {
                Optional<Vec3> entityHit = entityBox.clip(start, end);

                if (entityHit.isPresent()) {
                    hitPosition = entityHit.get();
                }
            }

            if (hitPosition != null) {
                double fraction = segmentFraction(start, end, hitPosition);

                if (fraction < bestFraction) {
                    bestFraction = fraction;
                    best = new CollisionResult(hitPosition, fraction);
                }
            }
        }

        SubLevelContainer container = SubLevelContainer.getContainer(serverLevel);

        if (container != null) {
            for (SubLevel candidate : container.getAllSubLevels()) {
                if (candidate == missileSubLevel || candidate.isRemoved()) {
                    continue;
                }

                double fraction = segmentAabbEntry(
                        start,
                        end,
                        candidate.boundingBox(),
                        CONTACT_RADIUS
                );

                if (Double.isFinite(fraction) && fraction < bestFraction) {
                    bestFraction = fraction;
                    best = new CollisionResult(
                            interpolate(start, end, fraction),
                            fraction
                    );
                }
            }
        }

        return best;
    }

    private void queueDetonation(
            ServerLevel serverLevel,
            ServerSubLevel missileSubLevel,
            Vec3 impactPosition
    ) {
        if (detonationQueued) {
            return;
        }

        detonationQueued = true;
        setChanged();

        BlockPos impactPos = BlockPos.containing(
                impactPosition.x,
                impactPosition.y,
                impactPosition.z
        );

        serverLevel.getServer().execute(
                () -> trigger(serverLevel, missileSubLevel, impactPos)
        );
    }

    private void trigger(
            ServerLevel serverLevel,
            ServerSubLevel missileSubLevel,
            BlockPos impactPos
    ) {
        if (missileSubLevel.isRemoved()) {
            return;
        }

        GenericExplosion.trigger(
                serverLevel,
                impactPos,
                100.0f,
                4.0f,
                3.0f
        );

        SoundWaveEffect.start(
                serverLevel,
                impactPos,
                8.0f,
                80.0f,
                CWSoundEvents.EXPLOSION.getMainEvent(),
                true
        );

        ExplosionParticleEffect.start(
                serverLevel,
                impactPos,
                1.2f
        );

        missileSubLevel.markRemoved();
    }

    private void emitLaunchParticles(
            ServerLevel serverLevel,
            ServerSubLevel missileSubLevel
    ) {
        Vec3 center = getWorldCenter(missileSubLevel);

        serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                center.x,
                center.y,
                center.z,
                20,
                0.25,
                0.25,
                0.25,
                0.04
        );
    }

    private void emitFlightParticles(
            ServerLevel serverLevel,
            ServerSubLevel missileSubLevel,
            Direction facing
    ) {
        Vec3 center = getWorldCenter(missileSubLevel);
        Vector3d forward = getWorldForward(missileSubLevel, facing);

        Vec3 tail = center.subtract(
                forward.x * 1.5,
                forward.y * 1.5,
                forward.z * 1.5
        );

        serverLevel.sendParticles(
                ParticleTypes.FLAME,
                tail.x,
                tail.y,
                tail.z,
                5,
                0.12,
                0.12,
                0.12,
                0.025
        );

        serverLevel.sendParticles(
                ParticleTypes.SMOKE,
                tail.x,
                tail.y,
                tail.z,
                4,
                0.15,
                0.15,
                0.15,
                0.02
        );
    }

    private static Vec3 getWorldCenter(ServerSubLevel subLevel) {
        BoundingBox3dc bounds = subLevel.boundingBox();

        if (Double.isFinite(bounds.minX())
                && Double.isFinite(bounds.minY())
                && Double.isFinite(bounds.minZ())
                && Double.isFinite(bounds.maxX())
                && Double.isFinite(bounds.maxY())
                && Double.isFinite(bounds.maxZ())) {
            return new Vec3(
                    (bounds.minX() + bounds.maxX()) * 0.5,
                    (bounds.minY() + bounds.maxY()) * 0.5,
                    (bounds.minZ() + bounds.maxZ()) * 0.5
            );
        }

        var position = subLevel.logicalPose().position();
        return new Vec3(position.x(), position.y(), position.z());
    }

    private static Vector3d getWorldForward(
            ServerSubLevel subLevel,
            Direction facing
    ) {
        Vector3d forward = new Vector3d(
                facing.getStepX(),
                facing.getStepY(),
                facing.getStepZ()
        );

        subLevel.logicalPose().orientation().transform(forward);

        if (forward.lengthSquared() < 1.0e-8) {
            return new Vector3d(0.0, 0.0, -1.0);
        }

        return forward.normalize();
    }

    private static CollisionResult earlierCollision(
            CollisionResult current,
            CollisionResult candidate
    ) {
        if (candidate == null) {
            return current;
        }

        if (current == null || candidate.fraction() < current.fraction()) {
            return candidate;
        }

        return current;
    }

    private static double segmentFraction(
            Vec3 start,
            Vec3 end,
            Vec3 point
    ) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();

        if (lengthSquared <= 1.0e-12) {
            return 0.0;
        }

        return clamp(
                point.subtract(start).dot(segment) / lengthSquared,
                0.0,
                1.0
        );
    }

    private static Vec3 interpolate(
            Vec3 start,
            Vec3 end,
            double fraction
    ) {
        return start.add(
                (end.x - start.x) * fraction,
                (end.y - start.y) * fraction,
                (end.z - start.z) * fraction
        );
    }

    private static double segmentAabbEntry(
            Vec3 start,
            Vec3 end,
            BoundingBox3dc bounds,
            double inflate
    ) {
        return segmentAabbEntry(
                start,
                end,
                bounds.minX() - inflate,
                bounds.minY() - inflate,
                bounds.minZ() - inflate,
                bounds.maxX() + inflate,
                bounds.maxY() + inflate,
                bounds.maxZ() + inflate
        );
    }

    private static double segmentAabbEntry(
            Vec3 start,
            Vec3 end,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        if (!Double.isFinite(minX)
                || !Double.isFinite(minY)
                || !Double.isFinite(minZ)
                || !Double.isFinite(maxX)
                || !Double.isFinite(maxY)
                || !Double.isFinite(maxZ)) {
            return Double.POSITIVE_INFINITY;
        }

        double tMin = 0.0;
        double tMax = 1.0;

        double[] source = {start.x, start.y, start.z};
        double[] delta = {
                end.x - start.x,
                end.y - start.y,
                end.z - start.z
        };
        double[] mins = {minX, minY, minZ};
        double[] maxs = {maxX, maxY, maxZ};

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(delta[axis]) < 1.0e-12) {
                if (source[axis] < mins[axis] || source[axis] > maxs[axis]) {
                    return Double.POSITIVE_INFINITY;
                }

                continue;
            }

            double first = (mins[axis] - source[axis]) / delta[axis];
            double second = (maxs[axis] - source[axis]) / delta[axis];

            if (first > second) {
                double temporary = first;
                first = second;
                second = temporary;
            }

            tMin = Math.max(tMin, first);
            tMax = Math.min(tMax, second);

            if (tMin > tMax) {
                return Double.POSITIVE_INFINITY;
            }
        }

        if (tMax < 0.0 || tMin > 1.0) {
            return Double.POSITIVE_INFINITY;
        }

        return clamp(tMin, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public boolean isActivated() {
        return activated;
    }

    public boolean isArmed() {
        return armed;
    }

    private record CollisionResult(Vec3 position, double fraction) {
    }
}