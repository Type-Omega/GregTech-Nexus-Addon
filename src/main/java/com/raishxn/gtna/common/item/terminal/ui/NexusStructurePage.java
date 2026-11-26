package com.raishxn.gtna.common.item.terminal.ui;

import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.item.terminal.plan.NexusDismantle;
import com.typeomega.lib.ui.tool.ToolControls;
import com.typeomega.lib.ui.tool.ToolStyle;
import com.typeomega.lib.ui.tool.ToolTextures;

/**
 * The Structure step (D-447): repetitions and modules, the six switches and, under them, the last scan or, while
 * Dismantle is on, the red warning with the preview of what it would take. Dismantle stays a switch here; there is
 * no tab of its own.
 */
final class NexusStructurePage {

    static final String[][] TOGGLES = { { "ReplaceMode", "gtna.terminal.nexus.replace_mode" },
            { "DemolitionMode", "gtna.terminal.nexus.demolition_mode" },
            { NexusDismantle.MODE_KEY, "gtna.terminal.nexus.dismantle_mode" },
            { "UseAE", "gtna.terminal.nexus.use_ae" }, { "MirrorBuild", "gtna.terminal.nexus.mirror_build" },
            { "NoHatchMode", "gtna.terminal.nexus.no_hatch" } };

    private NexusStructurePage() {}

    static WidgetGroup build(Player player, ItemStack stack, int cw, int ch) {
        return NexusUi.pane(0, 0, cw, ch, Component.translatable("gtna.terminal.nexus.ui.pane.structure"),
                page -> fill(player, stack, page));
    }

    private static void fill(Player player, ItemStack stack, WidgetGroup page) {
        int w = page.getSizeWidth(), h = page.getSizeHeight();
        int half = (w - 4) / 2;
        page.addWidget(ToolControls.number(NexusUi.THEME, 0, 0, half, Component.translatable(
                "gtna.terminal.nexus.repetitions"), 0, 1000, () -> stack.getOrCreateTag().getInt("Repetitions"),
                v -> stack.getOrCreateTag().putInt("Repetitions", v)));
        page.addWidget(ToolControls.number(NexusUi.THEME, half + 4, 0, half, Component.translatable(
                "gtna.terminal.nexus.module_build"), 0, 100, () -> stack.getOrCreateTag().getInt("ModuleBuild"),
                v -> stack.getOrCreateTag().putInt("ModuleBuild", v)));
        for (int i = 0; i < TOGGLES.length; i++) {
            String key = TOGGLES[i][0];
            ToolStyle style = key.equals(NexusDismantle.MODE_KEY) ? ToolStyle.DANGER : ToolStyle.NORMAL;
            var toggle = NexusUi.toggle(style, (i % 2) * (half + 4), 34 + (i / 2) * 17, half,
                    Component.translatable(TOGGLES[i][1]), () -> stack.getOrCreateTag().getBoolean(key),
                    () -> stack.getOrCreateTag().putBoolean(key, !stack.getOrCreateTag().getBoolean(key)));
            toggle.setHoverTooltips(Component.translatable(TOGGLES[i][1] + ".tooltip"));
            page.addWidget(toggle);
        }
        int top = 34 + 3 * 17 + 3, panelH = h - top;
        var calm = ToolTextures.display(NexusUi.THEME);
        var alert = ToolTextures.alert(NexusUi.THEME);
        page.addWidget(new ImageWidget(0, top, w, panelH,
                () -> NexusTerminalInfo.dismantleOn(stack) ? alert : calm));
        for (int line = 0; line < NexusDismantleView.LINES; line++) {
            int index = line;
            page.addWidget(NexusUi.label(5, top + 4 + 10 * line, () -> NexusTerminalInfo.panelLine(player, stack,
                    index)));
        }
    }
}
