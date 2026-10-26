package com.unddefined.sculkbaselib.api;

import com.unddefined.sculkbaselib.SculkBaseLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * SculkBaseLib 的方块标签。
 *
 * <p>标签内容由数据包提供，文件位于 {@code data/sculkbaselib/tags/block/} 下，运行时通过
 * {@code BlockState#is(TagKey)} 判定。其他模组可以直接往这些标签里加自己的方块，
 * 不需要编译期依赖。
 */
public final class SculkTags {
    private SculkTags() {
    }

    /** 幽匿系方块：幽匿生物站在其上回血、在其附近生成等规则共用。 */
    public static final TagKey<Block> SCULK_BLOCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(SculkBaseLib.MODID, "sculk_blocks"));
}
