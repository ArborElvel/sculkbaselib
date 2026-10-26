package com.unddefined.sculkbaselib;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Gameplay values for the shared sculk core.
 *
 * <p>These keys previously existed in both EnderEchoing and SculkBorne. Keep the
 * single source of truth here.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SCULK_VEIL_DARKNESS_DURATION = BUILDER
            .comment("Duration of the Darkness effect applied by Sculk Veil, in seconds.")
            .defineInRange("sculk_veil_darkness_duration", 20, 20, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SCULK_VEIL_GLOWING_DURATION = BUILDER
            .comment("Glowing duration granted when Sculk Veil expires, in seconds.")
            .defineInRange("sculk_veil_glowing_duration", 25, 20, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SCULK_VEIL_PLAYER_DARKNESS_TICKS = BUILDER
            .comment("Net accumulated veil ticks required for a player's permanent Darkness. The design document specifies 3 minutes.")
            .defineInRange("sculk_veil_player_darkness_ticks", 20 * 60 * 3, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SCULK_VEIL_MOB_DARKNESS_TICKS = BUILDER
            .comment("Net accumulated veil ticks required for a non-player entity's permanent Darkness.")
            .defineInRange("sculk_veil_mob_darkness_ticks", 20 * 60 * 2, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ECHO_DRUSE_MAX_GROWTH_VALUE = BUILDER
            .comment("Maximum growth value of an Echo Druse.")
            .defineInRange("echo_druse_max_growth_value", 40000, 4, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SCULK_SHRIEKER_CAN_SUMMON_CHANCE = BUILDER
            .comment("1 in N chance for a Sculk Shrieker to gain CAN_SUMMON when a nearby entity dies on a Sculk Catalyst.")
            .defineInRange("sculk_shrieker_can_summon_chance", 7, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
