
package com.julian.createwarfare.blocks.entities;

import com.julian.createwarfare.effects.server.particles.GroundExplosionParticleEffect;
import com.julian.createwarfare.explosions.types.GenericExplosion;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class HEWarheadBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    private static final float EXPLOSION_POWER = 8.0f;
    private static final float EXPLOSION_RADIUS = 12.0f;
    private static final float EXPLOSION_DAMAGE = 100.0f;

    private static final float PARTICLE_SCALE = 2.0f;

    private static final double ARMING_DISTANCE = 2.0D;
    private static final double GROUND_TOLERANCE = 1.25D;
    private static final double MINIMUM_MOVEMENT_SQUARED = 1.0E-10D;
    private static final double MINIMUM_DOWNWARD_SPEED = 0.05D;

    private Vec3 previousPosition;
    private BlockPos pendingImpact;

    private double travelledDistance;

    private boolean armed;
    private boolean detonated;

    public HEWarheadBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    public void sable$physicsTick(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep
    ) {
        if (detonated || pendingImpact != null || !handle.isValid()) {
            return;
        }

        Vec3 currentPosition = getWorldPosition(subLevel);

        if (previousPosition == null) {
            previousPosition = currentPosition;
            return;
        }

        Vec3 movement = currentPosition.subtract(previousPosition);

        if (movement.lengthSqr() < MINIMUM_MOVEMENT_SQUARED) {
            return;
        }

        travelledDistance += movement.length();

        if (!armed && travelledDistance >= ARMING_DISTANCE) {
            armed = true;
        }

        if (armed && movement.y < 0.0D) {
            Vector3d velocity = handle.getLinearVelocity(new Vector3d());

            if (velocity.y < -MINIMUM_DOWNWARD_SPEED) {
                ServerLevel level = subLevel.getLevel();

                int blockX = BlockPos.containing(currentPosition).getX();
                int blockZ = BlockPos.containing(currentPosition).getZ();

                int groundHeight = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING,
                        blockX,
                        blockZ
                );

                double distanceToGround = currentPosition.y - groundHeight;

                if (distanceToGround <= GROUND_TOLERANCE) {
                    BlockPos groundPosition = BlockPos.containing(
                            currentPosition.x,
                            groundHeight - 1.0D,
                            currentPosition.z
                    );

                    if (!level.getBlockState(groundPosition).isAir()) {
                        pendingImpact = groundPosition.immutable();
                    }
                }
            }
        }

        previousPosition = currentPosition;
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        if (detonated || pendingImpact == null) {
            return;
        }

        detonated = true;

        ServerLevel level = subLevel.getLevel();
        BlockPos impactPosition = pendingImpact;

        GroundExplosionParticleEffect.start(
                level,
                impactPosition,
                PARTICLE_SCALE
        );

        level.playSound(
                null,
                impactPosition,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                4.0f,
                0.8f
        );

        GenericExplosion.trigger(
                level,
                impactPosition,
                EXPLOSION_POWER,
                EXPLOSION_RADIUS,
                EXPLOSION_DAMAGE
        );

        subLevel.markRemoved();
    }

    private Vec3 getWorldPosition(ServerSubLevel subLevel) {
        Vector3d position = new Vector3d(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        );

        subLevel.logicalPose().transformPosition(position);

        return new Vec3(
                position.x,
                position.y,
                position.z
        );
    }
}
