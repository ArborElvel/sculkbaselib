package com.unddefined.sculkbaselib.server.events;

import com.unddefined.sculkbaselib.Config;
import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.api.event.SculkVeilEvent;
import com.unddefined.sculkbaselib.server.SculkBloom;
import com.unddefined.sculkbaselib.server.registry.DataRegistry;
import com.unddefined.sculkbaselib.server.registry.MobEffectRegistry;
import com.unddefined.sculkbaselib.server.registry.PotionRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = SculkBaseLib.MODID)
public final class SculkBaseEvents {

    @SubscribeEvent
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(Potions.AWKWARD, Items.GLOW_BERRIES, PotionRegistry.GLOWING);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) SculkBloom.serverTick(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) SculkBloom.discard(level);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        updateGlowingTime(player, now);
        long glowingInProgress = player.getData(DataRegistry.GLOWING_START) >= 0
                ? now - player.getData(DataRegistry.GLOWING_START) + 1 : 0;
        int threshold = Config.SCULK_VEIL_PLAYER_DARKNESS_TICKS.get();
        if (player.getData(DataRegistry.SCULK_VEIL_TOTAL) - player.getData(DataRegistry.GLOWING_TOTAL)
                - glowingInProgress >= threshold && !player.hasEffect(MobEffects.DARKNESS)) {
            player.addEffect(MobEffectRegistry.permanentDarkness());
            NeoForge.EVENT_BUS.post(new SculkVeilEvent.ThresholdReached(player, threshold));
        }
    }

    private static void updateGlowingTime(ServerPlayer player, long now) {
        long start = player.getData(DataRegistry.GLOWING_START);
        if (player.hasEffect(MobEffects.GLOWING)) {
            if (start < 0) player.setData(DataRegistry.GLOWING_START, now);
            player.setData(DataRegistry.GLOWING_LAST_TICK, now);
        } else if (start >= 0) {
            long lastTick = player.getData(DataRegistry.GLOWING_LAST_TICK);
            player.setData(DataRegistry.GLOWING_TOTAL,
                    player.getData(DataRegistry.GLOWING_TOTAL) + Math.max(0, lastTick - start + 1));
            player.setData(DataRegistry.GLOWING_START, -1L);
            player.setData(DataRegistry.GLOWING_LAST_TICK, -1L);
        }
    }

    @SubscribeEvent
    public static void onExpireEffect(MobEffectEvent.Expired event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null || !effect.is(MobEffectRegistry.SCULK_VEIL)) return;
        settleVeilSegment(event.getEntity());
        if (event.getEntity().level() instanceof ServerLevel) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    Config.SCULK_VEIL_GLOWING_DURATION.get() * 20));
            NeoForge.EVENT_BUS.post(new SculkVeilEvent.Expired(event.getEntity()));
        }
    }

    @SubscribeEvent
    public static void onRemoveEffect(MobEffectEvent.Remove event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null || !effect.is(MobEffectRegistry.SCULK_VEIL)) return;
        settleVeilSegment(event.getEntity());
        NeoForge.EVENT_BUS.post(new SculkVeilEvent.Removed(event.getEntity()));
    }

    /**
     * Settle the in-progress accumulation segment when the effect ends or is removed,
     * instead of waiting for the next application.
     */
    private static void settleVeilSegment(LivingEntity entity) {
        long start = entity.getData(DataRegistry.SCULK_VEIL_START);
        if (start < 0) return;
        long lastTick = entity.getData(DataRegistry.SCULK_VEIL_LAST_TICK);
        entity.setData(DataRegistry.SCULK_VEIL_TOTAL,
                entity.getData(DataRegistry.SCULK_VEIL_TOTAL) + Math.max(0, lastTick - start + 1));
        entity.setData(DataRegistry.SCULK_VEIL_START, -1L);
        entity.setData(DataRegistry.SCULK_VEIL_LAST_TICK, -1L);
    }

    private SculkBaseEvents() {
    }
}
