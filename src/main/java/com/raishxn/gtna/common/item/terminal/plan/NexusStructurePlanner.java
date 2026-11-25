package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.raishxn.gtna.common.item.terminal.NexusBlockPattern;
import com.raishxn.gtna.common.item.terminal.ui.BlockSelectionConfigWidget;
import com.raishxn.gtna.common.item.terminal.ui.BlockSelectionConfigWidget.BlockCategory;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory.AutoBuildSetting;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;

/**
 * Walks a pattern once and classifies every cell (D-439 P0.1, P0.3). Hatch limits are honoured in GTCEu's order
 * ({@link #pickCandidates}), so the plan is the structure GTCEu checks. The terminal's block picks are read once per
 * scan, and the candidate list of a cell is cached by the limits it was resolved under, so the thousands of casing
 * cells of a big structure share one list.
 * <p>
 * The classification (place, replace, blocked, clear) follows the scan of gt_shanhai's Ultimate Terminal
 * (LGPL-3.0), rewritten for GTNA; see THIRD_PARTY_NOTICES.md.
 */
public final class NexusStructurePlanner {

    private final Level level;
    private final MultiblockState state;
    private final AutoBuildSetting setting;
    private final BlockPos origin;
    private final Direction front, up;
    private final boolean mirrored;
    /** The terminal's picks, read from NBT once: selected item per category. */
    private final List<Item> selections = new ArrayList<>();
    private final Map<TraceabilityPredicate, Map<List<SimplePredicate>, List<ItemStack>>> candidateCache = new IdentityHashMap<>();
    private final Map<SimplePredicate, BlockInfo[]> optionCache = new IdentityHashMap<>();
    private final Map<SimplePredicate, Boolean> allowedCache = new IdentityHashMap<>();
    private final Map<Item, Boolean> partItems = new IdentityHashMap<>();
    private final NexusHatchPlan hatchPlan;

    /**
     * Block per world position that a paste of a saved structure asks for (D-440); a cell takes it when the
     * pattern allows that block there. Set only around one build, on the server thread.
     */
    public static final ThreadLocal<Map<BlockPos, Item>> OVERRIDES = new ThreadLocal<>();

    public NexusStructurePlanner(MultiblockState state, AutoBuildSetting setting, ItemStack terminal) {
        IMultiController controller = state.getController();
        this.level = controller.self().getLevel();
        this.state = state;
        this.setting = setting;
        this.origin = controller.self().getPos();
        this.front = controller.self().getFrontFacing();
        this.up = controller.self().getUpwardsFacing();
        this.mirrored = setting.isMirrorBuild() || controller.self().isFlipped();
        this.hatchPlan = NexusHatchPlan.read(terminal);
        for (BlockCategory category : BlockCategory.values()) {
            if (category == BlockCategory.GLASS || category == BlockCategory.LIGHT) continue;
            ItemStack picked = BlockSelectionConfigWidget.getSelectedBlock(terminal, category);
            if (picked != null) selections.add(picked.getItem());
        }
    }

    public NexusBuildPlan scan(NexusBlockPattern pattern) {
        NexusBuildPlan plan = new NexusBuildPlan(origin);
        state.clean();
        Object2IntOpenHashMap<SimplePredicate> perLayer = state.getLayerCount();
        TraceabilityPredicate[][][] cells = pattern.cells();
        int[] center = pattern.centerOffset();
        int[] repeat = pattern.repetitions(setting);
        // EN: start behind the controller by the aisles actually laid before it; GTCEu's -maxZ only fits
        // EN: patterns whose repeated aisles come after the controller (ME Storage repeats before it).
        // PT: começa atrás do controlador pelas fileiras realmente postas antes dele; o -maxZ do GTCEu só serve
        // PT: quando as fileiras repetidas vêm depois do controlador (o ME Storage repete antes).
        // ZH: 以控制器之前实际铺设的层数作为起点；GTCEu 的 -maxZ 只适用于重复层位于控制器之后的结构
        // ZH: （ME 存储的重复层在控制器之前）。
        int z = 0;
        for (int aisle = 0; aisle < center[2]; aisle++) z -= repeat[aisle];
        for (int aisle = 0; aisle < cells.length; aisle++) {
            for (int copy = 0; copy < repeat[aisle]; copy++, z++) {
                perLayer.clear();
                for (int row = 0; row < cells[aisle].length; row++) {
                    for (int col = 0; col < cells[aisle][row].length; col++) {
                        TraceabilityPredicate cell = cells[aisle][row][col];
                        if (cell.common.contains(SimplePredicate.ANY)) continue; // "don't care" space
                        BlockPos pos = origin.offset(pattern.toWorldOffset(col - center[0], row - center[1], z,
                                front, up, mirrored));
                        state.update(pos, cell);
                        classify(plan, pos, cell);
                    }
                }
            }
        }
        return plan;
    }

    private void classify(NexusBuildPlan plan, BlockPos pos, TraceabilityPredicate cell) {
        BlockState existing = level.getBlockState(pos);
        NexusBuildPlan.Kind kind;
        List<ItemStack> candidates = List.of();
        if (pos.equals(origin)) {
            kind = NexusBuildPlan.Kind.CONTROLLER;
        } else if (setting.isDemolitionMode() && mustBeAir(cell)) {
            kind = existing.isAir() ? NexusBuildPlan.Kind.SATISFIED :
                    existing.getDestroySpeed(level, pos) < 0 ? NexusBuildPlan.Kind.BLOCKED :
                            NexusBuildPlan.Kind.CLEAR;
        } else if (existing.isAir()) {
            candidates = candidates(cell);
            kind = NexusBuildPlan.Kind.PLACE;
        } else if (!setting.isReplaceMode() || isPartOrController(pos)) {
            // Counts the block already there against the hatch limits, as GTCEu's check does.
            cell.limited.forEach(limit -> limit.testLimited(state));
            kind = offers(cell, existing) ? NexusBuildPlan.Kind.SATISFIED : NexusBuildPlan.Kind.BLOCKED;
        } else {
            candidates = candidates(cell);
            Item there = existing.getBlock().asItem();
            kind = candidates.stream().anyMatch(stack -> stack.is(there)) ? NexusBuildPlan.Kind.SATISFIED :
                    NexusBuildPlan.Kind.REPLACE;
        }
        Map<BlockPos, Item> overrides = OVERRIDES.get();
        if (overrides != null && !candidates.isEmpty()) {
            Item wanted = overrides.get(pos);
            if (wanted != null && candidates.stream().anyMatch(stack -> stack.is(wanted))) {
                candidates = List.of(new ItemStack(wanted));
                if (kind == NexusBuildPlan.Kind.SATISFIED && !existing.is(((net.minecraft.world.item.BlockItem) wanted)
                        .getBlock())) {
                    kind = NexusBuildPlan.Kind.REPLACE;
                }
            }
        }
        boolean hatch = !candidates.isEmpty() && candidates.stream().allMatch(this::isPart);
        plan.cells.add(new NexusBuildPlan.Cell(pos, kind, existing, candidates, hatch));
        plan.occupied.put(pos, switch (kind) {
            case PLACE, REPLACE -> !candidates.isEmpty();
            case CLEAR -> false;
            default -> !existing.isAir();
        });
    }

    /**
     * Whether any predicate of the cell offers the block already there (by block, as the terminal places). A cell
     * whose predicates list no candidates cannot be judged and counts as fitting.
     */
    private boolean offers(TraceabilityPredicate cell, BlockState existing) {
        boolean listed = false;
        for (var group : List.of(cell.limited, cell.common)) {
            for (SimplePredicate p : group) {
                listed |= options(p).length > 0;
                if (offers(p, existing)) return true;
            }
        }
        return !listed;
    }

    private boolean offers(SimplePredicate predicate, BlockState existing) {
        for (BlockInfo info : options(predicate)) {
            if (info.getBlockState().getBlock() == existing.getBlock()) return true;
        }
        return false;
    }

    private static boolean mustBeAir(TraceabilityPredicate cell) {
        return cell.limited.isEmpty() && cell.common.size() == 1 && cell.common.contains(SimplePredicate.AIR);
    }

    private boolean isPartOrController(BlockPos pos) {
        MetaMachine machine = MetaMachine.getMachine(level, pos);
        return machine instanceof IMultiPart || machine instanceof IMultiController;
    }

    /** The items that may go in this cell, after hatch limits, no-hatch mode and the terminal's block picks. */
    private List<ItemStack> candidates(TraceabilityPredicate cell) {
        List<SimplePredicate> picked = pickCandidates(cell);
        return candidateCache.computeIfAbsent(cell, c -> new HashMap<>()).computeIfAbsent(picked,
                p -> resolve(cell, p));
    }

    private List<ItemStack> resolve(TraceabilityPredicate cell, List<SimplePredicate> picked) {
        List<BlockInfo> options = new ArrayList<>();
        for (SimplePredicate predicate : picked) options.addAll(Arrays.asList(options(predicate)));
        if (setting.isNoHatchMode()) options.removeIf(info -> isPart(info.getBlockState()));
        for (Item selected : selections) {
            for (BlockInfo info : options) {
                if (info.getBlockState().getBlock().asItem() == selected)
                    return hatchPlan.apply(List.of(new ItemStack(selected)));
            }
        }
        List<ItemStack> stacks = new ArrayList<>(options.size());
        Set<Item> seen = new HashSet<>();
        for (BlockInfo info : options) {
            ItemStack stack = info.getItemStackForm();
            if (!stack.isEmpty() && (stack.hasTag() || seen.add(stack.getItem()))) stacks.add(stack.copy());
        }
        return hatchPlan.apply(List.copyOf(stacks));
    }

    /**
     * Same priority as GTCEu: first a limited predicate still below its per-layer minimum, then one below its global
     * minimum, otherwise every limited predicate that has room left plus the common ones. Returns the predicates the
     * cell draws from; the counts are updated here.
     */
    private List<SimplePredicate> pickCandidates(TraceabilityPredicate cell) {
        Object2IntOpenHashMap<SimplePredicate> perLayer = state.getLayerCount();
        Object2IntOpenHashMap<SimplePredicate> global = state.getGlobalCount();
        for (SimplePredicate limit : cell.limited) {
            if (limit.minLayerCount > 0 && allowed(limit) &&
                    below(perLayer, limit, limit.minLayerCount, limit.maxLayerCount)) {
                perLayer.addTo(limit, 1);
                return List.of(limit);
            }
        }
        for (SimplePredicate limit : cell.limited) {
            if (limit.minCount > 0 && allowed(limit) && below(global, limit, limit.minCount, limit.maxCount)) {
                global.addTo(limit, 1);
                return List.of(limit);
            }
        }
        List<SimplePredicate> all = new ArrayList<>();
        for (SimplePredicate limit : cell.limited) {
            if (!allowed(limit) || full(perLayer, limit, limit.maxLayerCount) ||
                    full(global, limit, limit.maxCount)) {
                continue;
            }
            perLayer.addTo(limit, 1);
            global.addTo(limit, 1);
            all.add(limit);
        }
        for (SimplePredicate common : cell.common) {
            // With several choices, no-hatch mode drops the ones that are only hatches.
            if (cell.common.size() > 1 && !allowed(common)) continue;
            all.add(common);
        }
        return all;
    }

    private static boolean below(Object2IntOpenHashMap<SimplePredicate> counts, SimplePredicate limit, int min,
                                 int max) {
        int count = counts.getInt(limit);
        return count < min && (max == -1 || count < max);
    }

    private static boolean full(Object2IntOpenHashMap<SimplePredicate> counts, SimplePredicate limit, int max) {
        return max != -1 && counts.containsKey(limit) && counts.getInt(limit) == max;
    }

    private BlockInfo[] options(SimplePredicate predicate) {
        return optionCache.computeIfAbsent(predicate, p -> {
            BlockInfo[] infos = p.candidates == null ? null : p.candidates.get();
            return infos == null ? new BlockInfo[0] : infos;
        });
    }

    /** In no-hatch mode a predicate counts only if it offers at least one block that is not a hatch. */
    private boolean allowed(SimplePredicate predicate) {
        if (!setting.isNoHatchMode()) return true;
        return allowedCache.computeIfAbsent(predicate, p -> {
            BlockInfo[] infos = options(p);
            return infos.length == 0 || Arrays.stream(infos).anyMatch(info -> !isPart(info.getBlockState()));
        });
    }

    private boolean isPart(ItemStack stack) {
        return partItems.computeIfAbsent(stack.getItem(),
                item -> item instanceof net.minecraft.world.item.BlockItem b &&
                        b.getBlock() instanceof MetaMachineBlock block && createsPart(block));
    }

    private boolean isPart(BlockState blockState) {
        return blockState.getBlock() instanceof MetaMachineBlock && isPart(new ItemStack(blockState.getBlock()));
    }

    private static boolean createsPart(MetaMachineBlock block) {
        try {
            var definition = block.getDefinition();
            var entity = (IMachineBlockEntity) definition.getBlockEntityType().create(BlockPos.ZERO,
                    block.defaultBlockState());
            return definition.createMetaMachine(entity) instanceof MultiblockPartMachine;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
