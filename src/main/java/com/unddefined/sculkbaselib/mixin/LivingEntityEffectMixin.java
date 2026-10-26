package com.unddefined.sculkbaselib.mixin;

import com.unddefined.sculkbaselib.client.audio.SculkVeilAudio;
import com.unddefined.sculkbaselib.server.registry.MobEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让「效果刚被加上」这件事在客户端也能被看见。
 *
 * <p>客户端收包走 {@code forceAddEffect}，它不会把回调透给 {@code MobEffect}，
 * 所以本地玩家的声音表现只能挂在这里。这里绝不能取消原方法：
 * {@code LivingEntity#onEffectAdded} 同时负责属性修正、effectsDirty 与乘客同步。
 */
@Mixin(LivingEntity.class)
public class LivingEntityEffectMixin {

    @Inject(method = "onEffectAdded(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"))
    private void sculkbaselib$onEffectAdded(MobEffectInstance instance, Entity source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide) return;
        if (!instance.is(MobEffectRegistry.SCULK_VEIL)) return;
        if (!entity.hasEffect(MobEffectRegistry.SCULK_VEIL)) return;
        SculkVeilAudio.onVeilAdded(entity);
    }
}
