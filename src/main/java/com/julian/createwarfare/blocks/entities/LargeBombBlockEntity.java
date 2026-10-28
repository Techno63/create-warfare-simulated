package com.julian.createwarfare.blocks.entities;

import com.julian.createwarfare.blocks.LargeBombBlock;
import com.julian.createwarfare.effects.server.*;
import com.julian.createwarfare.effects.server.particles.GroundExplosionParticleEffect;
import com.julian.createwarfare.explosions.post.FragmentationPost;
import com.julian.createwarfare.explosions.post.IncinerationWavePost;
import com.julian.createwarfare.explosions.post.PressureWavePost;
import com.julian.createwarfare.explosions.types.GenericExplosion;
import com.julian.createwarfare.registry.CWSoundEvents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.List;

public class LargeBombBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    private static final double ARM_DELAY = 0.6;
    private static final double IMPACT_RAY_DISTANCE = 1.0;
    private static final double MIN_IMPACT_SPEED = 1.5;

    private boolean activated;
    private boolean armed;
    private boolean impacted;

    private double armingTime;

    public LargeBombBlockEntity(
            BlockEntityType<LargeBombBlockEntity> type,
            BlockPos pos,
            BlockState state
    ) {
        super(
                type,
                pos,
                state
        );
    }

    public void activate() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (activated) {
            return;
        }

        activated = true;
        setChanged();

        BlockPos pos =
                getBlockPos();

        Direction facing =
                getBlockState().getValue(
                        LargeBombBlock.FACING
                );

        BlockPos second =
                pos.relative(
                        facing
                );

        BlockPos third =
                pos.relative(
                        facing,
                        2
                );

        BoundingBox3i bounds =
                new BoundingBox3i(
                        Math.min(
                                pos.getX(),
                                Math.min(
                                        second.getX(),
                                        third.getX()
                                )
                        ),
                        Math.min(
                                pos.getY(),
                                Math.min(
                                        second.getY(),
                                        third.getY()
                                )
                        ),
                        Math.min(
                                pos.getZ(),
                                Math.min(
                                        second.getZ(),
                                        third.getZ()
                                )
                        ),
                        Math.max(
                                pos.getX(),
                                Math.max(
                                        second.getX(),
                                        third.getX()
                                )
                        ),
                        Math.max(
                                pos.getY(),
                                Math.max(
                                        second.getY(),
                                        third.getY()
                                )
                        ),
                        Math.max(
                                pos.getZ(),
                                Math.max(
                                        second.getZ(),
                                        third.getZ()
                                )
                        )
                );

        SubLevelAssemblyHelper.assembleBlocks(
                serverLevel,
                pos,
                List.of(
                        pos,
                        second,
                        third
                ),
                bounds
        );
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        tag.putBoolean(
                "Activated",
                activated
        );

        tag.putBoolean(
                "Armed",
                armed
        );

        tag.putBoolean(
                "Impacted",
                impacted
        );

        tag.putDouble(
                "ArmingTime",
                armingTime
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        activated =
                tag.getBoolean(
                        "Activated"
                );

        armed =
                tag.getBoolean(
                        "Armed"
                );

        impacted =
                tag.getBoolean(
                        "Impacted"
                );

        armingTime =
                tag.getDouble(
                        "ArmingTime"
                );
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        if (impacted) {
            return;
        }

        if (!activated) {
            return;
        }

        if (!armed) {
            armingTime += timeStep;

            if (armingTime >= ARM_DELAY) {
                armed = true;
                setChanged();
            }

            return;
        }

        Vector3d velocity =
                handle.getLinearVelocity(
                        new Vector3d()
                );

        double speed =
                velocity.length();

        if (speed < MIN_IMPACT_SPEED) {
            return;
        }

        ServerLevel level =
                subLevel.getLevel();

        SubLevel currentSubLevel =
                Sable.HELPER.getContaining(
                        this
                );

        if (currentSubLevel == null) {
            return;
        }

        Vector3d velocityDirection =
                velocity.normalize(
                        new Vector3d()
                );

        Vec3 direction =
                new Vec3(
                        velocityDirection.x,
                        velocityDirection.y,
                        velocityDirection.z
                );

        BlockPos rootPos =
                getBlockPos();

        Direction facing =
                getBlockState().getValue(
                        LargeBombBlock.FACING
                );

        BlockPos secondPos =
                rootPos.relative(
                        facing
                );

        BlockPos thirdPos =
                rootPos.relative(
                        facing,
                        2
                );

        BlockPos impactPos =
                null;

        for (
                BlockPos partPos :
                List.of(
                        rootPos,
                        secondPos,
                        thirdPos
                )
        ) {
            Vec3 localCenter =
                    new Vec3(
                            partPos.getX() + 0.5,
                            partPos.getY() + 0.5,
                            partPos.getZ() + 0.5
                    );

            Vec3 partWorldPos =
                    currentSubLevel
                            .logicalPose()
                            .transformPosition(
                                    localCenter
                            );

            Vec3 rayEnd =
                    partWorldPos.add(
                            direction.scale(
                                    IMPACT_RAY_DISTANCE
                            )
                    );

            ClipContext clipContext =
                    new ClipContext(
                            partWorldPos,
                            rayEnd,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            (Entity) null
                    );

            ((ClipContextExtension) clipContext)
                    .sable$setIgnoredSubLevel(
                            currentSubLevel
                    );

            BlockHitResult hit =
                    level.clip(
                            clipContext
                    );

            if (
                    hit.getType() !=
                            HitResult.Type.MISS
            ) {
                impactPos =
                        hit.getBlockPos();

                break;
            }
        }

        if (impactPos == null) {
            return;
        }

        impacted = true;
        setChanged();

        BlockPos bombBlockPos =
                getBlockPos();

        BlockPos finalImpactPos = impactPos;
        level.getServer().execute(
                () -> trigger(
                        level,
                        subLevel,
                        bombBlockPos,
                        finalImpactPos
                )
        );
    }

    private void trigger(
            ServerLevel level,
            ServerSubLevel subLevel,
            BlockPos bombBlockPos,
            BlockPos impactPos
    ) {
        removeBombFromSubLevel(
                subLevel,
                bombBlockPos
        );

        level.setBlock(
                impactPos,
                Blocks.AIR.defaultBlockState(),
                3
        );

        GenericExplosion.trigger(
                level,
                impactPos,
                800f,
                15f,
                15f
        );

        SoundWaveEffect.start(
                level,
                impactPos,
                17.15f,
                400f,
                CWSoundEvents.EXPLOSION.getMainEvent(),
                true
        );

        PressureWavePost.start(
                level,
                impactPos,
                2f,
                65f,
                8f,
                false,
                true
        );

        FragmentationPost.start(
                level,
                impactPos,
                2f,
                200f,
                750,
                2f,
                28f,
                false
        );

        IncinerationWavePost.start(
                level,
                impactPos,
                2f,
                70f,
                20f,
                200,
                true
        );

        WaveEffect.start(
                level,
                impactPos,
                2f,
                70f,
                0xfff0a8,
                0.5f,
                0,
                true
        );

        GlowEffect.start(
                level,
                impactPos,
                300f,
                0xfff0a8,
                1f,
                35,
                false
        );

        VignetteEffect.start(
                level,
                impactPos,
                150f,
                190,
                0.7f
        );

        ShakeEffect.start(
                level,
                impactPos,
                300f,
                24f,
                45,
                true
        );

        GroundExplosionParticleEffect.start(
                level,
                impactPos,
                4.3f
        );
    }

    private void removeBombFromSubLevel(
            ServerSubLevel subLevel,
            BlockPos bombBlockPos
    ) {
        BlockPos center =
                subLevel
                        .getPlot()
                        .getCenterBlock();

        Direction facing =
                getBlockState().getValue(
                        LargeBombBlock.FACING
                );

        BlockPos secondPos =
                bombBlockPos.relative(
                        facing
                );

        BlockPos thirdPos =
                bombBlockPos.relative(
                        facing,
                        2
                );

        BlockPos localFirst =
                new BlockPos(
                        bombBlockPos.getX() - center.getX(),
                        bombBlockPos.getY() - center.getY(),
                        bombBlockPos.getZ() - center.getZ()
                );

        BlockPos localSecond =
                new BlockPos(
                        secondPos.getX() - center.getX(),
                        secondPos.getY() - center.getY(),
                        secondPos.getZ() - center.getZ()
                );

        BlockPos localThird =
                new BlockPos(
                        thirdPos.getX() - center.getX(),
                        thirdPos.getY() - center.getY(),
                        thirdPos.getZ() - center.getZ()
                );

        var access =
                subLevel
                        .getPlot()
                        .getEmbeddedLevelAccessor();

        access.setBlock(
                localFirst,
                Blocks.AIR.defaultBlockState(),
                3
        );

        access.setBlock(
                localSecond,
                Blocks.AIR.defaultBlockState(),
                3
        );

        access.setBlock(
                localThird,
                Blocks.AIR.defaultBlockState(),
                3
        );
    }

    public boolean isActivated() {
        return activated;
    }

    public boolean isArmed() {
        return armed;
    }

    public boolean isImpacted() {
        return impacted;
    }
}