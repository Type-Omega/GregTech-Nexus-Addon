package com.raishxn.gtna.common.item.terminal.ui;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.item.terminal.plan.NexusBlueprints;
import com.raishxn.gtna.common.item.terminal.plan.NexusLastScan;
import com.raishxn.gtna.common.item.terminal.plan.NexusNearby;
import com.raishxn.gtna.common.item.terminal.plan.NexusScan;
import com.typeomega.lib.ui.tool.ToolTextures;

import java.util.List;

/**
 * The Machines and Saved Structures tabs of the Nexus Terminal (D-440). Rows are fixed widgets whose texts the
 * server fills (LDLib syncs them); a row's button acts on the row's entry in the server's current list.
 * <p>
 * Both follow ideas of GregTech Terminals (BgameXL, MIT): the Multi-Structure Manager's nearby list and the
 * Schematic Interface's named structures, rewritten for GTNA.
 */
final class NexusTerminalSidePages {

    static final int ROWS = 9, ROW_H = 17;

    private NexusTerminalSidePages() {}

    static WidgetGroup machines(Player player, ItemStack stack, int w, int h) {
        return NexusUi.pane(0, 0, w, h, Component.translatable("gtna.terminal.nexus.machines.header",
                NexusNearby.RADIUS), page -> {
                    int cw = page.getSizeWidth();
                    page.addWidget(NexusUi.display(0, 0, cw, ROWS * ROW_H + 4));
                    for (int row = 0; row < ROWS; row++) {
                        int index = row;
                        page.addWidget(NexusUi.label(6, 6 + row * ROW_H, () -> machineLine(player, index)));
                        page.addWidget(NexusUi.primary(cw - 82, 2 + row * ROW_H, 78, 15,
                                Component.translatable("gtna.terminal.nexus.machines.select"), cd -> {
                                    if (cd.isRemote || !(player instanceof ServerPlayer sp)) return;
                                    var list = NexusNearby.find(player, NexusNearby.RADIUS);
                                    if (index < list.size()) {
                                        NexusScan.scanAndShow(sp, list.get(index).controller(), stack);
                                    }
                                }));
                    }
                });
    }

    private static String machineLine(Player player, int row) {
        if (player.level().isClientSide) return "";
        List<NexusNearby.Entry> list = NexusNearby.find(player, NexusNearby.RADIUS);
        if (row >= list.size()) return row == 0 ?
                Component.translatable("gtna.terminal.nexus.machines.none").getString() : "";
        var e = list.get(row);
        var target = NexusLastScan.get(player);
        boolean current = target != null && target.controller().pos().equals(e.pos());
        return (current ? "§b▶ " : "") + (e.controller().isFormed() ? "§a● " : "§c○ ") + "§f" +
                e.controller().self().getDefinition().getItem().getDescription().getString() + " §7" +
                (int) e.distance() + " m";
    }

    static WidgetGroup blueprints(Player player, ItemStack stack, int w, int h) {
        return NexusUi.pane(0, 0, w, h, Component.translatable("gtna.terminal.nexus.blueprints.header"),
                page -> fillBlueprints(player, stack, page));
    }

    private static void fillBlueprints(Player player, ItemStack stack, WidgetGroup page) {
        int w = page.getSizeWidth();
        String[] name = { "" };
        var field = new TextFieldWidget(0, 0, w - 114, 18, () -> name[0], s -> name[0] = s);
        field.setMaxStringLength(32);
        field.setBordered(false).setTextColor(NexusUi.THEME.text());
        field.setBackground(ToolTextures.display(NexusUi.THEME));
        page.addWidget(field);
        page.addWidget(NexusUi.primary(w - 110, 0, 110, 18, Component.translatable(
                "gtna.terminal.nexus.blueprints.save"), cd -> save(player, name[0], cd.isRemote)));
        int top = 22;
        page.addWidget(new ImageWidget(0, top, w, ROWS * ROW_H + 4, ToolTextures.display(NexusUi.THEME)));
        for (int row = 0; row < ROWS; row++) {
            int index = row, y = top + 2 + row * ROW_H;
            page.addWidget(NexusUi.label(6, y + 4, () -> blueprintLine(player, index)));
            page.addWidget(NexusUi.success(w - 122, y, 56, 15,
                    Component.translatable("gtna.terminal.nexus.blueprints.paste"), cd -> {
                        if (!cd.isRemote) paste(player, stack, index);
                    }));
            page.addWidget(NexusUi.danger(w - 62, y, 58, 15,
                    Component.translatable("gtna.terminal.nexus.blueprints.delete"), cd -> {
                        if (cd.isRemote || player.getServer() == null) return;
                        var data = NexusBlueprints.get(player.getServer());
                        var list = data.list(player);
                        if (index < list.size()) data.delete(player, list.get(index).name());
                    }));
        }
    }

    private static void save(Player player, String typed, boolean remote) {
        if (remote || player.getServer() == null) return;
        var last = NexusLastScan.get(player);
        String n = typed.isBlank() ? "structure" : typed.trim();
        if (last == null) {
            player.displayClientMessage(Component.translatable("gtna.terminal.nexus.report.none"), true);
            return;
        }
        var level = player.getServer().getLevel(last.controller().dimension());
        if (level != null && MetaMachine.getMachine(level, last.controller().pos()) instanceof IMultiController c) {
            var saved = NexusBlueprints.get(player.getServer()).save(player, n, c, last.result());
            player.displayClientMessage(saved == null ? Component.translatable("gtna.terminal.nexus.blueprints.full") :
                    Component.translatable("gtna.terminal.nexus.blueprints.saved", n, saved.size()), true);
        }
    }

    private static String blueprintLine(Player player, int row) {
        if (player.level().isClientSide || player.getServer() == null) return "";
        var list = NexusBlueprints.get(player.getServer()).list(player);
        if (row >= list.size()) return row == 0 ?
                Component.translatable("gtna.terminal.nexus.blueprints.none").getString() : "";
        var b = list.get(row);
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(b.machine());
        return "§f" + b.name() + " §7" + item.getDescription().getString() + ", " + b.size();
    }

    /** Pastes the row's structure onto the controller of the last scan. */
    private static void paste(Player player, ItemStack stack, int row) {
        if (player.getServer() == null) return;
        var list = NexusBlueprints.get(player.getServer()).list(player);
        var last = NexusLastScan.get(player);
        if (row >= list.size() || last == null) {
            player.displayClientMessage(Component.translatable("gtna.terminal.nexus.report.none"), true);
            return;
        }
        var level = player.getServer().getLevel(last.controller().dimension());
        if (level == null || !(MetaMachine.getMachine(level, last.controller().pos()) instanceof IMultiController c))
            return;
        var result = NexusBlueprints.paste(player, c,
                com.raishxn.gtna.common.item.terminal.NexusBlockPattern.fromBlockPattern(c.getPattern()), stack,
                list.get(row));
        player.displayClientMessage(Component.translatable(result == NexusBlueprints.PasteResult.WRONG_MACHINE ?
                "gtna.terminal.nexus.blueprints.wrong_machine" : "gtna.terminal.nexus.blueprints.pasted",
                list.get(row).name()), true);
    }
}
