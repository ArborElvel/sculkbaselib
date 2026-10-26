package com.unddefined.sculkbaselib.server.registry;

import com.unddefined.sculkbaselib.SculkBaseLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreativeModeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SculkBaseLib.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SCULK_CORE =
            CREATIVE_MODE_TABS.register("sculk_core", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sculkbaselib.sculk_core"))
                    .icon(() -> new ItemStack(ItemRegistry.ECHO_DRUSE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ItemRegistry.ECHO_DRUSE.get());
                        output.accept(ItemRegistry.RHYME_SHARD.get());
                        output.accept(ItemRegistry.CALIBRATED_SCULK_SHRIEKER_ITEM.get());
                        output.accept(ItemRegistry.ECHO_DRUSE_STAGE1_ITEM.get());
                        output.accept(ItemRegistry.ECHO_DRUSE_STAGE2_ITEM.get());
                        output.accept(ItemRegistry.ECHO_DRUSE_STAGE3_ITEM.get());
                        output.accept(ItemRegistry.ECHO_DRUSE_STAGE4_ITEM.get());
                    })
                    .build());

    private CreativeModeTabRegistry() {
    }
}
