package com.raishxn.gtna.common.data.tooltip;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.machine.multiblock.noenergy.EyeOfHarmonyMachine;

import java.util.List;

import static com.raishxn.gtna.api.machine.feature.eyeofharmony.EyeOfHarmonyMath.*;
import static com.raishxn.gtna.common.data.tooltip.Fmt.change;
import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.pct;
import static com.raishxn.gtna.common.data.tooltip.Fmt.signed;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.typeomega.lib.tooltip.TooltipArg.configurable;
import static com.typeomega.lib.tooltip.TooltipArg.highlight;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * Endgame machines ported from GTNH (GT5-Unofficial, LGPL-3.0). The lines follow the GTNH controller tooltips and
 * their markdown pages; every number is read from the GTNA balance constants, so a GTNA balance change shows here.
 */
final class EndgameTooltips {

    private EndgameTooltips() {}

    static void register() {
        GTNATooltips.register("eye_of_harmony", EndgameTooltips::eyeOfHarmony);
    }

    private static List<Component> eyeOfHarmony() {
        String id = "eye_of_harmony";
        var b = start("spacetime_manipulator").line(own(id, "purpose")).separator()
                .line(own(id, "fields"), value(MAX_FIELD_TIER + 1))
                .line(own(id, "compression"), highlight(own(id, "compression_name")),
                        value(change(COMPRESSION_TIME_FACTOR)))
                .line(own(id, "dilation"), highlight(own(id, "dilation_name")), value(change(ACCELERATION_TIME_FACTOR)),
                        value(signed(-ACCELERATION_CHANCE_LOSS)))
                .line(own(id, "stabilisation"), highlight(own(id, "stabilisation_name")),
                        value(signed(STABILISATION_CHANCE_GAIN)), value(signed(-STABILISATION_YIELD_LOSS)))
                .line(own(id, "stabilisation_eu"), value(signed(-MAX_FIELD_TIER * STABILISATION_EU_LOSS)),
                        value(signed(STABILISATION_EU_LOSS)))
                .separator()
                .line(own(id, "planet"), highlight(own(id, "planet_block")))
                .line(own(id, "chance"), value(pct(1)), value(pct(PLANET_CHANCE_LOSS)))
                .line(own(id, "time"), value(num(BASE_SECONDS) + " s"), value(num(PLANET_TIME_GROWTH)))
                .line(own(id, "gas"), value(num(GAS_PER_TIER / 1000) + " B"))
                .line(own(id, "success"), value(pct(1)))
                .line(own(id, "failure"), value(num(FAILURE_SPACETIME)))
                .line(own(id, "pity"))
                .separator()
                .line(own(id, "buffer"), value(num(EyeOfHarmonyMachine.GAS_POLL_TICKS / 20) + " s"))
                .warning(own(id, "overflow"))
                .line(own(id, "overflow_ratio"))
                .line(own(id, "overflow_penalty"), value(num(OVERFLOW_SCALE)))
                .separator()
                .line(own(id, "wireless"), highlight(own(id, "data_stick")))
                .line(own(id, "circuit"), configurable(0 + "-" + MAX_CIRCUIT), value(CIRCUIT_COST_FACTOR + "x"))
                .line(own(id, "startup"), value(num(EU_PER_BASE_TICK)))
                .line(own(id, "return"), value(num(RETURN_BASE)), value(num(RETURN_TIER_DIVISOR)))
                .warning(own(id, "no_hatch"))
                .warning(own(id, "no_parallel"));
        return finish(b, id);
    }
}
