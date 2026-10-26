package com.unddefined.sculkbaselib.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 幽匿催发体绽放事件。
 *
 * <p>幽匿生物死亡、散播者死亡等场合会原地触发一次绽放：把死亡经验换成 cursor 电荷，
 * 再逐刻蔓延出去。事件在服务端派发。基类是抽象的，请监听具体子类。
 */
public abstract class SculkBloomEvent extends Event {
    private final ServerLevel level;
    private final BlockPos origin;
    private final int charge;

    protected SculkBloomEvent(ServerLevel level, BlockPos origin, int charge) {
        this.level = level;
        this.origin = origin;
        this.charge = charge;
    }

    /** 绽放所在的维度。 */
    public ServerLevel getLevel() {
        return level;
    }

    /** 绽放起点，已经由实体坐标换算成脚下的方块位置。 */
    public BlockPos getOrigin() {
        return origin;
    }

    /** 蔓延用的电荷，来自死亡经验。 */
    public int getCharge() {
        return charge;
    }

    /**
     * 绽放开始前派发，可取消。
     *
     * <p>取消后既不会添加蔓延 cursor，也不会播放绽放粒子与音效。
     */
    public static final class Pre extends SculkBloomEvent implements ICancellableEvent {
        public Pre(ServerLevel level, BlockPos origin, int charge) {
            super(level, origin, charge);
        }
    }

    /**
     * 绽放已经开始后派发，不可取消。
     *
     * <p>此时 cursor 已加入待蔓延列表、绽放粒子与音效也已播放；事件代表“绽放开始了”，
     * 不是“蔓延结束”。
     */
    public static final class Post extends SculkBloomEvent {
        public Post(ServerLevel level, BlockPos origin, int charge) {
            super(level, origin, charge);
        }
    }
}
