package com.raishxn.gtna.common.item.terminal.ui;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.item.terminal.ui.BlockSelectionConfigWidget.BlockCategory;
import com.typeomega.lib.ui.tool.ToolTextures;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Tiers tab: categories on the left, the blocks of the chosen one on the right. A pick makes every compatible
 * cell ask for that block; with Replace on, blocks of the same family in another tier are swapped.
 */
final class NexusTerminalTiersPage {

    private final ItemStack stack;
    private final Map<BlockCategory, WidgetGroup> grids = new EnumMap<>(BlockCategory.class);

    NexusTerminalTiersPage(ItemStack stack) {
        this.stack = stack;
    }

    WidgetGroup build(int width, int height) {
        WidgetGroup page = new WidgetGroup(0, 0, width, height);
        int left = 112;
        var categories = NexusUi.scroll(0, 0, left, height);
        int y = 2;
        for (BlockCategory category : BlockCategory.values()) {
            if (category == BlockCategory.GLASS || category == BlockCategory.LIGHT) continue;
            List<ItemStack> entries = BlockSelectionConfigWidget.getEntries(category);
            if (entries.isEmpty()) continue;
            int row = y;
            categories.addWidget(new ImageWidget(2, row, 18, 18, () -> {
                var selected = BlockSelectionConfigWidget.getSelectedBlock(stack, category);
                return NexusUi.slot(new ItemStackTexture(selected == null ? ItemStack.EMPTY : selected));
            }));
            var select = NexusUi.button(22, row, left - 30, 18, Component.translatable(category.translationKey),
                    cd -> show(category));
            select.setHoverTooltips(Component.translatable(category.translationKey));
            categories.addWidget(select);
            y += 20;
            WidgetGroup grid = grid(category, entries, left + 4, width - left - 4, height);
            grids.put(category, grid);
            page.addWidget(grid);
        }
        page.addWidget(categories);
        show(null);
        return page;
    }

    private void show(BlockCategory selected) {
        grids.forEach((category, group) -> {
            group.setVisible(category == selected);
            group.setActive(category == selected);
        });
    }

    private WidgetGroup grid(BlockCategory category, List<ItemStack> entries, int x0, int width, int height) {
        WidgetGroup group = new WidgetGroup(x0, 0, width, height);
        group.addWidget(NexusUi.label(2, 2, Component.translatable(category.translationKey)));
        var scroll = NexusUi.scroll(0, 14, width, height - 36);
        int perRow = Math.max(1, (width - 8) / 22);
        for (int i = 0; i < entries.size(); i++) {
            ItemStack item = entries.get(i);
            int x = (i % perRow) * 22 + 2, y = (i / perRow) * 22 + 2;
            scroll.addWidget(new ImageWidget(x, y, 20, 20, () -> {
                var selected = BlockSelectionConfigWidget.getSelectedBlock(stack, category);
                boolean on = selected != null && ItemStack.isSameItem(selected, item);
                return on ? ToolTextures.slotSelected(NexusUi.THEME) : NexusUi.slot();
            }));
            scroll.addWidget(new ImageWidget(x + 2, y + 2, 16, 16, new ItemStackTexture(item)));
            var button = new ButtonWidget(x, y, 20, 20, IGuiTexture.EMPTY,
                    cd -> BlockSelectionConfigWidget.select(stack, category, item));
            button.setHoverTooltips(item.getHoverName(), Component.translatable("gtna.terminal.nexus.select_hint"));
            scroll.addWidget(button);
        }
        group.addWidget(scroll);
        group.addWidget(NexusUi.button(0, height - 20, width, 18,
                Component.translatable("gtna.terminal.nexus.clear_choice"),
                cd -> BlockSelectionConfigWidget.clear(stack, category)));
        return group;
    }
}
