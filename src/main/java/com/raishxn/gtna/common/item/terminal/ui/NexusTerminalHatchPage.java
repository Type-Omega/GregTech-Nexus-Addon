package com.raishxn.gtna.common.item.terminal.ui;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.item.terminal.plan.NexusHatchPlan;
import com.raishxn.gtna.common.item.terminal.plan.NexusHatchPlan.Function;
import com.typeomega.lib.ui.tool.ToolWindow;

import java.util.List;

/**
 * The Hatches tab (GTIA-D-439 T7): one row per function; the arrows step through Automatic, None and every hatch of
 * that function (type and tier). The pattern's hatch limits still decide where hatches go.
 */
final class NexusTerminalHatchPage {

    private NexusTerminalHatchPage() {}

    static WidgetGroup build(ItemStack stack, int width, int height) {
        WidgetGroup page = new WidgetGroup(0, 0, width, height);
        page.addWidget(ToolWindow.label(0, 0, Component.translatable("gtna.terminal.nexus.hatch.header")));
        int y = 14;
        for (Function function : Function.values()) {
            page.addWidget(ToolWindow.label(0, y + 5, Component.translatable(function.key())));
            page.addWidget(new ImageWidget(110, y, 18, 18, () -> new GuiTextureGroup(GuiTextures.SLOT,
                    new ItemStackTexture(icon(stack, function)))));
            page.addWidget(ToolWindow.button(132, y, 16, 18, Component.literal("<"),
                    cd -> step(stack, function, cd.isShiftClick ? -10 : -1)));
            page.addWidget(ToolWindow.button(width - 16, y, 16, 18, Component.literal(">"),
                    cd -> step(stack, function, cd.isShiftClick ? 10 : 1)));
            page.addWidget(new ImageWidget(152, y, width - 172, 18, () -> new GuiTextureGroup(GuiTextures.DISPLAY,
                    new com.lowdragmc.lowdraglib.gui.texture.TextTexture(name(stack, function))
                            .setWidth(width - 176).setType(
                                    com.lowdragmc.lowdraglib.gui.texture.TextTexture.TextType.ROLL))));
            y += 20;
        }
        return page;
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
