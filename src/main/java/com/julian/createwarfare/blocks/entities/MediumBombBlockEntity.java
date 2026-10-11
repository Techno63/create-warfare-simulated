package com.julian.createwarfare.blocks.entities;

import com.julian.createwarfare.blocks.MediumBombBlock;
import com.julian.createwarfare.effects.server.ExplosionParticleEffect;
import com.julian.createwarfare.effects.server.FragmentationEffect;
import com.julian.createwarfare.effects.server.GlowEffect;
import com.julian.createwarfare.effects.server.IncinerationWaveEffect;
import com.julian.createwarfare.effects.server.PressureWaveEffect;
import com.julian.createwarfare.effects.server.ShakeEffect;
import com.julian.createwarfare.effects.server.SoundWaveEffect;
import com.julian.createwarfare.effects.server.VignetteEffect;
import com.julian.createwarfare.effects.server.WaveEffect;
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

public class MediumBombBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    private static final double ARM_DELAY = 2.0;
    private static final double IMPACT_RAY_DISTANCE = 1.0;
    private static final double MIN_IMPACT_SPEED = 1.5;

    private boolean activated;
    private boolean armed;
    private boolean impacted;

    private double armingTime;

    public MediumBombBlockEntity(
            BlockEntityType<MediumBombBlockEntity> type,
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

        BlockPos partner =
                MediumBombBlock.partnerPos(
                        pos,
                        getBlockState()
                );

        BoundingBox3i bounds =
                new BoundingBox3i(
                        Math.min(
                                pos.getX(),
                                partner.getX()
                        ),
                        Math.min(
                                pos.getY(),
                                partner.getY()
                        ),
                        Math.min(
                                pos.getZ(),
                                partner.getZ()
                        ),
                        Math.max(
                                pos.getX(),
                                partner.getX()
                        ),
                        Math.max(
                                pos.getY(),
                                partner.getY()
                        ),
                        Math.max(
                                pos.getZ(),
                                partner.getZ()
                        )
                );

        SubLevelAssemblyHelper.assembleBlocks(
                serverLevel,
                pos,
                List.of(
                        pos,
                        partner
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

        BlockPos partnerPos =
                MediumBombBlock.partnerPos(
                        rootPos,
                        getBlockState()
                );

        BlockPos impactPos =
                null;

        for (
                BlockPos partPos :
                List.of(
                        rootPos,
                        partnerPos
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
                600f,
                20f,
                12f
        );

        SoundWaveEffect.start(
                level,
                impactPos,
                17.15f,
                300f,
                CWSoundEvents.EXPLOSION.getMainEvent(),
                true
        );

        PressureWaveEffect.start(
                level,
                impactPos,
                2f,
                30f,
                4f,
                false,
                true
        );

        FragmentationEffect.start(
                level,
                impactPos,
                3f,
                100f,
                500,
                1.5f,
                20f,
                true
        );

        IncinerationWaveEffect.start(
                level,
                impactPos,
                2f,
                40f,
                12f,
                100,
                true
        );

        WaveEffect.start(
                level,
                impactPos,
                2f,
                30f,
                0xfff0a8,
                0.3f,
                0,
                true
        );

        GlowEffect.start(
                level,
                impactPos,
                200f,
                0xfff0a8,
                0.9f,
                25,
                false
        );

        VignetteEffect.start(
                level,
                impactPos,
                80f,
                160,
                0.6f
        );

        ShakeEffect.start(
                level,
                impactPos,
                220f,
                16f,
                20,
                true
        );

        ExplosionParticleEffect.start(
                level,
                impactPos,
                3f
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

        BlockPos localPos =
                new BlockPos(
                        bombBlockPos.getX() - center.getX(),
                        bombBlockPos.getY() - center.getY(),
                        bombBlockPos.getZ() - center.getZ()
                );

        BlockPos localPartner =
                localPos.relative(
                        getBlockState().getValue(
                                MediumBombBlock.FACING
                        )
                );

        var access =
                subLevel
                        .getPlot()
                        .getEmbeddedLevelAccessor();

        access.setBlock(
                localPos,
                Blocks.AIR.defaultBlockState(),
                3
        );

        access.setBlock(
                localPartner,
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