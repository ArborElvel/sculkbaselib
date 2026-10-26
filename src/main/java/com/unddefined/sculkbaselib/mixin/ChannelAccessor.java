package com.unddefined.sculkbaselib.mixin;

import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取 {@link Channel} 底层的 OpenAL source id：滤镜（EFX）是按 source 挂的，
 * 而 {@code Channel} 只把它当私有字段用，没有对外暴露。
 */
@Mixin(Channel.class)
public interface ChannelAccessor {

    @Accessor("source")
    int sculkbaselib$getSource();
}
