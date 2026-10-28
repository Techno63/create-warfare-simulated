package com.julian.createwarfare.explosions;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CustomExplosion {

    private static final float MAX_RADIUS = 96.0F;
    private static final int MIN_RAYS = 768;
    private static final int MAX_RAYS = 12000;
    private static final int MAX_BLOCKS = 150000;
    private static final int MAX_ENTITIES = 128;
    private static final int MAX_SUBLEVELS = 8;
    private static final int MAX_DROPS = 160;
    private static final float DROP_CHANCE = 0.0F;
    private static final double STEP = 0.5D;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0D - Math.sqrt(5.0D));
    private static final float UNBREAKABLE = 3600000.0F;
    private static final int FLAGS = 2 | 16;

    private CustomExplosion() {
    }

    public static void explode(ServerLevel level, BlockPos center, float radius, float power, float entityDamage, boolean destroyBlocks, boolean damageEntities) {
        if (level == null || center == null || radius <= 0.0F) {
            return;
        }

        float r = Math.min(radius, MAX_RADIUS);
        float p = Math.max(power, 0.0F);

        Vec3 local = Vec3.atCenterOf(center);
        SubLevelAccess home = containing(level, center);
        Vec3 origin = home == null ? local : project(level, local);
        List<Target> targets = buildTargets(level, center, origin, local, r, home);

        if (damageEntities && entityDamage > 0.0F) {
            damageEntities(level, targets, origin, r, p, entityDamage);
        }

        if (destroyBlocks && p > 0.0F) {
            Job job = new Job(origin, targets, r, p, level.random.nextLong());
            while (job.phase == 0) {
                castStage(level, job, Integer.MAX_VALUE);
            }
            while (job.phase == 1) {
                destroyBlocks(level, job, Integer.MAX_VALUE);
            }
            if (job.phase == 2) {
                applyEdgeConversions(level, job);
                job.phase = 3;
            }
        }
    }

    private static SubLevelAccess containing(ServerLevel level, BlockPos pos) {
        try {
            return SableCompanion.INSTANCE.getContaining(level, pos);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Vec3 project(ServerLevel level, Vec3 pos) {
        try {
            Vec3 projected = Sable.HELPER.projectOutOfSubLevel(level, pos);
            return projected == null ? pos : projected;
        } catch (Throwable t) {
            return pos;
        }
    }

    private static List<Target> buildTargets(ServerLevel level, BlockPos center, Vec3 origin, Vec3 local, float radius, SubLevelAccess home) {
        List<Target> list = new ArrayList<>(4);
        list.add(new Target(null, origin, null, home == null, center));

        if (home != null) {
            list.add(new Target(home, local, home.boundingBox().toMojang(), true, center));
        }

        try {
            BoundingBox3d box = new BoundingBox3d(new AABB(origin, origin).inflate(radius));
            Iterable<? extends SubLevelAccess> found = SableCompanion.INSTANCE.getAllIntersecting(level, box);
            int count = 0;

            for (SubLevelAccess sub : found) {
                if (sub == null) {
                    continue;
                }

                if (home != null && Objects.equals(sub.getUniqueId(), home.getUniqueId())) {
                    continue;
                }

                if (count++ >= MAX_SUBLEVELS) {
                    break;
                }

                Pose3dc pose = sub.logicalPose();
                Vec3 localOrigin = pose.transformPositionInverse(origin);
                AABB globalBox = sub.boundingBox().toMojang();

                list.add(new Target(sub, localOrigin, globalBox, false, center));
            }
        } catch (Throwable ignored) {
        }

        return list;
    }

    private static int castStage(ServerLevel level, Job job, int budget) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int used = 0;

        while (job.targetIndex < job.targets.size() && used < budget) {
            Target t = job.targets.get(job.targetIndex);

            if (t.sub != null) {
                t.stage = 2;
                job.targetIndex++;
                continue;
            }

            if (t.stage == 0) {
                used += castRays(level, job, t, pos, budget - used);
            } else {
                job.targetIndex++;
            }
        }

        if (job.targetIndex >= job.targets.size()) {
            buildQueues(job);
        }

        return used;
    }

    private static boolean addHit(Job job, Target t, long key) {
        if (t.hit.add(key)) {
            job.total++;
            return true;
        }

        return false;
    }

    private static int castRays(ServerLevel level, Job job, Target t, BlockPos.MutableBlockPos pos, int budget) {
        int used = 0;

        while (t.ray < job.rays && used < budget && job.total < MAX_BLOCKS) {
            used += castRay(level, job, t, t.ray, pos);
            t.ray++;
        }

        if (t.ray >= job.rays || job.total >= MAX_BLOCKS) {
            t.stage = 2;
        }

        return used;
    }

    private static int castRay(ServerLevel level, Job job, Target globalTarget, int index, BlockPos.MutableBlockPos pos) {
        double jitterA = jitter(job.seed ^ 0x4F1BBCDCBFA54001L, index);
        double jitterB = jitter(job.seed ^ 0x6A09E667F3BCC909L, index + job.rays);
        double y = 1.0D - 2.0D * ((index + 0.15D + jitterA * 0.7D) / job.rays);
        y = Mth.clamp(y, -1.0D, 1.0D);
        double ring = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
        double phi = index * GOLDEN_ANGLE + job.spin + (jitterB - 0.5D) * (GOLDEN_ANGLE * 0.7D);
        double dx = Math.cos(phi) * ring;
        double dy = y;
        double dz = Math.sin(phi) * ring;
        double base = job.power * (0.7D + 0.6D * jitter(job.seed, index));
        int targetCount = job.targets.size();
        int[] lastX = new int[targetCount];
        int[] lastY = new int[targetCount];
        int[] lastZ = new int[targetCount];
        Target[] candidates = new Target[targetCount];
        long[] candidateKeys = new long[targetCount];
        float[] candidateResistances = new float[targetCount];
        java.util.Arrays.fill(lastX, Integer.MIN_VALUE);
        java.util.Arrays.fill(lastY, Integer.MIN_VALUE);
        java.util.Arrays.fill(lastZ, Integer.MIN_VALUE);
        int steps = 0;
        double travelCost = 0.225D * (STEP / 0.3D);

        for (double d = STEP * 0.5D; d <= job.radius && base > 0.0D && job.total < MAX_BLOCKS; d += STEP) {
            steps++;
            double gx = job.gx + dx * d;
            double gy = job.gy + dy * d;
            double gz = job.gz + dz * d;
            int candidateCount = 0;
            float strongestResistance = 0.0F;
            boolean hardBlocker = false;

            for (int ti = 0; ti < targetCount; ti++) {
                Target target = job.targets.get(ti);
                double bx;
                double by;
                double bz;

                if (target.sub == null) {
                    bx = gx;
                    by = gy;
                    bz = gz;
                } else {
                    AABB bounds = target.globalBox;
                    if (bounds == null || !bounds.contains(gx, gy, gz)) {
                        continue;
                    }

                    Vec3 localPos;
                    try {
                        localPos = target.sub.logicalPose().transformPositionInverse(new Vec3(gx, gy, gz));
                    } catch (Throwable ignored) {
                        continue;
                    }

                    bx = localPos.x;
                    by = localPos.y;
                    bz = localPos.z;
                }

                int x = Mth.floor(bx);
                int yy = Mth.floor(by);
                int z = Mth.floor(bz);

                if (x == lastX[ti] && yy == lastY[ti] && z == lastZ[ti]) {
                    continue;
                }

                lastX[ti] = x;
                lastY[ti] = yy;
                lastZ[ti] = z;
                pos.set(x, yy, z);

                if (level.isOutsideBuildHeight(yy) || !level.hasChunkAt(pos)) {
                    continue;
                }

                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }

                float resistance = resistance(state);
                if (state.getDestroySpeed(level, pos) < 0.0F || resistance >= UNBREAKABLE) {
                    hardBlocker = true;
                    strongestResistance = UNBREAKABLE;
                    continue;
                }

                candidates[candidateCount] = target;
                candidateKeys[candidateCount] = pos.asLong();
                candidateResistances[candidateCount] = resistance;
                candidateCount++;
                strongestResistance = Math.max(strongestResistance, resistance);
            }

            if (candidateCount > 0) {
                for (int ci = 0; ci < candidateCount; ci++) {
                    if (base >= candidateResistances[ci]) {
                        addHit(job, candidates[ci], candidateKeys[ci]);
                    }
                }

                if (hardBlocker || base < strongestResistance) {
                    break;
                }

                base -= strongestResistance / 3.0D;
            } else if (hardBlocker) {
                break;
            }

            base -= travelCost;
        }

        return Math.max(steps, 1);
    }

    private static boolean slab(Job job, Target t, double dx, double dy, double dz) {
        AABB b = t.globalBox;
        double min = 0.0D;
        double max = job.radius;

        if (Math.abs(dx) < 1.0E-9D) {
            if (job.gx < b.minX || job.gx > b.maxX) {
                return false;
            }
        } else {
            double a = (b.minX - job.gx) / dx;
            double c = (b.maxX - job.gx) / dx;

            if (a > c) {
                double s = a;
                a = c;
                c = s;
            }

            min = Math.max(min, a);
            max = Math.min(max, c);

            if (min > max) {
                return false;
            }
        }

        if (Math.abs(dy) < 1.0E-9D) {
            if (job.gy < b.minY || job.gy > b.maxY) {
                return false;
            }
        } else {
            double a = (b.minY - job.gy) / dy;
            double c = (b.maxY - job.gy) / dy;

            if (a > c) {
                double s = a;
                a = c;
                c = s;
            }

            min = Math.max(min, a);
            max = Math.min(max, c);

            if (min > max) {
                return false;
            }
        }

        if (Math.abs(dz) < 1.0E-9D) {
            if (job.gz < b.minZ || job.gz > b.maxZ) {
                return false;
            }
        } else {
            double a = (b.minZ - job.gz) / dz;
            double c = (b.maxZ - job.gz) / dz;

            if (a > c) {
                double s = a;
                a = c;
                c = s;
            }

            min = Math.max(min, a);
            max = Math.min(max, c);

            if (min > max) {
                return false;
            }
        }

        t.enter = min;
        t.exit = max;

        return true;
    }

    private static float jitter(long seed, int index) {
        long z = seed + index * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;

        return (float) ((z >>> 40) / (double) (1L << 24));
    }

    private static int castCore(ServerLevel level, Job job, Target t, BlockPos.MutableBlockPos pos, int budget) {
        int size = job.coreSize;
        int total = size * size * size;
        int cr = job.coreRadius;
        int used = 0;

        while (t.coreIndex < total && used < budget && job.total < MAX_BLOCKS) {
            int idx = t.coreIndex++;
            used++;

            int ix = idx % size - cr;
            int iy = (idx / size) % size - cr;
            int iz = idx / (size * size) - cr;

            double d2 = ix * ix + iy * iy + iz * iz;

            if (d2 > cr * cr) {
                continue;
            }

            pos.set(t.center.getX() + ix, t.center.getY() + iy, t.center.getZ() + iz);

            if (level.isOutsideBuildHeight(pos.getY()) || !level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            float res = resistance(state);

            if (res >= UNBREAKABLE || state.getDestroySpeed(level, pos) < 0.0F) {
                continue;
            }

            float f = (float) (Math.sqrt(d2) / cr);

            if ((res + 0.3F) * 0.6F < job.power * 0.6F * (1.0F - 0.6F * f)) {
                addHit(job, t, pos.asLong());
            }
        }

        if (t.coreIndex >= total || job.total >= MAX_BLOCKS) {
            t.stage = 2;
        }

        return Math.max(used, 1);
    }

    private static void buildQueues(Job job) {
        int buckets = Mth.ceil(job.radius) + 2;

        for (Target t : job.targets) {
            long[] raw = t.hit.toLongArray();
            t.hit = null;

            int n = raw.length;

            if (n == 0) {
                t.queue = raw;
                continue;
            }

            int[] counts = new int[buckets + 1];
            int[] which = new int[n];

            for (int i = 0; i < n; i++) {
                double ddx = BlockPos.getX(raw[i]) + 0.5D - t.ox;
                double ddy = BlockPos.getY(raw[i]) + 0.5D - t.oy;
                double ddz = BlockPos.getZ(raw[i]) + 0.5D - t.oz;

                int b = Math.min(buckets - 1, (int) Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz));

                which[i] = b;
                counts[b + 1]++;
            }

            for (int b = 0; b < buckets; b++) {
                counts[b + 1] += counts[b];
            }

            long[] out = new long[n];

            for (int i = 0; i < n; i++) {
                out[counts[which[i]]++] = raw[i];
            }

            t.queue = out;
            t.shellStart = (int) (n * 0.8D);
        }

        job.phase = 1;
    }

    private static int destroyBlocks(ServerLevel level, Job job, int budget) {
        int active = 0;

        for (Target t : job.targets) {
            if (t.cursor < t.queue.length) {
                active++;
            }
        }

        int done = 0;

        if (active > 0) {
            int share = Math.max(1, budget / active);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (Target t : job.targets) {
                if (t.cursor < t.queue.length) {
                    done += drain(level, job, t, Math.min(share, budget - done), pos);

                    if (done >= budget) {
                        break;
                    }
                }
            }
        }

        boolean finished = true;

        for (Target t : job.targets) {
            if (t.cursor < t.queue.length) {
                finished = false;
                break;
            }
        }

        if (finished) {
            job.phase = 2;
        }

        return done;
    }

    private static int drain(ServerLevel level, Job job, Target t, int budget, BlockPos.MutableBlockPos pos) {
        long[] queue = t.queue;
        int done = 0;

        while (t.cursor < queue.length && done < budget) {
            int index = t.cursor++;
            pos.set(queue[index]);

            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);

            if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
                continue;
            }

            if (t.sub == null && job.drops < MAX_DROPS && !(state.getBlock() instanceof LiquidBlock) && job.random.nextFloat() < DROP_CHANCE) {
                BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                Block.dropResources(state, level, pos, be);
                job.drops++;
            }

            level.setBlock(pos, state.getFluidState().createLegacyBlock(), FLAGS);

            if (index >= t.shellStart) {
                level.blockUpdated(pos, Blocks.AIR);
            }

            done++;
        }

        return done;
    }

    private static void applyEdgeConversions(ServerLevel level, Job job) {
        for (Target target : job.targets) {
            if (target.queue.length == 0) {
                continue;
            }

            LongOpenHashSet cavity = new LongOpenHashSet(target.queue.length * 2);
            for (long key : target.queue) {
                cavity.add(key);
            }

            LongOpenHashSet visited = new LongOpenHashSet();
            BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();

            for (long key : target.queue) {
                BlockPos destroyed = BlockPos.of(key);
                for (Direction direction : Direction.values()) {
                    candidate.set(destroyed).move(direction);
                    long candidateKey = candidate.asLong();
                    if (cavity.contains(candidateKey) || !visited.add(candidateKey)) {
                        continue;
                    }
                    if (level.isOutsideBuildHeight(candidate.getY()) || !level.hasChunkAt(candidate)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(candidate);
                    if (state.isAir() || state.getDestroySpeed(level, candidate) < 0.0F || !isExposedEdge(level, candidate)) {
                        continue;
                    }

                    BlockState damaged = damagedState(state, job.random, true);
                    if (damaged != null) {
                        level.setBlock(candidate, damaged, FLAGS);
                        level.blockUpdated(candidate, damaged.getBlock());
                    }
                }
            }
        }
    }

    private static boolean isExposedEdge(ServerLevel level, BlockPos pos) {
        boolean exposed = false;
        boolean supported = false;

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);

            if (level.isOutsideBuildHeight(neighborPos.getY()) || !level.hasChunkAt(neighborPos)) {
                continue;
            }

            BlockState neighbor = level.getBlockState(neighborPos);

            if (neighbor.isAir() || !neighbor.getFluidState().isEmpty()) {
                exposed = true;
            } else if (neighbor.isFaceSturdy(level, neighborPos, direction.getOpposite())) {
                supported = true;
            }

            if (exposed && supported) {
                return true;
            }
        }

        return false;
    }

    private static BlockState damagedState(BlockState state, RandomSource random, boolean outerShell) {
        if (!outerShell) {
            return null;
        }

        if (state.is(Blocks.STONE) && random.nextFloat() < 0.34F) {
            return random.nextBoolean() ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.TUFF.defaultBlockState();
        }
        if (state.is(Blocks.GRASS_BLOCK) && random.nextFloat() < 0.48F) {
            return Blocks.PODZOL.defaultBlockState();
        }
        if (state.is(Blocks.DIRT) && random.nextFloat() < 0.42F) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (state.is(Blocks.DEEPSLATE) && random.nextFloat() < 0.34F) {
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        return null;
    }

    private static float resistance(BlockState state) {
        if (state.getBlock() instanceof LiquidBlock) {
            return 4.0F;
        }

        return Math.max(state.getBlock().getExplosionResistance(), state.getFluidState().getExplosionResistance());
    }

    private static void damageEntities(ServerLevel level, List<Target> targets, Vec3 origin, float radius, float power, float damage) {
        List<Victim> victims = new ArrayList<>();
        double r2 = (double) radius * radius;

        for (Target t : targets) {
            AABB box = new AABB(t.ox - radius, t.oy - radius, t.oz - radius, t.ox + radius, t.oy + radius, t.oz + radius);
            List<Entity> found = level.getEntities((Entity) null, box, e -> e.isAlive() && !e.isSpectator());

            for (Entity entity : found) {
                Vec3 c = entity.getBoundingBox().getCenter();
                Vec3 g = c;

                if (t.sub != null) {
                    try {
                        g = t.sub.logicalPose().transformPosition(c);
                    } catch (Throwable ignored) {
                        continue;
                    }
                }

                double d2 = g.distanceToSqr(origin);

                if (d2 <= r2) {
                    victims.add(new Victim(entity, t, g, d2));
                }
            }
        }

        if (victims.isEmpty()) {
            return;
        }

        victims.sort(Comparator.comparingDouble(Victim::distSq));

        DamageSource source = level.damageSources().explosion(null, null);
        int limit = Math.min(victims.size(), MAX_ENTITIES);

        for (int i = 0; i < limit; i++) {
            Victim v = victims.get(i);
            Entity entity = v.entity();
            Target t = v.target();
            double dist = Math.sqrt(v.distSq());

            float seen = Explosion.getSeenPercent(new Vec3(t.ox, t.oy, t.oz), entity);

            if (seen <= 0.0F) {
                continue;
            }

            float impact = (1.0F - (float) (dist / radius)) * seen;

            if (impact <= 0.0F) {
                continue;
            }

            float amount = damage * (float) Math.pow(impact, 1.5D);

            if (amount > 0.05F) {
                entity.hurt(source, amount);
            }

            if (!entity.isAlive()) {
                continue;
            }

            if (entity instanceof Player player && (player.isSpectator() || (player.isCreative() && player.getAbilities().flying))) {
                continue;
            }

            Vec3 dir = v.global().subtract(origin);
            double len = dir.length();

            dir = len < 1.0E-4D ? new Vec3(0.0D, 1.0D, 0.0D) : dir.scale(1.0D / len);

            if (t.sub != null) {
                try {
                    dir = t.sub.logicalPose().transformNormalInverse(dir);
                } catch (Throwable ignored) {
                    continue;
                }
            }

            double strength = impact * Math.min(3.0D, 0.4D + power * 0.06D);

            if (entity instanceof LivingEntity living) {
                strength *= 1.0D - Mth.clamp(living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
            }

            if (strength > 0.0D) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(dir.scale(strength)));
                entity.hurtMarked = true;
            }
        }
    }

    private record Victim(Entity entity, Target target, Vec3 global, double distSq) {
    }

    private static final class Target {
        final SubLevelAccess sub;
        final double ox;
        final double oy;
        final double oz;
        final AABB globalBox;
        final boolean primary;
        final BlockPos center;
        LongOpenHashSet hit = new LongOpenHashSet();
        long[] queue = new long[0];
        int stage;
        int ray;
        int coreIndex;
        int cursor;
        int shellStart;
        double enter;
        double exit;

        Target(SubLevelAccess sub, Vec3 local, AABB globalBox, boolean primary, BlockPos center) {
            this.sub = sub;
            this.ox = local.x;
            this.oy = local.y;
            this.oz = local.z;
            this.globalBox = globalBox;
            this.primary = primary;
            this.center = center;
        }
    }

    private static final class Job {
        final double gx;
        final double gy;
        final double gz;
        final float radius;
        final float power;
        final int rays;
        final int coreRadius;
        final int coreSize;
        final long seed;
        final double spin;
        final List<Target> targets;
        final RandomSource random;
        int phase;
        int targetIndex;
        int total;
        int drops;

        Job(Vec3 origin, List<Target> targets, float radius, float power, long seed) {
            this.gx = origin.x;
            this.gy = origin.y;
            this.gz = origin.z;
            this.targets = targets;
            this.radius = radius;
            this.power = power;
            this.seed = seed;
            this.spin = jitter(seed, -1) * Math.PI * 2.0D;
            this.rays = Mth.clamp((int) (radius * radius * 14.0F), MIN_RAYS, MAX_RAYS);
            this.coreRadius = Math.max(1, Math.min(8, Mth.floor(radius * 0.18F)));
            this.coreSize = this.coreRadius * 2 + 1;
            this.random = RandomSource.create(seed);
        }
    }
}