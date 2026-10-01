package com.julian.createwarfare.blocks;

import com.julian.createwarfare.effects.server.*;
import com.julian.createwarfare.explosions.post.SmokeRingPost;
import com.julian.createwarfare.explosions.types.GenericExplosion;
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

        WaveEffect.start(serverLevel, pos, 1.5f, 500f, 0xffe3b8, 0.8f);
        PressureWaveEffect.start(serverLevel, pos, 1.5f, 500f, 6f, true);
        SmokeRingPost.start(serverLevel, pos.above(20), 0.3f, 60f, 800);
        SmokeRingPost.start(serverLevel, pos.above(32), 0.3f, 45f, 800);
        SmokeRingPost.start(serverLevel, pos.above(50), 0.3f, 32f, 800);

        FlashEffect.start(serverLevel, pos, 200f, 1f, 100, 0xF5FAFF, false, true);
        VignetteEffect.start(serverLevel, pos, 300f, 300, 1f);
        ShakeEffect.start(serverLevel, pos, 300f, 24f, 150, true);
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