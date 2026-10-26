package com.unddefined.sculkbaselib;

import com.mojang.logging.LogUtils;
import com.unddefined.sculkbaselib.server.registry.BlockEntityRegistry;
import com.unddefined.sculkbaselib.server.registry.BlockRegistry;
import com.unddefined.sculkbaselib.server.registry.CreativeModeTabRegistry;
import com.unddefined.sculkbaselib.server.registry.DataRegistry;
import com.unddefined.sculkbaselib.server.registry.ItemRegistry;
import com.unddefined.sculkbaselib.server.registry.MobEffectRegistry;
import com.unddefined.sculkbaselib.server.registry.PotionRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Shared sculk core used by both EnderEchoing and SculkBorne.
 *
 * <p>Content identities live in this mod's namespace. The other two mods only add
 * their own content on top of it.
 */
@Mod(SculkBaseLib.MODID)
public class SculkBaseLib {
    public static final String MODID = "sculkbaselib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SculkBaseLib(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        BlockRegistry.BLOCKS.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        BlockEntityRegistry.BLOCK_ENTITY_TYPES.register(modEventBus);
        CreativeModeTabRegistry.CREATIVE_MODE_TABS.register(modEventBus);
        MobEffectRegistry.MOB_EFFECTS.register(modEventBus);
        PotionRegistry.POTIONS.register(modEventBus);
        DataRegistry.ATTACHMENT_TYPES.register(modEventBus);
    }
}
