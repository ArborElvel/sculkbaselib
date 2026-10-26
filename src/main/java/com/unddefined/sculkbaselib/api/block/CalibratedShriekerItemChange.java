package com.unddefined.sculkbaselib.api.block;

/**
 * 校频幽匿尖啸体里内含物品的变更类型。
 *
 * <p>由方块在 {@link CalibratedShriekerMechanism#onItemChanged} 里给出，回调时
 * {@link CalibratedShriekerContext#item()} 还是本次变更对应的那个堆叠——取出与破坏都发生在
 * 物品离开槽位之前。
 */
public enum CalibratedShriekerItemChange {

    /** 物品被放进方块：从无到有、换成别的物品、同类合并都会走这里。 */
    INSERTED,

    /** 物品被玩家取走：空手取出，或者换成了别的物品。 */
    EXTRACTED,

    /** 方块被破坏或替换，物品还在里面。 */
    BROKEN
}
