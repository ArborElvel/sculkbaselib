package com.unddefined.sculkbaselib.server;

import com.unddefined.sculkbaselib.api.event.SculkBloomEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 幽匿生物死亡时在原地触发的“幽匿催发体绽放”。
 *
 * <p>与幽匿催发体一致：先把死亡经验转成 cursor 电荷，再逐刻蔓延出去，
 * 直到电荷耗尽或到达最大存活刻数，粒子与音效同样模仿催发体绽放。
 */
public class SculkBloom {
    /** 单次绽放最多蔓延的刻数，避免电荷迟迟不消耗时长期滞留 */
    private static final int MAX_LIFETIME = 40;

    private static final List<ActiveBloom> ACTIVE_BLOOMS = new ArrayList<>();

    /** 把实体坐标换算成绽放的 cursor 原点：该坐标脚下所在的方块 */
    public static BlockPos bloomOrigin(Vec3 pos) {
        return BlockPos.containing(pos.x, pos.y + 0.5, pos.z);
    }

    /** 播放一次催发体绽放的粒子与音效 */
    public static void playBloomEffects(ServerLevel level, BlockPos pos) {
        playBloomParticles(level, new Vec3((double) pos.getX() + 0.5, (double) pos.getY() + 1.15, (double) pos.getZ() + 0.5));
        level.playSound(null, pos, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS,
                2.0F, 0.6F + level.getRandom().nextFloat() * 0.4F);
    }

    /**
     * 在任意坐标播放一次催发体绽放的粒子（不含音效）。
     *
     * <p>参数与原版催发体绽放一致：每次 2 颗幽匿灵魂粒子，水平随机偏移 0.2。
     * 方块位置的绽放走 {@link #playBloomEffects(ServerLevel, BlockPos)}（粒子 + 音效），
     * 散播者这类“动起来的催发体”只在头顶补一组粒子，音效由它触发的方块绽放负责。
     *
     * @param level 维度
     * @param pos   粒子的中心坐标
     */
    public static void playBloomParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 2, 0.2, 0.0, 0.2, 0.0);
    }

    /** 让 spreader 从指定位置蔓延一刻，与原版催发体每刻做的事一致 */
    public static void tickSpreader(ServerLevel level, BlockPos pos, SculkSpreader spreader) {
        spreader.updateCursors(level, pos, level.getRandom(), true);
    }

    /** 在指定位置触发一次绽放，charge 为蔓延用的电荷（来自死亡经验） */
    public static void bloom(ServerLevel level, Vec3 pos, int charge) {
        if (charge <= 0) return;

        BlockPos origin = bloomOrigin(pos);
        SculkBloomEvent.Pre pre = new SculkBloomEvent.Pre(level, origin, charge);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) return;
        SculkSpreader spreader = new SculkSpreader(false, BlockTags.SCULK_REPLACEABLE, 5, 2, 10, 5);
        spreader.addCursors(origin, charge);
        ACTIVE_BLOOMS.add(new ActiveBloom(level, origin, spreader));

        playBloomEffects(level, origin);
        NeoForge.EVENT_BUS.post(new SculkBloomEvent.Post(level, origin, charge));
    }

    /**
     * 只绽放、不铺开：照常播粒子音效、照常发 {@link SculkBloomEvent.Pre}/{@code Post}，
     * 只是**不建蔓延用的 cursor**，所以电荷不会被消耗、也不会把幽匿铺出去，留给调用方处理。
     *
     * <p>散播者的头顶回响晶簇还在长（stage 1~2）时用它把「蔓延」那一半拦下、电荷喂给晶簇，
     * 见设计稿「达到 stage3 时不再生长，继续触发幽匿催发体的效果」。
     *
     * @param level  绽放所在维度
     * @param pos    绽放位置
     * @param charge 这次的电荷，只用于事件参数
     */
    public static void bloomWithoutSpread(ServerLevel level, Vec3 pos, int charge) {
        if (charge <= 0) return;

        BlockPos origin = bloomOrigin(pos);
        SculkBloomEvent.Pre pre = new SculkBloomEvent.Pre(level, origin, charge);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) return;

        playBloomEffects(level, origin);
        NeoForge.EVENT_BUS.post(new SculkBloomEvent.Post(level, origin, charge));
    }

    /** 服务端每刻驱动该维度上未结束的绽放 */
    public static void serverTick(ServerLevel level) {
        Iterator<ActiveBloom> iterator = ACTIVE_BLOOMS.iterator();
        while (iterator.hasNext()) {
            ActiveBloom bloom = iterator.next();
            if (bloom.level != level) continue;

            tickSpreader(level, bloom.origin, bloom.spreader);
            if (--bloom.ticksLeft <= 0 || bloom.spreader.getCursors().isEmpty()) {
                iterator.remove();
            }
        }
    }

    /** 维度卸载时丢弃其上未完成的绽放，避免静态列表持有已卸载的世界 */
    public static void discard(ServerLevel level) {
        ACTIVE_BLOOMS.removeIf(bloom -> bloom.level == level);
    }

    private static class ActiveBloom {
        private final ServerLevel level;
        private final BlockPos origin;
        private final SculkSpreader spreader;
        private int ticksLeft = MAX_LIFETIME;

        private ActiveBloom(ServerLevel level, BlockPos origin, SculkSpreader spreader) {
            this.level = level;
            this.origin = origin;
            this.spreader = spreader;
        }
    }
}
