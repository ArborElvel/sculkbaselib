package com.unddefined.sculkbaselib.mixin;

import com.unddefined.sculkbaselib.Config;
import com.unddefined.sculkbaselib.api.SculkCatalystHooks;
import com.unddefined.sculkbaselib.api.SculkDruseEvent;
import com.unddefined.sculkbaselib.blocks.entity.EchoDruseBlockEntity;
import com.unddefined.sculkbaselib.server.SculkBloom;
import com.unddefined.sculkbaselib.server.registry.BlockRegistry;
import net.minecraft.Optionull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SculkCatalystBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Shared Sculk Catalyst handling: druse growth, shrieker CAN_SUMMON, and a hook
 * for content mods (SculkBorne's spreader transformation).
 */
@Mixin(targets = "net/minecraft/world/level/block/entity/SculkCatalystBlockEntity$CatalystListener")
public class CatalystListenerMixin {

    @Final
    @Shadow
    private PositionSource positionSource;

    @Inject(method = "handleGameEvent", at = @At("HEAD"), cancellable = true)
    private void sculkbaselib$handleGameEvent(ServerLevel level, Holder<GameEvent> gameEvent,
                                             GameEvent.Context context, Vec3 pos,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!gameEvent.is(GameEvent.ENTITY_DIE)) return;
        if (!(context.sourceEntity() instanceof LivingEntity livingEntity)
                || livingEntity.wasExperienceConsumed()) return;

        Optional<Vec3> catalystPosOpt = positionSource.getPosition(level);
        if (catalystPosOpt.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        BlockPos catalystPos = BlockPos.containing(catalystPosOpt.get());

        if (SculkCatalystHooks.dispatch(level, catalystPos, livingEntity)) {
            cir.setReturnValue(true);
            return;
        }

        int experienceReward = livingEntity.getExperienceReward(level,
                Optionull.map(livingEntity.getLastDamageSource(), DamageSource::getEntity));
        if (!livingEntity.shouldDropExperience() || experienceReward < 1) {
            cir.setReturnValue(false);
            return;
        }

        BlockState aboveState = level.getBlockState(catalystPos.above());
        if (aboveState.getBlock() == BlockRegistry.ECHO_DRUSE.get()
                && level.getBlockEntity(catalystPos.above()) instanceof EchoDruseBlockEntity echoDruse) {
            int maxGrowth = Config.ECHO_DRUSE_MAX_GROWTH_VALUE.get();
            if (echoDruse.getGrowthValue() <= maxGrowth) {
                echoDruse.setGrowthValue(echoDruse.getGrowthValue() + experienceReward);
                livingEntity.skipDropExperience();
                sculkbaselib$bloom(level, catalystPos);
                Entity killer = Optionull.map(livingEntity.getLastDamageSource(), DamageSource::getEntity);
                ServerPlayer source = killer instanceof ServerPlayer player ? player : null;
                NeoForge.EVENT_BUS.post(new SculkDruseEvent.Grown(
                        level, catalystPos.above(), echoDruse.getGrowthValue(), source));
                cir.setReturnValue(true);
                return;
            }
        }

        if (level.getBlockEntity(catalystPos.above()) instanceof SculkShriekerBlockEntity shrieker
                && !aboveState.getValue(SculkShriekerBlock.CAN_SUMMON)) {
            if (level.getRandom().nextInt(Config.SCULK_SHRIEKER_CAN_SUMMON_CHANCE.get()) == 0) {
                level.setBlock(catalystPos.above(),
                        aboveState.setValue(SculkShriekerBlock.CAN_SUMMON, true), 3);
                Player nearest = level.getNearestPlayer(
                        catalystPos.getX(), catalystPos.getY(), catalystPos.getZ(), 8, false);
                ServerPlayer player = nearest instanceof ServerPlayer serverPlayer ? serverPlayer : null;
                shrieker.tryShriek(level, player);
            }
            livingEntity.skipDropExperience();
            sculkbaselib$bloom(level, catalystPos);
            cir.setReturnValue(true);
        }
    }

    private void sculkbaselib$bloom(ServerLevel level, BlockPos catalystPos) {
        BlockState catalystState = level.getBlockState(catalystPos);
        level.setBlock(catalystPos, catalystState.setValue(SculkCatalystBlock.PULSE, Boolean.TRUE), 3);
        level.scheduleTick(catalystPos, catalystState.getBlock(), 8);
        SculkBloom.playBloomEffects(level, catalystPos);
    }
}
