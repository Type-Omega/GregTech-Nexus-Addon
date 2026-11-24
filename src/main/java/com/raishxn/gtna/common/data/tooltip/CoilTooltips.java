package com.raishxn.gtna.common.data.tooltip;

import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.machine.multiblock.electric.MegaAlloyBlastSmelterMachine;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.util.List;

import static com.raishxn.gtna.common.data.tooltip.Fmt.change;
import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.times;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.typeomega.lib.tooltip.TooltipArg.highlight;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/** Coiled multiblocks whose speed and EU depend on the heating coil, with GTCEu's EBF heat rules. */
final class CoilTooltips {

    private CoilTooltips() {}

    static void register() {
        GTNATooltips.register("mega_alloy_blast_smelter", CoilTooltips::megaAlloyBlastSmelter);
    }

    private static List<Component> megaAlloyBlastSmelter() {
        String id = "mega_alloy_blast_smelter";
        var b = start("alloy_blast_smelter").line(own(id, "purpose"))
                .line(own(id, "bonus"), value(change(MegaAlloyBlastSmelterMachine.EU_MULTIPLIER)),
                        value(change(MegaAlloyBlastSmelterMachine.DURATION_MULTIPLIER)))
                .separator();
        ebfHeat(b, MegaAlloyBlastSmelterMachine.HEAT_PER_VOLTAGE_TIER, MegaAlloyBlastSmelterMachine.HEAT_STEP,
                MegaAlloyBlastSmelterMachine.EU_DISCOUNT_PER_STEP);
        b.line(shared("parallel_hatch"), value(MegaAlloyBlastSmelterMachine.MAX_PARALLEL_HATCHES));
        return finish(b, id);
    }

    /** GTCEu's EBF heat rules (GTRecipeModifiers.ebfOverclock / OverclockingLogic.heatingCoilOC). */
    static MultiblockTooltipBuilder ebfHeat(MultiblockTooltipBuilder b, int heatPerTier, int step,
                                            double discount) {
        return b.line(shared("coil.heat"), highlight(shared("coil.heating_coil")), value(num(heatPerTier) + " K"))
                .line(shared("coil.discount"), value(change(discount)), value(num(step) + " K"))
                .line(shared("coil.perfect"), value(num(2L * step) + " K"),
                        value(times(OverclockingLogic.PERFECT_DURATION_FACTOR_INV)),
                        value(times(OverclockingLogic.STD_DURATION_FACTOR_INV)),
                        value(times(OverclockingLogic.STD_VOLTAGE_FACTOR)))
                .warning(shared("coil.too_cold"))
                .warning(shared("voltage_gate"));
    }
}
