package com.unddefined.sculkbaselib.items;

import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.api.block.CalibratedShriekerContext;
import com.unddefined.sculkbaselib.api.block.CalibratedShriekerMechanism;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EchoDruse extends Item implements CalibratedShriekerMechanism {
    /** 激活时让客户端播一次的扩散圆环；绘制实现在客户端注册。 */
    public static final ResourceLocation ECHO_PULSE = ResourceLocation.fromNamespaceAndPath(SculkBaseLib.MODID, "echo_pulse");
    /** 发光作用的半径，单位方块。 */
    private static final double GLOW_RADIUS = 32.0D;
    /** 发光持续时间：6 秒。 */
    private static final int GLOW_DURATION_TICKS = 6 * 20;
    /** 扩散圆环的播放时长：1 秒。 */
    private static final int PULSE_DURATION_TICKS = 60;
    /** 激活一次后的冷却刻数，同时也是刚放进方块时的预热。 */
    private static final int COOLDOWN_TICKS = 60;

    public EchoDruse(Properties properties) {
        super(properties.stacksTo(8));
    }

    /**
     * 红石接通时，消耗正对面「面对面」扣在一起那个尖啸体里的一个萤石粉，给半径 20 格内的生物挂 6 秒发光。
     *
     * <p>没有配对方块、上方不是萤石粉、或者还在冷却里都直接跳过：不消耗、不发光。
     */
    @Override
    public void onRedstoneSignal(CalibratedShriekerContext context) {
        if (!context.powered()) return;
        if (context.cooldownTicks() > 0) return;

        ItemStack opposite = context.oppositeItem();
        if (!opposite.is(Items.GLOWSTONE_DUST)) return;

        // 消耗 1 个萤石粉
        int remaining = opposite.getCount() - 1;
        context.setOppositeItem(remaining <= 0 ? ItemStack.EMPTY : opposite.copyWithCount(remaining));

        glow(context.level(), context.pos());
        // 让客户端画一次扩散圆环
        context.playEffect(ECHO_PULSE, PULSE_DURATION_TICKS);
        context.setCooldownTicks(COOLDOWN_TICKS);
    }

    @Override
    public int initialCooldownTicks() {
        return COOLDOWN_TICKS;
    }

    /** 给半径内的生物挂发光；先按方盒取候选，再按实际距离筛一遍。 */
    private static void glow(ServerLevel level, BlockPos pos) {
        Vec3 center = pos.getCenter();
        AABB box = AABB.ofSize(center, GLOW_RADIUS * 2, GLOW_RADIUS * 2, GLOW_RADIUS * 2);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity.position().distanceTo(center) > GLOW_RADIUS) continue;
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION_TICKS));
        }
    }
}
