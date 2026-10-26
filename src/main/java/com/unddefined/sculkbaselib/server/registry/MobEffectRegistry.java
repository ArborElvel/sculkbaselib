package com.unddefined.sculkbaselib.server.registry;

import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.effects.SculkVeilEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MobEffectRegistry {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, SculkBaseLib.MODID);

    public static final DeferredHolder<MobEffect, SculkVeilEffect> SCULK_VEIL =
            MOB_EFFECTS.register("sculk_veil", SculkVeilEffect::new);

    /**
     * Permanent Darkness that milk cannot remove.
     */
    public static MobEffectInstance permanentDarkness() {
        MobEffectInstance darkness = new MobEffectInstance(MobEffects.DARKNESS, Integer.MAX_VALUE, 0, false, true);
        darkness.getCures().clear();
        return darkness;
    }

    private MobEffectRegistry() {
    }
}
