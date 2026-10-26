package com.unddefined.sculkbaselib.client.model.block;

import com.unddefined.sculkbaselib.SculkBaseLib;
import com.unddefined.sculkbaselib.blocks.entity.CalibratedSculkShriekerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class CalibratedSculkShriekerModel<T extends CalibratedSculkShriekerBlockEntity> extends DefaultedBlockGeoModel<T> {
    public static final ResourceLocation ASSET =
            ResourceLocation.fromNamespaceAndPath(SculkBaseLib.MODID, "calibrated_sculk_shrieker");

    public CalibratedSculkShriekerModel() {
        super(ASSET);
    }
}
