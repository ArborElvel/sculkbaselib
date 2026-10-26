package com.unddefined.sculkbaselib.server.events;

import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.server.registry.BlockRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Calibrates a vanilla Sculk Shrieker with amethyst shards.
 */
@EventBusSubscriber(modid = SculkBaseLib.MODID)
public class SculkShriekerCalibrater {
    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        var stack = event.getItemStack();
        if (stack.getItem() != Items.AMETHYST_SHARD) return;
        var blockState = event.getLevel().getBlockState(event.getPos());
        if (blockState.getBlock() != Blocks.SCULK_SHRIEKER) return;
        if (!blockState.getValue(BlockStateProperties.CAN_SUMMON)) return;

        event.getLevel().playSound(null, event.getPos(), SoundEvents.SCULK_BLOCK_PLACE,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        event.getLevel().playSound(null, event.getPos(), SoundEvents.AMETHYST_BLOCK_HIT,
                SoundSource.BLOCKS, 0.3F, 0.9F);
        event.getLevel().gameEvent(event.getEntity(), GameEvent.BLOCK_PLACE, event.getPos());
        // TODO: 播放粒子效果

        if (stack.getCount() < 4) return;
        if (!event.getEntity().isCreative()) stack.shrink(4);

        event.getLevel().setBlock(event.getPos(),
                BlockRegistry.CALIBRATED_SCULK_SHRIEKER.get().defaultBlockState(), 3);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
