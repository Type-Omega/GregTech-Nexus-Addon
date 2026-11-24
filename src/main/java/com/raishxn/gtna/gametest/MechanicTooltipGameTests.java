package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.api.machine.feature.eyeofharmony.EyeOfHarmonyMath;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeData;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeMath;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeModuleStats;
import com.raishxn.gtna.common.data.tooltip.GTNATooltips;
import com.raishxn.gtna.common.machine.multiblock.electric.AdvancedFusionReactorMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.BlazeBlastFurnaceMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.CoilWorkableElectricMultipleRecipesMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.GTNALargeTurbineMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.MegaAlloyBlastSmelterMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.PlasmaForgeMachine;
import com.raishxn.gtna.common.machine.multiblock.energy.GeneratorArrayMachine;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamFlightModule;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamOilDrillModule;
import com.raishxn.gtna.common.machine.multiblock.noenergy.DimensionallyTranscendentDirtForgeMachine;
import com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The mechanic lines of the GTNH-style tooltips (GTIA-D-432): each migrated machine keeps at least as many lines as
 * the GTNH or GTNL original gives, and a few numbers are checked against the code that applies them.
 */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class MechanicTooltipGameTests {

    private static final String TEMPLATE = "empty_12";

    /** Minimum mechanic lines per machine: the count of the original's mechanic lines, not counting structure. */
    static final Map<String, Integer> MINIMUM = new LinkedHashMap<>();

    static {
        MINIMUM.put("eye_of_harmony", 22);
        MINIMUM.put("mega_alloy_blast_smelter", 7);
        MINIMUM.put("forge_of_gods", 16);
        MINIMUM.put("godforge_smelting_module", 13);
        MINIMUM.put("godforge_molten_module", 13);
        MINIMUM.put("godforge_plasma_module", 11);
        MINIMUM.put("godforge_exotic_module", 12);
        MINIMUM.put("nano_forge", 12);
        MINIMUM.put("dimensionally_transcendent_plasma_forge", 12);
        MINIMUM.put("transcendent_plasma_mixer", 9);
        MINIMUM.put("eye_of_wood", 12);
        MINIMUM.put("bec_assembler", 9);
        MINIMUM.put("bec_storage", 8);
        MINIMUM.put("bec_generator", 6);
        MINIMUM.put("bec_io_node", 15);
        MINIMUM.put("bec_diode", 7);
        MINIMUM.put("large_naquadah_reactor", 9);
        MINIMUM.put("naquadah_hyper_reactor", 13);
        MINIMUM.put("component_assembly_line", 8);
        MINIMUM.put("advanced_fusion_reactor", 15);
        MINIMUM.put("steam_elevator", 7);
        for (String t : new String[] { "i", "ii", "iii" }) {
            MINIMUM.put("steam_elevator_flight_module_" + t, 8);
            MINIMUM.put("steam_elevator_weather_module_" + t, 7);
            MINIMUM.put("steam_elevator_oil_drill_module_" + t, 9);
            MINIMUM.put("steam_elevator_entity_crusher_module_" + t, 10);
            MINIMUM.put("steam_elevator_monster_repellent_module_" + t, 6);
            MINIMUM.put("steam_elevator_beacon_module_" + t, 10);
        }
        MINIMUM.put("steam_elevator_greenhouse_module", 8);
        MINIMUM.put("steam_elevator_ore_processor_module", 10);
        MINIMUM.put("steam_elevator_apiary_module", 8);
        MINIMUM.put("steam_elevator_bee_breeding_module", 9);
        for (String id : new String[] { "cracker_hub", "crystal_builder", "desulfurizer",
                "dimensional_focus_engraving_array", "gemini_containment_system", "giant_electrochemical_workstation",
                "heavy_rolling", "magnetic_energy_reaction_furnace", "mega_brewer", "mega_vacuum_drying_furnace",
                "mega_wiremill", "nyarlathoteps_tentacle", "particle_stream_matrix_filling_machine",
                "phase_change_cube" }) {
            MINIMUM.put(id, 6);
        }
        MINIMUM.put("hyperdimensional_plasma_fusion_core", 6);
        MINIMUM.put("super_blast_smelter", 5);
        for (String id : new String[] { "extreme_compressor", "giant_flotation_tank", "hand_of_arachne",
                "high_energy_laser_lathe", "holy_separator", "integrated_assembler", "kerr_newman_homogenizer",
                "nanites_circuit_assembly_factory", "neutronium_wire_cutting", "rare_earth_centrifugal",
                "superconducting_electromagnetism", "superconducting_magnetic_presser",
                "star_ultimate_material_forge_factory" }) {
            MINIMUM.put(id, 4);
        }
        MINIMUM.put("blaze_blast_furnace", 7);
        MINIMUM.put("cold_ice_freezer", 6);
        MINIMUM.put("chemical_plant", 4);
        MINIMUM.put("component_assembler", 6);
        MINIMUM.put("fishing_ground", 7);
        MINIMUM.put("greenhouse", 5);
        MINIMUM.put("large_greenhouse", 3);
        MINIMUM.put("isa_mill", 6);
        MINIMUM.put("industrial_flotation_cell", 3);
        MINIMUM.put("vacuum_drying_furnace", 8);
        MINIMUM.put("evaporation_plant", 4);
        MINIMUM.put("large_cracker", 5);
        MINIMUM.put("large_pyrolyse_oven", 5);
        MINIMUM.put("reaction_furnace", 6);
        MINIMUM.put("engraving_laser_plant", 4);
        MINIMUM.put("large_incubator", 7);
        MINIMUM.put("giant_sintering_array", 6);
        MINIMUM.put("integrated_vapor_deposition_system", 4);
        MINIMUM.put("compound_extreme_cooling_unit", 4);
        MINIMUM.put("hyper_reactor", 10);
        MINIMUM.put("advanced_hyper_reactor", 10);
        MINIMUM.put("annihilate_generator", 4);
        MINIMUM.put("fuel_cell_generator", 8);
        MINIMUM.put("generator_array", 6);
        MINIMUM.put("rocket_large_turbine", 10);
        MINIMUM.put("supercritical_steam_turbine", 10);
        MINIMUM.put("lightning_rod", 4);
        MINIMUM.put("dyson_sphere_launch_silo", 7);
        MINIMUM.put("dyson_sphere_receiving_station", 9);
        MINIMUM.put("thermal_power_pump", 7);
        MINIMUM.put("liquefaction_furnace", 5);
        MINIMUM.put("brick_kiln", 4);
        MINIMUM.put("primitive_distillation_tower", 4);
        MINIMUM.put("void_miner_steam_gate_aged", 6);
        MINIMUM.put("leap_forward_one_blast_furnace", 5);
        MINIMUM.put("large_steam_solar_boiler", 5);
        MINIMUM.put("large_steam_storage_tank", 3);
        MINIMUM.put("me_storage", 5);
        MINIMUM.put("nexus_molecular_forge", 6);
        MINIMUM.put("infinity_fluid_drilling_rig", 5);
        MINIMUM.put("industrial_slaughterhouse", 8);
        MINIMUM.put("pcb_factory", 5);
        MINIMUM.put("space_elevator", 8);
        MINIMUM.put("assembler_module", 5);
        MINIMUM.put("resource_collection_module", 4);
        MINIMUM.put("space_probe_surface_reception", 4);
        MINIMUM.put("supercomputing_center", 9);
        MINIMUM.put("data_center", 5);
        MINIMUM.put("analysis_and_research_center", 5);
        MINIMUM.put("dimensionally_transcendent_dirt_forge", 4);
        MINIMUM.put("dimensionally_transcendent_steam_boiler", 6);
        MINIMUM.put("dimensionally_transcendent_steam_oven", 5);
        MINIMUM.put("electric_void_miner", 7);
        MINIMUM.put("incubator", 5);
        MINIMUM.put("void_fluid_drilling_rig", 7);
        MINIMUM.put("integrated_ore_processor", 6);
        MINIMUM.put("advanced_integrated_ore_processor", 8);
        MINIMUM.put("mega_distillery", 4);
        MINIMUM.put("universal_factory", 8);
        MINIMUM.put("primitive_stone_furnace", 4);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void mechanicLinesMeetTheMinimum(GameTestHelper h) {
        StringBuilder short_ = new StringBuilder();
        for (var e : MINIMUM.entrySet()) {
            List<Component> tooltip = GTNATooltips.build(e.getKey());
            h.assertTrue(tooltip != null, e.getKey() + " is not migrated to the lib layout");
            int lines = mechanicLines(tooltip);
            if (lines < e.getValue()) short_.append(e.getKey()).append(' ').append(lines).append('<')
                    .append(e.getValue()).append("; ");
        }
        h.assertTrue(short_.isEmpty(), "tooltips with fewer mechanic lines than the original: " + short_);
        h.succeed();
    }

    /** The Mega Alloy Blast Smelter shows GTCEu's coil discount, and the mirrored numbers match GTCEu. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void megaAlloyBlastSmelterCoilBonusMatchesGtceu(GameTestHelper h) {
        int step = MegaAlloyBlastSmelterMachine.HEAT_STEP;
        double discount = OverclockingLogic.getCoilEUtDiscount(1000, 1000 + step);
        h.assertTrue(discount == MegaAlloyBlastSmelterMachine.EU_DISCOUNT_PER_STEP,
                "one heat step must give the mirrored discount, GTCEu gives " + discount);
        h.assertTrue(OverclockingLogic.getCoilEUtDiscount(1000, 1000 + 2 * step) == discount * discount,
                "the discount stacks multiplicatively");
        h.assertTrue(OverclockingLogic.getCoilEUtDiscount(1000, 1000 + step - 1) == 1,
                "less than one step gives no discount");
        String text = text("mega_alloy_blast_smelter");
        for (String expected : new String[] { "-5% EU usage per 900 K", "1,800 K", "4x faster instead of 2x",
                "-20% EU usage and -40% recipe time" }) {
            h.assertTrue(text.contains(expected), "Mega Alloy Blast Smelter tooltip lacks '" + expected + "': " + text);
        }
        h.succeed();
    }

    /** The Eye of Harmony field numbers in the tooltip are the ones the plan applies. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void eyeOfHarmonyFieldNumbersMatchThePlan(GameTestHelper h) {
        var program = EyeOfHarmonyMath.Program.overworld(0);
        var history = EyeOfHarmonyMath.History.fresh();
        var base = EyeOfHarmonyMath.plan(program, new EyeOfHarmonyMath.Fields(0, 0, 0), 0, program.hydrogen(),
                program.helium(), history);
        var dilated = EyeOfHarmonyMath.plan(program, new EyeOfHarmonyMath.Fields(0, 1, 1), 0, program.hydrogen(),
                program.helium(), history);
        h.assertTrue(dilated.durationTicks() == (int) (base.durationTicks() *
                EyeOfHarmonyMath.ACCELERATION_TIME_FACTOR), "one dilation tier halves the time");
        double expected = 1 - EyeOfHarmonyMath.ACCELERATION_CHANCE_LOSS + EyeOfHarmonyMath.STABILISATION_CHANCE_GAIN;
        h.assertTrue(Math.abs(dilated.chance() - expected) < 1e-9, "chance " + dilated.chance() + " != " + expected);
        String text = text("eye_of_harmony");
        for (String needed : new String[] { "-3% time per tier", "-50% time per tier", "-9.25% success chance",
                "+5% success chance", "-40% at the lowest tier", "0-24", "14,400" }) {
            h.assertTrue(text.contains(needed), "Eye of Harmony tooltip lacks '" + needed + "': " + text);
        }
        h.succeed();
    }

    /** The base module numbers in the tooltips are what GodforgeMath gives a Forge with no upgrades. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void godforgeModuleNumbersMatchTheMath(GameTestHelper h) {
        var data = new GodforgeData();
        var smelting = new GodforgeModuleStats(GodforgeModuleStats.Type.SMELTING);
        GodforgeMath.maxHeat(smelting, data);
        GodforgeMath.maxParallel(smelting, data);
        h.assertTrue(smelting.heat == GodforgeMath.BASE_HEAT, "fuel factor 1 gives the base heat");
        String text = text("godforge_smelting_module");
        for (String needed : new String[] { "Base parallels: " + String.format(java.util.Locale.ROOT, "%,d",
                smelting.calculatedMaxParallel), "12,601 K", "-5% EU per 900 K", "15,000 K",
                "2,000,000,000 EU/t" }) {
            h.assertTrue(text.contains(needed), "Smelting module tooltip lacks '" + needed + "': " + text);
        }
        h.assertTrue(text("forge_of_gods").contains("Holds " + GodforgeMath.maxModuleCount(data) + " modules"),
                "the Forge lists the module slots GodforgeMath allows");
        h.succeed();
    }

    /** Plasma Forge discount and Eye of Wood peak come from the machine code. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void plasmaForgeAndEyeOfWoodNumbersMatchTheCode(GameTestHelper h) {
        h.assertTrue(PlasmaForgeMachine.discount((long) PlasmaForgeMachine.MAX_EFFICIENCY_TICKS) ==
                PlasmaForgeMachine.MAXIMUM_DISCOUNT, "full run time gives the maximum discount");
        String forge = text("dimensionally_transcendent_plasma_forge");
        for (String needed : new String[] { "after 8 h the catalysts cost up to 50% less", "100x", "16 h" }) {
            h.assertTrue(forge.contains(needed), "Plasma Forge tooltip lacks '" + needed + "': " + forge);
        }
        String wood = text("eye_of_wood");
        for (String needed : new String[] { "75% with exactly 256,000 mB", "60 s", "up to 270,000,000 mB",
                "Source:" }) {
            h.assertTrue(wood.contains(needed), "Eye of Wood tooltip lacks '" + needed + "': " + wood);
        }
        h.succeed();
    }

    /** Naquadah booster and fusion buffer numbers come from the recipes and the machine. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void naquadahAndFusionNumbersMatchTheCode(GameTestHelper h) {
        String hyper = text("naquadah_hyper_reactor");
        for (String needed : new String[] { GTNANaquadahReactorRecipes.OXYGEN_BOOST + "x parallels",
                "up to " + GTNANaquadahReactorRecipes.NITROGEN_BOOST + "x", "4x the EU per mB", "43.75 s" }) {
            h.assertTrue(hyper.contains(needed), "Naquadah Hyper Reactor tooltip lacks '" + needed + "': " + hyper);
        }
        h.assertTrue(AdvancedFusionReactorMachine.bufferCapacity(GTValues.LuV, 1) ==
                AdvancedFusionReactorMachine.BUFFER_PER_INPUT, "one LuV input holds the listed buffer");
        String fusion = text("advanced_fusion_reactor");
        for (String needed : new String[] { "10,000,000 EU per energy input at LuV", "up to UEV", "10,000 EU" }) {
            h.assertTrue(fusion.contains(needed), "Advanced Fusion tooltip lacks '" + needed + "': " + fusion);
        }
        h.succeed();
    }

    /** Steam Elevator module numbers are the per-tier formulas the modules run. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void steamElevatorModuleNumbersMatchTheFormulas(GameTestHelper h) {
        String flight = text("steam_elevator_flight_module_iii");
        h.assertTrue(flight.contains("Range: " + SteamFlightModule.rangeFor(3) + " m"), "flight III range: " + flight);
        String drill = text("steam_elevator_oil_drill_module_ii");
        for (String needed : new String[] { "Cycle: " + SteamOilDrillModule.cycleTicksFor(3) / 20 + " s",
                SteamOilDrillModule.rollsFor(3) + " rolls of " + SteamOilDrillModule.baseYieldFor(3) + " mB" }) {
            h.assertTrue(drill.contains(needed), "oil drill II tooltip lacks '" + needed + "': " + drill);
        }
        h.assertTrue(text("steam_elevator_monster_repellent_module_ii").contains("Radius: 128 m"),
                "repellent II radius is 2^(5 + 2)");
        h.assertTrue(text("steam_elevator_weather_module_iii").contains("lasts 3 h"), "weather III lasts 3 h");
        h.succeed();
    }

    /** Megafactory coil parallels and heat use the step the machine base applies. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void megafactoryCoilNumbersMatchTheBase(GameTestHelper h) {
        String hub = text("cracker_hub");
        for (String needed : new String[] {
                "2^(coil temperature / " + CoilWorkableElectricMultipleRecipesMachine.COIL_PARALLEL_STEP + " K)",
                CoilWorkableElectricMultipleRecipesMachine.HEAT_PER_TIER + " K per voltage tier above MV" }) {
            h.assertTrue(hub.contains(needed), "Cracker Hub tooltip lacks '" + needed + "': " + hub);
        }
        h.assertTrue(text("star_ultimate_material_forge_factory").contains("no limit"),
                "the Ultimate Material Forge has no parallel limit");
        h.succeed();
    }

    /** Blaze upkeep, fishing loot and chemical plant coil numbers are the machines' constants. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void gtoProcessNumbersMatchTheMachines(GameTestHelper h) {
        h.assertTrue(BlazeBlastFurnaceMachine.upkeep(GTValues.EV) == 4 * BlazeBlastFurnaceMachine.UPKEEP_BASE,
                "EV pays 2^(4 - 2) times the base upkeep");
        String blaze = text("blaze_blast_furnace");
        for (String needed : new String[] { BlazeBlastFurnaceMachine.MAX_PARALLEL + " parallels, 0.50x",
                "× " + BlazeBlastFurnaceMachine.UPKEEP_BASE + " mB every second" }) {
            h.assertTrue(blaze.contains(needed), "Blaze Blast Furnace tooltip lacks '" + needed + "': " + blaze);
        }
        h.assertTrue(text("fishing_ground").contains("up to 1,024"), "fishing loot draws");
        h.assertTrue(text("chemical_plant").contains("coil tier × 5%"), "chemical plant coil cut");
        h.succeed();
    }

    /** Turbine, generator array and slaughterhouse numbers are the machines' constants. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void gtoGeneratorNumbersMatchTheMachines(GameTestHelper h) {
        String rocket = text("rocket_large_turbine");
        long base = (long) (GTValues.V[GTNALargeTurbineMachine.ROCKET_TIER] * GTNALargeTurbineMachine.SPECIAL_OUTPUT);
        h.assertTrue(rocket.contains(String.format(java.util.Locale.ROOT, "%,d EU/t", base)),
                "rocket turbine base output " + base + ": " + rocket);
        h.assertTrue(text("generator_array").contains("up to " + GeneratorArrayMachine.MAX_GENERATORS),
                "generator array count");
        h.assertTrue(text("industrial_slaughterhouse").contains("Bosses: from ZPM"), "slaughterhouse boss tier");
        h.succeed();
    }

    /** GTL parallels come from the machine constants. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void gtlNumbersMatchTheMachines(GameTestHelper h) {
        String forge = text("dimensionally_transcendent_dirt_forge");
        h.assertTrue(forge.contains(String.format(java.util.Locale.ROOT, "%,d",
                DimensionallyTranscendentDirtForgeMachine.MAX_PARALLEL)), "dirt forge parallels: " + forge);
        h.assertTrue(text("dimensionally_transcendent_steam_oven").contains("100x"), "steam oven is 100x faster");
        h.succeed();
    }

    /** Lines from the machine's own or the shared mechanic texts; humour, type, origin and mod do not count. */
    static int mechanicLines(List<Component> tooltip) {
        int n = 0;
        for (Component line : tooltip) {
            if (!(line.getContents() instanceof TranslatableContents t)) continue;
            String key = t.getKey();
            if (!key.startsWith("gtna.") || key.endsWith(".humor") || key.equals("gtna.tooltip.source")) continue;
            n++;
        }
        return n;
    }

    private static String text(String path) {
        StringBuilder out = new StringBuilder();
        for (Component line : GTNATooltips.build(path)) out.append(line.getString()).append('\n');
        return out.toString();
    }

    private MechanicTooltipGameTests() {}
}
