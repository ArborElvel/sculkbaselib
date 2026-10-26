package com.unddefined.sculkbaselib.client.api;

/**
 * 校频幽匿尖啸体「触发效果」的客户端绘制入口（仅客户端）。
 *
 * <p>方块实体在触发时用 {@code CalibratedShriekerContext#playEffect} 记下一个效果 id 与持续刻数，
 * 这段信息会随方块实体同步给正在跟踪的客户端；{@code CalibratedSculkShriekerRenderer} 每帧按播放进度
 * 调用这里注册的绘制。没有注册的效果什么都不画。
 *
 * <p>这里只用原版渲染类型，不引用 sculkborne 的内部类，其它模组可以自己注册效果。
 */
@FunctionalInterface
public interface CalibratedShriekerEffectRenderer {

    /**
     * 画一帧效果。
     *
     * @param context 本帧的绘制参数与播放进度，只在本次调用期间有效
     */
    void render(CalibratedShriekerClientContext context);
}
