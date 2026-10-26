package com.unddefined.sculkbaselib.client.audio;

import com.unddefined.sculkbaselib.SculkBaseLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 影匿生效后延迟一秒，在耳边放一声被闷住的监守者低鸣（{@code entity.warden.nearby_closer}）。
 *
 * <p>声音只在本地播放：一是只有亲自中了影匿的人该听见，二是低通要挂在具体那个音源上，
 * 服务端播出来的声音没有音源可挂。</p>
 */
@EventBusSubscriber(modid = SculkBaseLib.MODID, value = Dist.CLIENT)
public final class SculkVeilAudio {
    /** 影匿生效到响这一声之间的间隔 */
    private static final int DELAY_TICKS = 10;
    private static final float VOLUME = 0.7F;
    /** 音高：原版音源是重采样播放，所以它同时也是播放速度，0.9 即放慢到九成。 */
    private static final float PITCH = 0.9F;

    /** 已起播、还等着挂低通的实例。音频线程会从这里取，所以用并发集合。 */
    private static final Set<SoundInstance> PENDING = ConcurrentHashMap.newKeySet();

    /** 由 {@code LivingEntityEffectMixin} 在影匿刚被加上时调用。 */
    public static void onVeilAdded(LivingEntity entity) {
        if (entity != Minecraft.getInstance().player) return;
        // 发光的生物拿不到影匿，服务端会当场把效果撤掉，别白响一声
        if (entity.isCurrentlyGlowing()) return;

        // relative + 不衰减：这是贴在耳边的一声，不是世界里某个方向传来的声音，坐标要留在听者身上
        SoundInstance tone = new SimpleSoundInstance(SoundEvents.WARDEN_NEARBY_CLOSER.getLocation(),
                SoundSource.HOSTILE, VOLUME, PITCH, SoundInstance.createUnseededRandom(),
                false, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
        PENDING.add(tone);
        Minecraft.getInstance().getSoundManager().playDelayed(tone, DELAY_TICKS);
    }

    /** 音源起播之后才拿得到 OpenAL source，这是唯一能给这一声改滤镜的时机。 */
    @SubscribeEvent
    public static void onSoundSource(PlaySoundSourceEvent event) {
        if (PENDING.remove(event.getSound()))
            ClientSoundFilters.applyLowpass(event.getChannel(), 0.07f);
    }

    private SculkVeilAudio() {}
}
