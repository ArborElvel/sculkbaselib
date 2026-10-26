package com.unddefined.sculkbaselib.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.api.SculkVeil;
import com.unddefined.sculkbaselib.client.renderer.SculkVeilRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Drives the veil post-processing chains. Content mods can contribute additional
 * "deep dark like" states and overlays that must be drawn after the veil.
 */
@EventBusSubscriber(modid = SculkBaseLib.MODID, value = Dist.CLIENT)
public final class SculkScreenEffects {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final int VEIL_TICKS_START = -21;
    private static final int VEIL_GAMETIME_STEP = 2;
    private static final List<Predicate<ClientLevel>> DEEP_DARK_SOURCES = new CopyOnWriteArrayList<>();
    private static final List<BiConsumer<RenderLevelStageEvent, Float>> AFTER_VEIL = new CopyOnWriteArrayList<>();
    private static int veilTicks = VEIL_TICKS_START;
    private static int deepDarkTicks = VEIL_TICKS_START;
    private static long lastTickGameTime = -1;

    public static void registerDeepDarkSource(Predicate<ClientLevel> source) {
        DEEP_DARK_SOURCES.add(source);
    }

    public static void registerAfterVeil(BiConsumer<RenderLevelStageEvent, Float> renderer) {
        AFTER_VEIL.add(renderer);
    }

    @SubscribeEvent
    public static void renderScreenEffects(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (mc.level == null || mc.player == null) return;

        float partialTicks = event.getPartialTick().getGameTimeDeltaTicks();
        float fadeDelta = partialTicks * 2f;
        boolean hasVeil = SculkVeil.isVeiled(mc.player);
        boolean inDeepDark = mc.level.getBiome(mc.player.blockPosition()).is(Biomes.DEEP_DARK)
                || DEEP_DARK_SOURCES.stream().anyMatch(source -> source.test(mc.level));

        long gameTime = mc.level.getGameTime();
        if (gameTime != lastTickGameTime) {
            lastTickGameTime = gameTime;
            advanceTicks(VEIL_GAMETIME_STEP);
        }

        SculkVeilRenderer.BUFF.updateFadeProgress(hasVeil && !inDeepDark, fadeDelta);
        SculkVeilRenderer.DEEP_DARK.DARKNESS_STRENGTH = hasVeil ? 1f : 0f;
        SculkVeilRenderer.DEEP_DARK.fogDensity = hasVeil ? 0.15f : 0.06f;
        SculkVeilRenderer.DEEP_DARK.updateFadeProgress(inDeepDark, fadeDelta);

        if (SculkVeilRenderer.BUFF.fadeProgress != 0f || SculkVeilRenderer.DEEP_DARK.fadeProgress != 0f)
            renderVeilChains(event, partialTicks);

        for (BiConsumer<RenderLevelStageEvent, Float> renderer : AFTER_VEIL) {
            renderer.accept(event, partialTicks);
        }
    }

    private static void renderVeilChains(RenderLevelStageEvent event, float partialTicks) {
        Matrix4f modelView = event.getModelViewMatrix();
        Matrix4f projection = event.getProjectionMatrix();

        var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(modelView);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
        try {
            mc.getMainRenderTarget().bindWrite(false);
            RenderSystem.disableDepthTest();
            if (SculkVeilRenderer.BUFF.fadeProgress != 0f)
                SculkVeilRenderer.BUFF.render(veilTicks, partialTicks, modelView, projection);
            if (SculkVeilRenderer.DEEP_DARK.fadeProgress != 0f)
                SculkVeilRenderer.DEEP_DARK.render(deepDarkTicks, partialTicks, modelView, projection);
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.restoreProjectionMatrix();
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static void advanceTicks(int step) {
        veilTicks = SculkVeilRenderer.BUFF.fadeProgress != 0f ? veilTicks + step : VEIL_TICKS_START;
        deepDarkTicks = SculkVeilRenderer.DEEP_DARK.fadeProgress != 0f ? deepDarkTicks + step : VEIL_TICKS_START;
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        veilTicks = VEIL_TICKS_START;
        deepDarkTicks = VEIL_TICKS_START;
        lastTickGameTime = -1;
        SculkVeilRenderer.BUFF.fadeProgress = 0f;
        SculkVeilRenderer.DEEP_DARK.fadeProgress = 0f;
    }

    private SculkScreenEffects() {
    }
}
