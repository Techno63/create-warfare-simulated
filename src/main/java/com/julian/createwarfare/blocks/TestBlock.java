package com.julian.createwarfare.blocks;

import com.julian.createwarfare.effects.server.*;
import com.julian.createwarfare.effects.server.particles.MushroomCloudParticleEffect;
import com.julian.createwarfare.effects.server.particles.SplashParticleEffect;
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

        SplashParticleEffect.start(serverLevel, pos, 1f);

        //MushroomCloudParticleEffect.start(serverLevel, pos, 6f);
        //GlowEffect.start(serverLevel, pos, 2048f, 0xFF8C32, 0.7f, 360, true);

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