package com.julian.createwarfare.explosions.post;

import com.julian.createwarfare.network.TracerPayload;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SubLevelInclusiveLevelEntityGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class FragmentationPost {

    private static final List<Fragmentation> EFFECTS = new ArrayList<>();
    private static final AtomicInteger NEXT_TRACER_ID = new AtomicInteger();

    private FragmentationPost() {
    }

    public static void start(
            ServerLevel level,
            BlockPos pos,
            float speed,
            float radius,
            int fragments,
            float power,
            float damage,
            boolean falloff
    ) {
        Vec3 origin = new Vec3(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        );

        SubLevel subLevel = Sable.HELPER.getContaining(level, pos);

        if (subLevel != null) {
            origin = subLevel.logicalPose().transformPosition(origin);
        }

        EFFECTS.add(new Fragmentation(
                level,
                origin,
                speed,
                radius,
                fragments,
                power,
                damage,
                falloff
        ));
    }

    public static void tick() {
        EFFECTS.removeIf(Fragmentation::tick);
    }

    private static class Fragmentation {

        private final ServerLevel level;
        private final Vec3 origin;
        private final float baseSpeed;
        private final float radius;
        private final float basePower;
        private final float baseDamage;
        private final boolean falloff;
        private final List<Fragment> fragments;

        private Fragmentation(
                ServerLevel level,
                Vec3 origin,
                float speed,
                float radius,
                int fragmentCount,
                float power,
                float damage,
                boolean falloff
        ) {
            this.level = level;
            this.origin = origin;
            this.baseSpeed = speed;
            this.radius = radius;
            this.basePower = power;
            this.baseDamage = damage;
            this.falloff = falloff;
            this.fragments = new ArrayList<>(Math.max(0, fragmentCount));

            Random random = new Random();

            for (int i = 0; i < fragmentCount; i++) {
                fragments.add(new Fragment(
                        randomDirection(random),
                        NEXT_TRACER_ID.incrementAndGet()
                ));
            }
        }

        private boolean tick() {
            boolean allDead = true;

            for (Fragment fragment : fragments) {
                if (fragment.dead) {
                    continue;
                }

                allDead = false;

                float multiplier = getFalloff(fragment.distance);
                float currentSpeed = baseSpeed * multiplier;
                float nextDistance = fragment.distance + currentSpeed;

                if (nextDistance > radius) {
                    nextDistance = radius;
                }

                Vec3 from = origin.add(
                        fragment.direction.scale(fragment.distance)
                );

                Vec3 to = origin.add(
                        fragment.direction.scale(nextDistance)
                );

                processSegment(
                        fragment,
                        from,
                        to,
                        nextDistance,
                        currentSpeed
                );

                fragment.distance = nextDistance;

                if (fragment.distance >= radius) {
                    fragment.dead = true;
                }
            }

            return allDead;
        }

        private void processSegment(
                Fragment fragment,
                Vec3 from,
                Vec3 to,
                float distance,
                float currentSpeed
        ) {
            ClipContext context = new ClipContext(
                    from,
                    to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
            );

            BlockHitResult blockHit = level.clip(context);

            boolean blockWasHit = blockHit.getType() == HitResult.Type.BLOCK;

            Vec3 endPosition = blockWasHit
                    ? blockHit.getLocation()
                    : to;

            sendTracerUpdate(
                    endPosition,
                    fragment.id,
                    0.04F,
                    currentSpeed
            );

            damageEntities(fragment, from, endPosition, distance);

            if (!blockWasHit) {
                return;
            }

            BlockPos blockPos = blockHit.getBlockPos();
            BlockState state = level.getBlockState(blockPos);

            if (state.isAir()) {
                return;
            }

            float hardness = state.getDestroySpeed(level, blockPos);

            if (hardness < 0.0F) {
                fragment.dead = true;
                return;
            }

            float multiplier = falloff ? getFalloff(distance) : 1.0F;
            float currentPower = basePower * multiplier;

            if (hardness > currentPower) {
                fragment.dead = true;
                return;
            }

            level.setBlock(
                    blockPos,
                    Blocks.AIR.defaultBlockState(),
                    3
            );
        }

        private void sendTracerUpdate(
                Vec3 position,
                int id,
                float width,
                float speed
        ) {
            PacketDistributor.sendToPlayersInDimension(
                    level,
                    new TracerPayload(
                            position.x,
                            position.y,
                            position.z,
                            width,
                            speed,
                            id
                    )
            );
        }

        @SuppressWarnings("unchecked")
        private void damageEntities(
                Fragment fragment,
                Vec3 from,
                Vec3 to,
                float distance
        ) {
            AABB area = new AABB(
                    Math.min(from.x, to.x) - 1.0,
                    Math.min(from.y, to.y) - 1.0,
                    Math.min(from.z, to.z) - 1.0,
                    Math.max(from.x, to.x) + 1.0,
                    Math.max(from.y, to.y) + 1.0,
                    Math.max(from.z, to.z) + 1.0
            );

            SubLevelInclusiveLevelEntityGetter<Entity> getter =
                    (SubLevelInclusiveLevelEntityGetter<Entity>)
                            (Object) level.getEntities();

            List<LivingEntity> entities = new ArrayList<>();

            getter.get(
                    area,
                    entity -> {
                        if (entity instanceof LivingEntity livingEntity) {
                            entities.add(livingEntity);
                        }
                    }
            );

            for (LivingEntity entity : entities) {
                damageEntity(fragment, entity, from, to, distance);
            }
        }

        private void damageEntity(
                Fragment fragment,
                LivingEntity entity,
                Vec3 from,
                Vec3 to,
                float distance
        ) {
            UUID id = entity.getUUID();

            if (fragment.hitEntities.contains(id)) {
                return;
            }

            Vec3 entityPosition = entity.position();
            SubLevel entitySubLevel = Sable.HELPER.getContaining(entity);

            if (entitySubLevel != null) {
                entityPosition = entitySubLevel.logicalPose()
                        .transformPosition(entityPosition);
            }

            Vec3 hitPoint = getClosestPointOnSegment(
                    entityPosition,
                    from,
                    to
            );

            if (entityPosition.distanceTo(hitPoint) > 1.0) {
                return;
            }

            fragment.hitEntities.add(id);

            float multiplier = falloff ? getFalloff(distance) : 1.0F;

            entity.hurt(
                    level.damageSources().generic(),
                    baseDamage * multiplier
            );
        }

        private Vec3 getClosestPointOnSegment(
                Vec3 point,
                Vec3 start,
                Vec3 end
        ) {
            Vec3 segment = end.subtract(start);
            double lengthSquared = segment.lengthSqr();

            if (lengthSquared <= 0.000001) {
                return start;
            }

            double t = point.subtract(start).dot(segment) / lengthSquared;
            t = Mth.clamp(t, 0.0, 1.0);

            return start.add(segment.scale(t));
        }

        private float getFalloff(float distance) {
            if (!falloff || radius <= 0.0F) {
                return 1.0F;
            }

            return Mth.clamp(
                    1.0F - (distance / radius) * 0.5F,
                    0.5F,
                    1.0F
            );
        }

        private Vec3 randomDirection(Random random) {
            Vec3 direction = new Vec3(
                    random.nextDouble() * 2.0 - 1.0,
                    random.nextDouble() * 2.0 - 1.0,
                    random.nextDouble() * 2.0 - 1.0
            );

            if (direction.lengthSqr() < 0.0001) {
                return new Vec3(0.0, 1.0, 0.0);
            }

            return direction.normalize();
        }
    }

    private static class Fragment {

        private final Vec3 direction;
        private final int id;
        private final Set<UUID> hitEntities = new HashSet<>();

        private float distance;
        private boolean dead;

        private Fragment(Vec3 direction, int id) {
            this.direction = direction;
            this.id = id;
        }
    }
}