package com.unddefined.sculkbaselib.mixin;

import com.unddefined.sculkbaselib.api.SculkVeil;
import com.unddefined.sculkbaselib.api.SculkVibrations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "dampensVibrations", at = @At("HEAD"), cancellable = true)
    private void sculkbaselib$dampensVibrations(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if ((self instanceof Player player && SculkVeil.isVeiled(player))
                || SculkVibrations.dampens(self)) {
            cir.setReturnValue(true);
        }
    }
}
