package com.unddefined.sculkbaselib.effects;

import com.unddefined.sculkbaselib.Config;
import com.unddefined.sculkbaselib.api.SculkVeilDebuffs;
import com.unddefined.sculkbaselib.api.SculkVeilImmune;
import com.unddefined.sculkbaselib.api.event.SculkVeilEvent;
import com.unddefined.sculkbaselib.server.registry.DataRegistry;
import com.unddefined.sculkbaselib.server.registry.MobEffectRegistry;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

public class SculkVeilEffect extends MobEffect {
    private static final int DEBUFF_INTERVAL = 60;

    public SculkVeilEffect() {
        super(MobEffectCategory.NEUTRAL, 0x4215441);
    }

    private static long darknessLine(LivingEntity entity) {
        return entity instanceof Player
                ? Config.SCULK_VEIL_PLAYER_DARKNESS_TICKS.get()
                : Config.SCULK_VEIL_MOB_DARKNESS_TICKS.get();
    }

    @Override
    public void onEffectAdded(LivingEntity livingEntity, int pAmplifier) {
        if (livingEntity.level() instanceof ServerLevel level) {
            long now = level.getGameTime();
            long start = livingEntity.getData(DataRegistry.SCULK_VEIL_START);
            long lastTick = livingEntity.getData(DataRegistry.SCULK_VEIL_LAST_TICK);
            long total = livingEntity.getData(DataRegistry.SCULK_VEIL_TOTAL);
            if (start >= 0) {
                total += Math.max(0, lastTick - start + 1);
                livingEntity.setData(DataRegistry.SCULK_VEIL_TOTAL, total);
            }
            livingEntity.setData(DataRegistry.SCULK_VEIL_START, now);
            livingEntity.setData(DataRegistry.SCULK_VEIL_LAST_TICK, now);
            if (total >= darknessLine(livingEntity)) applyPermanentDarkness(livingEntity);
            NeoForge.EVENT_BUS.post(new SculkVeilEvent.Applied(livingEntity));
        }
        // 发光的生物不保留影匿
        if (livingEntity.isCurrentlyGlowing()) {
            livingEntity.removeEffect(MobEffectRegistry.SCULK_VEIL);
            return;
        }

        var targetingCondition = TargetingConditions.forCombat().ignoreLineOfSight()
                .selector(e -> ((Mob) e).getTarget() == livingEntity);

        // 清除正在锁定该实体的敌对目标；幽匿生物免疫这条规则
        livingEntity.level().getNearbyEntities(Mob.class, targetingCondition, livingEntity,
                        livingEntity.getBoundingBox().inflate(40D))
                .stream().filter(e -> !(e instanceof SculkVeilImmune))
                .forEach(e -> {
                    e.setTarget(null);
                    e.targetSelector.getAvailableGoals().forEach(WrappedGoal::stop);
                    e.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                });
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int pDuration, int pAmplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int pAmplifier) {
        // 用实体自己的效果剩余时长驱动脉冲，不在单例 effect 上保存跨实体状态
        if (entity.level() instanceof ServerLevel) {
            long now = entity.level().getGameTime();
            entity.setData(DataRegistry.SCULK_VEIL_LAST_TICK, now);
            long start = entity.getData(DataRegistry.SCULK_VEIL_START);
            long glowingTotal = entity.getData(DataRegistry.GLOWING_TOTAL);
            long glowingStart = entity.getData(DataRegistry.GLOWING_START);
            if (glowingStart >= 0) glowingTotal += now - glowingStart + 1;
            if (start >= 0 && entity.getData(DataRegistry.SCULK_VEIL_TOTAL) + now - start + 1 - glowingTotal
                    >= darknessLine(entity)) {
                entity.setData(DataRegistry.SCULK_VEIL_TOTAL, darknessLine(entity));
                applyPermanentDarkness(entity);
            }
            MobEffectInstance veil = entity.getEffect(MobEffectRegistry.SCULK_VEIL);
            if (veil != null && veil.getDuration() > 0 && veil.getDuration() % DEBUFF_INTERVAL == 0)
                applyDebuffPulse(entity, veil.getDuration());
        }
        return !entity.isCurrentlyGlowing();
    }

    private static void applyPermanentDarkness(LivingEntity entity) {
        entity.addEffect(MobEffectRegistry.permanentDarkness());
        NeoForge.EVENT_BUS.post(new SculkVeilEvent.ThresholdReached(entity, darknessLine(entity)));
    }

    /** 随机施加两个负面效果与黑暗效果，持续时间与影匿剩余时长一致。 */
    private static void applyDebuffPulse(LivingEntity entity, int remaining) {
        List<MobEffectInstance> pool = new ArrayList<>(4);
        pool.add(new MobEffectInstance(MobEffects.WEAKNESS, remaining));
        pool.add(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, remaining));
        pool.add(new MobEffectInstance(MobEffects.HUNGER, remaining));
        pool.add(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, remaining));
        for (Holder<MobEffect> extra : SculkVeilDebuffs.pulseEffects()) {
            pool.add(new MobEffectInstance(extra, remaining));
        }

        // 确保只添加两个不同的随机效果
        int firstEffectIndex = entity.getRandom().nextInt(pool.size());
        int secondEffectIndex;
        do {
            secondEffectIndex = entity.getRandom().nextInt(pool.size());
        } while (secondEffectIndex == firstEffectIndex);
        entity.addEffect(pool.get(firstEffectIndex));
        entity.addEffect(pool.get(secondEffectIndex));
        entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS,
                Config.SCULK_VEIL_DARKNESS_DURATION.get() * 20, 1));
        for (SculkVeilDebuffs.Bonus bonus : SculkVeilDebuffs.bonusEffects()) {
            if (entity.getRandom().nextInt(bonus.oneIn()) == 0) {
                entity.addEffect(new MobEffectInstance(bonus.effect(), remaining));
            }
        }
    }
}
