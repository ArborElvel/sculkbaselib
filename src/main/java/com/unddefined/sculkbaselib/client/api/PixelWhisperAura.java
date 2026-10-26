package com.unddefined.sculkbaselib.client.api;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.unddefined.sculkbaselib.SculkBaseLib;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.WeakHashMap;

/**
 * Draws a procedural, pixelated sculk whisper band around any living entity.
 *
 * <p>The appearance is derived from the two-row whisper sprites: short dark
 * horizontal dashes with sparse teal tips. It does not use a model texture or
 * any model-specific UVs, so it works with vanilla and GeckoLib entities alike.
 *
 * <p>Attach from client code. Each client-visible entity can have an independent
 * specification. The effect is made from four thin panels around the entity
 * bounds and changes pattern every {@link Spec#frameTicks()} game ticks.
 */
@EventBusSubscriber(modid = SculkBaseLib.MODID, value = Dist.CLIENT)
public final class PixelWhisperAura {
    public record Spec(float bandHeightFactor, float verticalOffsetFactor, float horizontalPadding,
                       int columns, int frameTicks, int frameCount, int opacity, long seed) {
        public Spec {
            if (bandHeightFactor <= 0.0F) {
                throw new IllegalArgumentException("bandHeightFactor must be > 0");
            }
            if (horizontalPadding < 0.0F) {
                throw new IllegalArgumentException("horizontalPadding must be >= 0");
            }
            if (columns < 8) {
                throw new IllegalArgumentException("columns must be >= 8");
            }
            if (frameTicks < 1) {
                throw new IllegalArgumentException("frameTicks must be >= 1");
            }
            if (frameCount < 1) {
                throw new IllegalArgumentException("frameCount must be >= 1");
            }
            if (opacity < 0 || opacity > 255) {
                throw new IllegalArgumentException("opacity must be in [0, 255]");
            }
        }
    }

    public static final Spec DEFAULT = new Spec(
            0.38F, 0.0F, 0.035F,
            16, 3, 39, 235,
            0x53434C4B5F5748L);

    private static final int ROWS = 18;
    private static final int CORE_RED = 17;
    private static final int CORE_GREEN = 27;
    private static final int CORE_BLUE = 33;
    private static final int EDGE_RED = 5;
    private static final int EDGE_GREEN = 42;
    private static final int EDGE_BLUE = 50;

    private static final Map<LivingEntity, Spec> ACTIVE = new WeakHashMap<>();

    /** Attach the effect to a client-side entity. No-op on the logical server. */
    public static void attach(LivingEntity entity, Spec spec) {
        if (!entity.level().isClientSide) return;
        ACTIVE.put(entity, Objects.requireNonNull(spec, "spec"));
    }

    public static void detach(LivingEntity entity) {
        ACTIVE.remove(entity);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Spec spec = ACTIVE.get(entity);
        if (spec == null) return;
        if (entity.isRemoved()) {
            ACTIVE.remove(entity);
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.debugQuads());

        float entityHeight = Math.max(entity.getBbHeight(), 0.05F);
        float bandHeight = entityHeight * spec.bandHeightFactor();
        float centerY = entityHeight * (0.5F + spec.verticalOffsetFactor());
        float panelWidth = Math.max(entity.getBbWidth(), 0.05F) + spec.horizontalPadding() * 2.0F;
        float halfPanel = panelWidth * 0.5F;
        float rowHeight = bandHeight / ROWS;
        long frame = (long) entity.tickCount / spec.frameTicks() % spec.frameCount();
        long seed = spec.seed()
                ^ entity.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(entity.getUUID().getLeastSignificantBits(), 17)
                ^ frame * 0x9E3779B97F4A7C15L;
        SplittableRandom random = new SplittableRandom(seed);

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();
        float bottom = centerY - bandHeight * 0.5F;

        drawFace(buffer, pose, random, spec, halfPanel, -halfPanel, 1.0F, 0.0F, bottom, rowHeight);
        drawFace(buffer, pose, random, spec, halfPanel, halfPanel, -1.0F, 0.0F, bottom, rowHeight);
        drawFace(buffer, pose, random, spec, halfPanel, -halfPanel, 0.0F, 1.0F, bottom, rowHeight);
        drawFace(buffer, pose, random, spec, halfPanel, halfPanel, 0.0F, -1.0F, bottom, rowHeight);

        poseStack.popPose();
    }

    /**
     * Draws one vertical texture face. {@code ux/uz} define its left-to-right
     * direction and {@code plane} is the fixed axis of the face.
     */
    private static void drawFace(VertexConsumer buffer, Matrix4f pose, SplittableRandom random,
                                 Spec spec, float halfPanel, float plane, float ux, float uz,
                                 float bottom, float rowHeight) {
        float columnWidth = halfPanel * 2.0F / spec.columns();

        for (int row = 0; row < ROWS; row++) {
            if (!hasWhisperGlyphs(row)) continue;

            float y0 = bottom + row * rowHeight;
            float y1 = y0 + rowHeight;
            int column = random.nextInt(3);

            while (column < spec.columns()) {
                int length = 2 + random.nextInt(3);
                int end = Math.min(spec.columns(), column + length);
                int edge = random.nextInt(4) == 0
                        ? (random.nextBoolean() ? column : end - 1)
                        : -1;

                for (int x = column; x < end; x++) {
                    float u0 = -halfPanel + x * columnWidth;
                    float u1 = u0 + columnWidth;
                    boolean teal = x == edge;
                    int red = teal ? EDGE_RED : CORE_RED;
                    int green = teal ? EDGE_GREEN : CORE_GREEN;
                    int blue = teal ? EDGE_BLUE : CORE_BLUE;

                    if (ux != 0.0F) {
                        addQuad(buffer, pose, u0 * ux, y0, plane, u1 * ux, y1, plane,
                                red, green, blue, spec.opacity());
                    } else {
                        addQuad(buffer, pose, plane, y0, u0 * uz, plane, y1, u1 * uz,
                                red, green, blue, spec.opacity());
                    }
                }

                column = end + 2 + random.nextInt(5);
            }
        }
    }

    private static boolean hasWhisperGlyphs(int row) {
        return row >= 1 && row <= 7 || row >= 13 && row <= 17;
    }

    private static void addQuad(VertexConsumer buffer, Matrix4f pose,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                int red, int green, int blue, int alpha) {
        buffer.addVertex(pose, x0, y0, z0).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, x1, y0, z1).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, x0, y1, z0).setColor(red, green, blue, alpha);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    private PixelWhisperAura() {
    }
}
