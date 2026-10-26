package com.unddefined.sculkbaselib.api.event;

import com.unddefined.sculkbaselib.api.SculkVeil;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

/**
 * 影匿（Sculk Veil）状态事件。
 *
 * <p>影匿的累积时长保存在服务端实体附件里，事件也都在服务端派发。基类是抽象的，
 * 请监听具体子类。
 *
 * <p>本族事件都是通知，不带取消语义：效果一旦挂上或移除就已经生效，想拦挂上/移除
 * 请直接监听 NeoForge 的 {@code MobEffectEvent}。
 */
public abstract class SculkVeilEvent extends Event {
    private final LivingEntity entity;

    protected SculkVeilEvent(LivingEntity entity) {
        this.entity = entity;
    }

    /** 被影匿影响的实体。 */
    public LivingEntity getEntity() {
        return entity;
    }

    /** 该实体累计被影匿影响的游戏刻数，不含正在进行、尚未结算的这一段。 */
    public long getAccumulatedTicks() {
        return SculkVeil.accumulatedTicks(entity);
    }

    /**
     * 影匿效果刚挂到实体上时派发。
     *
     * <p>若实体此前的影匿尚未结算，累计值会先并入 {@link #getAccumulatedTicks()}。
     * 实体正处于发光时影匿会被立即摘掉，此时本事件后面紧跟着一个 {@link Removed}。
     */
    public static final class Applied extends SculkVeilEvent {
        public Applied(LivingEntity entity) {
            super(entity);
        }
    }

    /** 影匿效果自然到期、并已把这一段时长结算进累计值后派发。 */
    public static final class Expired extends SculkVeilEvent {
        public Expired(LivingEntity entity) {
            super(entity);
        }
    }

    /** 影匿效果在到期前被移除（发光抵消、指令清除等）后派发。 */
    public static final class Removed extends SculkVeilEvent {
        public Removed(LivingEntity entity) {
            super(entity);
        }
    }

    /**
     * 累计影匿达到永久黑暗阈值、黑暗已经施加后派发。
     *
     * <p>阈值有两档：影匿效果持续期间的 2 分钟（2400 刻），以及玩家身上的 4 分钟（4800 刻）
     * 兜底判定；{@link #getThreshold()} 给出触发的是哪一档。发光时长会从累计值里扣除。
     */
    public static final class ThresholdReached extends SculkVeilEvent {
        private final long threshold;

        public ThresholdReached(LivingEntity entity, long threshold) {
            super(entity);
            this.threshold = threshold;
        }

        /** 触发这次永久黑暗的累计刻数阈值。 */
        public long getThreshold() {
            return threshold;
        }
    }
}
