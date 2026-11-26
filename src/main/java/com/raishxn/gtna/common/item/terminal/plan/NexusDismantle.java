package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import appeng.api.stacks.AEItemKey;
import com.raishxn.gtna.common.item.terminal.NexusBlockPattern;
import com.raishxn.gtna.common.item.terminal.NexusBuildCheckGuard;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory.AutoBuildSetting;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dismantling of a structure (D-439 T6): every structure block the pattern recognises (casings, coils, glass,
 * frames...) goes to the linked ME, then to the inventory. The controller and every part machine (hatches, buses,
 * anything that is an {@link IMultiPart}) stay where they are, so the wiring and the contents survive and the
 * structure is rebuilt around them. It is all or nothing: if the drops do not fit, nothing is removed. Other blocks
 * are removed only when they hold nothing (an item or fluid inside would be spilled).
 * <p>
 * The terminal's Dismantle mode switches itself off after one use ({@link #useOnce}).
 * <p>
 * Follows gt_shanhai's dismantle mode (LGPL-3.0) without its SDA disk packing.
 */
public final class NexusDismantle {

    public enum Outcome {
        DONE,
        NO_ROOM,
        NOTHING,
        UNREADABLE
    }

    public static final String MODE_KEY = "DismantleMode";

    public record Result(Outcome outcome, int removed, int skipped) {}

    private NexusDismantle() {}

    /**
     * One right-click in Dismantle mode: switches the mode off first (so the next click builds again, whatever the
     * outcome), then dismantles.
     */
    public static Result useOnce(Player player, IMultiController controller, ItemStack terminal) {
        terminal.getOrCreateTag().putBoolean(MODE_KEY, false);
        return run(player, controller, terminal);
    }

    public static Result run(Player player, IMultiController controller, ItemStack terminal) {
        return run(player, controller, NexusBlockPattern.fromBlockPattern(controller.getPattern()), terminal);
    }

    public static Result run(Player player, IMultiController controller, NexusBlockPattern pattern,
                             ItemStack terminal) {
        if (pattern == null || !(player.level() instanceof ServerLevel level)) {
            return new Result(Outcome.UNREADABLE, 0, 0);
        }
        AutoBuildSetting setting = AutoBuildSetting.getSetting(terminal);
        var state = controller.getMultiblockState();
        Targets found = collect(player, level, controller, pattern.plan(state, setting, terminal), terminal, true);
        List<BlockPos> targets = found.positions();
        int skipped = found.parts() + found.blocked();
        if (targets.isEmpty()) return new Result(Outcome.NOTHING, 0, skipped);
        List<List<ItemStack>> drops = new ArrayList<>();
        Object2LongLinkedOpenHashMap<AEItemKey> total = new Object2LongLinkedOpenHashMap<>();
        for (BlockPos pos : targets) {
            BlockState blockState = level.getBlockState(pos);
            // EN: the block's own item (as pick-block gives it), not its loot: casings that need a pickaxe to drop
            // EN: would otherwise come back as nothing.
            // PT: o item do próprio bloco (como o pick-block dá), não o loot: casings que precisam de picareta para
            // PT: dropar voltariam como nada.
            // ZH: 返还方块自身的物品（与中键选取相同），而非战利品：否则需要镐才掉落的外壳会什么都不返还。
            ItemStack item = blockState.getBlock().getCloneItemStack(level, pos, blockState);
            List<ItemStack> list = player.isCreative() || item.isEmpty() ? List.of() : List.of(item);
            drops.add(list);
            for (ItemStack drop : list) {
                AEItemKey key = AEItemKey.of(drop);
                if (key != null) total.addTo(key, drop.getCount());
            }
        }
        NexusAE2Link.Session me = setting.isUseAE() && !player.isCreative() ? NexusAE2Link.open(terminal, player) :
                null;
        if (!fits(player, me, total)) return new Result(Outcome.NO_ROOM, 0, skipped);
        NexusBuildCheckGuard.run(state, () -> {
            for (BlockPos pos : targets) level.removeBlock(pos, false);
        });
        NexusMaterials sink = new NexusMaterials(player, me);
        for (Object2LongMap.Entry<AEItemKey> entry : total.object2LongEntrySet()) {
            sink.store(entry.getKey(), entry.getLongValue());
        }
        return new Result(Outcome.DONE, targets.size(), skipped);
    }

    /** A block kind and how many of it a dismantle would take. */
    public record Entry(Block block, int count) {}

    /**
     * What a dismantle would do, for the terminal's quick preview: the blocks it would take by kind (most first),
     * the controller and part machines that stay ({@code parts}) and the blocks it would leave because they are not
     * removable or not empty ({@code blocked}). Nothing is changed.
     */
    public record Preview(int remove, int parts, int blocked, List<Entry> byBlock) {}

    /** The preview of a dismantle of {@code controller}, or null when its pattern cannot be read. */
    @Nullable
    public static Preview preview(Player player, IMultiController controller, ItemStack terminal) {
        NexusBlockPattern pattern = NexusBlockPattern.fromBlockPattern(controller.getPattern());
        if (pattern == null || !(player.level() instanceof ServerLevel level)) return null;
        var plans = pattern.plan(controller.getMultiblockState(), AutoBuildSetting.getSetting(terminal), terminal);
        // EN: no BreakEvent here: the preview runs about once a second while the screen is open, and claim mods
        // EN: would answer each event. The real dismantle still asks them, so a protected block is only a surprise
        // EN: in the count, never in the world.
        // PT: sem BreakEvent aqui: a prévia roda cerca de uma vez por segundo com a tela aberta, e mods de claim
        // PT: responderiam a cada evento. A desmontagem de verdade ainda os consulta; bloco protegido só surpreende
        // PT: na contagem, nunca no mundo.
        // ZH: 此处不触发 BreakEvent：界面打开时预览约每秒运行一次，领地类模组会对每个事件作出响应。真正拆除时仍会询问它们，
        // ZH: 因此受保护的方块最多只会让数量出现偏差，绝不会影响世界。
        Targets found = collect(player, level, controller, plans, terminal, false);
        Map<Block, Integer> counts = new LinkedHashMap<>();
        for (BlockPos pos : found.positions()) counts.merge(level.getBlockState(pos).getBlock(), 1, Integer::sum);
        List<Entry> entries = new ArrayList<>();
        counts.forEach((block, n) -> entries.add(new Entry(block, n)));
        entries.sort(Comparator.comparingInt(Entry::count).reversed()
                .thenComparing(e -> e.block().getDescriptionId()));
        return new Preview(found.positions().size(), found.parts(), found.blocked(), entries);
    }

    private record Targets(List<BlockPos> positions, int parts, int blocked) {}

    /**
     * Walks the plan and sorts every satisfied cell: taken, kept because it is a part, or kept because it cannot or
     * should not be removed. Each position counts once.
     */
    private static Targets collect(Player player, ServerLevel level, IMultiController controller,
                                   List<NexusBuildPlan> plans, ItemStack terminal, boolean askProtection) {
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> targets = new ArrayList<>();
        int parts = 0, blocked = 0;
        BlockPos controllerPos = controller.self().getPos();
        for (NexusBuildPlan plan : plans) {
            for (NexusBuildPlan.Cell cell : plan.cells) {
                if (cell.kind != NexusBuildPlan.Kind.SATISFIED || cell.existing.isAir()) continue;
                if (cell.pos.equals(controllerPos) || !seen.add(cell.pos)) continue;
                if (isPart(level, cell.pos)) {
                    parts++;
                } else if (!canRemove(player, level, cell.pos, cell.existing, askProtection) ||
                        holdsContents(level, cell.pos)) {
                            blocked++;
                        } else {
                            targets.add(cell.pos);
                        }
            }
        }
        return new Targets(targets, parts, blocked);
    }

    // EN: the controller and the part machines (every hatch and bus, by IMultiPart) are never taken, even empty.
    // PT: o controlador e as máquinas de parte (todo hatch e bus, por IMultiPart) nunca são retirados, nem vazios.
    // ZH: 控制器和部件机器（所有仓和总线，按 IMultiPart 判断）永远不会被拆除，即使是空的。
    private static boolean isPart(ServerLevel level, BlockPos pos) {
        return MetaMachine.getMachine(level, pos) instanceof IMultiPart;
    }

    private static boolean canRemove(Player player, ServerLevel level, BlockPos pos, BlockState blockState,
                                     boolean askProtection) {
        if (blockState.isAir() || blockState.getDestroySpeed(level, pos) < 0) return false;
        if (!level.mayInteract(player, pos) || !player.mayBuild()) return false;
        return !askProtection ||
                !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, blockState, player));
    }

    /** True when the block entity at {@code pos} holds items or fluids. */
    static boolean holdsContents(ServerLevel level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be == null) return false;
        var items = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)
                .orElse(null);
        if (items != null) {
            for (int i = 0; i < items.getSlots(); i++) if (!items.getStackInSlot(i).isEmpty()) return true;
        }
        var fluids = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER)
                .orElse(null);
        if (fluids != null) {
            for (int i = 0; i < fluids.getTanks(); i++) if (!fluids.getFluidInTank(i).isEmpty()) return true;
        }
        return false;
    }

    /**
     * Whether everything fits: the ME takes what it can (simulated per item), the rest is laid into a copy of the
     * inventory's main slots.
     */
    static boolean fits(Player player, NexusAE2Link.Session me, Object2LongLinkedOpenHashMap<AEItemKey> total) {
        if (player.isCreative()) return true;
        var inv = player.getInventory();
        ItemStack[] shadow = new ItemStack[inv.items.size()];
        for (int i = 0; i < shadow.length; i++) shadow[i] = inv.items.get(i).copy();
        for (Object2LongMap.Entry<AEItemKey> entry : total.object2LongEntrySet()) {
            AEItemKey key = entry.getKey();
            long left = entry.getLongValue();
            if (me != null) left -= me.insert(key, left, true);
            int max = key.getMaxStackSize();
            for (int i = 0; i < shadow.length && left > 0; i++) {
                ItemStack slot = shadow[i];
                if (slot.isEmpty()) {
                    int put = (int) Math.min(left, max);
                    shadow[i] = key.toStack(put);
                    left -= put;
                } else if (key.matches(slot) && slot.getCount() < max) {
                    int put = (int) Math.min(left, max - slot.getCount());
                    slot.grow(put);
                    left -= put;
                }
            }
            if (left > 0) return false;
        }
        return true;
    }

    public static Component message(Result result) {
        return switch (result.outcome) {
            case DONE -> Component.translatable("gtna.terminal.nexus.dismantle.done", result.removed, result.skipped);
            case NO_ROOM -> Component.translatable("gtna.terminal.nexus.dismantle.no_room");
            case NOTHING -> Component.translatable("gtna.terminal.nexus.dismantle.nothing");
            case UNREADABLE -> Component.translatable("gtna.terminal.nexus.dismantle.nothing");
        };
    }
}
