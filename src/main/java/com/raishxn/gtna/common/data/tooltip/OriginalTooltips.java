package com.raishxn.gtna.common.data.tooltip;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.api.machine.feature.collider.ColliderCore;
import com.raishxn.gtna.api.machine.feature.lance.LanceCore;
import com.raishxn.gtna.api.machine.feature.stellar.StellarCore;
import com.raishxn.gtna.common.machine.multiblock.energy.NexusFluxMatrixMachine;
import com.raishxn.gtna.common.machine.multiblock.energy.NexusMEHyperCoreMachine;
import com.raishxn.gtna.common.machine.multiblock.originals.LanceMachine;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.util.List;

import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.pct;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * GTNA originals (Kinetic Orbital Lance, Dimensional Rift Gate, Antimatter Collider Ring, Stellar Containment
 * Chamber) and GTNA systems (Nexus Flux Matrix, Nexus ME Hypercore, Duration Tester): every rule from the machine
 * cores, numbers from their public constants.
 */
final class OriginalTooltips {

    private OriginalTooltips() {}

    static void register() {
        GTNATooltips.register("kinetic_orbital_lance", OriginalTooltips::lance);
        GTNATooltips.register("dimensional_rift_gate", OriginalTooltips::rift);
        GTNATooltips.register("antimatter_collider_ring", OriginalTooltips::collider);
        GTNATooltips.register("stellar_containment_chamber", OriginalTooltips::stellar);
        GTNATooltips.register("nexus_flux_matrix", OriginalTooltips::fluxMatrix);
        GTNATooltips.register("nexus_me_hypercore", OriginalTooltips::hypercore);
        GTNATooltips.register("duration_tester", OriginalTooltips::durationTester);
    }

    private static MultiblockTooltipBuilder begin(String id) {
        return start(id).line(own(id, "purpose")).separator();
    }

    private static MultiblockTooltipBuilder lines(MultiblockTooltipBuilder b, String id, String... keys) {
        for (String key : keys) b.line(own(id, key));
        return b;
    }

    private static String sci(double v) {
        return String.format(java.util.Locale.ROOT, "%.3g", v);
    }

    private static List<Component> lance() {
        String id = "kinetic_orbital_lance";
        var b = begin(id).line(own(id, "charge"), value(sci(LanceMachine.CHARGE_PER_SECOND) + " EU/s"))
                .line(own(id, "bank"), value(sci(LanceCore.BASE_BANK) + " EU"))
                .line(own(id, "budget"))
                .line(own(id, "efficiency"), value(pct(LanceCore.BASE_EFFICIENCY)),
                        value(pct(LanceCore.MAX_EFFICIENCY)))
                .line(own(id, "rails"))
                .line(own(id, "shot"))
                .line(own(id, "regimes"))
                .line(own(id, "recovered"))
                .line(own(id, "aim"), value(num(LanceMachine.AIM_CWUT)))
                .line(own(id, "survey"))
                .line(own(id, "nexus"))
                .line(own(id, "capsules"))
                .separator()
                .line(own(id, "heat"), value(sci(LanceCore.BARREL_HEAT_CAPACITY) + " EU/K"))
                .warning(own(id, "misfire"), value(num(LanceCore.MISFIRE_TEMPERATURE) + " K"))
                .warning(own(id, "drift"))
                .line(own(id, "liner"));
        return finish(b, id);
    }

    private static List<Component> rift() {
        String id = "dimensional_rift_gate";
        var b = lines(begin(id), id, "cycle", "tuning", "resonance", "probes", "harmonics", "chord", "exotic",
                "output", "depth", "biomes", "atlas")
                .warning(own(id, "collapse"))
                .warning(own(id, "dissonance"));
        return finish(b, id);
    }

    private static List<Component> collider() {
        String id = "antimatter_collider_ring";
        var b = begin(id).line(own(id, "cycle"))
                .line(own(id, "energy"), value(num(ColliderCore.RING_RADIUS_M) + " m"))
                .line(own(id, "luminosity"))
                .line(own(id, "significance"))
                .line(own(id, "yield"))
                .line(own(id, "proposals"))
                .line(own(id, "electrons"), value(num(ColliderCore.ELECTRON_BEAM_GEV) + " GeV"))
                .line(own(id, "helium"))
                .line(own(id, "traps"))
                .warning(own(id, "quench"))
                .warning(own(id, "annihilate"));
        return finish(b, id);
    }

    private static List<Component> stellar() {
        String id = "stellar_containment_chamber";
        var b = lines(begin(id), id, "cycle", "mass", "spin", "extraction")
                .line(own(id, "cap"))
                .line(own(id, "quakes"))
                .line(own(id, "regions"))
                .line(own(id, "collapse"), value(num(StellarCore.TOV) + " M☉"),
                        value(num(StellarCore.TOV_EXOTIC) + " M☉"))
                .line(own(id, "black_hole"), value(num(StellarCore.MAX_SPIN)))
                .line(own(id, "bands"))
                .warning(own(id, "kepler"))
                .warning(own(id, "magnetar"))
                .warning(own(id, "unpowered"));
        return finish(b, id);
    }

    private static List<Component> fluxMatrix() {
        String id = "nexus_flux_matrix";
        var b = begin(id).line(own(id, "capacitors"))
                .line(own(id, "efficiency"), value(pct(NexusFluxMatrixMachine.DEFAULT_EFFICIENCY)))
                .line(own(id, "transfer"), value(num(NexusFluxMatrixMachine.BASE_TRANSFER) + " EU/t"))
                .line(own(id, "unlimited"), value(NexusFluxMatrixMachine.MAX_CAPACITORS))
                .line(own(id, "team"))
                .line(own(id, "balance"));
        return finish(b, id);
    }

    private static List<Component> hypercore() {
        String id = "nexus_me_hypercore";
        var b = begin(id).line(own(id, "slots"), value(NexusMEHyperCoreMachine.TOTAL_MODULE_SLOTS))
                .line(own(id, "threads"), value(NexusMEHyperCoreMachine.MODULE_I_THREADS),
                        value(NexusMEHyperCoreMachine.MODULE_II_THREADS),
                        value(NexusMEHyperCoreMachine.MODULE_III_THREADS),
                        value(num(NexusMEHyperCoreMachine.MODULE_IV_THREADS)))
                .line(own(id, "coprocessors"), value(NexusMEHyperCoreMachine.MODULE_I_COPROCESSORS),
                        value(num(NexusMEHyperCoreMachine.MODULE_IV_COPROCESSORS)))
                .line(own(id, "shared"))
                .line(own(id, "transcendent"))
                .warning(own(id, "interface"));
        return finish(b, id);
    }

    private static List<Component> durationTester() {
        String id = "duration_tester";
        var b = begin(id);
        HatchTooltips.special(b, id);
        HatchTooltips.standardOc(b).warning(own(id, "dev"));
        return finish(b, id);
    }
}
