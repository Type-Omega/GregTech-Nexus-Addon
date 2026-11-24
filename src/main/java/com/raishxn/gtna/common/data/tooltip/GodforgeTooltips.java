package com.raishxn.gtna.common.data.tooltip;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.api.machine.feature.godforge.GodforgeData;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeMath;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeModuleStats;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeModuleStats.Type;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeOverclock;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeUpgrade;
import com.raishxn.gtna.common.machine.multiblock.godforge.ForgeOfGodsMachine;
import com.raishxn.gtna.common.machine.multiblock.godforge.GodforgeExoticModuleMachine;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.util.List;

import static com.raishxn.gtna.api.machine.feature.godforge.GodforgeMath.*;
import static com.raishxn.gtna.common.data.tooltip.Fmt.change;
import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.times;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.typeomega.lib.tooltip.TooltipArg.highlight;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * Forge of Gods and its four modules, from the GTNH controller tooltips and the GTNH Godforge rules
 * (GT5-Unofficial, LGPL-3.0). Base values come from {@link GodforgeMath} run on a Forge with no upgrades.
 */
final class GodforgeTooltips {

    private GodforgeTooltips() {}

    static void register() {
        GTNATooltips.register("forge_of_gods", GodforgeTooltips::forge);
        GTNATooltips.register("godforge_smelting_module", () -> module("godforge_smelting_module", Type.SMELTING));
        GTNATooltips.register("godforge_molten_module", () -> module("godforge_molten_module", Type.MOLTEN));
        GTNATooltips.register("godforge_plasma_module", () -> module("godforge_plasma_module", Type.PLASMA));
        GTNATooltips.register("godforge_exotic_module", () -> module("godforge_exotic_module", Type.EXOTIC));
    }

    static Component upgrade(GodforgeUpgrade upgrade) {
        return highlight(Component.translatable("gtna.godforge.upgrade.tt." + upgrade.ordinal()));
    }

    /** Stats a module gets from a lit Forge with no upgrades. */
    static GodforgeModuleStats base(Type type) {
        GodforgeData data = new GodforgeData();
        GodforgeModuleStats stats = new GodforgeModuleStats(type);
        GodforgeMath.maxHeat(stats, data);
        GodforgeMath.maxParallel(stats, data);
        GodforgeMath.miscParameters(stats, data);
        GodforgeMath.processingVoltage(stats, data);
        return stats;
    }

    private static List<Component> forge() {
        String id = "forge_of_gods";
        var b = start("stellar_forge").line(own(id, "purpose")).separator()
                .line(own(id, "modules"), value(BASE_MODULES), value(MODULES_PER_RING), upgrade(GodforgeUpgrade.CD),
                        value(MODULES_PER_RING), upgrade(GodforgeUpgrade.END))
                .line(shared("godforge.wireless"))
                .line(own(id, "update"), value(num(ForgeOfGodsMachine.UPDATE_PERIOD / 20) + " s"))
                .separator()
                .line(own(id, "startup"), value(num(STARTUP_FUEL_PER_FACTOR)), value(num(STARTUP_FUEL_GROWTH)))
                .line(own(id, "fuel_cap"), value(BASE_FUEL_CAP))
                .line(own(id, "residue"), value(num(RESIDUE_PER_FACTOR)), value(num(RESIDUE_GROWTH)))
                .line(own(id, "stellar_plasma"), value(num(STELLAR_PLASMA_PER_FACTOR)),
                        value(num(STELLAR_PLASMA_GROWTH)))
                .line(own(id, "magmatter"), value(num(MAGMATTER_DIVISOR)))
                .line(shared("godforge.heat"), value(num(BASE_HEAT) + " K"), value(num(HEAT_LOG_BASE)))
                .line(own(id, "battery"))
                .warning(own(id, "out_of_fuel"))
                .separator()
                .line(own(id, "upgrades"), value(GodforgeUpgrade.values().length))
                .line(own(id, "milestones"))
                .line(own(id, "inversion"))
                .line(own(id, "logo"));
        return finish(b, id);
    }

    private static List<Component> module(String id, Type type) {
        GodforgeModuleStats stats = base(type);
        var b = start(type == Type.SMELTING ? "smelting_module" : type == Type.MOLTEN ? "molten_module" :
                type == Type.PLASMA ? "plasma_module" : "exotic_module").line(own(id, "purpose"))
                .line(own(id, "specialty")).separator()
                .line(shared("godforge.part"));
        GodforgeUpgrade unlock = switch (type) {
            case SMELTING -> null;
            case MOLTEN -> GodforgeUpgrade.FDIM;
            case PLASMA -> GodforgeUpgrade.GPCI;
            case EXOTIC -> GodforgeUpgrade.QGPIU;
        };
        if (unlock != null) b.line(shared("godforge.unlock"), upgrade(unlock));
        b.line(shared("godforge.wireless"))
                .line(shared("godforge.parallel"), value(num(stats.calculatedMaxParallel)))
                .line(shared("godforge.voltage"), value(num(stats.processingVoltage) + " EU/t"))
                .line(shared("godforge.overclock"), value(times(stats.overclockTimeFactor)),
                        value(times(com.gregtechceu.gtceu.api.recipe.OverclockingLogic.STD_VOLTAGE_FACTOR)))
                .line(shared("godforge.threads"), value(GTValues.VN[stats.maxThreadHatchTier]));
        if (type == Type.SMELTING || type == Type.MOLTEN) heated(b, stats);
        switch (type) {
            case SMELTING -> b.warning(own(id, "voltage_cap"));
            case MOLTEN -> b.line(own(id, "solid"));
            case PLASMA -> b.line(own(id, "plasma_tier"), value(stats.plasmaTier + 1),
                    upgrade(GodforgeUpgrade.SEDS), upgrade(GodforgeUpgrade.EE))
                    .line(own(id, "multi_step"), upgrade(GodforgeUpgrade.TPTP));
            case EXOTIC -> b.line(own(id, "recipe"), value(GodforgeExoticModuleMachine.NUMBER_OF_INPUTS),
                    value(Fmt.seconds(GodforgeExoticModuleMachine.RECIPE_DURATION)))
                    .line(own(id, "output"), value(num(GodforgeExoticModuleMachine.QGP_PER_PARALLEL) + " mB"),
                            value(num(GodforgeExoticModuleMachine.MAGMATTER_PER_PARALLEL) + " mB"))
                    .line(own(id, "magmatter"), upgrade(GodforgeUpgrade.EE))
                    .warning(own(id, "isolated"), upgrade(GodforgeUpgrade.PA));
        }
        return finish(b, id);
    }

    private static void heated(MultiblockTooltipBuilder b, GodforgeModuleStats stats) {
        b.line(shared("godforge.heat"), value(num(stats.heat) + " K"), value(num(HEAT_LOG_BASE)))
                .line(shared("godforge.heat_bonus"), value(change(stats.heatEnergyDiscount())),
                        value(num(GodforgeOverclock.HEAT_DISCOUNT_THRESHOLD) + " K"),
                        value(times(GodforgeOverclock.DURATION_DECREASE_PER_HEAT_OC)),
                        value(num(GodforgeOverclock.HEAT_OVERCLOCK_THRESHOLD) + " K"))
                .line(shared("godforge.heat_cap"), value(num(BASE_OC_HEAT_CAP) + " K"),
                        value(num(CNTI_OC_HEAT_CAP) + " K"), upgrade(GodforgeUpgrade.CNTI))
                .warning(shared("godforge.too_hot"));
    }
}
