package com.unddefined.sculkbaselib.server.registry;

import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.blocks.CalibratedSculkShriekerBlock;
import com.unddefined.sculkbaselib.blocks.EchoDruseBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SculkBaseLib.MODID);

    public static final DeferredBlock<CalibratedSculkShriekerBlock> CALIBRATED_SCULK_SHRIEKER =
            BLOCKS.register("calibrated_sculk_shrieker", CalibratedSculkShriekerBlock::new);
    public static final DeferredBlock<EchoDruseBlock> ECHO_DRUSE =
            BLOCKS.register("echo_druse_block", EchoDruseBlock::new);

    private BlockRegistry() {
    }
}
