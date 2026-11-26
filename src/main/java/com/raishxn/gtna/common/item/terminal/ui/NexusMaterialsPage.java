package com.raishxn.gtna.common.item.terminal.ui;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.AEItemKey;
import com.raishxn.gtna.common.item.terminal.plan.NexusCrafting;
import com.raishxn.gtna.common.item.terminal.plan.NexusLastScan;
import com.raishxn.gtna.common.item.terminal.plan.NexusMaterials;
import com.raishxn.gtna.common.item.terminal.plan.NexusScan;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Materials step: need, inventory, ME and missing per item from the last scan, green when complete and pink
 * when short, with the crafting state of the ME next to it; "Scan again" and "Ask the ME" below.
 */
final class NexusMaterialsPage {

    static final int ROWS = 48;

    private NexusMaterialsPage() {}

    static WidgetGroup build(Player player, ItemStack stack, int cw, int ch) {
        return NexusUi.pane(0, 0, cw, ch, Component.translatable("gtna.terminal.nexus.ui.pane.materials"),
                page -> fill(player, stack, page));
    }

    private static void fill(Player player, ItemStack stack, WidgetGroup page) {
        int w = page.getSizeWidth(), h = page.getSizeHeight();
        var list = NexusUi.scroll(0, 0, w, h - 22);
        for (int row = 0; row < ROWS; row++) {
            int index = row;
            list.addWidget(NexusUi.label(4, 3 + row * 10, () -> materialLine(player, index)));
        }
        page.addWidget(list);
        int bw = (w - 4) / 2, by = h - 18;
        page.addWidget(NexusUi.button(0, by, bw, 18, Component.translatable("gtna.terminal.nexus.materials.refresh"),
                cd -> {
                    if (!cd.isRemote) {
                        var c = NexusTerminalInfo.target(player);
                        if (c != null) NexusScan.scanAndShow((ServerPlayer) player, c, stack);
                        else noScan(player);
                    }
                }));
        page.addWidget(NexusUi.primary(bw + 4, by, bw, 18, Component.translatable("gtna.terminal.nexus.materials.ask"),
                cd -> {
                    if (!cd.isRemote) askMe(player, stack);
                }));
    }

    private static void noScan(Player player) {
        player.displayClientMessage(Component.translatable("gtna.terminal.nexus.report.none"), true);
    }

    /** "Ask the ME": crafts every missing item of the last scan that has a pattern (D-439 T9). */
    private static void askMe(Player player, ItemStack stack) {
        NexusLastScan.Entry last = NexusLastScan.get(player);
        if (last == null) {
            noScan(player);
            return;
        }
        NexusAE2Link.Session me = NexusAE2Link.open(stack, player);
        if (me == null) {
            player.displayClientMessage(Component.translatable("gtna.terminal.nexus.ae2.tooltip.not_linked"), true);
            return;
        }
        int started = NexusCrafting.request(player, me, NexusMaterials.missing(last.result().plans()));
        player.displayClientMessage(Component.translatable("gtna.terminal.nexus.materials.asked", started), true);
    }

    private static String materialLine(Player player, int row) {
        NexusScan.Result r = NexusTerminalInfo.last(player);
        if (r == null) return "";
        List<Map.Entry<AEItemKey, Long>> rows = new ArrayList<>(r.need().entrySet());
        if (row >= rows.size()) return "";
        AEItemKey key = rows.get(row).getKey();
        long need = rows.get(row).getValue();
        long[] stock = r.stock().getOrDefault(key, new long[2]);
        long missing = r.creative() ? 0 : Math.max(0, need - stock[0] - stock[1]);
        var status = NexusCrafting.statuses(player).get(key);
        String text = Component.translatable("gtna.terminal.nexus.report.material", key.getDisplayName(), need,
                stock[0], stock[1], missing).getString();
        if (status != null) text += " " + Component.translatable(
                "gtna.terminal.nexus.craft." + status.name().toLowerCase(java.util.Locale.ROOT)).getString();
        return (missing > 0 ? "§d" : "§a") + text;
    }
}
