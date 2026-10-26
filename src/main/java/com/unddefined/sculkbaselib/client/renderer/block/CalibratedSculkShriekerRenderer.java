package com.unddefined.sculkbaselib.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.unddefined.sculkbaselib.blocks.CalibratedSculkShriekerBlock;
import com.unddefined.sculkbaselib.blocks.entity.CalibratedSculkShriekerBlockEntity;
import com.unddefined.sculkbaselib.client.api.CalibratedShriekerClientContext;
import com.unddefined.sculkbaselib.client.api.CalibratedShriekerClientEffectRegistry;
import com.unddefined.sculkbaselib.client.api.CalibratedShriekerEffectRenderer;
import com.unddefined.sculkbaselib.client.model.block.CalibratedSculkShriekerModel;
import com.unddefined.sculkbaselib.client.renderer.layer.CalibratedSculkShriekerLayer;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class CalibratedSculkShriekerRenderer extends GeoBlockRenderer<CalibratedSculkShriekerBlockEntity> {
    public CalibratedSculkShriekerRenderer() {
        super(new CalibratedSculkShriekerModel<>());
        // 添加渲染层以渲染物品
        addRenderLayer(new CalibratedSculkShriekerLayer(this));
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.translate(0, -0.5, -0.5);
            }
            case WEST -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                poseStack.translate(0.5, -0.5, 0);
            }
            case NORTH -> {
                poseStack.mulPose(Axis.XN.rotationDegrees(90));
                poseStack.translate(0, -0.5, 0.5);
            }
            case EAST -> {
                poseStack.mulPose(Axis.ZN.rotationDegrees(90));
                poseStack.translate(-0.5, -0.5, 0);
            }
            case DOWN -> {
                poseStack.mulPose(Axis.XN.rotationDegrees(180));
                poseStack.translate(0, -1.0, 0);
            }
            default -> poseStack.translate(0, 0, 0);

        }
    }

    @Override
    protected Direction getFacing(CalibratedSculkShriekerBlockEntity block) {
        BlockState blockState = block.getBlockState();

        // 检查我们自己的FACING属性
        if (blockState.hasProperty(CalibratedSculkShriekerBlock.FACING))
            return blockState.getValue(CalibratedSculkShriekerBlock.FACING);

        return Direction.UP;
    }

    @Override
    public void render(CalibratedSculkShriekerBlockEntity animatable, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        renderTriggeredEffect(animatable, partialTick, poseStack, bufferSource, packedLight);
    }

    /**
     * 触发时由机制指定的效果（{@code CalibratedShriekerContext#playEffect}）：模型画完之后调用，
     * 姿势栈已经回到方块原点，朝向带来的旋转已经弹掉，所以效果可以直接按世界坐标画。
     * 绘制本体在 {@link CalibratedShriekerClientEffectRegistry} 里注册，没注册就什么都不画。
     */
    private static void renderTriggeredEffect(CalibratedSculkShriekerBlockEntity block, float partialTick,
                                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ResourceLocation effect = block.effectId();
        if (effect == null || block.effectDurationTicks() <= 0) return;
        CalibratedShriekerEffectRenderer renderer = CalibratedShriekerClientEffectRegistry.get(effect);
        if (renderer == null) return;
        Level level = block.getLevel();
        if (level == null) return;

        float progress = (level.getGameTime() + partialTick - block.effectStartTick()) / block.effectDurationTicks();
        if (progress < 0.0F || progress >= 1.0F) return;

        renderer.render(new CalibratedShriekerClientContext(poseStack, bufferSource, partialTick, packedLight,
                progress, block.getBlockPos(), block.getBlockState(), block.effectPos()));
    }
}
