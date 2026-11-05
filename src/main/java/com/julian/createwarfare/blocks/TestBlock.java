package com.julian.createwarfare.blocks;

import com.julian.createwarfare.effects.server.FlashEffect;
import com.julian.createwarfare.effects.server.GlowEffect;
import com.julian.createwarfare.effects.server.particles.MushroomCloudParticleEffect;
import com.julian.createwarfare.gui.CWGuiBuilder;
import com.julian.createwarfare.registry.CWBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TestBlock extends Block {

    public TestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        MushroomCloudParticleEffect.start((ServerLevel) level, pos, 6f);
        GlowEffect.start((ServerLevel) level, pos, 512f, 0xFFFF8000, 1.2f, 120, true);
        FlashEffect.start((ServerLevel) level, pos, 1024f, 1f, 50, 0xFFFFFF, false, false);

        return InteractionResult.CONSUME;
    }
}
