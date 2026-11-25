package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The hatch plan of the Nexus Terminal (GTIA-D-439 T7): for each function (input bus, energy hatch...) the player
 * picks one hatch (type and tier), "none", or leaves it automatic. The planner applies it after the pattern's hatch
 * limits have chosen what a cell may hold, so the limits are always kept; when a plan would leave a cell with no
 * candidate at all (a hatch the structure requires), the cell keeps its own candidates.
 * <p>
 * Functions are told apart by the machine ID, as gt_shanhai's chamber classifier does (LGPL-3.0).
 */
public final class NexusHatchPlan {

    public enum Function {

        INPUT_BUS("input_bus", "import_bus"),
        OUTPUT_BUS("output_bus", "export_bus"),
        INPUT_HATCH("input_hatch", "import_hatch"),
        OUTPUT_HATCH("output_hatch", "export_hatch"),
        ENERGY("energy_input", "laser_input", "substation_input", "wireless_energy_input"),
        DYNAMO("energy_output", "dynamo", "laser_output", "substation_output"),
        MAINTENANCE("maintenance"),
        MUFFLER("muffler"),
        PARALLEL("parallel");

        final String[] markers;

        Function(String... markers) {
            this.markers = markers;
        }

        public String key() {
            return "gtna.terminal.nexus.hatch." + name().toLowerCase(Locale.ROOT);
        }
    }

    private static final String NBT = "NexusHatchPlan";
    /** Stored in place of an item ID: the function gets no hatch where the pattern allows it. */
    public static final String NONE = "none";

    private static Map<Function, List<ItemStack>> entries;

    private final Map<Function, String> picks = new EnumMap<>(Function.class);

    private NexusHatchPlan() {}

    public static NexusHatchPlan read(ItemStack terminal) {
        NexusHatchPlan plan = new NexusHatchPlan();
        CompoundTag tag = terminal.getTag();
        if (tag == null || !tag.contains(NBT, 10)) return plan;
        CompoundTag picks = tag.getCompound(NBT);
        for (Function function : Function.values()) {
            if (picks.contains(function.name())) plan.picks.put(function, picks.getString(function.name()));
        }
        return plan;
    }

    /** Sets the pick of a function: an item ID, {@link #NONE}, or null for automatic. */
    public static void set(ItemStack terminal, Function function, @Nullable String pick) {
        CompoundTag picks = terminal.getOrCreateTagElement(NBT);
        if (pick == null) picks.remove(function.name());
        else picks.putString(function.name(), pick);
    }

    @Nullable
    public String pick(Function function) {
        return picks.get(function);
    }

    public boolean isEmpty() {
        return picks.isEmpty();
    }

    @Nullable
    public static Function classify(Item item) {
        if (!(item instanceof BlockItem block) || !(block.getBlock() instanceof MetaMachineBlock)) return null;
        String id = BuiltInRegistries.ITEM.getKey(item).getPath();
        // Energy and dynamo first: their IDs also contain "input_hatch" / "output_hatch".
        for (Function function : List.of(Function.ENERGY, Function.DYNAMO, Function.INPUT_BUS, Function.OUTPUT_BUS,
                Function.INPUT_HATCH, Function.OUTPUT_HATCH, Function.MAINTENANCE, Function.MUFFLER,
                Function.PARALLEL)) {
            for (String marker : function.markers) if (id.contains(marker)) return function;
        }
        return null;
    }

    /** Every registered hatch of a function, sorted by ID (so tiers come in order within a mod). */
    public static List<ItemStack> entries(Function function) {
        if (entries == null) {
            Map<Function, List<ItemStack>> all = new EnumMap<>(Function.class);
            for (Function f : Function.values()) all.put(f, new ArrayList<>());
            for (Item item : BuiltInRegistries.ITEM) {
                Function f = classify(item);
                if (f != null) all.get(f).add(new ItemStack(item));
            }
            all.values().forEach(list -> list.sort(Comparator.comparing(
                    s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())));
            entries = all;
        }
        return entries.get(function);
    }

    /** {@code candidates} after the plan; never empty when {@code candidates} was not. */
    public List<ItemStack> apply(List<ItemStack> candidates) {
        if (picks.isEmpty() || candidates.isEmpty()) return candidates;
        List<ItemStack> out = new ArrayList<>(candidates.size());
        Set<Function> pickedPresent = EnumSet.noneOf(Function.class);
        for (ItemStack stack : candidates) {
            Function f = classify(stack.getItem());
            String pick = f == null ? null : picks.get(f);
            if (pick != null && !pick.equals(NONE) &&
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.tryParse(pick))) {
                pickedPresent.add(f);
            }
        }
        for (ItemStack stack : candidates) {
            Function f = classify(stack.getItem());
            String pick = f == null ? null : picks.get(f);
            if (pick == null) out.add(stack);
            else if (pick.equals(NONE)) continue;
            else if (!pickedPresent.contains(f) ||
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.tryParse(pick))) {
                        out.add(stack);
                    }
        }
        return out.isEmpty() ? candidates : List.copyOf(out);
    }
}
