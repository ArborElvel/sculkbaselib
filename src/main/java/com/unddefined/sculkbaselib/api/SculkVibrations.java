package com.unddefined.sculkbaselib.api;

import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Entities that dampen vibrations regardless of the usual player check.
 */
public final class SculkVibrations {
    private static final List<Predicate<Entity>> DAMPENERS = new CopyOnWriteArrayList<>();

    public static void registerDampener(Predicate<Entity> predicate) {
        DAMPENERS.add(predicate);
    }

    public static boolean dampens(Entity entity) {
        for (Predicate<Entity> predicate : DAMPENERS) {
            if (predicate.test(entity)) return true;
        }
        return false;
    }

    private SculkVibrations() {
    }
}
