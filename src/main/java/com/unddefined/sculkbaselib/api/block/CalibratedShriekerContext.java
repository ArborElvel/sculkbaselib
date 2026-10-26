package com.unddefined.sculkbaselib.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * {@link CalibratedShriekerMechanism} 回调时携带的上下文。
 *
 * <p>由方块在回调前创建，只在本次回调期间有效；物品不要把它存起来跨刻使用。
 */
public interface CalibratedShriekerContext {

    /** 方块所在的维度，保证是 {@link ServerLevel}。 */
    ServerLevel level();

    /** 方块坐标。 */
    BlockPos pos();

    /** 方块状态，含朝向与 {@code redstone_charged} 属性。 */
    BlockState blockState();

    /** 方块里放着的物品堆叠，与物品自身是同一个堆叠。 */
    ItemStack item();

    /**
     * 写回方块里的物品堆叠。
     *
     * <p>写入走方块自己的 contents-changed，会打上存档标记并把新内容同步给客户端，
     * 所以机制可以放心用它消耗或替换内含物品；清空槽位传 {@link ItemStack#EMPTY}。
     *
     * <p>这条写入不会再派发 {@link CalibratedShriekerMechanism#onItemChanged}：那个钩子只描述
     * 玩家交互与方块被破坏引起的槽位变更。
     */
    void setItem(ItemStack stack);

    /**
     * 正对面那个校频幽匿尖啸体里的内含物品。
     *
     * <p>「对面」指本方块 {@code FACING} 指向的那一格，并且要求那一格是同种方块、朝向与本方块相反，
     * 也就是两块尖啸体面对面扣在一起；不满足时返回 {@link ItemStack#EMPTY}。朝上摆时对面就是正上方那一格，
     * 朝下或朝四个水平方向时取各自方向上的那一格。
     *
     * <p>返回的是对方方块实体槽里那个堆叠本身，和 {@link #item()} 一样只读；要改就用
     * {@link #setOppositeItem(ItemStack)}。
     */
    ItemStack oppositeItem();

    /**
     * 写回正对面配对方块里的物品堆叠，判定与 {@link #oppositeItem()} 相同；没有配对方块时什么都不做。
     *
     * <p>写入走的是对方方块自己的 contents-changed（打存档标记并同步给客户端），
     * 清空传 {@link ItemStack#EMPTY}。
     */
    void setOppositeItem(ItemStack stack);

    /**
     * 让客户端把这个方块渲染成「正在播放某个效果」（服务端调用）。
     *
     * <p>{@code effect} 是效果 id，客户端要用 {@code client.api.CalibratedShriekerClientEffectRegistry}
     * 注册对应的绘制；没注册就什么都不画。信息随方块实体同步给正在跟踪的客户端，
     * 客户端按游戏刻算进度，所以断了再回来只会看到还没播完的那部分。
     *
     * @param effect        效果 id，通常用自己的命名空间
     * @param durationTicks 从这一刻起持续多少刻；不大于 0 表示立刻清掉当前效果
     */
    void playEffect(ResourceLocation effect, int durationTicks);

    /**
     * 内含物品与效果在方块里的落点（世界坐标）。
     *
     * <p>把方块中心按 {@code FACING} 偏移到模型内部，黑雾团、扩散圆环这类效果都拿它当中心，
     * 各个朝向的模型都能对上位置；朝向是 {@code UP} 或状态里没有 {@code FACING} 时就是方块中心。
     */
    Vec3 effectPos();

    /** 本次信号变化后的状态：{@code true} 为接通，{@code false} 为断开；等价于 {@link #signalStrength()} 大于 0。 */
    boolean powered();

    /**
     * 本次信号变化后的红石信号强度，取值 0~15。
     *
     * <p>与原版 {@code Level#getBestNeighborSignal} 同源：取方块六面里最强的一路，
     * 所以 {@link #powered()} 为 {@code true} 时这里至少是 1。
     *
     * <p>回调只在断开与接通之间翻转时发生，所以这里拿到的是翻转当刻的强度；
     * 已经接通时强度从 15 掉到 7 不会再回调。
     */
    int signalStrength();

    /**
     * 本方块为内含物品保留的冷却计时，单位是刻。
     *
     * <p>第一件带机制的内含物品出现时按 {@link CalibratedShriekerMechanism#initialCooldownTicks()}
     * 初始化，之后方块每刻把它递减到 0 为止；计时含义（衰减强度、能否再次触发等）完全由机制自己决定，
     * 方块只负责存储与倒数。
     */
    int cooldownTicks();

    /** 写入冷却计时；下一次回调与方块 tick 读到的就是这里写进去的值。 */
    void setCooldownTicks(int ticks);
}
