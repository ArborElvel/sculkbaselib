package com.unddefined.sculkbaselib.client.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 校频幽匿尖啸体触发效果的一帧绘制参数。
 *
 * <p>由渲染器每帧现造，效果不要把它存起来跨帧使用。
 *
 * @param poseStack    已经平移到方块原点的姿势栈：模型先画完，此时坐标轴与世界一致，朝向带来的旋转已经弹掉
 * @param bufferSource 顶点缓冲
 * @param partialTick  本帧的插值时间
 * @param packedLight  打包光照；要自发光可以换 {@code LightTexture.FULL_BRIGHT}
 * @param progress     播放进度，刚触发的那一帧是 0，趋近 1；已经播完的帧不会再回调
 * @param pos          方块坐标
 * @param blockState   方块状态，含朝向与 {@code redstone_charged}
 * @param effectPos    内含物品与效果在方块里的落点（世界坐标）：姿势栈在方块原点，
 *                     画的时候用它与 {@link #pos()} 的差值当本地偏移
 */
public record CalibratedShriekerClientContext(PoseStack poseStack, MultiBufferSource bufferSource,
                                              float partialTick, int packedLight, float progress,
                                              BlockPos pos, BlockState blockState, Vec3 effectPos) {
}
