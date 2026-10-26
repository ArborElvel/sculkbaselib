/**
 * 校频幽匿尖啸体的内含物品机制扩展点。
 *
 * <p>{@link com.unddefined.sculkbaselib.api.block.CalibratedShriekerMechanism} 是
 * 「放进校频幽匿尖啸体后被红石信号触发」的机制契约：方块在信号接通与断开时各回调一次，
 * 具体做什么由机制决定。物品类可以直接实现它，也可以用
 * {@link com.unddefined.sculkbaselib.api.block.CalibratedShriekerMechanisms} 给任意物品（含原版与
 * 其他模组的物品）挂一个；接入说明见仓库根目录的 {@code API.md}。
 */
package com.unddefined.sculkbaselib.api.block;
