package com.raishxn.gtna.common.item.terminal.ui;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.item.terminal.plan.NexusHatchPlan;
import com.raishxn.gtna.common.item.terminal.plan.NexusHatchPlan.Function;
import com.typeomega.lib.ui.tool.ToolTextures;

import java.util.List;

/**
 * The Hatches tab (D-439 T7): one row per function; the arrows step through Automatic, None and every hatch of
 * that function (type and tier). The pattern's hatch limits still decide where hatches go.
 */
final class NexusTerminalHatchPage {

    private NexusTerminalHatchPage() {}

    static WidgetGroup build(ItemStack stack, int cw, int ch) {
        return NexusUi.pane(0, 0, cw, ch, Component.translatable("gtna.terminal.nexus.ui.pane.hatches"),
                page -> fill(stack, page));
    }

    /** Nine rows of 20 px in a scrolling list: the pane has room for about seven. */
    private static void fill(ItemStack stack, WidgetGroup page) {
        int width = page.getSizeWidth() - 4;
        var list = NexusUi.scroll(0, 0, page.getSizeWidth(), page.getSizeHeight());
        int y = 2;
        for (Function function : Function.values()) {
            list.addWidget(NexusUi.label(4, y + 5, Component.translatable(function.key())));
            list.addWidget(new ImageWidget(100, y, 18, 18, () -> NexusUi.slot(new ItemStackTexture(icon(stack,
                    function)))));
            list.addWidget(NexusUi.button(122, y, 16, 18, Component.literal("<"),
                    cd -> step(stack, function, cd.isShiftClick ? -10 : -1)));
            list.addWidget(NexusUi.button(width - 18, y, 16, 18, Component.literal(">"),
                    cd -> step(stack, function, cd.isShiftClick ? 10 : 1)));
            list.addWidget(new ImageWidget(142, y, width - 162, 18, () -> new GuiTextureGroup(
                    ToolTextures.display(NexusUi.THEME), new TextTexture(name(stack, function))
                            .setWidth(width - 166).setType(TextTexture.TextType.ROLL))));
            y += 20;
        }
        page.addWidget(list);
    }

    private static ItemStack icon(ItemStack stack, Function function) {
        String pick = NexusHatchPlan.read(stack).pick(function);
        if (pick == null || pick.equals(NexusHatchPlan.NONE)) return ItemStack.EMPTY;
        var id = ResourceLocation.tryParse(pick);
        return id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    private static String name(ItemStack stack, Function function) {
        String pick = NexusHatchPlan.read(stack).pick(function);
        if (pick == null) return Component.translatable("gtna.terminal.nexus.hatch.auto").getString();
        if (pick.equals(NexusHatchPlan.NONE))
            return Component.translatable("gtna.terminal.nexus.hatch.none").getString();
        return icon(stack, function).getHoverName().getString();
    }

    /** Index -2 is Automatic, -1 None, then the hatches of the function. */
    static void step(ItemStack stack, Function function, int by) {
        List<ItemStack> entries = NexusHatchPlan.entries(function);
        String pick = NexusHatchPlan.read(stack).pick(function);
        int index = -2;
        if (NexusHatchPlan.NONE.equals(pick)) index = -1;
        else if (pick != null) {
            for (int i = 0; i < entries.size(); i++) {
                if (BuiltInRegistries.ITEM.getKey(entries.get(i).getItem()).toString().equals(pick)) index = i;
            }
        }
        int size = entries.size() + 2;
        int next = Math.floorMod(index + 2 + by, size) - 2;
        NexusHatchPlan.set(stack, function, next == -2 ? null : next == -1 ? NexusHatchPlan.NONE :
                BuiltInRegistries.ITEM.getKey(entries.get(next).getItem()).toString());
    }
}
