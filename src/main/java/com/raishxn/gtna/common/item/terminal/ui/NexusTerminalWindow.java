package com.raishxn.gtna.common.item.terminal.ui;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.item.terminal.NexusAutoBuilder;
import com.raishxn.gtna.common.item.terminal.plan.NexusBuildJobs;
import com.raishxn.gtna.common.item.terminal.plan.NexusFamilySwap;
import com.raishxn.gtna.common.item.terminal.plan.NexusReport;
import com.raishxn.gtna.common.item.terminal.plan.NexusScan;
import com.typeomega.lib.ui.tool.SyncedFlag;
import com.typeomega.lib.ui.tool.ToolControls;
import com.typeomega.lib.ui.tool.ToolStyle;
import com.typeomega.lib.ui.tool.ToolTextures;
import com.typeomega.lib.ui.tool.ToolWindow;

import java.util.function.Consumer;

/**
 * The Nexus Terminal window (D-439, console layout D-440, look D-447): the lib's {@link ToolWindow} in the
 * {@code NEXUS_PURPLE} theme, 420 by 236, with a title bar (target and mode), the Engineering Console (steps, the
 * step's page, a fixed action bar), the nearby machines and the saved structures. Buttons act on the controller of
 * the last scan; the texts the server owns are synced labels ({@link NexusTerminalInfo}).
 * <p>
 * EN: text sizes stay at the font's own 8 px with 2 px or more around them, so the layout reads at GUI scale 3 and 4.
 * PT: o texto fica no tamanho próprio da fonte (8 px), com 2 px ou mais em volta, para ler na escala de GUI 3 e 4.
 * ZH: 文字保持字体自身的 8 像素，并留出至少 2 像素的间距，以便在 GUI 缩放 3 和 4 下阅读。
 */
public final class NexusTerminalWindow {

    public static final int WIDTH = 420, HEIGHT = 236;
    static final int STEP_W = 94, BAR_H = 24;

    private final Player player;
    private final ItemStack stack;

    public NexusTerminalWindow(Player player, ItemStack stack) {
        this.player = player;
        this.stack = stack;
    }

    public ToolWindow build() {
        ToolWindow window = new ToolWindow(WIDTH, HEIGHT, NexusUi.THEME);
        window.header(new ItemStack(stack.getItem()), Component.translatable("gtna.terminal.nexus.ui.title"),
                () -> NexusTerminalInfo.targetLine(player), () -> NexusTerminalInfo.badge(player, stack),
                () -> NexusTerminalInfo.dismantleOn(stack));
        window.tab(Component.translatable("gtna.terminal.nexus.tab.build"), consolePage(window));
        window.tab(Component.translatable("gtna.terminal.nexus.tab.machines"),
                NexusTerminalSidePages.machines(player, stack, window.pageWidth(), window.pageHeight()));
        window.tab(Component.translatable("gtna.terminal.nexus.tab.blueprints"),
                NexusTerminalSidePages.blueprints(player, stack, window.pageWidth(), window.pageHeight()));
        return window;
    }

    /**
     * The Engineering Console (D-440): numbered steps on the left (structure, tiers, hatches, materials) with the ME
     * state under them, the step's page in the middle and a fixed action bar at the bottom.
     */
    private WidgetGroup consolePage(ToolWindow window) {
        int pw = window.pageWidth(), ph = window.pageHeight();
        int cw = pw - STEP_W - 4, ch = ph - BAR_H - 4;
        WidgetGroup page = new WidgetGroup(0, 0, pw, ph);
        WidgetGroup[] steps = { NexusStructurePage.build(player, stack, cw, ch), tiersStep(cw, ch),
                NexusTerminalHatchPage.build(stack, cw, ch), NexusMaterialsPage.build(player, stack, cw, ch) };
        String[] names = { "gtna.terminal.nexus.step.structure", "gtna.terminal.nexus.step.tiers",
                "gtna.terminal.nexus.step.hatches", "gtna.terminal.nexus.step.materials" };
        int[] active = { 0 };
        page.addWidget(new ImageWidget(0, 0, STEP_W, ch, ToolTextures.panel(NexusUi.THEME)));
        for (int i = 0; i < steps.length; i++) {
            int index = i;
            steps[i].setSelfPosition(STEP_W + 4, 0);
            steps[i].setVisible(i == 0);
            steps[i].setActive(i == 0);
            page.addWidget(steps[i]);
            page.addWidget(ToolControls.step(NexusUi.THEME, 4, 4 + i * 23, STEP_W - 8, 20, i + 1,
                    Component.translatable(names[i]), () -> active[0] == index, () -> {
                        active[0] = index;
                        for (int k = 0; k < steps.length; k++) {
                            steps[k].setVisible(k == index);
                            steps[k].setActive(k == index);
                        }
                    }));
        }
        int meY = ch - 30;
        page.addWidget(NexusUi.display(4, meY, STEP_W - 8, 26));
        page.addWidget(NexusUi.dim(8, meY + 4, () -> Component.translatable("gtna.terminal.nexus.me").getString()));
        page.addWidget(NexusUi.label(8, meY + 15, () -> NexusTerminalInfo.meShort(player, stack)));
        actionBar(page, pw, ph);
        return page;
    }

    /** The fixed bar at the bottom: the scan summary on the left, the four actions on the right. */
    private void actionBar(WidgetGroup page, int pw, int ph) {
        int by = ph - BAR_H;
        page.addWidget(new ImageWidget(0, by, pw, BAR_H, ToolTextures.bar(NexusUi.THEME)));
        page.addWidget(NexusUi.label(6, by + 3, () -> NexusTerminalInfo.scanLine(player, 0)));
        page.addWidget(NexusUi.label(6, by + 13, () -> NexusTerminalInfo.scanLine(player, 2)));
        int bw = 56, bx = pw - 4 * (bw + 2) - 1, y = by + 3;
        page.addWidget(NexusUi.button(bx, y, bw, 18, Component.translatable("gtna.terminal.nexus.scan"), cd -> {
            if (!cd.isRemote) onController(c -> NexusScan.scanAndShow((ServerPlayer) player, c, stack));
        }));
        page.addWidget(NexusUi.primary(bx + bw + 2, y, bw, 18, Component.translatable("gtna.terminal.nexus.build"),
                cd -> {
                    if (!cd.isRemote) onController(c -> NexusAutoBuilder.autoBuild(player, c, stack));
                }));
        page.addWidget(ToolControls.button(NexusUi.THEME, ToolStyle.DANGER,
                bx + 2 * (bw + 2), y, bw, 18, Component.translatable("gtna.terminal.nexus.cancel"),
                new SyncedFlag(() -> NexusTerminalInfo.jobRunning(player)), cd -> {
                    if (!cd.isRemote) NexusBuildJobs.cancel(player);
                }));
        page.addWidget(NexusUi.button(bx + 3 * (bw + 2), y, bw, 18,
                Component.translatable("gtna.terminal.nexus.report.button"), cd -> {
                    if (!cd.isRemote && player instanceof ServerPlayer sp && !NexusReport.send(sp)) {
                        sp.displayClientMessage(Component.translatable("gtna.terminal.nexus.report.none"), true);
                    }
                }));
    }

    /** Tiers step: the category picker, and the tier swap of the families the scanned structure holds. */
    private WidgetGroup tiersStep(int cw, int ch) {
        var pane = NexusUi.pane(0, 0, cw, ch, Component.translatable("gtna.terminal.nexus.ui.pane.tiers"), page -> {
            int w = page.getSizeWidth(), h = page.getSizeHeight();
            page.addWidget(new NexusTerminalTiersPage(stack).build(w, h - 36));
            page.addWidget(NexusUi.dim(1, h - 32, () -> familyLine()));
            page.addWidget(NexusUi.primary(0, h - 18, w, 18, Component.translatable("gtna.terminal.nexus.family.swap"),
                    cd -> {
                        if (!cd.isRemote) swapFamilies();
                    }));
        });
        pane.addWidget(NexusUi.button(cw - ToolControls.PANE_PAD - 104, 2, 104, 14,
                Component.translatable("gtna.terminal.nexus.clear_all"),
                cd -> BlockSelectionConfigWidget.clearAll(stack)));
        return pane;
    }

    /** Swaps every family of the last scan whose pick differs from the tier in the world. */
    private void swapFamilies() {
        NexusScan.Result r = NexusTerminalInfo.last(player);
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
        NexusScan.Result r = NexusTerminalInfo.last(player);
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

    private void onController(Consumer<IMultiController> action) {
        IMultiController controller = NexusTerminalInfo.target(player);
        if (controller == null) {
            player.displayClientMessage(Component.translatable("gtna.terminal.nexus.report.none"), true);
            return;
        }
        action.accept(controller);
    }
}
