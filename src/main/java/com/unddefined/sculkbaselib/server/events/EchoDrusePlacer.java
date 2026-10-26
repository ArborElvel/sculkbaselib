package com.unddefined.sculkbaselib.server.events;

import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.server.registry.BlockRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Plants an echo shard on a Sculk Catalyst and grows an Echo Druse above it.
 */
@EventBusSubscriber(modid = SculkBaseLib.MODID)
public final class EchoDrusePlacer {
    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != Items.ECHO_SHARD) return;

        BlockState blockState = event.getLevel().getBlockState(event.getPos());
        if (blockState.getBlock() != Blocks.SCULK_CATALYST) return;
        if (!event.getLevel().getBlockState(event.getPos().above()).isAir()) return;

        if (!event.getEntity().isCreative()) stack.shrink(1);
        event.getLevel().setBlock(event.getPos().above(),
                BlockRegistry.ECHO_DRUSE.get().defaultBlockState(), 3);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.getLevel().playSound(null, event.getPos(), SoundEvents.SCULK_BLOCK_PLACE,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        event.getLevel().gameEvent(event.getEntity(), GameEvent.BLOCK_PLACE, event.getPos());
    }

    private EchoDrusePlacer() {
    }
}
