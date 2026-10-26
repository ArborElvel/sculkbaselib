package com.unddefined.sculkbaselib.api;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Content-mod additions to Sculk Veil's periodic debuff pulse.
 */
public final class SculkVeilDebuffs {
    public record Bonus(Holder<MobEffect> effect, int oneIn) {
    }

    private static final List<Holder<MobEffect>> PULSE = new CopyOnWriteArrayList<>();
    private static final List<Bonus> BONUSES = new CopyOnWriteArrayList<>();

    /** Add an effect to the two-of-N random pulse pool. */
    public static void registerPulse(Holder<MobEffect> effect) {
        PULSE.add(effect);
    }

    /** Add an extra effect rolled independently with a 1-in-N chance. */
    public static void registerBonus(Holder<MobEffect> effect, int oneIn) {
        BONUSES.add(new Bonus(effect, Math.max(1, oneIn)));
    }

    public static List<Holder<MobEffect>> pulseEffects() {
        return List.copyOf(PULSE);
    }

    public static List<Bonus> bonusEffects() {
        return List.copyOf(BONUSES);
    }

    private SculkVeilDebuffs() {
    }
}
