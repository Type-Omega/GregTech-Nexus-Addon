package com.raishxn.gtna.common.data.tooltip;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.machine.multiblock.electric.BlazeBlastFurnaceMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.ChemicalPlantMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.CoilWorkableElectricMultipleRecipesMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.ComponentAssemblerMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.FishingGroundMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.GreenhouseMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.IsaMillParallel;
import com.raishxn.gtna.common.machine.multiblock.electric.MegaAlloyBlastSmelterMachine;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.util.List;

import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.pct;
import static com.raishxn.gtna.common.data.tooltip.Fmt.seconds;
import static com.raishxn.gtna.common.data.tooltip.Fmt.signed;
import static com.raishxn.gtna.common.data.tooltip.Fmt.times;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.raishxn.gtna.common.data.tooltip.HatchTooltips.tier;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * GTOCore processing ports with their own machine class (Blaze Blast Furnace, Cold Ice Freezer, Chemical Plant,
 * Fishing Ground, ISA Mill, greenhouses, coil machines and the glass-tiered parallels): mechanics read from the GTNA
 * classes, text of GTNA's own.
 */
final class ProcessTooltips {

    private ProcessTooltips() {}

    /** GTCEu 7.5.3 cracker overclock: EU/t × (1 − 0.1 × coil tier). Mirrored, GTCEu keeps it inline. */
    public static final double CRACKER_EU_PER_COIL_TIER = 0.1;
    /** GTCEu 7.5.3 pyrolyse oven: Cupronickel coils take 4/3 of the time, tier N takes 2 / (N + 1). */
    public static final double PYROLYSE_CUPRONICKEL_TIME = 4.0 / 3.0;

    static void register() {
        GTNATooltips.register("blaze_blast_furnace", () -> fluidUpkeep("blaze_blast_furnace", true));
        GTNATooltips.register("cold_ice_freezer", () -> fluidUpkeep("cold_ice_freezer", false));
        GTNATooltips.register("chemical_plant", ProcessTooltips::chemicalPlant);
        GTNATooltips.register("component_assembler", ProcessTooltips::componentAssembler);
        GTNATooltips.register("fishing_ground", ProcessTooltips::fishingGround);
        GTNATooltips.register("greenhouse", ProcessTooltips::greenhouse);
        GTNATooltips.register("large_greenhouse", () -> simple("large_greenhouse", false));
        GTNATooltips.register("isa_mill", ProcessTooltips::isaMill);
        GTNATooltips.register("industrial_flotation_cell", () -> simple("industrial_flotation_cell", true));
        GTNATooltips.register("vacuum_drying_furnace", ProcessTooltips::vacuumDrying);
        GTNATooltips.register("evaporation_plant", ProcessTooltips::evaporation);
        GTNATooltips.register("large_cracker", ProcessTooltips::cracker);
        GTNATooltips.register("large_pyrolyse_oven", ProcessTooltips::pyrolyse);
        GTNATooltips.register("reaction_furnace", ProcessTooltips::reactionFurnace);
        GTNATooltips.register("engraving_laser_plant", ProcessTooltips::engraving);
        GTNATooltips.register("large_incubator", ProcessTooltips::incubator);
        GTNATooltips.register("giant_sintering_array", () -> glass("giant_sintering_array", true));
        GTNATooltips.register("integrated_vapor_deposition_system",
                () -> glass("integrated_vapor_deposition_system", false));
        GTNATooltips.register("compound_extreme_cooling_unit", ProcessTooltips::cooling);
    }

    private static MultiblockTooltipBuilder begin(String id) {
        return start(id).line(own(id, "purpose")).separator();
    }

    /** Special hatches, and "one recipe at a time" when the pattern has no Thread Hatch. */
    private static MultiblockTooltipBuilder hatches(MultiblockTooltipBuilder b, String id) {
        HatchTooltips.special(b, id);
        if (!HatchTooltips.abilities(id).contains("thread_hatch")) b.line(shared("mega.one_recipe"));
        return b;
    }

    private static MultiblockTooltipBuilder ebf(MultiblockTooltipBuilder b) {
        return CoilTooltips.ebfHeat(b, MegaAlloyBlastSmelterMachine.HEAT_PER_VOLTAGE_TIER,
                MegaAlloyBlastSmelterMachine.HEAT_STEP, MegaAlloyBlastSmelterMachine.EU_DISCOUNT_PER_STEP);
    }

    /** Blaze Blast Furnace and Cold Ice Freezer: fixed parallels, half time, a fluid paid every second. */
    private static List<Component> fluidUpkeep(String id, boolean heated) {
        var b = begin(id).line(shared("gto.fixed_bonus"), value(BlazeBlastFurnaceMachine.MAX_PARALLEL),
                value(times(BlazeBlastFurnaceMachine.DURATION_MULTIPLIER)));
        if (heated) MegaTooltips.heat(b);
        b.line(own(id, "upkeep"), value(BlazeBlastFurnaceMachine.UPKEEP_BASE + " mB"))
                .warning(shared("gto.upkeep_stop"));
        if (!heated) b.line(own(id, "module"));
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> chemicalPlant() {
        String id = "chemical_plant";
        var b = begin(id).line(own(id, "coil"), value(pct(ChemicalPlantMachine.COIL_DISCOUNT_PER_TIER)));
        hatches(b, id);
        HatchTooltips.perfectOc(b);
        return finish(b, id);
    }

    private static List<Component> componentAssembler() {
        String id = "component_assembler";
        var b = begin(id).line(own(id, "tier"), value(tier(ComponentAssemblerMachine.BASE_CASING_CAP)),
                value(tier(ComponentAssemblerMachine.EXTENDED_CASING_CAP)))
                .line(shared("component.same"))
                .warning(shared("component.gate"));
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> fishingGround() {
        String id = "fishing_ground";
        var b = begin(id).line(own(id, "bait"))
                .line(own(id, "loot"))
                .line(own(id, "draws"), value(num(FishingGroundMachine.MAX_LOOT_DRAWS)),
                        value(num(FishingGroundMachine.LOOT_EU_PER_DRAW) + " EU/t"),
                        value(seconds(FishingGroundMachine.LOOT_DURATION)))
                .warning(own(id, "tagged"), value(FishingGroundMachine.MAX_TAGGED_ITEMS));
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> greenhouse() {
        String id = "greenhouse";
        var b = begin(id).line(own(id, "light"))
                .warning(own(id, "dark"), value(GreenhouseMachine.DIM_LIGHT),
                        value(GreenhouseMachine.DIM_REGRESS_TICKS + " ticks"));
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    /** A machine whose mechanics are its hatches and its overclock. */
    private static List<Component> simple(String id, boolean perfect) {
        var b = hatches(begin(id), id);
        if (perfect) HatchTooltips.perfectOc(b);
        else HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> isaMill() {
        String id = "isa_mill";
        var b = begin(id).line(own(id, "ball"))
                .line(own(id, "wear"))
                .warning(own(id, "broken"))
                .line(own(id, "parallel"), value(IsaMillParallel.MAX_PARALLEL));
        HatchTooltips.special(b, id);
        HatchTooltips.perfectOc(b);
        return finish(b, id);
    }

    private static List<Component> vacuumDrying() {
        String id = "vacuum_drying_furnace";
        var b = begin(id).line(own(id, "drying"));
        ebf(b).line(own(id, "dehydrator"),
                value(num(CoilWorkableElectricMultipleRecipesMachine.COIL_PARALLEL_STEP) + " K"));
        hatches(b, id);
        return finish(b, id);
    }

    private static List<Component> evaporation() {
        String id = "evaporation_plant";
        var b = begin(id).line(own(id, "layers"));
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> cracker() {
        String id = "large_cracker";
        var b = begin(id).line(own(id, "coil"), value(signed(-CRACKER_EU_PER_COIL_TIER)));
        hatches(b, id);
        HatchTooltips.standardOc(b).warning(shared("voltage_gate"));
        return finish(b, id);
    }

    private static List<Component> pyrolyse() {
        String id = "large_pyrolyse_oven";
        var b = begin(id).line(own(id, "coil"), value(times(PYROLYSE_CUPRONICKEL_TIME)));
        hatches(b, id);
        HatchTooltips.standardOc(b).warning(shared("voltage_gate"));
        return finish(b, id);
    }

    private static List<Component> reactionFurnace() {
        String id = "reaction_furnace";
        var b = ebf(begin(id));
        hatches(b, id);
        return finish(b, id);
    }

    private static List<Component> engraving() {
        String id = "engraving_laser_plant";
        var b = begin(id).line(own(id, "precision"));
        hatches(b, id);
        HatchTooltips.perfectOc(b);
        return finish(b, id);
    }

    private static List<Component> incubator() {
        String id = "large_incubator";
        var b = begin(id).line(own(id, "glass"))
                .line(own(id, "filter"))
                .warning(own(id, "filter_gate"));
        hatches(b, id);
        HatchTooltips.standardOc(b).warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> glass(String id, boolean heated) {
        var b = begin(id);
        if (heated) MegaTooltips.heat(b);
        b.line(shared("mega.glass_parallel"));
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }

    private static List<Component> cooling() {
        String id = "compound_extreme_cooling_unit";
        var b = begin(id).line(own(id, "plasma"));
        hatches(b, id);
        HatchTooltips.standardOc(b);
        return finish(b, id);
    }
}
