package com.unddefined.sculkbaselib.api;

import com.unddefined.sculkbaselib.server.registry.DataRegistry;
import com.unddefined.sculkbaselib.server.registry.MobEffectRegistry;
import net.minecraft.world.entity.LivingEntity;

/**
 * 影匿（Sculk Veil）状态的只读查询。
 *
 * <p>累计值保存在服务端实体附件里，不会同步到客户端：服务端逻辑可以直接读，
 * 客户端只能拿到 {@code 0}，要做 HUD 得自己发包。
 */
public final class SculkVeil {
    private SculkVeil() {
    }

    /** 该实体当前是否带着影匿效果。 */
    public static boolean isVeiled(LivingEntity entity) {
        return entity.hasEffect(MobEffectRegistry.SCULK_VEIL);
    }

    /** 累计被影匿影响的游戏刻数（不含正在进行、尚未结算的这一段）。 */
    public static long accumulatedTicks(LivingEntity entity) {
        return entity.getData(DataRegistry.SCULK_VEIL_TOTAL);
    }

    /** 累计处于发光状态的游戏刻数，用来抵消影匿的黑暗积累。 */
    public static long glowingTicks(LivingEntity entity) {
        return entity.getData(DataRegistry.GLOWING_TOTAL);
    }
}
