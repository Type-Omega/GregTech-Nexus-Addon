package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.item.terminal.NexusBlockPattern;
import com.raishxn.gtna.common.item.terminal.NexusBuildCheckGuard;
import com.raishxn.gtna.common.item.terminal.ui.BlockSelectionConfigWidget;
import com.raishxn.gtna.common.item.terminal.ui.BlockSelectionConfigWidget.BlockCategory;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory.AutoBuildSetting;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Tier swap by family (GTIA-D-440): the tiered families a built structure holds (coils, casings, capacitors...),
 * each with the tier found in the world, and a one-step swap of a whole family to another tier. The swap is a
 * replace-mode build with that family's pick, so it keeps the safe swap rules (old block back to the ME or the
 * inventory, nothing replaced without room).
 * <p>
 * The idea of upgrading a structure family by family comes from the Multi-Structure Manager of GregTech Terminals
 * (BgameXL, MIT), rewritten for GTNA; see THIRD_PARTY_NOTICES.md.
 */
public final class NexusFamilySwap {

    /** A family present in the structure: its category, the item most cells hold, and how many cells hold it. */
    public record Family(BlockCategory category, ItemStack current, int cells) {}

    private NexusFamilySwap() {}

    /** The families of the structure, read from the blocks of the cells that are already right. */
    public static List<Family> families(NexusScan.Result result) {
        Map<Item, Integer> held = new HashMap<>();
        for (NexusBuildPlan plan : result.plans()) {
            for (NexusBuildPlan.Cell cell : plan.cells) {
                if (cell.kind == NexusBuildPlan.Kind.SATISFIED && !cell.existing.isAir()) {
                    held.merge(cell.existing.getBlock().asItem(), 1, Integer::sum);
                }
            }
        }
        List<Family> out = new ArrayList<>();
        for (BlockCategory category : BlockCategory.values()) {
            if (category == BlockCategory.GLASS || category == BlockCategory.LIGHT) continue;
            ItemStack best = null;
            int count = 0;
            for (ItemStack entry : BlockSelectionConfigWidget.getEntries(category)) {
                int n = held.getOrDefault(entry.getItem(), 0);
                if (n > count) {
                    count = n;
                    best = entry;
                }
            }
            if (best != null) out.add(new Family(category, best.copy(), count));
        }
        return out;
    }

    /**
     * Swaps the family to {@code target}: picks it for the category and runs one replace-mode build of the
     * structure (the terminal's own replace setting is restored afterwards). Returns false if the target is not a
     * tier of that family or the structure cannot be read.
     */
    public static boolean swap(Player player, IMultiController controller, ItemStack terminal, BlockCategory category,
                               ItemStack target) {
        return swap(player, controller, NexusBlockPattern.fromBlockPattern(controller.getPattern()), terminal,
                category, target);
    }

    public static boolean swap(Player player, IMultiController controller, @Nullable NexusBlockPattern pattern,
                               ItemStack terminal, BlockCategory category, ItemStack target) {
        if (BlockSelectionConfigWidget.getEntries(category).stream()
                .noneMatch(stack -> ItemStack.isSameItem(stack, target))) {
            return false;
        }
        if (pattern == null) return false;
        @Nullable
        ItemStack before = BlockSelectionConfigWidget.getSelectedBlock(terminal, category);
        if (before == null || !ItemStack.isSameItem(before, target)) {
            BlockSelectionConfigWidget.select(terminal, category, target);
        }
        var tag = terminal.getOrCreateTag();
        boolean replace = tag.getBoolean("ReplaceMode");
        tag.putBoolean("ReplaceMode", true);
        boolean[] ok = new boolean[1];
        try {
            var state = controller.getMultiblockState();
            NexusBuildCheckGuard.run(state,
                    () -> ok[0] = pattern.autoBuild(player, state, AutoBuildSetting.getSetting(terminal), terminal));
        } finally {
            tag.putBoolean("ReplaceMode", replace);
        }
        return ok[0];
    }
}
