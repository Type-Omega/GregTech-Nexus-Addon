package com.raishxn.gtna.data.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.data.recipes.FinishedRecipe;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.raishxn.gtna.integration.ae2.wireless.WirelessAeContent;

import java.util.function.Consumer;

/**
 * Recipes of the wireless ME network (D-444). Both are EV assembler recipes: the core needs AE2's quantum
 * chambers, which the AE2 line only reaches after the first quantum bridge, and an EV field generator, which is the
 * tier of GTNA's ME Mini Pattern Buffer (HV) and ME Pattern Buffer (LuV) neighbours in the same ME progression.
 */
public final class GTNAWirelessRecipes {

    private GTNAWirelessRecipes() {}

    public static void register(Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("gtna_wireless_network_core")
                .inputItems(GTMachines.HULL[GTValues.EV].asStack().getItem())
                .inputItems(AEBlocks.QUANTUM_LINK.asItem(), 2)
                .inputItems(AEBlocks.QUANTUM_RING.asItem(), 8)
                .inputItems(CustomTags.EV_CIRCUITS, 4)
                .inputItems(GTItems.FIELD_GENERATOR_EV.asStack().getItem())
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                .outputItems(WirelessAeContent.WIRELESS_NETWORK_CORE.asStack())
                .duration(400)
                .EUt(GTValues.VA[GTValues.EV])
                .save(provider);

        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("gtna_wireless_network_binding_tool")
                .inputItems(AEItems.WIRELESS_RECEIVER.asItem())
                .inputItems(AEItems.ENGINEERING_PROCESSOR.asItem(), 2)
                .inputItems(CustomTags.EV_CIRCUITS)
                .inputItems(GTItems.EMITTER_EV.asStack().getItem())
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(144))
                .outputItems(WirelessAeContent.WIRELESS_NETWORK_BINDING_TOOL.asStack())
                .duration(200)
                .EUt(GTValues.VA[GTValues.EV])
                .save(provider);
    }
}
