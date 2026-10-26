package com.unddefined.sculkbaselib.blocks;

import com.unddefined.sculkbaselib.api.block.CalibratedShriekerItemChange;
import com.unddefined.sculkbaselib.api.block.CalibratedShriekerMechanism;
import com.unddefined.sculkbaselib.api.block.CalibratedShriekerMechanisms;
import com.unddefined.sculkbaselib.blocks.entity.CalibratedSculkShriekerBlockEntity;
import com.unddefined.sculkbaselib.server.registry.BlockEntityRegistry;
import com.unddefined.sculkbaselib.server.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CalibratedSculkShriekerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());
    public static final BooleanProperty RCHARGED = BooleanProperty.create("redstone_charged");

    public CalibratedSculkShriekerBlock() {
        super(Properties.of()
                .noOcclusion()
                .sound(SoundType.SCULK_SHRIEKER)
                .explosionResistance(1.0F)
                .destroyTime(1.5F)
                .pushReaction(PushReaction.DESTROY)
                .dynamicShape()
                .lightLevel(state -> 3)
        );
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.UP).setValue(RCHARGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RCHARGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D);
            case SOUTH -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D);
            case EAST -> Block.box(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 16.0D);
            case WEST -> Block.box(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            case UP -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
            case DOWN -> Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        };
    }

    /**
     * 踩踏交给内含物品的机制处理，例如幽匿精华会在这里献祭并召唤监守者。
     */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!(level instanceof ServerLevel)) return;
        if (!(level.getBlockEntity(pos) instanceof CalibratedSculkShriekerBlockEntity shrieker)) return;
        CalibratedShriekerMechanism mechanism = CalibratedShriekerMechanisms.get(shrieker.getTheItem());
        if (mechanism == null) return;
        mechanism.onSteppedOn(shrieker, entity);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CalibratedSculkShriekerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!(level instanceof ServerLevel)) return;
        boolean charged = state.getValue(RCHARGED);
        boolean powered = level.hasNeighborSignal(pos);
        if (charged == powered) return;
        if (!(level.getBlockEntity(pos) instanceof CalibratedSculkShriekerBlockEntity blockEntity)) return;
        CalibratedShriekerMechanism mechanism = CalibratedShriekerMechanisms.get(blockEntity.getTheItem());
        if (mechanism == null) return;
        // 方块自己的红石状态照旧维护，机制交给内含物品：接通与断开各回调一次
        if (charged) level.scheduleTick(pos, this, 4);
        else level.setBlock(pos, state.cycle(RCHARGED), 2);
        mechanism.onRedstoneSignal(blockEntity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(RCHARGED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(RCHARGED), 2);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel)) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof CalibratedSculkShriekerBlockEntity shrieker)) {
            return ItemInteractionResult.SUCCESS;
        }

        ItemStack slotStack = shrieker.getTheItem();
        if (stack.isEmpty() && !slotStack.isEmpty()) {
            // 回调在物品离开槽位之前，机制读到的是变更当刻的堆叠与冷却
            notifyItemChanged(shrieker, slotStack, CalibratedShriekerItemChange.EXTRACTED);
            player.setItemInHand(hand, slotStack);
            shrieker.setTheItem(ItemStack.EMPTY);
        } else if (!stack.isEmpty() && slotStack.isEmpty()) {
            shrieker.setTheItem(stack.copy());
            stack.shrink(stack.getCount());
            notifyItemChanged(shrieker, shrieker.getTheItem(), CalibratedShriekerItemChange.INSERTED);
        } else if (ItemStack.isSameItemSameComponents(stack, slotStack)) {
            int max = stack.getItem().getDefaultMaxStackSize();
            int total = slotStack.getCount() + stack.getCount();
            slotStack.setCount(Math.min(max, total));
            stack.setCount(Math.max(0, total - max));
            notifyItemChanged(shrieker, slotStack, CalibratedShriekerItemChange.INSERTED);
        } else {
            notifyItemChanged(shrieker, slotStack, CalibratedShriekerItemChange.EXTRACTED);
            player.setItemInHand(hand, slotStack);
            shrieker.setTheItem(stack);
            notifyItemChanged(shrieker, stack, CalibratedShriekerItemChange.INSERTED);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) return;
        if (level.getBlockEntity(pos) instanceof CalibratedSculkShriekerBlockEntity shrieker) {
            ItemStack stack = shrieker.getTheItem();
            if (!stack.isEmpty()) {
                CalibratedShriekerMechanism mechanism = CalibratedShriekerMechanisms.get(stack);
                // 机制回调只在服务端派发，掉落这条两端都照常走
                if (mechanism != null && level instanceof ServerLevel) {
                    mechanism.onItemChanged(shrieker, CalibratedShriekerItemChange.BROKEN);
                }
                if (mechanism == null || !mechanism.shattersOnBreak()) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** 把内含物品的变更交给物品自己的机制；调用时槽位还是变更当刻的状态。 */
    private static void notifyItemChanged(CalibratedSculkShriekerBlockEntity shrieker, ItemStack stack,
                                          CalibratedShriekerItemChange change) {
        CalibratedShriekerMechanism mechanism = CalibratedShriekerMechanisms.get(stack);
        if (mechanism != null) mechanism.onItemChanged(shrieker, change);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(ItemRegistry.CALIBRATED_SCULK_SHRIEKER_ITEM.get()));
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> typeA, BlockEntityType<E> typeB, BlockEntityTicker<? super E> ticker) {
        return typeA == typeB ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, BlockEntityRegistry.CALIBRATED_SCULK_SHRIEKER.get(),
                CalibratedSculkShriekerBlockEntity::tick);
    }
}
