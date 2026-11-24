package com.raishxn.gtna.data.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.registries.ForgeRegistries;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAMaterials;
import com.raishxn.gtna.common.data.GTNANaquadahReactors;
import com.raishxn.gtna.common.data.GTNANaquadahRecipeTypes;
import com.raishxn.gtna.common.machine.multiblock.electric.HyperReactorMachine;

import java.util.function.Consumer;

import static com.raishxn.gtna.common.data.material.NaquadahFuelMaterials.EnrichedNaquadahFuel;
import static com.raishxn.gtna.common.data.material.NaquadahFuelMaterials.NaquadahFuel;

/**
 * Default recipes of the naquadah reactor ladder: the two fuels (GTOCore {@code AtomicEnergyExcitation} inputs, made in
 * GTCEu's Large Chemical Reactor instead of GTO's own machine), the reactor fuels (GTOCore {@code FuelRecipe} for the
 * Large Naquadah Reactor) and the controllers. Packs replace them by id.
 */
public final class GTNANaquadahReactorRecipes {

    /** Fuel and hydrogen per run in both reactors. */
    public static final int FUEL_PER_RUN = 16;
    public static final int HYDROGEN_PER_RUN = 80;
    /** Run time of Naquadah Fuel and Enriched Naquadah Fuel, in ticks. */
    public static final int NAQUADAH_DURATION = 875;
    public static final int ENRICHED_DURATION = 1250;
    /** Large reactor plasma recipes: ten times the fuel at UV, 32 times as long (GTO). */
    public static final int PLASMA_FUEL_PER_RUN = 160;
    public static final int PLASMA_PER_RUN = 40;
    public static final int PLASMA_DURATION_FACTOR = 32;
    public static final int LARGE_TIER = GTValues.UHV;
    public static final int LARGE_PLASMA_TIER = GTValues.UV;
    public static final int HYPER_TIER = GTValues.UEV;
    public static final int OXYGEN_BOOST = 8;
    public static final int NITROGEN_BOOST = 16;

    private GTNANaquadahReactorRecipes() {}

    public static void register(Consumer<FinishedRecipe> provider) {
        fuels(provider);
        largeReactor(provider);
        hyperReactor(provider);
        controllers(provider);
    }

    private static void fuels(Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.LARGE_CHEMICAL_RECIPES.recipeBuilder(GTNACORE.id("naquadah_fuel"))
                .inputItems(TagPrefix.dust, GTMaterials.Naquadah, 16)
                .inputFluids(GTMaterials.Hydrogen.getFluid(30000))
                .inputFluids(GTMaterials.Nitrogen.getFluid(15000))
                .inputFluids(GTMaterials.Fluorine.getFluid(8000))
                .inputFluids(GTMaterials.NitricAcid.getFluid(8000))
                .outputFluids(NaquadahFuel.getFluid(20000))
                .EUt(GTValues.VA[GTValues.UV]).duration(4000).save(provider);
        GTRecipeTypes.LARGE_CHEMICAL_RECIPES.recipeBuilder(GTNACORE.id("enriched_naquadah_fuel"))
                .inputItems(TagPrefix.dust, GTMaterials.NaquadahEnriched, 16)
                .inputItems(TagPrefix.dust, GTMaterials.Antimony, 4)
                .inputFluids(GTMaterials.Hydrogen.getFluid(48000))
                .inputFluids(GTMaterials.Nitrogen.getFluid(30000))
                .inputFluids(GTMaterials.Fluorine.getFluid(12000))
                .inputFluids(GTMaterials.Xenon.getFluid(8000))
                .inputFluids(GTMaterials.Radon.getFluid(6000))
                .outputFluids(EnrichedNaquadahFuel.getFluid(20000))
                .EUt(GTValues.VA[GTValues.UV]).duration(4000).save(provider);
    }

    /** GTO values: hydrogen gives UHV power; a plasma burns ten times the fuel at UV for 32 times as long. */
    private static void largeReactor(Consumer<FinishedRecipe> provider) {
        var type = GTNANaquadahRecipeTypes.LARGE_NAQUADAH_REACTOR_RECIPES;
        type.recipeBuilder(GTNACORE.id("naquadah_fuel"))
                .inputFluids(NaquadahFuel.getFluid(FUEL_PER_RUN))
                .inputFluids(GTMaterials.Hydrogen.getFluid(HYDROGEN_PER_RUN))
                .duration(NAQUADAH_DURATION).EUt(-GTValues.V[LARGE_TIER]).save(provider);
        type.recipeBuilder(GTNACORE.id("enriched_naquadah_fuel"))
                .inputFluids(EnrichedNaquadahFuel.getFluid(FUEL_PER_RUN))
                .inputFluids(GTMaterials.Hydrogen.getFluid(HYDROGEN_PER_RUN))
                .duration(ENRICHED_DURATION).EUt(-GTValues.V[LARGE_TIER]).save(provider);
        type.recipeBuilder(GTNACORE.id("naquadah_fuel_oxygen_plasma"))
                .inputFluids(NaquadahFuel.getFluid(PLASMA_FUEL_PER_RUN))
                .inputFluids(GTMaterials.Oxygen.getFluid(FluidStorageKeys.PLASMA, PLASMA_PER_RUN))
                .duration(PLASMA_DURATION_FACTOR * NAQUADAH_DURATION).EUt(-GTValues.V[LARGE_PLASMA_TIER])
                .save(provider);
        type.recipeBuilder(GTNACORE.id("enriched_naquadah_fuel_nitrogen_plasma"))
                .inputFluids(EnrichedNaquadahFuel.getFluid(PLASMA_FUEL_PER_RUN))
                .inputFluids(GTMaterials.Nitrogen.getFluid(FluidStorageKeys.PLASMA, PLASMA_PER_RUN))
                .duration(PLASMA_DURATION_FACTOR * ENRICHED_DURATION).EUt(-GTValues.V[LARGE_PLASMA_TIER])
                .save(provider);
    }

    // EN: Same fuel and duration as the Large reactor at UEV: four times the EU per mB, which makes it the best
    // naquadah reactor. Boosters multiply the run (HyperReactorMachine) without changing the EU per mB.
    // PT: Mesmo combustível e duração do Large, em UEV: quatro vezes o EU por mB, o que o torna o melhor reator de
    // naquadah. Os reforços multiplicam a operação (HyperReactorMachine) sem mudar o EU por mB.
    // ZH: 与大型反应堆相同的燃料和时长，但在 UEV 运行：每 mB 产能为 4 倍，因此是最强的硅岩反应堆。增幅只倍增运行次数
    // （HyperReactorMachine），不改变每 mB 的产能。
    private static void hyperReactor(Consumer<FinishedRecipe> provider) {
        var type = GTNANaquadahRecipeTypes.NAQUADAH_HYPER_REACTOR_RECIPES;
        type.recipeBuilder(GTNACORE.id("naquadah_fuel"))
                .inputFluids(NaquadahFuel.getFluid(FUEL_PER_RUN))
                .inputFluids(GTMaterials.Hydrogen.getFluid(HYDROGEN_PER_RUN))
                .duration(NAQUADAH_DURATION).EUt(-GTValues.V[HYPER_TIER])
                .addData(HyperReactorMachine.BOOSTERS, boosters())
                .save(provider);
        type.recipeBuilder(GTNACORE.id("enriched_naquadah_fuel"))
                .inputFluids(EnrichedNaquadahFuel.getFluid(FUEL_PER_RUN))
                .inputFluids(GTMaterials.Hydrogen.getFluid(HYDROGEN_PER_RUN))
                .duration(ENRICHED_DURATION).EUt(-GTValues.V[HYPER_TIER])
                .addData(HyperReactorMachine.BOOSTERS, boosters())
                .save(provider);
    }

    private static ListTag boosters() {
        var list = new ListTag();
        list.add(booster(GTMaterials.Oxygen, OXYGEN_BOOST));
        list.add(booster(GTMaterials.Nitrogen, NITROGEN_BOOST));
        return list;
    }

    private static CompoundTag booster(com.gregtechceu.gtceu.api.data.chemical.material.Material material,
                                       int parallel) {
        var tag = new CompoundTag();
        tag.putString("fluid", ForgeRegistries.FLUIDS.getKey(material.getFluid(FluidStorageKeys.PLASMA)).toString());
        tag.putInt("amount", PLASMA_PER_RUN);
        tag.putInt("parallel", parallel);
        return tag;
    }

    private static void controllers(Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(GTNACORE.id("extreme_strength_tritanium_casing"))
                .inputItems(TagPrefix.frameGt, GTMaterials.Tritanium)
                .inputItems(TagPrefix.plate, GTMaterials.Tritanium, 6)
                .circuitMeta(6)
                .outputItems(GTNANaquadahReactors.EXTREME_STRENGTH_TRITANIUM_CASING.asStack())
                .EUt(16).duration(50).save(provider);
        // GTO assembly line; GTO researches its single-block naquadah reactor, which GTNA does not have.
        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder(GTNACORE.id("large_naquadah_reactor"))
                .inputItems(GTItems.HIGH_POWER_INTEGRATED_CIRCUIT.asStack(64))
                .inputItems(GTItems.ULTRA_HIGH_POWER_INTEGRATED_CIRCUIT.asStack(64))
                .inputItems(TagPrefix.wireGtSingle, GTMaterials.UraniumRhodiumDinaquadide, 32)
                .inputItems(TagPrefix.foil, GTMaterials.NaquadahEnriched, 24)
                .inputItems(TagPrefix.gear, GTMaterials.Duranium, 16)
                .inputItems(TagPrefix.plateDouble, GTMaterials.Duranium, 8)
                .inputItems(TagPrefix.plateDouble, GTMaterials.Naquadria, 8)
                .inputItems(GTItems.ELECTRIC_PISTON_UV.asStack(2))
                .inputItems(GTItems.FIELD_GENERATOR_UV.asStack())
                .inputItems(GTItems.ELECTRIC_PUMP_UV.asStack())
                .inputItems(CustomTags.UHV_CIRCUITS, 4)
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(5760))
                .inputFluids(GTNAMaterials.Indalloy140.getFluid(2304))
                .inputFluids(GTMaterials.Trinium.getFluid(2304))
                .outputItems(GTNANaquadahReactors.LARGE_NAQUADAH_REACTOR.asStack())
                .EUt(90000).duration(1000)
                .stationResearch(b -> b.researchStack(GTItems.FIELD_GENERATOR_UV.asStack()).CWUt(64))
                .save(provider);
        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder(GTNACORE.id("naquadah_hyper_reactor"))
                .inputItems(GTNANaquadahReactors.LARGE_NAQUADAH_REACTOR.asStack(4))
                .inputItems(GTItems.ULTRA_HIGH_POWER_INTEGRATED_CIRCUIT.asStack(64))
                .inputItems(CustomTags.UEV_CIRCUITS, 8)
                .inputItems(GTItems.FIELD_GENERATOR_UEV.asStack(4))
                .inputItems(GTItems.ELECTRIC_PUMP_UEV.asStack(4))
                .inputItems(TagPrefix.plateDouble, GTMaterials.Naquadria, 16)
                .inputItems(TagPrefix.plateDouble, GTMaterials.NaquadahAlloy, 32)
                .inputItems(TagPrefix.gear, GTMaterials.Neutronium, 8)
                .inputItems(TagPrefix.foil, GTMaterials.NaquadahEnriched, 64)
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(9216))
                .inputFluids(GTNAMaterials.Indalloy140.getFluid(4608))
                .inputFluids(EnrichedNaquadahFuel.getFluid(16000))
                .outputItems(GTNANaquadahReactors.NAQUADAH_HYPER_REACTOR.asStack())
                .EUt(GTValues.VA[GTValues.UEV]).duration(2400)
                .stationResearch(b -> b.researchStack(GTNANaquadahReactors.LARGE_NAQUADAH_REACTOR.asStack())
                        .CWUt(512))
                .save(provider);
    }
}
