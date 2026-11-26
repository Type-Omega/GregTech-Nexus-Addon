package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.common.item.terminal.plan.NexusLastScan;
import com.raishxn.gtna.common.item.terminal.plan.NexusScan;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalInfo;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalWindow;
import com.typeomega.lib.test.Previews;
import com.typeomega.lib.ui.tool.ToolTheme;

import java.util.Map;

import static com.raishxn.gtna.gametest.NexusTerminalPlanGameTests.player;
import static com.raishxn.gtna.gametest.NexusTerminalPlanGameTests.terminal;

/** The Nexus Terminal window in its purple theme (D-447): geometry, texts and the Dismantle warning. */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class NexusTerminalWindowGameTests {

    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    private NexusTerminalWindowGameTests() {}

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void windowUsesThePurpleThemeAtItsFixedSize(GameTestHelper h) {
        var player = player(h, GameType.SURVIVAL);
        var window = new NexusTerminalWindow(player, terminal(0)).build();
        h.assertTrue(window.theme() == ToolTheme.NEXUS_PURPLE, "the Nexus Terminal uses NEXUS_PURPLE");
        h.assertTrue(NexusTerminalWindow.WIDTH == 420 && NexusTerminalWindow.HEIGHT == 236, "fixed 420x236");
        h.assertTrue(window.totalHeight() == 236 + 18, "tabs on top of the 236 px frame: " + window.totalHeight());
        h.assertTrue(window.pageWidth() == 410, "page width " + window.pageWidth());
        h.assertTrue(window.pageHeight() >= 200, "the page keeps room for the console: " + window.pageHeight());
        h.assertTrue(window.build().widgets.size() == 3, "frame, title bar and the three tabs");
        h.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 100)
    public static void dismantlePanelWarnsAndPreviewsOnlyWhileTheModeIsOn(GameTestHelper h) {
        try (var placed = Previews.place(h, GTMultiMachines.ELECTRIC_BLAST_FURNACE, ORIGIN)) {
            h.assertTrue(placed.forms(), "the blast furnace must form first: " + placed.error());
            var player = player(h, GameType.SURVIVAL);
            var terminal = terminal(0);
            NexusLastScan.put(player, placed.controller(), new NexusScan.Result(java.util.List.of(), Map.of(), Map.of(),
                    true, false));

            String off = NexusTerminalInfo.panelLine(player, terminal, 0);
            h.assertTrue(off.contains(tr("gtna.terminal.nexus.last_scan")),
                    "Dismantle off shows the last scan: " + off);
            h.assertTrue(NexusTerminalInfo.badge(player, terminal).contains(tr("gtna.terminal.nexus.ui.mode.build")),
                    "the badge says Build");

            terminal.getOrCreateTag().putBoolean("DismantleMode", true);
            String warning = NexusTerminalInfo.panelLine(player, terminal, 0);
            h.assertTrue(warning.equals(tr("gtna.terminal.nexus.ui.dismantle.title")), "red header: " + warning);
            h.assertTrue(warning.startsWith("§c"), "the header is red");
            h.assertTrue(NexusTerminalInfo.badge(player, terminal).contains(
                    tr("gtna.terminal.nexus.ui.mode.dismantle")), "the badge says Dismantle");
            String removes = NexusTerminalInfo.panelLine(player, terminal, 1);
            h.assertFalse(removes.isBlank() || removes.equals(tr("gtna.terminal.nexus.ui.dismantle.no_target")),
                    "a target is known, so the preview counts: " + removes);
            h.assertFalse(NexusTerminalInfo.panelLine(player, terminal, 2).isBlank(),
                    "the third line tells what stays");
            h.assertFalse(NexusTerminalInfo.panelLine(player, terminal, 3).isBlank(), "block kinds are listed");

            terminal.getOrCreateTag().putBoolean("DismantleMode", false);
            h.assertTrue(NexusTerminalInfo.panelLine(player, terminal, 0).contains(
                    tr("gtna.terminal.nexus.last_scan")), "the warning goes away with the mode");
        }
        h.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void withoutATargetThePanelAsksForOne(GameTestHelper h) {
        var player = player(h, GameType.SURVIVAL);
        var terminal = terminal(0);
        NexusLastScan.clear(player);
        terminal.getOrCreateTag().putBoolean("DismantleMode", true);
        h.assertTrue(NexusTerminalInfo.panelLine(player, terminal, 1).equals(
                tr("gtna.terminal.nexus.ui.dismantle.no_target")), "no scan, no preview");
        h.assertTrue(NexusTerminalInfo.targetLine(player).equals(tr("gtna.terminal.nexus.ui.no_target")),
                "the title bar says there is no target");
        h.succeed();
    }
}
