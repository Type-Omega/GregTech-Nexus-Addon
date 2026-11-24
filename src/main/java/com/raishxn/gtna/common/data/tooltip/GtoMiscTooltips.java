package com.raishxn.gtna.common.data.tooltip;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.data.DysonSphereData;
import com.raishxn.gtna.common.machine.multiblock.electric.AnalysisAndResearchCenterMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.FuelCellGeneratorMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.GTNALargeTurbineMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.HyperReactorMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.InfinityFluidDrillMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.PCBFactoryMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.SupercomputingCenterMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.space.SpaceElevatorMachine;
import com.raishxn.gtna.common.machine.multiblock.energy.GeneratorArrayMachine;
import com.raishxn.gtna.common.machine.multiblock.energy.IndustrialSlaughterhouse;
import com.raishxn.gtna.common.machine.multiblock.noenergy.BrickKilnMachine;
import com.raishxn.gtna.common.machine.multiblock.noenergy.LeapForwardBlastFurnace;
import com.raishxn.gtna.common.machine.multiblock.noenergy.ThermalPowerPumpMachine;
import com.raishxn.gtna.common.machine.multiblock.steam.LargeSteamSolarBoilerMachine;
import com.raishxn.gtna.common.machine.multiblock.steam.LargeSteamStorageTank;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.util.List;

import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.pct;
import static com.raishxn.gtna.common.data.tooltip.Fmt.seconds;
import static com.raishxn.gtna.common.data.tooltip.Fmt.times;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.raishxn.gtna.common.data.tooltip.HatchTooltips.tier;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * GTOCore generators, turbines, primitive machines, storage, research and space ports: mechanics read from the GTNA
 * classes, text of GTNA's own.
 */
final class GtoMiscTooltips {

    private GtoMiscTooltips() {}

    static void register() {
        GTNATooltips.register("hyper_reactor", () -> hyper("hyper_reactor"));
        GTNATooltips.register("advanced_hyper_reactor", () -> hyper("advanced_hyper_reactor"));
        GTNATooltips.register("annihilate_generator", GtoMiscTooltips::artificialStar);
        GTNATooltips.register("fuel_cell_generator", GtoMiscTooltips::fuelCell);
        GTNATooltips.register("generator_array", GtoMiscTooltips::generatorArray);
        GTNATooltips.register("rocket_large_turbine", () -> turbine("rocket_large_turbine",
                GTNALargeTurbineMachine.ROCKET_TIER, GTNALargeTurbineMachine.SPECIAL_OUTPUT));
        GTNATooltips.register("supercritical_steam_turbine", () -> turbine("supercritical_steam_turbine",
                GTNALargeTurbineMachine.SUPERCRITICAL_TIER, GTNALargeTurbineMachine.NORMAL_OUTPUT));
        GTNATooltips.register("lightning_rod", () -> plain("lightning_rod", "bolt"));
        GTNATooltips.register("dyson_sphere_launch_silo", GtoMiscTooltips::launchSilo);
        GTNATooltips.register("dyson_sphere_receiving_station", GtoMiscTooltips::receivingStation);
        GTNATooltips.register("thermal_power_pump", GtoMiscTooltips::thermalPump);
        GTNATooltips.register("liquefaction_furnace", GtoMiscTooltips::liquefaction);
        GTNATooltips.register("brick_kiln", GtoMiscTooltips::brickKiln);
        GTNATooltips.register("primitive_distillation_tower", GtoMiscTooltips::distillation);
        GTNATooltips.register("void_miner_steam_gate_aged", GtoMiscTooltips::steamVoidMiner);
        GTNATooltips.register("leap_forward_one_blast_furnace", GtoMiscTooltips::leapForward);
        GTNATooltips.register("large_steam_solar_boiler", GtoMiscTooltips::solarBoiler);
        GTNATooltips.register("large_steam_storage_tank", GtoMiscTooltips::storageTank);
        GTNATooltips.register("me_storage", GtoMiscTooltips::meStorage);
        GTNATooltips.register("nexus_molecular_forge", GtoMiscTooltips::molecularForge);
        GTNATooltips.register("infinity_fluid_drilling_rig", GtoMiscTooltips::infinityDrill);
        GTNATooltips.register("industrial_slaughterhouse", GtoMiscTooltips::slaughterhouse);
        GTNATooltips.register("pcb_factory", GtoMiscTooltips::pcbFactory);
        GTNATooltips.register("space_elevator", GtoMiscTooltips::spaceElevator);
        GTNATooltips.register("assembler_module", () -> spaceModule("assembler_module"));
        GTNATooltips.register("resource_collection_module", () -> spaceModule("resource_collection_module"));
        GTNATooltips.register("space_probe_surface_reception", () -> plain("space_probe_surface_reception",
                "space", "sky"));
        GTNATooltips.register("supercomputing_center", GtoMiscTooltips::supercomputer);
        GTNATooltips.register("data_center", GtoMiscTooltips::dataCenter);
        GTNATooltips.register("analysis_and_research_center", GtoMiscTooltips::analysisCenter);
    }

    private static MultiblockTooltipBuilder begin(String id) {
        return start(id).line(own(id, "purpose")).separator();
    }

    private static MultiblockTooltipBuilder lines(MultiblockTooltipBuilder b, String id, String... keys) {
        for (String key : keys) b.line(own(id, key));
        return b;
    }

    private static MultiblockTooltipBuilder hatches(MultiblockTooltipBuilder b, String id) {
        HatchTooltips.special(b, id);
        if (!HatchTooltips.abilities(id).contains("thread_hatch")) b.line(shared("mega.one_recipe"));
        return b;
    }

    /** Machine whose own lines are enough, plus its hatches and the standard overclock. */
    private static List<Component> plain(String id, String... keys) {
        var b = lines(begin(id), id, keys);
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> hyper(String id) {
        var b = begin(id).line(shared("hyper.booster"))
                .line(shared("hyper.best"))
                .line(own("naquadah_hyper_reactor", "parallel"))
                .line(own("naquadah_hyper_reactor", "scale"))
                .warning(own("naquadah_hyper_reactor", "consumed"))
                .line(own("naquadah_hyper_reactor", "unboosted"))
                .line(own("naquadah_hyper_reactor", "default"), value(HyperReactorMachine.DEFAULT_BOOST + "x"))
                .line(shared("oc.generator"))
                .warning(shared("hyper.pack"));
        return finish(b, id);
    }

    private static List<Component> artificialStar() {
        String id = "annihilate_generator";
        var b = begin(id).line(own(id, "overclock"), value(times(4)), value(times(4)))
                .line(own(id, "limit"))
                .line(own(id, "output"));
        return finish(b, id);
    }

    private static List<Component> fuelCell() {
        String id = "fuel_cell_generator";
        var b = begin(id).line(own(id, "absorption"))
                .line(own(id, "decay"))
                .line(own(id, "recovery"), value(num(FuelCellGeneratorMachine.RECOVERY_WATER) + " mB"),
                        value(pct(FuelCellGeneratorMachine.DECAY_RECOVERY)))
                .line(own(id, "transfer"))
                .line(own(id, "release"), value(FuelCellGeneratorMachine.MAX_RELEASE_PARALLEL))
                .warning(own(id, "membrane"), value(pct(FuelCellGeneratorMachine.MEMBRANE_DAMAGE_CHANCE)))
                .warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> generatorArray() {
        String id = "generator_array";
        var b = begin(id).line(own(id, "generators"), value(GeneratorArrayMachine.MAX_GENERATORS))
                .line(own(id, "budget"), value(times(GeneratorArrayMachine.OUTPUT_MULTIPLIER)))
                .line(own(id, "parallel"))
                .line(own(id, "wireless"), value(GeneratorArrayMachine.WIRELESS_LOSS_PERCENT + "%"))
                .line(shared("oc.generator"));
        return finish(b, id);
    }

    private static List<Component> turbine(String id, int tier, double output) {
        var b = begin(id).line(shared("turbine.base"), value(num((long) (GTValues.V[tier] * output)) + " EU/t"),
                value(tier(tier)), value(times(output)))
                .line(shared("turbine.cap"))
                .line(shared("turbine.parallel"))
                .line(shared("turbine.efficiency"))
                .warning(shared("turbine.rotor"))
                .line(shared("turbine.module"), value(times(GTNALargeTurbineMachine.MODULE_OUTPUT)),
                        value(pct(GTNALargeTurbineMachine.MODULE_EFFICIENCY - 1)),
                        value(times(GTNALargeTurbineMachine.MODULE_DAMAGE)))
                .line(shared("turbine.high_speed"), value(times(GTNALargeTurbineMachine.HIGH_SPEED_OUTPUT_MULTIPLIER)),
                        value(GTNALargeTurbineMachine.HIGH_SPEED_ROTOR_DAMAGE),
                        value(times(GTNALargeTurbineMachine.HIGH_SPEED_MACHINE_FAULT)))
                .warning(shared("turbine.voids"))
                .warning(shared("turbine.not_ported"));
        return finish(b, id);
    }

    private static List<Component> launchSilo() {
        String id = "dyson_sphere_launch_silo";
        var b = lines(begin(id), id, "surface", "time", "module", "repair")
                .line(own(id, "max"), value(num(DysonSphereData.MAX_MODULES)))
                .line(shared("dyson.distance"));
        return finish(b, id);
    }

    private static List<Component> receivingStation() {
        String id = "dyson_sphere_receiving_station";
        var b = lines(begin(id), id, "output", "cwu", "helium", "one", "damage", "loss")
                .line(shared("dyson.distance"))
                .warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> thermalPump() {
        String id = "thermal_power_pump";
        var b = lines(begin(id), id, "biome", "formula")
                .line(own(id, "rain"))
                .line(own(id, "cycle"), value(seconds(ThermalPowerPumpMachine.CYCLE_TICKS)))
                .warning(own(id, "nether"))
                .line(own(id, "no_energy"));
        return finish(b, id);
    }

    private static List<Component> liquefaction() {
        String id = "liquefaction_furnace";
        var b = begin(id);
        MegaTooltips.heat(b);
        lines(b, id, "module");
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> brickKiln() {
        String id = "brick_kiln";
        var b = begin(id).line(own(id, "parallel"), value(BrickKilnMachine.MAX_PARALLEL))
                .line(own(id, "no_energy"))
                .line(shared("oc.none"));
        return finish(b, id);
    }

    private static List<Component> distillation() {
        String id = "primitive_distillation_tower";
        var b = lines(begin(id), id, "steam", "layers", "no_energy");
        return finish(b, id);
    }

    private static List<Component> steamVoidMiner() {
        String id = "void_miner_steam_gate_aged";
        var b = lines(begin(id), id, "ores", "boosts", "cost", "fluid", "config");
        return finish(b, id);
    }

    private static List<Component> leapForward() {
        String id = "leap_forward_one_blast_furnace";
        var b = begin(id).line(own(id, "parallel"), value(LeapForwardBlastFurnace.BASE_PARALLEL),
                value(num(LeapForwardBlastFurnace.MAX_PARALLEL_CAP)))
                .line(own(id, "time"), value(seconds(LeapForwardBlastFurnace.BASE_TICKS)),
                        value(seconds(LeapForwardBlastFurnace.TICKS_PER_LAYER)))
                .line(own(id, "layers"))
                .line(own(id, "no_energy"));
        return finish(b, id);
    }

    private static List<Component> solarBoiler() {
        String id = "large_steam_solar_boiler";
        var b = begin(id).line(own(id, "cells"))
                .line(own(id, "size"), value(LargeSteamSolarBoilerMachine.MAX_SIDE),
                        value(LargeSteamSolarBoilerMachine.MAX_BACK))
                .warning(own(id, "night"))
                .line(own(id, "cycle"), value(seconds(LargeSteamSolarBoilerMachine.TICK_INTERVAL)));
        return finish(b, id);
    }

    private static List<Component> storageTank() {
        String id = "large_steam_storage_tank";
        var b = begin(id).line(own(id, "capacity"), value(num(LargeSteamStorageTank.CAPACITY) + " mB"))
                .line(own(id, "steam_only"));
        return finish(b, id);
    }

    private static List<Component> meStorage() {
        String id = "me_storage";
        var b = lines(begin(id), id, "sections", "access")
                .line(own(id, "infinite"), value("1 TB"))
                .warning(own(id, "one_access"));
        return finish(b, id);
    }

    private static List<Component> molecularForge() {
        String id = "nexus_molecular_forge";
        var b = lines(begin(id), id, "queue", "batch", "free", "power", "parallel");
        return finish(b, id);
    }

    private static List<Component> infinityDrill() {
        String id = "infinity_fluid_drilling_rig";
        var b = begin(id).line(own(id, "depletion"))
                .line(own(id, "yield"), value(InfinityFluidDrillMachine.BASIS + "x"),
                        value(tier(InfinityFluidDrillMachine.RIG_TIER)))
                .line(own(id, "overclock"), value(tier(InfinityFluidDrillMachine.RIG_TIER)))
                .warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> slaughterhouse() {
        String id = "industrial_slaughterhouse";
        var b = begin(id).line(own(id, "modes"))
                .line(own(id, "passive"), value(tier(IndustrialSlaughterhouse.PASSIVE_TIER)),
                        value(IndustrialSlaughterhouse.PASSIVE_BASE))
                .line(own(id, "hostile"), value(tier(IndustrialSlaughterhouse.HOSTILE_TIER)),
                        value(IndustrialSlaughterhouse.HOSTILE_BASE))
                .line(own(id, "boss"), value(tier(IndustrialSlaughterhouse.BOSS_TIER)),
                        value(IndustrialSlaughterhouse.BOSS_BASE))
                .line(own(id, "dragon"), value(tier(IndustrialSlaughterhouse.DRAGON_TIER)),
                        value(IndustrialSlaughterhouse.DRAGON_BASE))
                .line(own(id, "loops"))
                .warning(own(id, "low_tier"));
        hatches(b, id);
        return finish(b, id);
    }

    private static List<Component> pcbFactory() {
        String id = "pcb_factory";
        var b = begin(id).line(own(id, "tiers"))
                .line(own(id, "caps"), value(num(PCBFactoryMachine.TIER_1_MAX_EUT) + " EU/t"),
                        value(num(PCBFactoryMachine.TIER_2_MAX_EUT) + " EU/t"))
                .warning(own(id, "not_ported"));
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> spaceElevator() {
        String id = "space_elevator";
        var b = begin(id).line(own(id, "tier"))
                .line(own(id, "upkeep"), value(SpaceElevatorMachine.CYCLE_TICKS + " ticks"),
                        value(SpaceElevatorMachine.CWU_PER_TIER))
                .line(own(id, "spools"), value(SpaceElevatorMachine.MAX_SPOOLS))
                .line(own(id, "slots"))
                .line(own(id, "power"))
                .warning(own(id, "same_mk"))
                .warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> spaceModule(String id) {
        var b = begin(id).line(shared("space.parallel"))
                .line(shared("space.time"))
                .warning(shared("space.running"));
        if (id.equals("assembler_module")) b.warning(own(id, "gate"));
        return finish(b, id);
    }

    private static List<Component> supercomputer() {
        String id = "supercomputing_center";
        var b = lines(begin(id), id, "tiers", "mainframe")
                .line(own(id, "cycle"), value(SupercomputingCenterMachine.CYCLE + " ticks"))
                .line(own(id, "cooling"))
                .warning(own(id, "damage"))
                .line(own(id, "decay"))
                .warning(own(id, "match"))
                .warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> dataCenter() {
        String id = "data_center";
        var b = lines(begin(id), id, "share", "chain", "cooling", "power");
        return finish(b, id);
    }

    private static List<Component> analysisCenter() {
        String id = "analysis_and_research_center";
        var b = begin(id).line(own(id, "modes"))
                .line(own(id, "computation"))
                .line(own(id, "one_output"), value(num(AnalysisAndResearchCenterMachine.CHANCE_SCALE)))
                .warning(own(id, "not_ported"));
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }
}
