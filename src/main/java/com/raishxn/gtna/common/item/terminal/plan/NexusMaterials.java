package com.raishxn.gtna.common.item.terminal.plan;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import appeng.api.stacks.AEItemKey;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Materials of one build (D-439 P0.2, P0.6): availability is read once per distinct item (one inventory pass,
 * one ME simulation per item), every cell is assigned an item, and the whole need is taken in one batch per item
 * (ME first, then the inventory). What the build does not use goes back at the end.
 * <p>
 * The batch reservation follows gt_shanhai's {@code prepareBuildBatch} (LGPL-3.0), rewritten for GTNA.
 */
public final class NexusMaterials {

    private final Player player;
    private final boolean creative;
    @Nullable
    private final NexusAE2Link.Session me;
    @Nullable
    private final IItemHandler inventory;
    /** Per item: how many the inventory holds, and how many the ME holds (read once). */
    private final Map<AEItemKey, long[]> stock = new HashMap<>();
    /** Taken and not yet placed. */
    private final Object2LongLinkedOpenHashMap<AEItemKey> pool = new Object2LongLinkedOpenHashMap<>();
    /** Taken from the ME, so leftovers of that item go back there first. */
    private final Set<AEItemKey> fromMe = new HashSet<>();

    public NexusMaterials(Player player, @Nullable NexusAE2Link.Session me) {
        this.player = player;
        this.creative = player.isCreative();
        this.me = me;
        this.inventory = player.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
    }

    @Nullable
    public NexusAE2Link.Session me() {
        return me;
    }

    /** Counts what the inventory and the ME have of every candidate in the plans. */
    public void survey(List<NexusBuildPlan> plans) {
        Set<AEItemKey> wanted = new LinkedHashSet<>();
        for (NexusBuildPlan plan : plans) {
            for (NexusBuildPlan.Cell cell : plan.cells) {
                if (!cell.needsItem()) continue;
                for (ItemStack stack : cell.candidates) {
                    if (stack.getItem() instanceof BlockItem) {
                        AEItemKey key = AEItemKey.of(stack);
                        if (key != null) wanted.add(key);
                    }
                }
            }
        }
        for (AEItemKey key : wanted) stock.put(key, new long[2]);
        if (creative) return;
        if (inventory != null) countInventory(inventory, 0);
        if (me != null) for (AEItemKey key : wanted) stock.get(key)[1] = me.available(key);
    }

    private void countInventory(IItemHandler handler, int depth) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            long[] counts = stock.get(AEItemKey.of(stack));
            if (counts != null) counts[0] += stack.getCount();
            IItemHandler inner = depth < 2 ? stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null) : null;
            if (inner != null) countInventory(inner, depth + 1);
        }
    }

    /** Inventory and ME count of {@code key}; creative counts as unlimited. */
    public long[] stockOf(AEItemKey key) {
        long[] counts = stock.get(key);
        return counts == null ? new long[2] : counts;
    }

    public boolean creative() {
        return creative;
    }

    /**
     * Gives every cell that needs an item the first candidate still available, and returns the total need per item
     * (also for cells left without one, by their first candidate: the materials list shows those as missing).
     */
    public Map<AEItemKey, Long> assign(List<NexusBuildPlan> plans) {
        Map<AEItemKey, Long> budget = new HashMap<>();
        stock.forEach((key, counts) -> budget.put(key, creative ? Long.MAX_VALUE : counts[0] + counts[1]));
        Map<AEItemKey, Long> need = new LinkedHashMap<>();
        for (NexusBuildPlan plan : plans) {
            for (NexusBuildPlan.Cell cell : plan.cells) {
                if (!cell.needsItem()) continue;
                cell.chosen = null;
                AEItemKey first = null;
                for (ItemStack stack : cell.candidates) {
                    if (!(stack.getItem() instanceof BlockItem)) continue;
                    AEItemKey key = AEItemKey.of(stack);
                    if (key == null) continue;
                    if (first == null) first = key;
                    long left = budget.getOrDefault(key, 0L);
                    if (left > 0) {
                        budget.put(key, left - 1);
                        cell.chosen = key;
                        break;
                    }
                }
                AEItemKey counted = cell.chosen != null ? cell.chosen : first;
                if (counted != null) need.merge(counted, 1L, Long::sum);
            }
        }
        return need;
    }

    /** Cells left without an item after {@link #assign}, counted by their first candidate. */
    public static Map<AEItemKey, Long> missing(List<NexusBuildPlan> plans) {
        Map<AEItemKey, Long> missing = new LinkedHashMap<>();
        for (NexusBuildPlan plan : plans) {
            for (NexusBuildPlan.Cell cell : plan.cells) {
                if (!cell.needsItem() || cell.chosen != null) continue;
                for (ItemStack stack : cell.candidates) {
                    AEItemKey key = stack.getItem() instanceof BlockItem ? AEItemKey.of(stack) : null;
                    if (key != null) {
                        missing.merge(key, 1L, Long::sum);
                        break;
                    }
                }
            }
        }
        return missing;
    }

    /** Takes everything the assigned cells need: one ME extraction and one inventory pass per item. */
    public void reserve(List<NexusBuildPlan> plans) {
        Object2LongLinkedOpenHashMap<AEItemKey> need = new Object2LongLinkedOpenHashMap<>();
        for (NexusBuildPlan plan : plans) {
            for (NexusBuildPlan.Cell cell : plan.cells) if (cell.chosen != null) need.addTo(cell.chosen, 1);
        }
        if (creative) {
            pool.putAll(need);
            return;
        }
        Object2LongLinkedOpenHashMap<AEItemKey> fromInventory = new Object2LongLinkedOpenHashMap<>();
        for (Object2LongMap.Entry<AEItemKey> entry : need.object2LongEntrySet()) {
            AEItemKey key = entry.getKey();
            long wanted = entry.getLongValue(), got = 0;
            if (me != null) {
                got = me.extract(key, Math.min(wanted, stockOf(key)[1]));
                if (got > 0) fromMe.add(key);
            }
            if (got < wanted) fromInventory.put(key, wanted - got);
            pool.addTo(key, got);
        }
        if (inventory != null && !fromInventory.isEmpty()) takeInventory(inventory, fromInventory, 0);
    }

    private void takeInventory(IItemHandler handler, Object2LongLinkedOpenHashMap<AEItemKey> wanted, int depth) {
        for (int slot = 0; slot < handler.getSlots() && !wanted.isEmpty(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            AEItemKey key = AEItemKey.of(stack);
            long left = wanted.getLong(key);
            if (left > 0) {
                ItemStack got = handler.extractItem(slot, (int) Math.min(left, stack.getCount()), false);
                pool.addTo(key, got.getCount());
                if (left - got.getCount() <= 0) wanted.removeLong(key);
                else wanted.put(key, left - got.getCount());
            }
            IItemHandler inner = depth < 2 ? stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null) : null;
            if (inner != null) takeInventory(inner, wanted, depth + 1);
        }
    }

    /** One item of {@code key} from the reservation, or false if none is left. */
    public boolean take(AEItemKey key) {
        long left = pool.getLong(key);
        if (left <= 0) return false;
        pool.put(key, left - 1);
        return true;
    }

    public void putBack(AEItemKey key) {
        pool.addTo(key, 1);
    }

    /** Returns everything still reserved: to the ME when it came from there, otherwise to the inventory. */
    public void returnLeftovers() {
        if (!creative) {
            for (Object2LongMap.Entry<AEItemKey> entry : pool.object2LongEntrySet()) {
                give(entry.getKey(), entry.getLongValue(), fromMe.contains(entry.getKey()));
            }
        }
        pool.clear();
    }

    /** Whether {@code key} can be stored somewhere (ME first, then the inventory); creative always can. */
    public boolean canStore(AEItemKey key, long amount) {
        if (creative) return true;
        if (me != null && me.insert(key, amount, true) >= amount) return true;
        ItemStack probe = key.toStack((int) Math.min(amount, key.getMaxStackSize()));
        var inv = player.getInventory();
        return inv.getSlotWithRemainingSpace(probe) >= 0 || inv.getFreeSlot() >= 0;
    }

    /** Stores {@code amount} of {@code key}: ME first, then the inventory, the rest at the player's feet. */
    public void store(AEItemKey key, long amount) {
        if (!creative) give(key, amount, true);
    }

    private void give(AEItemKey key, long amount, boolean meFirst) {
        if (amount <= 0) return;
        if (meFirst && me != null) amount -= me.insert(key, amount, false);
        while (amount > 0) {
            int part = (int) Math.min(amount, key.getMaxStackSize());
            ItemStack stack = key.toStack(part);
            amount -= part;
            if (!player.getInventory().add(stack)) {
                if (me != null && !meFirst) {
                    long in = me.insert(key, stack.getCount(), false);
                    stack.shrink((int) in);
                }
                if (!stack.isEmpty()) player.drop(stack, false);
            }
        }
    }
}
