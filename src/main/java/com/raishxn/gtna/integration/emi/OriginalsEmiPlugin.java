package com.raishxn.gtna.integration.emi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAOriginals;
import com.raishxn.gtna.integration.originals.OriginalsPages;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Native EMI categories of the four original multiblocks (D-435, U4/U5): Antimatter Collider Ring, Dimensional
 * Rift Gate, Kinetic Orbital Lance and Stellar Containment Chamber. Their GT recipe type is a dummy, so without these
 * pages they were missing from EMI.
 */
@EmiEntrypoint
public final class OriginalsEmiPlugin implements EmiPlugin {

    /** Machine that is each category's icon and workstation. */
    public static ItemStack machine(ResourceLocation category) {
        if (category.equals(OriginalsPages.COLLIDER)) return GTNAOriginals.ANTIMATTER_COLLIDER_RING.asStack();
        if (category.equals(OriginalsPages.RIFT)) return GTNAOriginals.DIMENSIONAL_RIFT_GATE.asStack();
        if (category.equals(OriginalsPages.LANCE)) return GTNAOriginals.KINETIC_ORBITAL_LANCE.asStack();
        return GTNAOriginals.STELLAR_CONTAINMENT_CHAMBER.asStack();
    }

    /** Opens one of the categories in EMI (dev screenshots). */
    public static void show(ResourceLocation category) {
        for (var c : dev.emi.emi.api.EmiApi.getRecipeManager().getCategories())
            if (c.getId().equals(category)) dev.emi.emi.api.EmiApi.displayRecipeCategory(c);
    }

    @Override
    public void register(EmiRegistry registry) {
        Map<ResourceLocation, EmiRecipeCategory> categories = new LinkedHashMap<>();
        for (var id : OriginalsPages.CATEGORIES) {
            var icon = EmiStack.of(machine(id));
            var category = new EmiRecipeCategory(id, icon);
            categories.put(id, category);
            registry.addCategory(category);
            registry.addWorkstation(category, icon);
        }
        for (var page : OriginalsPages.all()) {
            try {
                registry.addRecipe(new OriginalsEmiRecipe(page, categories.get(page.category())));
            } catch (RuntimeException broken) {
                GTNACORE.LOGGER.error("Original EMI page {} could not be built", page.id(), broken);
            }
        }
    }
}
