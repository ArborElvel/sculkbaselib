package com.unddefined.sculkbaselib.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Events for the shared Echo Druse.
 */
public abstract class SculkDruseEvent extends Event {
    private final ServerLevel level;
    private final BlockPos pos;
    private final int growthValue;
    @Nullable
    private final ServerPlayer source;

    protected SculkDruseEvent(ServerLevel level, BlockPos pos, int growthValue, @Nullable ServerPlayer source) {
        this.level = level;
        this.pos = pos;
        this.growthValue = growthValue;
        this.source = source;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    public GlobalPos getPos() {
        return GlobalPos.of(level.dimension(), pos);
    }

    public int getGrowthValue() {
        return growthValue;
    }

    @Nullable
    public ServerPlayer getSource() {
        return source;
    }

    /**
     * Posted after a druse gains growth from a catalyst death or a random tick.
     */
    public static final class Grown extends SculkDruseEvent {
        public Grown(ServerLevel level, BlockPos pos, int growthValue, @Nullable ServerPlayer source) {
            super(level, pos, growthValue, source);
        }
    }
}
