package com.raishxn.gtna.common.item.terminal.ui;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;

import com.typeomega.lib.ui.tool.ToolControls;
import com.typeomega.lib.ui.tool.ToolStyle;
import com.typeomega.lib.ui.tool.ToolTextures;
import com.typeomega.lib.ui.tool.ToolTheme;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Shortcuts to the lib's {@link ToolControls} in the Nexus theme (D-447), so the pages of the terminal read as
 * {@code NexusUi.button(...)} and cannot drift to another look by accident.
 */
final class NexusUi {

    static final ToolTheme THEME = ToolTheme.NEXUS_PURPLE;

    private NexusUi() {}

    static LabelWidget label(int x, int y, Component text) {
        return ToolControls.label(THEME, x, y, text);
    }

    static LabelWidget label(int x, int y, Supplier<String> text) {
        return ToolControls.label(THEME, x, y, text);
    }

    static LabelWidget dim(int x, int y, Supplier<String> text) {
        return ToolControls.dimLabel(THEME, x, y, text);
    }

    static ButtonWidget button(int x, int y, int w, int h, Component text, Consumer<ClickData> action) {
        return ToolControls.button(THEME, ToolStyle.NORMAL, x, y, w, h, text, action);
    }

    static ButtonWidget primary(int x, int y, int w, int h, Component text, Consumer<ClickData> action) {
        return ToolControls.button(THEME, ToolStyle.PRIMARY, x, y, w, h, text, action);
    }

    static ButtonWidget success(int x, int y, int w, int h, Component text, Consumer<ClickData> action) {
        return ToolControls.button(THEME, ToolStyle.SUCCESS, x, y, w, h, text, action);
    }

    static ButtonWidget danger(int x, int y, int w, int h, Component text, Consumer<ClickData> action) {
        return ToolControls.button(THEME, ToolStyle.DANGER, x, y, w, h, text, action);
    }

    static WidgetGroup toggle(ToolStyle style, int x, int y, int w, Component text, BooleanSupplier value,
                              Runnable flip) {
        return ToolControls.toggle(THEME, style, x, y, w, text, value, flip);
    }

    /** A recessed area for lists and read-outs. */
    static ImageWidget display(int x, int y, int w, int h) {
        return new ImageWidget(x, y, w, h, ToolTextures.display(THEME));
    }

    /** A slot frame holding an item or a read-out. */
    static IGuiTexture slot() {
        return ToolTextures.slot(THEME);
    }

    static IGuiTexture slot(IGuiTexture content) {
        return new GuiTextureGroup(ToolTextures.slot(THEME), content);
    }

    static DraggableScrollableWidgetGroup scroll(int x, int y, int w, int h) {
        var group = new DraggableScrollableWidgetGroup(x, y, w, h);
        group.setYScrollBarWidth(4);
        group.setBackground(ToolTextures.display(THEME));
        return group;
    }

    /** The titled panel of a page: {@code build} fills its content area, which starts under the title rule. */
    static WidgetGroup pane(int x, int y, int w, int h, Component title, Consumer<WidgetGroup> build) {
        var pane = ToolControls.pane(THEME, x, y, w, h, title);
        var content = new WidgetGroup(ToolControls.PANE_PAD, ToolControls.PANE_TOP, w - 2 * ToolControls.PANE_PAD,
                h - ToolControls.PANE_TOP - ToolControls.PANE_PAD);
        build.accept(content);
        pane.addWidget(content);
        return pane;
    }
}
