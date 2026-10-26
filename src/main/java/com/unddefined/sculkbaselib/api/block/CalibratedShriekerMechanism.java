package com.unddefined.sculkbaselib.api.block;

import net.minecraft.world.entity.Entity;

/**
 * 校频幽匿尖啸体的内含物品机制。
 *
 * <p>方块里放着的物品带着这个机制时，「红石触发」就交给它：红石信号接通与断开各回调一次，
 * 具体做什么由机制决定。方块自身的红石状态（{@code redstone_charged} 属性与失电后的延迟复位）
 * 仍由方块维护，只有信号真正发生变化时才回调这里。
 *
 * <p>接法见 {@link CalibratedShriekerMechanisms}：物品类可以直接实现本接口，也可以注册进去——
 * 按物品注册，或者按物品堆叠条件注册（后者能区分同一个物品的不同组件与 NBT）。
 *
 * <p>回调只在服务端发生：{@link CalibratedShriekerContext#level()} 保证是服务端维度。
 * 接通与断开各回调一次，{@link CalibratedShriekerContext#powered()} 是本次变化后的状态，
 * {@link CalibratedShriekerContext#signalStrength()} 是同一时刻的红石强度（0~15，取六面最强的一路），
 * 只关心充能的物品自己判断即可（内置幽鸣晶簇就只在接通时爆发次声波）。
 *
 * <p>另外三个触发点都是默认空实现，不需要的机制不用管：方块每刻回调一次 {@link #onShriekerTick}
 * （内置幽鸣晶簇用它刷新随冷却变化的粒子）；内含物品被放进、取出或随方块一起被破坏时回调
 * {@link #onItemChanged}；实体踩上方块时回调 {@link #onSteppedOn}（内置幽匿精华靠它召唤监守者）。
 *
 * <p>{@link #onRedstoneSignal} 是唯一的抽象方法，所以既能用 lambda 注册，也能被物品类直接实现；
 * 只关心其它触发点的实现类把它留空即可。
 */
public interface CalibratedShriekerMechanism {

    /**
     * 内含物品所在方块的红石信号发生变化时调用（仅服务端）。
     *
     * @param context 本次回调的上下文，只在回调期间有效，不要跨刻保存
     */
    void onRedstoneSignal(CalibratedShriekerContext context);

    /**
     * 内含物品所在方块每刻的 tick 回调（仅服务端），默认什么都不做。
     *
     * <p>只会在方块里放着带机制物品时调用，而且发生在方块递减冷却之前：
     * {@link CalibratedShriekerContext#cooldownTicks()} 读到的是本刻开始时的值。
     */
    default void onShriekerTick(CalibratedShriekerContext context) {
    }

    /**
     * 内含物品发生变更时调用（仅服务端）。
     *
     * <p>{@link CalibratedShriekerItemChange#INSERTED} 与 {@link CalibratedShriekerItemChange#EXTRACTED}
     * 只在玩家交互改动了槽位时出现；{@link CalibratedShriekerItemChange#BROKEN} 是方块被破坏或替换、
     * 物品还在里面时的那一次。回调时物品还没离开槽位，所以
     * {@link CalibratedShriekerContext#item()}、{@link CalibratedShriekerContext#cooldownTicks()}
     * 拿到的都是变更当刻的状态。
     *
     * <p>默认什么都不做。
     */
    default void onItemChanged(CalibratedShriekerContext context, CalibratedShriekerItemChange change) {
    }

    /**
     * 实体踩上内含物品所在方块时调用（仅服务端），默认什么都不做。
     *
     * <p>与其它回调一样，只在内含物品挂着机制时调用；非玩家的实体也会进来，需要玩家的机制自己
     * 用 {@code SculkShriekerBlockEntity#tryGetPlayer(entity)} 取（载具、弹射物等情况由它处理）。
     */
    default void onSteppedOn(CalibratedShriekerContext context, Entity entity) {
    }

    /**
     * 方块被破坏、物品还在里面时，这个物品是否跟着碎裂（{@code true} = 不产生掉落物）。
     *
     * <p>默认 {@code false}，照常掉出来；内置幽鸣晶簇返回 {@code true}，对应设计里的
     * 「载体被破坏时会碎裂」。
     */
    default boolean shattersOnBreak() {
        return false;
    }

    /**
     * 机制在方块里的初始冷却刻数，默认 0。
     *
     * <p>方块建好时多半还没有内含物品，所以方块会在第一次遇到带机制的内含物品时拿这个值初始化冷却，
     * 之后只负责逐刻递减、不再重置（换物品也不会重来）。需要「刚放进去还没充能」这种预热语义的机制
     * 把它设成满冷却：内置幽鸣晶簇返回配置的冷却时长 × 20，于是刚放好时粒子最小、爆发最弱，
     * 冷却走完后才是满强度。
     */
    default int initialCooldownTicks() {
        return 0;
    }
}
