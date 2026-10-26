package com.unddefined.sculkbaselib.server.registry;

import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.blocks.entity.CalibratedSculkShriekerBlockEntity;
import com.unddefined.sculkbaselib.blocks.entity.EchoDruseBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SculkBaseLib.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CalibratedSculkShriekerBlockEntity>> CALIBRATED_SCULK_SHRIEKER =
            BLOCK_ENTITY_TYPES.register("calibrated_sculk_shrieker_blockentity", () -> BlockEntityType.Builder.of(
                    CalibratedSculkShriekerBlockEntity::new,
                    BlockRegistry.CALIBRATED_SCULK_SHRIEKER.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EchoDruseBlockEntity>> ECHO_DRUSE =
            BLOCK_ENTITY_TYPES.register("echo_druse_blockentity", () -> BlockEntityType.Builder.of(
                    EchoDruseBlockEntity::new,
                    BlockRegistry.ECHO_DRUSE.get()
            ).build(null));

    private BlockEntityRegistry() {
    }
}
