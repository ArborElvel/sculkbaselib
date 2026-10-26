package com.unddefined.sculkbaselib.blocks.entity;

import com.unddefined.sculkbaselib.api.block.CalibratedShriekerContext;
import com.unddefined.sculkbaselib.api.block.CalibratedShriekerMechanism;
import com.unddefined.sculkbaselib.api.block.CalibratedShriekerMechanisms;
import com.unddefined.sculkbaselib.blocks.CalibratedSculkShriekerBlock;
import com.unddefined.sculkbaselib.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ContainerSingleItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CalibratedSculkShriekerBlockEntity extends BlockEntity implements GeoBlockEntity,
        ContainerSingleItem.BlockContainerSingleItem, CalibratedShriekerContext {
    /** 方块建好时还没有内含物品、也就没有冷却来源，先记成这个值，等第一件带机制的物品出现再初始化。 */
    private static final int UNINITIALIZED_COOLDOWN = -1;
    private static final RawAnimation itemRenderAnimation = RawAnimation.begin().thenPlay("item");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private int cooldownTicks = UNINITIALIZED_COOLDOWN;
    /** 正在播放的触发效果，随方块实体同步给客户端；没有时为 {@code null}。 */
    @Nullable private ResourceLocation effectId;
    private long effectStartTick;
    private int effectDurationTicks;

    public CalibratedSculkShriekerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.CALIBRATED_SCULK_SHRIEKER.get(), pos, blockState);
    }

    /**
     * 内含物品的逐刻回调：方块只负责调度与冷却，具体做什么由机制决定。
     *
     * <p>回调发生在递减之前，所以机制读到的是本刻开始时的冷却值。
     */
    public static void tick(Level level, BlockPos pos, BlockState state, CalibratedSculkShriekerBlockEntity self) {
        if (!(level instanceof ServerLevel)) return;
        CalibratedShriekerMechanism mechanism = CalibratedShriekerMechanisms.get(self.getTheItem());
        if (mechanism == null) return;
        mechanism.onShriekerTick(self);
        if (self.cooldownTicks() > 0) self.setCooldownTicks(self.cooldownTicks() - 1);
    }

    // CalibratedShriekerContext：方块实体本身就是回调上下文，省掉每次回调的临时对象

    @Override
    public ServerLevel level() {
        return (ServerLevel) this.level;
    }

    @Override
    public BlockPos pos() {
        return this.worldPosition;
    }

    @Override
    public BlockState blockState() {
        return this.getBlockState();
    }

    @Override
    public ItemStack item() {
        return this.getTheItem();
    }

    @Override
    public void setItem(ItemStack stack) {
        this.setTheItem(stack);
    }

    @Override
    public ItemStack oppositeItem() {
        CalibratedSculkShriekerBlockEntity opposite = this.shriekerOpposite();
        return opposite == null ? ItemStack.EMPTY : opposite.getTheItem();
    }

    @Override
    public void setOppositeItem(ItemStack stack) {
        CalibratedSculkShriekerBlockEntity opposite = this.shriekerOpposite();
        if (opposite != null) opposite.setTheItem(stack);
    }

    /**
     * 正对面那个校频幽匿尖啸体：在本方块 {@code FACING} 指向的那一格，且它的朝向与本方块相反；
     * 取不到返回 {@code null}。
     *
     * <p>两块必须面对面：本方块朝着对方，对方也朝着本方块。朝上摆时对方就在正上方那一格，
     * 朝下或朝四个水平方向时取各自方向上的那一格。
     */
    @Nullable
    private CalibratedSculkShriekerBlockEntity shriekerOpposite() {
        if (this.level == null) return null;
        BlockState state = this.getBlockState();
        Direction facing = state.getValue(CalibratedSculkShriekerBlock.FACING);
        BlockPos opposite = this.worldPosition.relative(facing);
        BlockState oppositeState = this.level.getBlockState(opposite);
        if (!(oppositeState.getBlock() instanceof CalibratedSculkShriekerBlock)) return null;
        if (oppositeState.getValue(CalibratedSculkShriekerBlock.FACING) != facing.getOpposite()) return null;
        return this.level.getBlockEntity(opposite) instanceof CalibratedSculkShriekerBlockEntity shrieker
                ? shrieker : null;
    }

    @Override
    public boolean powered() {
        return this.signalStrength() > 0;
    }

    @Override
    public int signalStrength() {
        return this.level == null ? 0 : this.level.getBestNeighborSignal(this.worldPosition);
    }

    @Override
    public int cooldownTicks() {
        if (this.cooldownTicks < 0) {
            CalibratedShriekerMechanism mechanism = CalibratedShriekerMechanisms.get(this.getTheItem());
            // 还没有遇到带机制的物品时先按 0 返回，等真的放进来了再按机制的初始值落定
            if (mechanism == null) return 0;
            this.cooldownTicks = mechanism.initialCooldownTicks();
        }
        return this.cooldownTicks;
    }

    @Override
    public void setCooldownTicks(int ticks) {
        this.cooldownTicks = ticks;
    }

    @Override
    public void playEffect(ResourceLocation effect, int durationTicks) {
        this.effectId = durationTicks > 0 ? effect : null;
        this.effectDurationTicks = Math.max(0, durationTicks);
        this.effectStartTick = this.level == null ? 0L : this.level.getGameTime();
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public Vec3 effectPos() {
        Vec3 center = this.worldPosition.getCenter();
        return switch (this.getBlockState().getValue(CalibratedSculkShriekerBlock.FACING)) {
            case NORTH -> center.add(0, -0.4, -0.4);
            case SOUTH -> center.add(0, -0.4, +0.4);
            case EAST -> center.add(+0.4, -0.4, 0);
            case WEST -> center.add(-0.4, -0.4, 0);
            case DOWN -> center.add(0, -0.8, 0);
            default -> center;
        };
    }

    /** 客户端渲染用：当前效果 id；没有效果时为 {@code null}。 */
    @Nullable
    public ResourceLocation effectId() {
        return this.effectId;
    }

    /** 客户端渲染用：效果开始时那一刻的游戏时间。 */
    public long effectStartTick() {
        return this.effectStartTick;
    }

    /** 客户端渲染用：效果持续刻数；没有效果时为 0。 */
    public int effectDurationTicks() {
        return this.effectDurationTicks;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Activation", 0, state -> {
            if (!itemHandler.getStackInSlot(0).isEmpty())
                return state.setAndContinue(itemRenderAnimation);

            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("ItemHandler"));
        this.effectId = tag.contains("Effect") ? ResourceLocation.tryParse(tag.getString("Effect")) : null;
        this.effectStartTick = tag.getLong("EffectStart");
        this.effectDurationTicks = tag.getInt("EffectDuration");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ItemHandler", itemHandler.serializeNBT(registries));
        if (this.effectId != null) {
            tag.putString("Effect", this.effectId.toString());
            tag.putLong("EffectStart", this.effectStartTick);
            tag.putInt("EffectDuration", this.effectDurationTicks);
        }
    }

    @Override
    public @NotNull BlockEntity getContainerBlockEntity() {
        return this;
    }

    @Override
    public @NotNull ItemStack getTheItem() {
        return itemHandler.getStackInSlot(0);
    }

    @Override
    public void setTheItem(@NotNull ItemStack item) {
        itemHandler.setStackInSlot(0, item);
    }
}
