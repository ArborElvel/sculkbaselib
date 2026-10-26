package com.unddefined.sculkbaselib.client.api;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.FastBoneFilterGeoLayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Draws chosen GeckoLib bones with a separate texture.
 *
 * <p>The named bones are hidden during the main model pass, then redrawn after
 * all bones have been written to the main buffer. This avoids ending the main
 * {@link RenderType} batch in the middle of the model, which can drop later
 * bones when an outline buffer source is active.
 *
 * <p>This layer works for any GeckoLib model that has the named overlay bones;
 * vanilla/non-GeckoLib renderers should use a world-space particle or overlay
 * renderer instead.
 *
 * <p>Textures with an {@code .mcmeta} animation are advanced through GeckoLib's
 * {@link AnimatableTexture}. A function can return a different texture for each
 * animatable when the overlay texture is not shared.
 */
public class GeoBoneOverlayLayer<T extends GeoAnimatable> extends FastBoneFilterGeoLayer<T> {
    private final List<String> bones;
    private final Function<T, ResourceLocation> texture;
    private final Predicate<T> visible;
    private final Function<ResourceLocation, RenderType> renderTypeFactory;
    private final boolean animatedTexture;
    private final Map<String, BonePose> poses = new LinkedHashMap<>();

    public GeoBoneOverlayLayer(GeoRenderer<T> renderer, List<String> bones, ResourceLocation texture) {
        this(renderer, bones, animatable -> texture, animatable -> true,
                RenderType::entityTranslucent, true);
    }

    public GeoBoneOverlayLayer(GeoRenderer<T> renderer, List<String> bones,
                               Function<T, ResourceLocation> texture) {
        this(renderer, bones, texture, animatable -> true, RenderType::entityTranslucent, true);
    }

    public GeoBoneOverlayLayer(GeoRenderer<T> renderer, List<String> bones,
                               Function<T, ResourceLocation> texture,
                               Predicate<T> visible, Function<ResourceLocation, RenderType> renderType,
                               boolean animatedTexture) {
        super(renderer, () -> List.copyOf(bones), (bone, animatable, partialTick) -> {
            if (!visible.test(animatable)) return;
            bone.setHidden(true);
            // setHidden also hides children; keep the overlay subtree traversable.
            bone.setChildrenHidden(false);
        });
        this.bones = List.copyOf(bones);
        this.texture = Objects.requireNonNull(texture, "texture");
        this.visible = visible;
        this.renderTypeFactory = renderType;
        this.animatedTexture = animatedTexture;
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                          @Nullable RenderType renderType, MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, float partialTick,
                          int packedLight, int packedOverlay) {
        super.preRender(poseStack, animatable, bakedModel, renderType, bufferSource,
                buffer, partialTick, packedLight, packedOverlay);
        poses.clear();
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        if (!visible.test(animatable) || !bones.contains(bone.getName())) return;
        poses.put(bone.getName(), new BonePose(bone,
                new Matrix4f(poseStack.last().pose()),
                new Matrix3f(poseStack.last().normal())));
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel,
                       @Nullable RenderType renderType, MultiBufferSource bufferSource,
                       @Nullable VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        if (poses.isEmpty() || buffer == null || !visible.test(animatable)) return;

        ResourceLocation overlayTexture = texture.apply(animatable);
        if (overlayTexture == null) return;
        if (animatedTexture) AnimatableTexture.setAndUpdate(overlayTexture);
        int colour = getRenderer().getRenderColor(animatable, partialTick, packedLight).argbInt();
        VertexConsumer overlayBuffer = bufferSource.getBuffer(renderTypeFactory.apply(overlayTexture));

        for (BonePose bonePose : poses.values()) {
            poseStack.pushPose();
            poseStack.last().pose().set(bonePose.pose());
            poseStack.last().normal().set(bonePose.normal());
            for (GeoCube cube : bonePose.bone().getCubes()) {
                poseStack.pushPose();
                getRenderer().renderCube(poseStack, cube, overlayBuffer, packedLight, packedOverlay, colour);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        poses.clear();
    }

    private record BonePose(GeoBone bone, Matrix4f pose, Matrix3f normal) {
    }
}
