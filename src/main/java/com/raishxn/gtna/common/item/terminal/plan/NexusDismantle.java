package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import java.util.ArrayList;
import java.util.List;

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
        List<NexusBuildPlan> plans = pattern.plan(state, setting, terminal);
        List<BlockPos> targets = new ArrayList<>();
        List<List<ItemStack>> drops = new ArrayList<>();
        Object2LongLinkedOpenHashMap<AEItemKey> total = new Object2LongLinkedOpenHashMap<>();
        int skipped = 0;
        BlockPos controllerPos = controller.self().getPos();
        for (NexusBuildPlan plan : plans) {
            for (NexusBuildPlan.Cell cell : plan.cells) {
                if (cell.kind != NexusBuildPlan.Kind.SATISFIED || cell.existing.isAir()) continue;
                if (cell.pos.equals(controllerPos) || targets.contains(cell.pos)) continue;
                if (isPart(level, cell.pos)) {
                    skipped++;
                    continue;
                }
                if (!canRemove(player, level, cell.pos, cell.existing) || holdsContents(level, cell.pos)) {
                    skipped++;
                    continue;
                }
                targets.add(cell.pos);
            }
        }
        if (targets.isEmpty()) return new Result(Outcome.NOTHING, 0, skipped);
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

    // EN: the controller and the part machines (every hatch and bus, by IMultiPart) are never taken, even empty.
    // PT: o controlador e as máquinas de parte (todo hatch e bus, por IMultiPart) nunca são retirados, nem vazios.
    // ZH: 控制器和部件机器（所有仓和总线，按 IMultiPart 判断）永远不会被拆除，即使是空的。
    private static boolean isPart(ServerLevel level, BlockPos pos) {
        return MetaMachine.getMachine(level, pos) instanceof IMultiPart;
    }

    private static boolean canRemove(Player player, ServerLevel level, BlockPos pos, BlockState blockState) {
        if (blockState.isAir() || blockState.getDestroySpeed(level, pos) < 0) return false;
        if (!level.mayInteract(player, pos) || !player.mayBuild()) return false;
        return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, blockState, player));
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
