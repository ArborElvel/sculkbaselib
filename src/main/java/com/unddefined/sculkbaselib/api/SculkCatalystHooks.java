package com.unddefined.sculkbaselib.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Extension point for content mods that react to a Sculk Catalyst activation.
 *
 * <p>The shared catalyst listener calls these handlers before its own druse and
 * shrieker handling. Returning {@code true} consumes the activation, matching the
 * old per-mod mixin behavior.
 */
public final class SculkCatalystHooks {
    @FunctionalInterface
    public interface EntityDeathHandler {
        boolean onEntityDeath(ServerLevel level, BlockPos catalystPos, LivingEntity entity);
    }

    private static final List<EntityDeathHandler> HANDLERS = new CopyOnWriteArrayList<>();

    public static void register(EntityDeathHandler handler) {
        HANDLERS.add(handler);
    }

    public static boolean dispatch(ServerLevel level, BlockPos catalystPos, LivingEntity entity) {
        for (EntityDeathHandler handler : HANDLERS) {
            if (handler.onEntityDeath(level, catalystPos, entity)) return true;
        }
        return false;
    }

    private SculkCatalystHooks() {
    }
}
