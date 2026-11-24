package com.raishxn.gtna.common.data.tooltip;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.machine.multiblock.noenergy.HyperPressureReactor;
import com.raishxn.gtna.common.machine.multiblock.noenergy.InfernalCokeOven;

import java.util.List;

import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * GTNL steam-age machines outside the parallel-button family: the Infernal Coke Oven and the two Hyper Pressure
 * Reactors. Lines start from GTNL's controller tooltips; numbers come from the machine classes.
 */
final class SteamReactorTooltips {

    private SteamReactorTooltips() {}

    static void register() {
        GTNATooltips.register("infernal_coke_oven", SteamReactorTooltips::cokeOven);
        GTNATooltips.register("hyper_pressure_reactor", () -> reactor("hyper_pressure_reactor"));
        GTNATooltips.register("compact_hyper_pressure_reactor", () -> reactor("compact_hyper_pressure_reactor"));
    }

    private static List<Component> cokeOven() {
        String id = "infernal_coke_oven";
        var b = start("coke_oven").line(own(id, "purpose"))
                .line(own(id, "speed"), value(InfernalCokeOven.SPEED_STEP_TICKS / 20 + " s"),
                        value(Math.round(InfernalCokeOven.SPEED_STEP * 100) + "%"),
                        value(LargeSteamTooltips.times(InfernalCokeOven.MAX_SPEED)))
                .line(own(id, "parallel"), value(InfernalCokeOven.BASE_PARALLEL),
                        value(InfernalCokeOven.PARALLEL_INCREASE),
                        value(InfernalCokeOven.TICKS_PER_STAGE / 20 / 60 + " min"),
                        value(InfernalCokeOven.MAX_PARALLEL_CAP))
                .line(own(id, "steam"), value(InfernalCokeOven.BASE_STEAM_PER_TICK + " mB"),
                        value(InfernalCokeOven.STEAM_INCREASE_PER_STAGE + " mB"))
                .separator()
                .warning(own(id, "cooling"));
        return finish(b, id);
    }

    private static List<Component> reactor(String id) {
        var b = start("high_pressure_reactor").line(own(id, "purpose"));
        b.line(own(id, "parallel"), value(HyperPressureReactor.COMPACT_PARALLEL));
        b.separator().line(own("hyper_pressure_reactor", "energy"));
        return finish(b, id);
    }
}
