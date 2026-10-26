package com.unddefined.sculkbaselib.client.model.block;

import com.unddefined.sculkbaselib.blocks.entity.CalibratedSculkShriekerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class CalibratedSculkShriekerModel<T extends CalibratedSculkShriekerBlockEntity> extends DefaultedBlockGeoModel<T> {
    public CalibratedSculkShriekerModel() {
        super(ResourceLocation.fromNamespaceAndPath("sculkbaselib", "calibrated_sculk_shrieker"));
    }
}
