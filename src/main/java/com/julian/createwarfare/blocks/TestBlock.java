package com.julian.createwarfare.blocks;

import com.julian.createwarfare.effects.particles.explosions.ExplosionParticleType;
import com.julian.createwarfare.effects.server.*;
import com.julian.createwarfare.explosions.post.SmokeRingPost;
import com.julian.createwarfare.explosions.types.GenericExplosion;
import com.julian.createwarfare.registry.CWServerTicker;
import com.julian.createwarfare.registry.CWSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TestBlock extends Block {

    public TestBlock(Properties properties) {
        super(properties);
    }

    private void trigger(
            Level level,
            BlockPos pos
    ) {
        level.removeBlock(pos, false);

        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 vec = new Vec3(pos.getX(), pos.getY(), pos.getZ());

        GenericExplosion.trigger(level, pos, 1200f, 50f, 35f);

        SoundWaveEffect.start(serverLevel, pos, 17.15f, 3000f, CWSoundEvents.EXPLOSION.getMainEvent(), true);

        WaveEffect.start(serverLevel, pos, 2.5f, 35f, 0xfff0a8, 0.95f, 80, true);
        WaveEffect.start(serverLevel, pos, 2f, 30f, 0xFFA033, 0.95f, 80, true);
        WaveEffect.start(serverLevel, pos, 1.5f, 25f, 0xfff0a8, 0.95f, 80, true);
        WaveEffect.start(serverLevel, pos, 1f, 20f, 0xFFA033, 0.95f, 80, true);

        GlowEffect.start(serverLevel, pos, 200f, 0xfff0a8, 1.5f, 120, false);

        MushroomCapEffect.start(serverLevel, pos.below(2), 200f, 7f,
                120f, 30f,
                 15f, 140f,
                   300);

        PressureWaveEffect.start(serverLevel, pos, 2f, 300f, 6f, true);
        WaveEffect.start(serverLevel, pos, 2f, 300f, 0xFFFFFF, 0.3f, 0, false);
        SmokeRingPost.start(serverLevel, pos.above(48), 0.3f, 45f, 600);
        SmokeRingPost.start(serverLevel, pos.above(65), 0.3f, 55f, 700);
        ExplosionParticleEffect.spawn(serverLevel, pos, 20, 100);

        FlashEffect.start(serverLevel, pos, 200f, 1f, 100, 0xFFF4C2, false, true);
        VignetteEffect.start(serverLevel, pos, 300f, 300, 1f);
        ShakeEffect.start(serverLevel, pos, 300f, 24f, 80, true);
        TinnitusEffect.start(serverLevel, pos, 350f, 400, false);
        HeartbeatEffect.start(serverLevel, pos, 300f, 450, 0.5f, false);
        BlurEffect.start(serverLevel, pos, 250f, 16f, 150, true);

    }


    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        if (level.isClientSide) {

            return;
        }

        if (!level.hasNeighborSignal(pos)) {
            return;
        }

        trigger(level, pos);
    }
}