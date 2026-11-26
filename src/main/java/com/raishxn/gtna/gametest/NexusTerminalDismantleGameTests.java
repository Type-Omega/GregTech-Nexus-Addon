package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.common.item.terminal.NexusTerminalBehavior;
import com.raishxn.gtna.common.item.terminal.plan.NexusDismantle;
import com.typeomega.lib.test.Previews;

import java.util.ArrayList;
import java.util.List;

import static com.raishxn.gtna.gametest.NexusTerminalPlanGameTests.player;
import static com.raishxn.gtna.gametest.NexusTerminalPlanGameTests.terminal;

/**
 * The Nexus Terminal's Dismantle mode on a real GTCEu structure (the Electric Blast Furnace): the controller and every
 * hatch and bus stay, only the structure blocks go, and the mode switches itself off after one use.
 */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class NexusTerminalDismantleGameTests {

    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    private NexusTerminalDismantleGameTests() {}

    /** Positions of the part machines and of the other structure blocks of a placed structure (absolute). */
    private record Layout(BlockPos controller, List<BlockPos> parts, List<BlockPos> structure) {}

    private static Layout layout(GameTestHelper h, Previews.Placed placed) {
        BlockPos controller = placed.controller().getPos();
        List<BlockPos> parts = new ArrayList<>();
        List<BlockPos> structure = new ArrayList<>();
        for (BlockPos local : placed.blocks()) {
            BlockPos pos = h.absolutePos(local);
            if (pos.equals(controller)) continue;
            (MetaMachine.getMachine(h.getLevel(), pos) instanceof IMultiPart ? parts : structure).add(pos);
        }
        return new Layout(controller, parts, structure);
    }

    private static UseOnContext click(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    @GameTest(template = "empty_16", timeoutTicks = 100)
    public static void dismantleKeepsTheControllerAndEveryPart(GameTestHelper h) {
        try (var placed = Previews.place(h, GTMultiMachines.ELECTRIC_BLAST_FURNACE, ORIGIN)) {
            h.assertTrue(placed.forms(), "the blast furnace must form first: " + placed.error());
            Layout layout = layout(h, placed);
            h.assertFalse(layout.parts().isEmpty(), "the blast furnace has hatches");
            h.assertFalse(layout.structure().isEmpty(), "the blast furnace has casings and coils");
            var player = player(h, GameType.SURVIVAL);
            var terminal = terminal(0);
            terminal.getOrCreateTag().putBoolean("DismantleMode", true);
            player.setItemInHand(InteractionHand.MAIN_HAND, terminal);

            var result = NexusDismantle.useOnce(player, placed.controller(), terminal);
            h.assertTrue(result.outcome() == NexusDismantle.Outcome.DONE, "dismantle outcome " + result);
            h.assertTrue(MetaMachine.getMachine(h.getLevel(), layout.controller()) == placed.controller(),
                    "the controller must stay");
            for (BlockPos part : layout.parts()) {
                h.assertTrue(MetaMachine.getMachine(h.getLevel(), part) instanceof IMultiPart,
                        "the hatch or bus at " + part + " must stay");
            }
            for (BlockPos block : layout.structure()) {
                h.assertTrue(h.getLevel().getBlockState(block).isAir(),
                        "the structure block at " + block + " must be gone");
            }
            h.assertTrue(result.removed() == layout.structure().size(), "removed " + result.removed() + " of " +
                    layout.structure().size());
            h.assertFalse(player.getInventory().isEmpty(), "the casings and coils came back to the player");
        }
        h.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 100)
    public static void dismantleModeSwitchesOffAfterOneUse(GameTestHelper h) {
        try (var placed = Previews.place(h, GTMultiMachines.ELECTRIC_BLAST_FURNACE, ORIGIN)) {
            h.assertTrue(placed.forms(), "the blast furnace must form first: " + placed.error());
            var player = player(h, GameType.SURVIVAL);
            var terminal = terminal(0);
            terminal.getOrCreateTag().putBoolean("DismantleMode", true);
            player.setItemInHand(InteractionHand.MAIN_HAND, terminal);
            player.setShiftKeyDown(true);

            NexusTerminalBehavior.INSTANCE.useOn(click(player, placed.controller().getPos()));
            h.assertFalse(terminal.getOrCreateTag().getBoolean("DismantleMode"),
                    "the mode must be off after one use");
        }
        h.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 100)
    public static void aSecondRightClickDoesNotDismantleAgain(GameTestHelper h) {
        try (var placed = Previews.place(h, GTMultiMachines.ELECTRIC_BLAST_FURNACE, ORIGIN)) {
            h.assertTrue(placed.forms(), "the blast furnace must form first: " + placed.error());
            Layout layout = layout(h, placed);
            var player = player(h, GameType.SURVIVAL);
            var terminal = terminal(0);
            terminal.getOrCreateTag().putBoolean("DismantleMode", true);
            player.setItemInHand(InteractionHand.MAIN_HAND, terminal);
            player.setShiftKeyDown(true);
            var context = click(player, layout.controller());

            NexusTerminalBehavior.INSTANCE.useOn(context);
            for (BlockPos block : layout.structure()) {
                h.assertTrue(h.getLevel().getBlockState(block).isAir(), "the first click leaves " + block);
            }
            // The second click is the normal build: it lays the structure again from what the first one returned.
            NexusTerminalBehavior.INSTANCE.useOn(context);
            h.assertFalse(terminal.getOrCreateTag().getBoolean("DismantleMode"), "the mode came back on");
            h.assertTrue(MetaMachine.getMachine(h.getLevel(), layout.controller()) == placed.controller(),
                    "the controller must stay");
            for (BlockPos part : layout.parts()) {
                h.assertTrue(MetaMachine.getMachine(h.getLevel(), part) instanceof IMultiPart,
                        "the hatch or bus at " + part + " must stay");
            }
            long rebuilt = layout.structure().stream().filter(p -> !h.getLevel().getBlockState(p).isAir()).count();
            h.assertTrue(rebuilt > 0, "the second click must build, not dismantle: nothing was laid again");
        }
        h.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 100)
    public static void previewCountsMatchWhatDismantleRemoves(GameTestHelper h) {
        try (var placed = Previews.place(h, GTMultiMachines.ELECTRIC_BLAST_FURNACE, ORIGIN)) {
            h.assertTrue(placed.forms(), "the blast furnace must form first: " + placed.error());
            Layout layout = layout(h, placed);
            var player = player(h, GameType.SURVIVAL);
            var terminal = terminal(0);
            terminal.getOrCreateTag().putBoolean("DismantleMode", true);
            player.setItemInHand(InteractionHand.MAIN_HAND, terminal);

            var preview = NexusDismantle.preview(player, placed.controller(), terminal);
            h.assertTrue(preview != null, "the blast furnace pattern is readable");
            h.assertTrue(preview.remove() == layout.structure().size(), "the preview takes " + preview.remove() +
                    " of " + layout.structure().size() + " structure blocks");
            h.assertTrue(preview.parts() == layout.parts().size(), "the preview keeps " + preview.parts() +
                    " of " + layout.parts().size() + " hatches and buses");
            h.assertTrue(preview.byBlock().stream().mapToInt(NexusDismantle.Entry::count).sum() == preview.remove(),
                    "the counts by block add up to the total");
            for (int i = 1; i < preview.byBlock().size(); i++) {
                h.assertTrue(preview.byBlock().get(i - 1).count() >= preview.byBlock().get(i).count(),
                        "most numerous first");
            }
            for (BlockPos block : layout.structure()) {
                h.assertFalse(h.getLevel().getBlockState(block).isAir(), "the preview must not remove " + block);
            }
            h.assertTrue(player.getInventory().items.stream().filter(stack -> stack != terminal).allMatch(
                    net.minecraft.world.item.ItemStack::isEmpty), "the preview gives nothing but the terminal in hand");
            h.assertTrue(terminal.getOrCreateTag().getBoolean("DismantleMode"), "the preview keeps the mode");

            var result = NexusDismantle.useOnce(player, placed.controller(), terminal);
            h.assertTrue(result.removed() == preview.remove(), "dismantle removed " + result.removed() +
                    ", the preview said " + preview.remove());
            h.assertTrue(result.skipped() == preview.parts() + preview.blocked(), "kept " + result.skipped() +
                    ", the preview said " + (preview.parts() + preview.blocked()));
            var after = NexusDismantle.preview(player, placed.controller(), terminal);
            h.assertTrue(after != null && after.remove() == 0, "nothing left to take after it ran");
        }
        h.succeed();
    }
}
