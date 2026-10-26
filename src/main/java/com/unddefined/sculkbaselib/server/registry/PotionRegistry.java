package com.unddefined.sculkbaselib.server.registry;

import com.unddefined.sculkbaselib.SculkBaseLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PotionRegistry {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, SculkBaseLib.MODID);

    /** Glowing potion: 3 minutes, used to offset veil darkness accumulation. */
    public static final DeferredHolder<Potion, Potion> GLOWING =
            POTIONS.register("glowing", () -> new Potion(
                    new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 3)
            ));

    private PotionRegistry() {
    }
}
