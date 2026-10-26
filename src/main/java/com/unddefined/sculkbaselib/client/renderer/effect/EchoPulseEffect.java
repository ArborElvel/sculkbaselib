package com.unddefined.sculkbaselib.client.renderer.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.unddefined.sculkbaselib.client.api.CalibratedShriekerClientContext;
import com.unddefined.sculkbaselib.client.api.CalibratedShriekerEffectRenderer;
import com.unddefined.sculkbaselib.items.EchoDruse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * 回响晶簇激活时的一次扩散圆环（仅客户端）。
 *
 * <p>圆环贴着方块中心水平铺开，半径从 0 扩到 {@link #MAX_RADIUS}、末段淡出，正好一次扩散。
 * 颜色取萤石粉贴图的主色（非透明像素的平均值），跟着资源包走。用无贴图的
 * {@link RenderType#debugQuads()} 画，不额外依赖贴图资源；注册在
 * {@code SculkBaseLibClient#onClientSetup}，效果 id 见 {@link EchoDruse#ECHO_PULSE}。
 */
@OnlyIn(Dist.CLIENT)
public final class EchoPulseEffect implements CalibratedShriekerEffectRenderer {

    /** 圆环扩散到的最大半径，与回响晶簇的发光半径一致。 */
    private static final float MAX_RADIUS = 20.0F;
    /** 圆环带的基准厚度，以及随半径变宽的部分。 */
    private static final float BAND_BASE = 0.5F;
    private static final float BAND_GROWTH = 0.06F;
    /** 一圈分多少段。 */
    private static final int SEGMENTS = 32;
    /** 取不到萤石粉贴图时的兜底颜色（萤石粉的暖黄）。 */
    private static final float[] FALLBACK_COLOR = {0.97F, 0.86F, 0.53F};

    /** 用来取主色的萤石粉堆叠，固定一份。 */
    private static final ItemStack GLOWSTONE_DUST = new ItemStack(Items.GLOWSTONE_DUST);
    /** 缓存的萤石粉主色；按贴图实例缓存，换了资源包就重算。 */
    private static TextureAtlasSprite cachedSprite;
    private static float[] cachedColor = FALLBACK_COLOR;

    @Override
    public void render(CalibratedShriekerClientContext context) {
        float[] color = dustColor();
        float progress = context.progress();
        // 先快后慢地铺开，同时淡出
        float radius = MAX_RADIUS * (1.0F - (1.0F - progress) * (1.0F - progress));
        float alpha = 1.0F - progress;
        float halfBand = (BAND_BASE + radius * BAND_GROWTH) * 0.5F;

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        // 姿势栈在方块原点，把落点换算成相对方块的本地偏移
        poseStack.translate((float) (context.effectPos().x - context.pos().getX()),
                (float) (context.effectPos().y - context.pos().getY()),
                (float) (context.effectPos().z - context.pos().getZ()));
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer buffer = context.bufferSource().getBuffer(RenderType.debugQuads());

        for (int i = 0; i < SEGMENTS; i++) {
            float from = (float) (i * Math.PI * 2 / SEGMENTS);
            float to = (float) ((i + 1) * Math.PI * 2 / SEGMENTS);
            // 内外各半条带：中间最亮、两侧渐隐，得到一条软边圆环
            band(buffer, matrix, color, radius - halfBand, radius, from, to, 0.0F, alpha);
            band(buffer, matrix, color, radius, radius + halfBand, from, to, alpha, 0.0F);
        }

        poseStack.popPose();
    }

    /** 萤石粉的主色：整张贴图里非透明像素的平均值，按贴图实例缓存。 */
    private static float[] dustColor() {
        TextureAtlasSprite sprite = glowstoneDustSprite();
        if (sprite != cachedSprite) {
            cachedSprite = sprite;
            cachedColor = averageColor(sprite);
        }
        return cachedColor;
    }

    private static TextureAtlasSprite glowstoneDustSprite() {
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(GLOWSTONE_DUST, null, null, 0);
        return model.getParticleIcon();
    }

    /** 贴图里所有非透明像素的平均色；一个都没有时用兜底色。 */
    private static float[] averageColor(TextureAtlasSprite sprite) {
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        long count = 0L;
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = sprite.getPixelRGBA(0, x, y); // 贴图像素按 ABGR 打包
                if (pixel >>> 24 == 0) continue;
                red += pixel & 0xFF;
                green += pixel >> 8 & 0xFF;
                blue += pixel >> 16 & 0xFF;
                count++;
            }
        }
        if (count == 0L) return FALLBACK_COLOR;
        return new float[]{red / (count * 255.0F), green / (count * 255.0F), blue / (count * 255.0F)};
    }

    /** 一段环形条带，四个顶点依次是内/外半径在起止角度上的角点。 */
    private static void band(VertexConsumer buffer, Matrix4f matrix, float[] color, float inner, float outer,
                             float from, float to, float innerAlpha, float outerAlpha) {
        vertex(buffer, matrix, color, inner, from, innerAlpha);
        vertex(buffer, matrix, color, outer, from, outerAlpha);
        vertex(buffer, matrix, color, outer, to, outerAlpha);
        vertex(buffer, matrix, color, inner, to, innerAlpha);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float[] color,
                               float radius, float angle, float alpha) {
        buffer.addVertex(matrix, radius * (float) Math.cos(angle), 0.0F, radius * (float) Math.sin(angle))
                .setColor(color[0], color[1], color[2], alpha);
    }
}
