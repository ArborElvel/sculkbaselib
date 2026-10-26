package com.unddefined.sculkbaselib.client;

import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.client.api.CalibratedShriekerClientEffectRegistry;
import com.unddefined.sculkbaselib.client.renderer.block.CalibratedSculkShriekerRenderer;
import com.unddefined.sculkbaselib.client.renderer.effect.EchoPulseEffect;
import com.unddefined.sculkbaselib.items.EchoDruse;
import com.unddefined.sculkbaselib.server.registry.BlockEntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.io.IOException;

@Mod(value = SculkBaseLib.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SculkBaseLib.MODID, value = Dist.CLIENT)
public final class SculkBaseLibClient {
    private static final Minecraft mc = Minecraft.getInstance();
    public static PostChain sculkVeilPostChain;
    public static PostChain deepDarkVeilPostChain;

    public SculkBaseLibClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                ResourceLocation veil = ResourceLocation.fromNamespaceAndPath(
                        SculkBaseLib.MODID, "shaders/post/sculk_veil.json");
                sculkVeilPostChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(),
                        mc.getMainRenderTarget(), veil);
                deepDarkVeilPostChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(),
                        mc.getMainRenderTarget(), veil);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            BlockEntityRenderers.register(BlockEntityRegistry.CALIBRATED_SCULK_SHRIEKER.get(),
                    context -> new CalibratedSculkShriekerRenderer());
            CalibratedShriekerClientEffectRegistry.register(EchoDruse.ECHO_PULSE, new EchoPulseEffect());
        });
    }
}
