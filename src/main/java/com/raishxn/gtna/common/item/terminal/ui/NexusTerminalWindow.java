package com.raishxn.gtna.common.item.terminal.ui;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.AEItemKey;
import com.raishxn.gtna.common.item.terminal.NexusAutoBuilder;
import com.raishxn.gtna.common.item.terminal.plan.*;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;
import com.typeomega.lib.ui.tool.ToolWindow;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Nexus Terminal window (GTIA-D-439, console layout GTIA-D-440) in the lib's GTNH-style {@link ToolWindow}: the
 * Engineering Console (steps, page, action bar), the nearby machines and the saved structures. Buttons act on the
 * controller of the
 * last scan; the texts the server owns (summary, ME state, materials) are synced labels.
 */
public final class NexusTerminalWindow {

    public static final int WIDTH = 420, HEIGHT = 236;
    static final int STEP_W = 74, BAR_H = 22;
    static final int MATERIAL_ROWS = 48;

    private final Player player;
    private final ItemStack stack;

    public NexusTerminalWindow(Player player, ItemStack stack) {
        this.player = player;
        this.stack = stack;
    }

    public ToolWindow build() {
        ToolWindow window = new ToolWindow(WIDTH, HEIGHT);
        window.tab(Component.translatable("gtna.terminal.nexus.tab.build"), consolePage(window));
        window.tab(Component.translatable("gtna.terminal.nexus.tab.machines"),
                NexusTerminalSidePages.machines(player, stack, window.pageWidth(), window.pageHeight()));
        window.tab(Component.translatable("gtna.terminal.nexus.tab.blueprints"),
                NexusTerminalSidePages.blueprints(player, stack, window.pageWidth(), window.pageHeight()));
        return window;
    }

    /**
     * The Engineering Console (GTIA-D-440): numbered steps on the left (structure, tiers, hatches, materials), the
     * step's page in the middle and a fixed action bar at the bottom with the scan summary and the actions.
     */
    private WidgetGroup consolePage(ToolWindow window) {
        int pw = window.pageWidth(), ph = window.pageHeight();
        int cw = pw - STEP_W - 4, chh = ph - BAR_H - 4;
        WidgetGroup page = new WidgetGroup(0, 0, pw, ph);
        WidgetGroup[] steps = { settingsPage(cw, chh), tiersStep(cw, chh),
                NexusTerminalHatchPage.build(stack, cw, chh), materialsPage(cw, chh) };
        String[] names = { "gtna.terminal.nexus.step.structure", "gtna.terminal.nexus.step.tiers",
                "gtna.terminal.nexus.step.hatches", "gtna.terminal.nexus.step.materials" };
        int[] active = { 0 };
        for (int i = 0; i < steps.length; i++) {
            int index = i;
            steps[i].setSelfPosition(STEP_W + 4, 0);
            steps[i].setVisible(i == 0);
            steps[i].setActive(i == 0);
            page.addWidget(steps[i]);
            page.addWidget(ToolWindow.button(0, i * 22, STEP_W, 20,
                    Component.literal((i + 1) + "  ").append(Component.translatable(names[i])), cd -> {
                        active[0] = index;
                        for (int k = 0; k < steps.length; k++) {
                            steps[k].setVisible(k == index);
                            steps[k].setActive(k == index);
                        }
                    }));
        }
        page.addWidget(ToolWindow.label(2, steps.length * 22 + 6, Component.translatable("gtna.terminal.nexus.me")));
        page.addWidget(ToolWindow.label(2, steps.length * 22 + 17, this::meState));
        // Action bar: summary on the left, actions on the right.
        int by = ph - BAR_H;
        page.addWidget(new com.lowdragmc.lowdraglib.gui.widget.ImageWidget(0, by - 2, pw, BAR_H + 2,
                GuiTextures.DISPLAY));
        page.addWidget(ToolWindow.label(4, by + 2, () -> scanLine(0)).setTextColor(0xFFFFFFFF));
        page.addWidget(ToolWindow.label(4, by + 11, () -> scanLine(2) + "  " + jobLine()).setTextColor(0xFFFFFFFF));
        int bw = 52, bx = pw - 4 * (bw + 2);
        page.addWidget(ToolWindow.button(bx, by + 1, bw, 18, Component.translatable("gtna.terminal.nexus.scan"),
                cd -> {
                    if (!cd.isRemote) onController(c -> NexusScan.scanAndShow((ServerPlayer) player, c, stack));
                }));
        page.addWidget(ToolWindow.button(bx + bw + 2, by + 1, bw, 18,
                Component.translatable("gtna.terminal.nexus.build"), cd -> {
                    if (!cd.isRemote) onController(c -> NexusAutoBuilder.autoBuild(player, c, stack));
                }));
        page.addWidget(ToolWindow.button(bx + 2 * (bw + 2), by + 1, bw, 18,
                Component.translatable("gtna.terminal.nexus.cancel"), cd -> {
                    if (!cd.isRemote) NexusBuildJobs.cancel(player);
                }));
        page.addWidget(ToolWindow.button(bx + 3 * (bw + 2), by + 1, bw, 18,
                Component.translatable("gtna.terminal.nexus.report.button"), cd -> {
                    if (!cd.isRemote && player instanceof ServerPlayer sp && !NexusReport.send(sp)) {
                        sp.displayClientMessage(Component.translatable("gtna.terminal.nexus.report.none"), true);
                    }
                }));
        return page;
    }

    private WidgetGroup settingsPage(int w, int h) {
        WidgetGroup page = new WidgetGroup(0, 0, w, h);
        int half = w / 2 - 4;
        page.addWidget(ToolWindow.number(0, 0, half, Component.translatable("gtna.terminal.nexus.repetitions"), 0,
                1000, () -> stack.getOrCreateTag().getInt("Repetitions"),
                v -> stack.getOrCreateTag().putInt("Repetitions", v)));
        page.addWidget(ToolWindow.number(half + 8, 0, half, Component.translatable("gtna.terminal.nexus.module_build"),
                0, 100, () -> stack.getOrCreateTag().getInt("ModuleBuild"),
                v -> stack.getOrCreateTag().putInt("ModuleBuild", v)));
        String[][] toggles = { { "ReplaceMode", "gtna.terminal.nexus.replace_mode" },
                { "DemolitionMode", "gtna.terminal.nexus.demolition_mode" },
                { "DismantleMode", "gtna.terminal.nexus.dismantle_mode" },
                { "UseAE", "gtna.terminal.nexus.use_ae" }, { "MirrorBuild", "gtna.terminal.nexus.mirror_build" },
                { "NoHatchMode", "gtna.terminal.nexus.no_hatch" } };
        for (int i = 0; i < toggles.length; i++) {
            String key = toggles[i][0];
            page.addWidget(ToolWindow.toggle((i % 2) * (half + 8), 38 + (i / 2) * 18, half,
                    Component.translatable(toggles[i][1]), () -> stack.getOrCreateTag().getBoolean(key),
                    () -> stack.getOrCreateTag().putBoolean(key, !stack.getOrCreateTag().getBoolean(key))));
        }
        page.addWidget(ToolWindow.label(0, 100, Component.translatable("gtna.terminal.nexus.last_scan")));
        for (int line = 0; line < 4; line++) {
            int index = line;
            page.addWidget(ToolWindow.label(0, 112 + 10 * line, () -> scanLine(index)));
        }
        return page;
    }

    /** Tiers step: the category picker, and the tier swap of the families the scanned structure holds. */
    private WidgetGroup tiersStep(int w, int h) {
        WidgetGroup page = new WidgetGroup(0, 0, w, h);
        WidgetGroup picker = new NexusTerminalTiersPage(stack).build(w, h - 34);
        page.addWidget(picker);
        page.addWidget(ToolWindow.label(0, h - 32, this::familyLine));
        page.addWidget(ToolWindow.button(0, h - 20, w, 18, Component.translatable("gtna.terminal.nexus.family.swap"),
                cd -> {
                    if (!cd.isRemote) swapFamilies();
                }));
        return page;
    }

    /** Swaps every family of the last scan whose pick differs from the tier in the world. */
    private void swapFamilies() {
        NexusScan.Result r = last();
        if (r == null) {
            player.displayClientMessage(Component.translatable("gtna.terminal.nexus.report.none"), true);
            return;
        }
        int[] swapped = { 0 };
        onController(c -> {
            for (var family : NexusFamilySwap.families(r)) {
                var pick = BlockSelectionConfigWidget.getSelectedBlock(stack, family.category());
                if (pick != null && !ItemStack.isSameItem(pick, family.current()) &&
                        NexusFamilySwap.swap(player, c, stack, family.category(), pick)) {
                    swapped[0]++;
                }
            }
        });
        player.displayClientMessage(Component.translatable("gtna.terminal.nexus.family.swapped", swapped[0]), true);
    }

    private String familyLine() {
        NexusScan.Result r = last();
        if (r == null) return "";
        StringBuilder out = new StringBuilder();
        for (var family : NexusFamilySwap.families(r)) {
            if (out.length() > 60) break;
            var pick = BlockSelectionConfigWidget.getSelectedBlock(stack, family.category());
            out.append(out.length() == 0 ? "" : "  ").append(family.current().getHoverName().getString());
            if (pick != null && !ItemStack.isSameItem(pick, family.current()))
                out.append(" → ").append(pick.getHoverName().getString());
        }
        return out.toString();
    }

    private WidgetGroup materialsPage(int pw, int ph) {
        WidgetGroup page = new WidgetGroup(0, 0, pw, ph);
        page.addWidget(ToolWindow.label(0, 0, Component.translatable("gtna.terminal.nexus.materials.header")));
        var list = new DraggableScrollableWidgetGroup(0, 12, pw, ph - 36);
        list.setBackground(GuiTextures.DISPLAY);
        list.setYScrollBarWidth(4);
        for (int row = 0; row < MATERIAL_ROWS; row++) {
            int index = row;
            list.addWidget(ToolWindow.label(3, 3 + row * 10, () -> materialLine(index))
                    .setTextColor(0xFFFFFFFF));
        }
        page.addWidget(list);
        int bw = (pw - 4) / 2, by = ph - 20;
        page.addWidget(ToolWindow.button(0, by, bw, 18, Component.translatable("gtna.terminal.nexus.materials.refresh"),
                cd -> {
                    if (!cd.isRemote) onController(c -> NexusScan.scanAndShow((ServerPlayer) player, c, stack));
                }));
        page.addWidget(
                ToolWindow.button(bw + 4, by, bw, 18, Component.translatable("gtna.terminal.nexus.materials.ask"),
                        cd -> {
                            if (!cd.isRemote) askMe();
                        }));
        return page;
    }

    /** "Ask the ME": crafts every missing item of the last scan that has a pattern (GTIA-D-439 T9). */
    private void askMe() {
        NexusLastScan.Entry last = NexusLastScan.get(player);
        if (last == null) {
            player.displayClientMessage(Component.translatable("gtna.terminal.nexus.report.none"), true);
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

    private void onController(java.util.function.Consumer<IMultiController> action) {
        NexusLastScan.Entry last = NexusLastScan.get(player);
        if (last == null || player.getServer() == null) {
            player.displayClientMessage(Component.translatable("gtna.terminal.nexus.report.none"), true);
            return;
        }
        var level = player.getServer().getLevel(last.controller().dimension());
        if (level != null && MetaMachine.getMachine(level, last.controller().pos()) instanceof IMultiController c) {
            action.accept(c);
        }
    }

    private String meState() {
        if (player.level().isClientSide) return "";
        if (!NexusAE2Link.isLinked(stack)) return Component.translatable("gtna.terminal.nexus.me.unlinked").getString();
        return NexusAE2Link.grid(stack, player.getServer()) != null ?
                Component.translatable("gtna.terminal.nexus.me.online").getString() :
                Component.translatable("gtna.terminal.nexus.me.offline").getString();
    }

    @Nullable
    private NexusScan.Result last() {
        NexusLastScan.Entry entry = player.level().isClientSide ? null : NexusLastScan.get(player);
        return entry == null ? null : entry.result();
    }

    private String scanLine(int line) {
        NexusScan.Result r = last();
        if (r == null) return line == 0 ? Component.translatable("gtna.terminal.nexus.no_scan").getString() : "";
        return switch (line) {
            case 0 -> Component.translatable("gtna.terminal.nexus.line.place", r.count(NexusBuildPlan.Kind.PLACE),
                    r.count(NexusBuildPlan.Kind.REPLACE)).getString();
            case 1 -> Component.translatable("gtna.terminal.nexus.line.blocked",
                    r.count(NexusBuildPlan.Kind.BLOCKED), r.count(NexusBuildPlan.Kind.CLEAR)).getString();
            case 2 -> Component.translatable("gtna.terminal.nexus.line.missing", r.missing()).getString();
            default -> Component.translatable("gtna.terminal.nexus.line.hatches", r.hatches()).getString();
        };
    }

    private String jobLine() {
        if (player.level().isClientSide || !NexusBuildJobs.isRunning(player)) return "";
        return Component.translatable("gtna.terminal.nexus.job.running").getString();
    }

    private String materialLine(int row) {
        NexusScan.Result r = last();
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
