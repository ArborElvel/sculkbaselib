package com.unddefined.sculkbaselib.blocks.entity;

import com.unddefined.sculkbaselib.Config;
import com.unddefined.sculkbaselib.blocks.EchoDruseBlock;
import com.unddefined.sculkbaselib.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EchoDruseBlockEntity extends BlockEntity {
    int growthValue = initGrowthValue();

    private static int maxGrowth() {
        return Config.ECHO_DRUSE_MAX_GROWTH_VALUE.get();
    }

    public int initGrowthValue(){
        return switch (getBlockState().getValue(EchoDruseBlock.GROWTH_STAGE)){
            case 2 -> maxGrowth() / 4; // 25%
            case 3 -> maxGrowth() / 2; // 50%
            case 4 -> maxGrowth() * 3 / 4; // 75%
            default -> 0;
        };
    }

    public EchoDruseBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.ECHO_DRUSE.get(), pos, blockState);
        this.growthValue = initGrowthValue();
    }

    public int getGrowthValue() {return growthValue;}

    public void setGrowthValue(int growthValue) {
        this.growthValue = Math.max(1, Math.min(growthValue, maxGrowth()));
        setChanged();
    }
}
