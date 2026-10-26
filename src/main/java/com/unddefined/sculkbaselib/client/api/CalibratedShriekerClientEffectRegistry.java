package com.unddefined.sculkbaselib.client.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 触发效果 id 到绘制实现的注册表（仅客户端）。
 *
 * <p>效果 id 由服务端在触发时指定（见 {@code CalibratedShriekerContext#playEffect}），客户端在这里
 * 查对应的绘制实现；查不到就什么都不画，所以没装效果的附属模组不会报错。
 *
 * <p>注册建议放在 {@code FMLClientSetupEvent} 里；重复注册同一个 id 以最后一次为准。
 * 绘制发生在客户端渲染线程，不要在里面改世界状态。
 */
public final class CalibratedShriekerClientEffectRegistry {

    private static final Map<ResourceLocation, CalibratedShriekerEffectRenderer> EFFECTS = new ConcurrentHashMap<>();

    private CalibratedShriekerClientEffectRegistry() {
    }

    /** 注册一个效果的绘制实现；重复注册同一个 id 以最后一次为准。 */
    public static void register(ResourceLocation id, CalibratedShriekerEffectRenderer renderer) {
        EFFECTS.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(renderer, "renderer"));
    }

    /** 取得该 id 的绘制实现；没有注册时返回 {@code null}。 */
    @Nullable
    public static CalibratedShriekerEffectRenderer get(ResourceLocation id) {
        return EFFECTS.get(id);
    }
}
