package com.raishxn.gtna.integration.emi;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.resources.ResourceLocation;

import com.raishxn.gtna.integration.originals.OriginalsPages;
import com.raishxn.gtna.integration.originals.OriginalsPages.Content;
import com.raishxn.gtna.integration.originals.OriginalsPages.Page;
import com.typeomega.lib.ui.DisplaySlots;
import com.typeomega.lib.ui.IllustratedPanel;
import com.typeomega.lib.ui.InfoLinesWidget;
import com.typeomega.lib.ui.PanelLayout;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One illustrated page of an original multiblock in EMI (GTIA-D-435, U5): the lib's panel with the art of the
 * machine, read-only slots placed in the drawing and the lines from the core. LDLib's {@link ModularEmiRecipe} reads
 * the inputs, outputs and catalysts from the slots, so "uses" and "recipes" lookups find the page.
 */
public final class OriginalsEmiRecipe extends ModularEmiRecipe<Widget> {

    private final Page page;
    private final EmiRecipeCategory category;

    public OriginalsEmiRecipe(Page page, EmiRecipeCategory category) {
        super(() -> panel(page));
        this.page = page;
        this.category = category;
    }

    /** The panel of a page; the same widgets a machine tab would use. */
    public static IllustratedPanel panel(Page page) {
        var lines = OriginalsPages.lineCount(page.category());
        var panel = new IllustratedPanel(page.layout(), lines);
        var slots = page.layout().slots();
        panel.slots(slot -> widget(slot, page.contents().get(slots.indexOf(slot))));
        panel.flows();
        panel.lines(new InfoLinesWidget(page.lines()));
        return panel;
    }

    /** The slot of one content; none for an empty one, as GTNH shows only what the page uses. */
    private static @Nullable Widget widget(PanelLayout.Slot slot, Content content) {
        if (content.isEmpty()) return null;
        if (slot.kind() == PanelLayout.Kind.FLUID) {
            List<FluidStack> fluids = content.fluid() == null ? List.of() :
                    List.of(FluidStack.create(content.fluid(), content.amount()));
            return DisplaySlots.fluid(fluids, slot.role());
        }
        return content.chance() < 1 ? DisplaySlots.item(content.items(), slot.role(), content.chance()) :
                DisplaySlots.item(content.items(), slot.role());
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return page.category().withPath(page.category().getPath() + "/" + page.id());
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }
}
