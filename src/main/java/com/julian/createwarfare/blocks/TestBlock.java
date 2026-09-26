package com.julian.createwarfare.blocks;

import com.julian.createwarfare.effects.particles.shockwaves.ShockwaveParticleOptions;
import com.julian.createwarfare.effects.server.*;
import com.julian.createwarfare.explosions.post.ShockwavePost;
import com.julian.createwarfare.explosions.post.radiation.RadiationChunks;
import com.julian.createwarfare.explosions.types.GenericExplosion;
import com.julian.createwarfare.registry.CWSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

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

        ShockwavePost.create(serverLevel, vec, 2f, 200f, 16f, 10, 20, true);
        RadiationChunks.addRadiation(serverLevel, pos, 12, 120f);
        GenericExplosion.trigger(level, pos, 1200f, 50f, 35f);

        MushroomCapEffect.start(serverLevel, pos.below(50), 150f, 25f, 140f);
        ShockwaveEffect.start(serverLevel, pos, 2f, 300f);
        FireballEffect.start(serverLevel, pos, 0.5f, 150f);

        PressureWaveEffect.start(serverLevel, pos, 1f, 250f, 20f, true);

        FlashEffect.start(serverLevel, pos, 300f, 1f, 100, 0xF5FAFF, false, true);
        VignetteEffect.start(serverLevel, pos, 300f, 300, 1f);
        ShakeEffect.start(serverLevel, pos, 300f, 24f, 150, true);
        TinnitusEffect.start(serverLevel, pos, 350f, 400, false);
        HeartbeatEffect.start(serverLevel, pos, 300f, 450, 0.8f, false);
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