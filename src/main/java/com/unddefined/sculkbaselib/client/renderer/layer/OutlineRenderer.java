package com.unddefined.sculkbaselib.client.renderer.layer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * GeckoLib Geo 模型的几何描边：顶点沿面法线外扩。
 *
 * <p>从 EnderEchoing 的 {@code OutlineRenderer} 整份拷来的，只有缩放中心从写死的 {@code Y = 3/16}
 * （回响核心模型）改成了调用方传入的 {@code pivotY}，这样别的模型也能用同一套描边。
 *
 * <p>只画几何的背面（顶点绕序反转 + CULL），近侧表面被剔除：描边因此不会盖住它包着的核心，
 * 而是从外壳的镂空里露出一圈贴着核心的光。
 *
 * <p>外扩方向取的是<b>顶点平滑法线</b>（同一位置在相邻几个面上出现的法线之和），而不是逐个面的面法线：
 * 按面法线外扩时，立方体一个角上的三份顶点会各朝自己那面的方向跑，相邻面在棱上错开、露出一条缝；
 * 沿平滑法线走，三份顶点才会落在同一点，外壳保持闭合。
 *
 * <p>必须在基础模型之后调用，并且调用前先把基础模型的批次 flush 掉：此时模型已写入深度，
 * 比描边更近的外壳/核心会把描边挡掉，比描边更远的方块和实体也会被描边挡住（描边写深度），
 * 前后遮挡都交给正常的深度测试，不需要关掉深度测试（关掉反而会被自己的模型按绘制顺序盖住）。
 */
public final class OutlineRenderer {

    private OutlineRenderer() {
    }

    /**
     * 用描边自己的批次画一圈描边。
     *
     * @param targetBone 目标骨骼名，找不到时什么都不画
     * @param scale      放大倍数，绕 {@code pivotY} 缩放
     * @param color      ARGB 颜色
     * @param offset     沿面法线外扩的偏移量
     * @param pivotY     缩放的竖直中心（模型空间，单位为格），取模型中心可避免缩放把模型整体顶偏
     */
    public static void render(PoseStack poseStack, BakedGeoModel model, String targetBone, float scale, int color,
                              float offset, float pivotY) {
        ByteBufferBuilder outlineBuilder = new ByteBufferBuilder(256);
        try (outlineBuilder) {
            var outlineSource = MultiBufferSource.immediate(outlineBuilder);
            var outlineBuffer = outlineSource.getBuffer(OUTLINE_RENDER_TYPE);
            render(poseStack, model, outlineBuffer, targetBone, scale, color, offset, pivotY, true);
            outlineSource.endBatch();
        }
    }

    public static void render(PoseStack poseStack, BakedGeoModel model, VertexConsumer consumer, String targetBone,
                              float scale, int color, float offset, float pivotY) {
        render(poseStack, model, consumer, targetBone, scale, color, offset, pivotY, true);
    }

    /**
     * 只写深度用的预处理遍：按正常绕序（正面）把模型本身的几何写进 {@code consumer}，不缩放也不外扩。
     *
     * <p>给半透明本体用：半透明的实体渲染类型默认只写颜色不写深度，描边（外扩壳的背面）会因此
     * 穿过本体铺满整个轮廓，看起来不像描边而像一整块外壳。先用这一遍把模型的深度垫进深度缓冲，
     * 描边内层就会被正常挡掉，只剩轮廓外缘那一圈。
     *
     * @param targetBone 只写这根骨骼；找不到时什么都不写
     * @param pivotY     与描边保持同一套缩放中心（这一遍 scale = 1，其实用不到）
     */
    public static void renderDepth(PoseStack poseStack, BakedGeoModel model, VertexConsumer consumer, String targetBone,
                                   float pivotY) {
        render(poseStack, model, consumer, targetBone, 1.0F, 0xFFFFFFFF, 0.0F, pivotY, false);
    }

    private static void render(PoseStack poseStack, BakedGeoModel model, VertexConsumer consumer, String targetBone,
                               float scale, int color, float offset, float pivotY, boolean backFaces) {
        for (GeoBone bone : model.topLevelBones()) {
            GeoBone target = findBone(bone, targetBone);
            if (target != null) {
                poseStack.pushPose();
                try {
                    // 绕模型中心缩放：直接 scale 会以模型原点为基准，把整体往一侧顶出去
                    poseStack.translate(0F, pivotY, 0F);
                    poseStack.scale(scale, scale, scale);
                    poseStack.translate(0F, -pivotY, 0F);
                    renderBone(poseStack, target, consumer, color, offset, backFaces);
                } finally {
                    poseStack.popPose();
                }
                return;
            }
        }
    }

    private static GeoBone findBone(GeoBone bone, String name) {
        if (bone.getName().equals(name)) return bone;
        for (GeoBone child : bone.getChildBones()) {
            GeoBone result = findBone(child, name);
            if (result != null) return result;
        }
        return null;
    }

    private static void renderBone(PoseStack poseStack, GeoBone bone, VertexConsumer consumer, int color, float offset,
                                   boolean backFaces) {
        if (bone.isHidden()) return;
        poseStack.pushPose();
        try {
            RenderUtil.prepMatrixForBone(poseStack, bone);

            for (GeoCube cube : bone.getCubes()) renderCube(poseStack, cube, consumer, color, offset, backFaces);

            if (!bone.isHidingChildren())
                for (GeoBone child : bone.getChildBones()) renderBone(poseStack, child, consumer, color, offset, backFaces);

        } finally {
            poseStack.popPose();
        }
    }

    private static void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer consumer, int color, float offset,
                                   boolean backFaces) {
        poseStack.pushPose();
        try {
            RenderUtil.translateToPivotPoint(poseStack, cube);
            RenderUtil.rotateMatrixAroundCube(poseStack, cube);
            RenderUtil.translateAwayFromPivotPoint(poseStack, cube);

            Matrix4f matrix = new Matrix4f(poseStack.last().pose());
            Vector3f normal = new Vector3f();
            Map<VertexKey, Vector3f> smoothedNormals = offset == 0F ? Map.of() : collectSmoothedNormals(cube);

            for (GeoQuad quad : cube.quads()) {
                if (quad == null) continue;

                // 外扩必须使用模型空间法线；变换后的法线只用于写入顶点属性。
                Vector3f localNormal = new Vector3f(quad.normal()).normalize();
                normal.set(localNormal);
                poseStack.last().normal().transform(normal).normalize();

                // 描边逆序写入：GeckoLib 的 quad 是按"从外侧看逆时针"排的正面，
                // 反过来配合 CULL 就只留下背面那一层，描边才不会糊在核心正面。
                // 深度预处理遍用正常绕序，只留正面，写下的才是最近的那一层深度。
                GeoVertex[] vertices = quad.vertices();
                for (int i = 0; i < vertices.length; i++) {
                    int index = backFaces ? vertices.length - 1 - i : i;
                    writeVertex(consumer, matrix, vertices[index], localNormal, normal, smoothedNormals, offset, color);
                }
            }
        } finally {
            poseStack.popPose();
        }
    }

    /** 写出一个顶点：先按（可选的）平滑法线外扩，再应用骨骼/立方体矩阵。 */
    private static void writeVertex(VertexConsumer consumer, Matrix4f matrix, GeoVertex vertex, Vector3f localNormal,
                                    Vector3f transformedNormal, Map<VertexKey, Vector3f> smoothedNormals,
                                    float offset, int color) {
        Vector3f position = new Vector3f(vertex.position());
        expandVertex(position, vertex.position(), localNormal, smoothedNormals, offset);
        matrix.transformPosition(position);

        consumer.addVertex(position.x(), position.y(), position.z()).setColor(color)
                .setUv(vertex.texU(), vertex.texV()).setUv1(0, 1).setUv2(2, 0)
                .setNormal(transformedNormal.x, transformedNormal.y, transformedNormal.z);
    }

    /**
     * 把一个顶点向外扩。有相邻面的平滑法线时沿它走，这样棱上的几份顶点会落到同一点；
     * 为了让每个面都正好外扩 {@code offset}，沿平滑法线要走的距离是
     * {@code offset / cos(平滑法线, 面法线)}——立方体一个角上这个系数是 {@code √3}。
     *
     * @param out     写出的位置（原地修改）
     * @param source  该顶点的模型空间坐标，用来查平滑法线
     * @param face    所在面的法线（已归一化），兜底与夹角修正都用它
     * @param offset  面沿自身法线外扩的距离
     */
    private static void expandVertex(Vector3f out, Vector3f source, Vector3f face,
                                     Map<VertexKey, Vector3f> smoothedNormals, float offset) {
        if (offset == 0F) return;

        Vector3f smoothed = smoothedNormals.get(VertexKey.of(source));
        if (smoothed == null) {
            out.fma(offset, face);
            return;
        }

        float cosine = Math.max(smoothed.dot(face), 1.0E-4F);
        out.fma(offset / cosine, smoothed);
    }

    /** 累加每个顶点在相邻面上出现的法线，得到顶点的平滑法线；同一个立方体内部才合并。 */
    private static Map<VertexKey, Vector3f> collectSmoothedNormals(GeoCube cube) {
        Map<VertexKey, Vector3f> normals = new HashMap<>();

        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;

            Vector3f faceNormal = new Vector3f(quad.normal()).normalize();
            for (GeoVertex vertex : quad.vertices()) {
                normals.computeIfAbsent(VertexKey.of(vertex.position()), key -> new Vector3f()).add(faceNormal);
            }
        }

        for (Vector3f normal : normals.values()) {
            if (normal.lengthSquared() > 1.0E-8F) normal.normalize();
        }
        return normals;
    }

    /**
     * 顶点位置作为 key：模型顶点坐标本身就有同名顶点共享同一坐标，这里再按 1/1000 取整，
     * 抹掉不同面各自算出来时的浮点误差，避免同一个角被拆成两份。
     */
    private record VertexKey(int x, int y, int z) {
        static VertexKey of(Vector3f position) {
            return new VertexKey(Math.round(position.x * 1000F), Math.round(position.y * 1000F),
                    Math.round(position.z * 1000F));
        }
    }

    /**
     * 描边用的渲染类型。
     *
     * <p>加法混合（{@code GLINT_TRANSPARENCY}）只看 RGB、不看 alpha，所以淡入淡出要缩放颜色通道，
     * 而不是改 alpha。背面剔除配合上面反转的顶点绕序：只画远离相机的那层几何。
     * 写深度让描边能挡住它后面的实体，否则后画的实体还会从描边里透出来。
     */
    public static final RenderType OUTLINE_RENDER_TYPE = RenderType.create(
            "sculkbaselib_outline",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setCullState(RenderStateShard.CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .createCompositeState(false)
    );
}
