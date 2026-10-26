package com.unddefined.sculkbaselib.api.block;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 「物品 → 内含物品机制」的注册表，用来给任意物品挂上校频幽匿尖啸体的红石触发机制。
 *
 * <p>三种接法，越靠前越具体、越优先：
 *
 * <ol>
 *   <li>{@link #register(Predicate, CalibratedShriekerMechanism)} —— 按物品堆叠条件挂机制，
 *       用来区分同一个物品的不同组件 / NBT；条目之间后注册的优先。</li>
 *   <li>{@link #register(Item, CalibratedShriekerMechanism)} —— 按物品挂机制，该物品的任意堆叠都命中。</li>
 *   <li>物品类直接实现 {@link CalibratedShriekerMechanism} —— 适合自家物品，
 *       内置的幽鸣晶簇就走这条。</li>
 * </ol>
 *
 * <p>注册越早越好，至少要赶在对应世界运行起来之前（{@code @Mod} 构造函数或
 * {@code FMLCommonSetupEvent}）。条件注册每次都要求值，方块每刻都会查一次，
 * 条件请保持廉价、只读，不要修改传进来的堆叠。
 */
public final class CalibratedShriekerMechanisms {

    private static final Map<Item, CalibratedShriekerMechanism> BY_ITEM = new ConcurrentHashMap<>();
    private static final List<FilteredMechanism> BY_FILTER = new CopyOnWriteArrayList<>();

    private CalibratedShriekerMechanisms() {
    }

    /**
     * 按物品挂上校频幽匿尖啸体的内含物品机制，该物品的任意堆叠都命中。
     *
     * <p>{@link CalibratedShriekerMechanism} 是函数式接口，也可以直接传 lambda。
     *
     * @param item      物品，例如 {@code Items.ECHO_SHARD}；重复注册同一个物品时以最后一次为准
     * @param mechanism 红石信号变化时调用的机制
     */
    public static void register(Item item, CalibratedShriekerMechanism mechanism) {
        BY_ITEM.put(Objects.requireNonNull(item, "item"), Objects.requireNonNull(mechanism, "mechanism"));
    }

    /**
     * 按物品堆叠条件挂上校频幽匿尖啸体的内含物品机制，用来区分同一个物品的不同组件 / NBT。
     *
     * <p>条件命中的条目优先于物品注册与物品类自身的实现；条目之间后注册的优先。
     * 查询发生在方块每次红石触发与每刻的冷却检查里，条件请写轻量、只读的判定
     * （例如 {@code stack -> stack.get(YourComponents.CHARGED) != null}）。
     *
     * @param filter    堆叠条件，不要修改传进来的堆叠
     * @param mechanism 条件命中时红石信号变化时调用的机制
     */
    public static void register(Predicate<ItemStack> filter, CalibratedShriekerMechanism mechanism) {
        BY_FILTER.add(new FilteredMechanism(Objects.requireNonNull(filter, "filter"),
                Objects.requireNonNull(mechanism, "mechanism")));
    }

    /**
     * 查询物品堆叠挂着的机制，按「条件注册 → 物品注册 → 物品类自身实现」的顺序取第一个命中的。
     *
     * @return 红石触发时该调用的机制；{@code null} 表示这个物品没有机制
     */
    @Nullable
    public static CalibratedShriekerMechanism get(ItemStack stack) {
        if (stack.isEmpty()) return null;
        // 后注册的条件优先，倒着找
        for (int i = BY_FILTER.size() - 1; i >= 0; i--) {
            FilteredMechanism entry = BY_FILTER.get(i);
            if (entry.filter().test(stack)) return entry.mechanism();
        }
        CalibratedShriekerMechanism registered = BY_ITEM.get(stack.getItem());
        if (registered != null) return registered;
        return stack.getItem() instanceof CalibratedShriekerMechanism mechanism ? mechanism : null;
    }

    /** 条件注册的一条记录，后注册的排在后面、查询时优先。 */
    private record FilteredMechanism(Predicate<ItemStack> filter, CalibratedShriekerMechanism mechanism) {
    }
}
