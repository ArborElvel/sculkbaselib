package com.unddefined.sculkbaselib.client.audio;

import com.mojang.blaze3d.audio.Channel;
import com.unddefined.sculkbaselib.mixin.ChannelAccessor;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

/**
 * 用 OpenAL EFX 的滤镜给单个音源加闷声。原版的音频层只用 AL10 设音量 / 音高 / 位置，
 * 一个滤镜都没有，所以这类效果只能自己往 OpenAL 上挂。
 *
 * <p>滤镜挂在 source 上：音源起播时从 {@link Channel} 取到它的 OpenAL source id
 * （{@link ChannelAccessor}），再把 {@code AL_DIRECT_FILTER} 指过去。原版的音源池每次取用都新建
 * source、归还时销毁，所以挂上去的滤镜只作用于这一个音效，不会漏给下一个。</p>
 *
 * <p>滤镜对象本身是共享的，改参数等于改所有挂着它的音源，所以这里只维护一个固定档位；
 * 以后要用别的档位，按档位各生成一个。</p>
 */
public final class ClientSoundFilters {
    /** 滤镜所属的 OpenAL 上下文；上下文换了（资源重载会重建音频引擎）就得重建滤镜。 */
    private static long context;
    private static int lowpass;

    /** 给这一个音源挂上低通。设备不支持 EFX 时什么也不做，调用方照常发声。 */
    public static void applyLowpass(Channel channel, float gainHF) {
        long current = ALC10.alcGetCurrentContext();
        if (current != context) prepare(current);
        if (lowpass == 0) return;
        EXTEfx.alFilterf(lowpass, EXTEfx.AL_LOWPASS_GAIN, 1.0F);
        EXTEfx.alFilterf(lowpass, EXTEfx.AL_LOWPASS_GAINHF, gainHF);
        AL11.alSourcei(((ChannelAccessor) channel).sculkbaselib$getSource(), EXTEfx.AL_DIRECT_FILTER, lowpass);
    }

    /**
     * 在当前上下文中备好低通滤镜。只能在音频线程上调用——OpenAL 的上下文是按线程 current 的，
     * 主线程上取不到，所以不放在客户端初始化里。
     */
    private static void prepare(long current) {
        context = current;
        lowpass = 0;
        if (current == 0L) return;
        long device = ALC10.alcGetContextsDevice(current);
        if (device == 0L || !ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) return;
        lowpass = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(lowpass, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
    }

    private ClientSoundFilters() {}
}
