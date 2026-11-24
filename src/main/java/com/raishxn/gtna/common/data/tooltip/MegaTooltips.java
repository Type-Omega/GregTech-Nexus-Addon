package com.raishxn.gtna.common.data.tooltip;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.machine.multiblock.electric.CoilWorkableElectricMultipleRecipesMachine;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.util.List;
import java.util.Set;

import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.typeomega.lib.tooltip.TooltipArg.highlight;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * GTOCore megafactories ported on GTNA's multiple-recipe bases ({@code WorkableElectricMultipleRecipesMachine} and
 * {@code CoilWorkableElectricMultipleRecipesMachine}): the parallel, heat and hatch rules come from those classes and
 * the pattern, the text is GTNA's own. GTO recipe types GTNA does not have are named in a "not ported" line.
 */
final class MegaTooltips {

    private MegaTooltips() {}

    /** Parallels from the coil (2^(temperature / step)) or the Parallel Hatch, heat checked. */
    private static final int COIL_PARALLEL = 0;
    /** Heat checked, parallels from the Parallel Hatch. */
    private static final int COIL = 1;
    /** Parallels from the Parallel Hatch. */
    private static final int MULTI = 2;
    /** No parallel limit. */
    private static final int UNLIMITED = 3;

    private record Spec(String id, int kind, boolean notPorted) {}

    private static final List<Spec> SPECS = List.of(
            new Spec("cracker_hub", COIL_PARALLEL, false),
            new Spec("crystal_builder", COIL_PARALLEL, true),
            new Spec("desulfurizer", COIL_PARALLEL, true),
            new Spec("dimensional_focus_engraving_array", COIL_PARALLEL, false),
            new Spec("gemini_containment_system", COIL_PARALLEL, true),
            new Spec("giant_electrochemical_workstation", COIL_PARALLEL, true),
            new Spec("heavy_rolling", COIL_PARALLEL, true),
            new Spec("magnetic_energy_reaction_furnace", COIL_PARALLEL, false),
            new Spec("mega_brewer", COIL_PARALLEL, false),
            new Spec("mega_vacuum_drying_furnace", COIL_PARALLEL, false),
            new Spec("mega_wiremill", COIL_PARALLEL, true),
            new Spec("nyarlathoteps_tentacle", COIL_PARALLEL, true),
            new Spec("particle_stream_matrix_filling_machine", COIL_PARALLEL, false),
            new Spec("phase_change_cube", COIL_PARALLEL, false),
            new Spec("hyperdimensional_plasma_fusion_core", COIL, true),
            new Spec("super_blast_smelter", COIL, false),
            new Spec("extreme_compressor", MULTI, true),
            new Spec("giant_flotation_tank", MULTI, false),
            new Spec("hand_of_arachne", MULTI, false),
            new Spec("high_energy_laser_lathe", MULTI, false),
            new Spec("holy_separator", MULTI, false),
            new Spec("integrated_assembler", MULTI, true),
            new Spec("kerr_newman_homogenizer", MULTI, false),
            new Spec("nanites_circuit_assembly_factory", MULTI, false),
            new Spec("neutronium_wire_cutting", MULTI, false),
            new Spec("rare_earth_centrifugal", MULTI, false),
            new Spec("superconducting_electromagnetism", MULTI, false),
            new Spec("superconducting_magnetic_presser", MULTI, false),
            new Spec("star_ultimate_material_forge_factory", UNLIMITED, false));

    static void register() {
        for (Spec spec : SPECS) GTNATooltips.register(spec.id(), () -> build(spec));
    }

    private static List<Component> build(Spec spec) {
        String id = spec.id();
        Set<String> abilities = HatchTooltips.abilities(id);
        var b = start(id).line(own(id, "purpose")).separator();
        if (spec.kind() == COIL_PARALLEL || spec.kind() == COIL) heat(b);
        switch (spec.kind()) {
            case COIL_PARALLEL -> b.line(shared("mega.coil_parallel"),
                    value(num(CoilWorkableElectricMultipleRecipesMachine.COIL_PARALLEL_STEP) + " K"));
            case UNLIMITED -> b.line(shared("mega.unlimited"));
            default -> {
                if (!abilities.contains("parallel_hatch")) b.line(shared("mega.no_parallel"));
            }
        }
        HatchTooltips.special(b, id);
        if (!abilities.contains("thread_hatch")) b.line(shared("mega.one_recipe"));
        HatchTooltips.standardOc(b);
        if (spec.notPorted()) b.warning(own(id, "not_ported"));
        return finish(b, id);
    }

    /** Heat of {@code CoilWorkableElectricMultipleRecipesMachine}: coil temperature plus a step per tier above MV. */
    static MultiblockTooltipBuilder heat(MultiblockTooltipBuilder b) {
        return b.line(shared("coil.heat"), highlight(shared("coil.heating_coil")),
                value(num(CoilWorkableElectricMultipleRecipesMachine.HEAT_PER_TIER) + " K"))
                .warning(shared("coil.too_cold"));
    }
}
