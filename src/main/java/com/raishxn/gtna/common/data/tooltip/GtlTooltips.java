package com.raishxn.gtna.common.data.tooltip;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.data.GTNAMachines;
import com.raishxn.gtna.common.machine.multiblock.noenergy.DimensionallyTranscendentDirtForgeMachine;
import com.raishxn.gtna.common.machine.multiblock.noenergy.PrimitiveStoneFurnaceMachine;
import com.raishxn.gtna.common.machine.multiblock.steam.DimensionallyTranscendentSteamOvenMachine;
import com.raishxn.gtna.config.GTNABalance;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.util.List;

import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.times;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.raishxn.gtna.common.data.tooltip.HatchTooltips.tier;
import static com.typeomega.lib.tooltip.TooltipArg.configurable;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * GregTech Leisure (GTLCore, licence not confirmed, so the text is GTNA's own from the code) and GTLsupb ports:
 * Dimensionally Transcendent primitives, void miners and drills, ore processors, the Mega Distillery, the Universal
 * Factory and the Primitive Stone Furnace.
 */
final class GtlTooltips {

    private GtlTooltips() {}

    static void register() {
        GTNATooltips.register("dimensionally_transcendent_dirt_forge", GtlTooltips::dirtForge);
        GTNATooltips.register("dimensionally_transcendent_steam_boiler", GtlTooltips::steamBoiler);
        GTNATooltips.register("dimensionally_transcendent_steam_oven", GtlTooltips::steamOven);
        GTNATooltips.register("electric_void_miner", GtlTooltips::voidMiner);
        GTNATooltips.register("incubator", GtlTooltips::incubator);
        GTNATooltips.register("void_fluid_drilling_rig", GtlTooltips::fluidRig);
        GTNATooltips.register("integrated_ore_processor", () -> ore("integrated_ore_processor", false));
        GTNATooltips.register("advanced_integrated_ore_processor",
                () -> ore("advanced_integrated_ore_processor", true));
        GTNATooltips.register("mega_distillery", GtlTooltips::megaDistillery);
        GTNATooltips.register("universal_factory", GtlTooltips::universalFactory);
        GTNATooltips.register("primitive_stone_furnace", GtlTooltips::stoneFurnace);
    }

    private static MultiblockTooltipBuilder begin(String id) {
        return start(id).line(own(id, "purpose")).separator();
    }

    private static MultiblockTooltipBuilder hatches(MultiblockTooltipBuilder b, String id) {
        HatchTooltips.special(b, id);
        if (!HatchTooltips.abilities(id).contains("thread_hatch")) b.line(shared("mega.one_recipe"));
        return b;
    }

    private static List<Component> dirtForge() {
        String id = "dimensionally_transcendent_dirt_forge";
        var b = begin(id).line(own(id, "tick"))
                .line(own(id, "parallel"), value(num(DimensionallyTranscendentDirtForgeMachine.MAX_PARALLEL)))
                .line(own(id, "no_energy"));
        HatchTooltips.special(b, id);
        return finish(b, id);
    }

    private static List<Component> steamBoiler() {
        String id = "dimensionally_transcendent_steam_boiler";
        var b = begin(id).line(own(id, "steam"), value(num(GTNAMachines.DT_STEAM_BOILER_STEAM) + " mB"))
                .line(own(id, "heat"), value(GTNAMachines.DT_STEAM_BOILER_HEAT_SPEED))
                .line(own(id, "fuel"))
                .warning(own(id, "water"))
                .line(own(id, "throttle"));
        return finish(b, id);
    }

    private static List<Component> steamOven() {
        String id = "dimensionally_transcendent_steam_oven";
        var b = begin(id).line(own(id, "speed"), value(times(1 / DimensionallyTranscendentSteamOvenMachine.DURATION)))
                .line(own(id, "threads"), value(DimensionallyTranscendentSteamOvenMachine.THREADS))
                .line(own(id, "parallel"), value(num(DimensionallyTranscendentSteamOvenMachine.MAX_PARALLEL)))
                .line(own(id, "steam"));
        return finish(b, id);
    }

    private static List<Component> voidMiner() {
        String id = "electric_void_miner";
        var b = begin(id).line(own(id, "precise"))
                .line(own(id, "random"))
                .line(own(id, "circuits"))
                .line(own(id, "parallel"), configurable(tier(GTNABalance.getElectricVoidMinerParallelTier())))
                .warning(own(id, "fluid"));
        HatchTooltips.special(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> incubator() {
        String id = "incubator";
        var b = begin(id).line(own(id, "data"))
                .line(own(id, "inputs"))
                .line(own(id, "output"));
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> fluidRig() {
        String id = "void_fluid_drilling_rig";
        var b = begin(id).line(own(id, "data"))
                .line(own(id, "parallel"))
                .warning(own(id, "origin"))
                .warning(own(id, "fluid"))
                .line(own(id, "checks"));
        HatchTooltips.special(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> ore(String id, boolean advanced) {
        var b = begin(id).line(shared("ore.routes"))
                .line(shared("ore.routes_wash"))
                .line(shared("ore.routes_bath"));
        if (advanced) b.line(own(id, "parallel")).warning(own(id, "laser"));
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> megaDistillery() {
        String id = "mega_distillery";
        var b = begin(id).line(own(id, "outputs"));
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> universalFactory() {
        String id = "universal_factory";
        var balance = GTNABalance.getUniversalFactory();
        var b = begin(id).line(own(id, "mode"), configurable(balance.scalingMode));
        if ("LEGACY".equals(balance.scalingMode)) {
            b.line(own(id, "legacy_threads"), configurable(GTNABalance.getUniversalFactoryBaseThreads()))
                    .line(own(id, "legacy_parallel"), configurable(GTNABalance.getUniversalFactoryBaseParallel()));
        } else {
            b.line(own(id, "budget"));
        }
        b.line(own(id, "warmup"), configurable(times(GTNABalance.getUniversalFactoryMaxWarmup())),
                configurable(num(GTNABalance.getUniversalFactoryWarmupTau()) + " s"))
                .line(own(id, "overload"), configurable(GTNABalance.getUniversalFactoryOverloadTime() + " s"))
                .line(own(id, "batch"), configurable(times(GTNABalance.getUniversalFactoryMaxBatchMultiplier())))
                .warning(own(id, "config"));
        HatchTooltips.special(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> stoneFurnace() {
        String id = "primitive_stone_furnace";
        var b = begin(id).line(own(id, "tick"), value(PrimitiveStoneFurnaceMachine.RECIPE_DURATION))
                .line(own(id, "unlimited"))
                .line(own(id, "no_energy"));
        HatchTooltips.special(b, id);
        return finish(b, id);
    }
}
