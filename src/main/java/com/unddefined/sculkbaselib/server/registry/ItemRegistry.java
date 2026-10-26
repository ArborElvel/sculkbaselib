package com.unddefined.sculkbaselib.server.registry;

import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.blocks.EchoDruseBlock;
import com.unddefined.sculkbaselib.items.EchoDruse;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SculkBaseLib.MODID);

    public static final DeferredItem<EchoDruse> ECHO_DRUSE = ITEMS.registerItem("echo_druse", EchoDruse::new);

    public static final DeferredItem<Item> RHYME_SHARD = ITEMS.registerSimpleItem("rhyme_shard", new Item.Properties()
            .rarity(Rarity.UNCOMMON).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    public static final DeferredItem<BlockItem> CALIBRATED_SCULK_SHRIEKER_ITEM =
            ITEMS.registerSimpleBlockItem("calibrated_sculk_shrieker", BlockRegistry.CALIBRATED_SCULK_SHRIEKER);

    public static final DeferredItem<BlockItem> ECHO_DRUSE_STAGE1_ITEM = registerEchoDruse("echo_druse_stage1", 1);
    public static final DeferredItem<BlockItem> ECHO_DRUSE_STAGE2_ITEM = registerEchoDruse("echo_druse_stage2", 2);
    public static final DeferredItem<BlockItem> ECHO_DRUSE_STAGE3_ITEM = registerEchoDruse("echo_druse_stage3", 3);
    public static final DeferredItem<BlockItem> ECHO_DRUSE_STAGE4_ITEM = registerEchoDruse("echo_druse_stage4", 4);

    private static DeferredItem<BlockItem> registerEchoDruse(String name, int stage) {
        return ITEMS.register(name, () -> new BlockItem(BlockRegistry.ECHO_DRUSE.get(), new Item.Properties()) {
            @Override
            public BlockState getPlacementState(BlockPlaceContext context) {
                return BlockRegistry.ECHO_DRUSE.get().defaultBlockState()
                        .setValue(EchoDruseBlock.GROWTH_STAGE, stage);
            }
        });
    }

    private ItemRegistry() {
    }
}
