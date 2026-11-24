package com.raishxn.gtna.common.data.tooltip;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.data.GTNANaquadahReactors;
import com.raishxn.gtna.common.machine.multiblock.electric.AdvancedFusionReactorMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.ComponentAssemblyLineMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.HyperReactorMachine;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.util.List;

import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.seconds;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.raishxn.gtna.common.data.tooltip.HatchTooltips.eut;
import static com.raishxn.gtna.common.data.tooltip.HatchTooltips.tier;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.ENRICHED_DURATION;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.FUEL_PER_RUN;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.HYDROGEN_PER_RUN;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.HYPER_TIER;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.LARGE_PLASMA_TIER;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.LARGE_TIER;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.NAQUADAH_DURATION;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.NITROGEN_BOOST;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.OXYGEN_BOOST;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.PLASMA_DURATION_FACTOR;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.PLASMA_FUEL_PER_RUN;
import static com.raishxn.gtna.data.recipe.GTNANaquadahReactorRecipes.PLASMA_PER_RUN;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * Naquadah reactors, Component Assembly Line and Advanced Fusion Reactor (GTOCore ports and a GTNA original): the
 * mechanics are read from GTNA's code and default recipes, the text is GTNA's own.
 */
final class ReactorTooltips {

    private ReactorTooltips() {}

    static void register() {
        GTNATooltips.register("large_naquadah_reactor", ReactorTooltips::largeNaquadah);
        GTNATooltips.register("naquadah_hyper_reactor", ReactorTooltips::hyperNaquadah);
        GTNATooltips.register("component_assembly_line", ReactorTooltips::componentAssemblyLine);
        GTNATooltips.register("advanced_fusion_reactor", ReactorTooltips::advancedFusion);
    }

    private static MultiblockTooltipBuilder runTime(MultiblockTooltipBuilder b, String key) {
        return b.line(shared(key), value(seconds(NAQUADAH_DURATION)), value(seconds(ENRICHED_DURATION)));
    }

    private static MultiblockTooltipBuilder reactorHatches(MultiblockTooltipBuilder b) {
        return b.line(shared("naquadah.hatches"), value(GTNANaquadahReactors.MAX_FLUID_INPUTS));
    }

    private static List<Component> largeNaquadah() {
        String id = "large_naquadah_reactor";
        var b = start("generator").line(own(id, "purpose")).separator()
                .line(own(id, "hydrogen"), value(FUEL_PER_RUN + " mB"), value(HYDROGEN_PER_RUN + " mB"),
                        value(eut(LARGE_TIER)));
        runTime(b, "naquadah.time")
                .line(own(id, "plasma"), value(PLASMA_FUEL_PER_RUN + " mB"), value(PLASMA_PER_RUN + " mB"),
                        value(eut(LARGE_PLASMA_TIER)), value(PLASMA_DURATION_FACTOR + "x"))
                .line(own(id, "plasma_pair"))
                .line(shared("oc.generator"));
        reactorHatches(b).line(own(id, "rotation"))
                .warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> hyperNaquadah() {
        String id = "naquadah_hyper_reactor";
        var b = start("generator").line(own(id, "purpose"), value(tier(HYPER_TIER))).separator()
                .line(own(id, "hydrogen"), value(FUEL_PER_RUN + " mB"), value(HYDROGEN_PER_RUN + " mB"),
                        value(eut(HYPER_TIER)), value(num(GTValues.V[HYPER_TIER] / GTValues.V[LARGE_TIER]) + "x"));
        runTime(b, "naquadah.time")
                .separator()
                .line(own(id, "boosters"), value(PLASMA_PER_RUN + " mB"), value(OXYGEN_BOOST + "x"),
                        value(PLASMA_PER_RUN + " mB"), value(NITROGEN_BOOST + "x"))
                .line(own(id, "best"))
                .line(own(id, "parallel"))
                .line(own(id, "scale"))
                .warning(own(id, "consumed"))
                .line(own(id, "unboosted"))
                .line(own(id, "default"), value(HyperReactorMachine.DEFAULT_BOOST + "x"))
                .separator()
                .line(shared("oc.generator"));
        reactorHatches(b).line(own(id, "ring"));
        return finish(b, id);
    }

    private static List<Component> componentAssemblyLine() {
        String id = "component_assembly_line";
        var b = start("component_assembly").line(own(id, "purpose")).separator()
                .line(own(id, "tier"), value(tier(ComponentAssemblyLineMachine.MAX_CASING_TIER)))
                .line(own(id, "same"))
                .warning(own(id, "gate"));
        HatchTooltips.special(b, id);
        HatchTooltips.standardOc(b);
        b.line(own(id, "energy"), value(ComponentAssemblyLineMachine.MAX_ENERGY_HATCHES),
                value(ComponentAssemblyLineMachine.MAX_ENERGY_HATCHES))
                .warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> advancedFusion() {
        String id = "advanced_fusion_reactor";
        int max = GTValues.LuV + AdvancedFusionReactorMachine.TIER_EXTENSIONS;
        var b = start("fusion_reactor").line(own(id, "purpose")).separator()
                .line(own(id, "tier"), value(tier(GTValues.LuV)), value(tier(max)))
                .line(own(id, "order"))
                .line(own(id, "buffer"), value(num(AdvancedFusionReactorMachine.BUFFER_PER_INPUT) + " EU"),
                        value(tier(GTValues.LuV)))
                .line(own(id, "startup"))
                .warning(own(id, "too_big"))
                .warning(own(id, "decay"), value(num(AdvancedFusionReactorMachine.HEAT_DECAY_PER_TICK) + " EU"))
                .line(own(id, "cross"))
                .separator();
        // EN: Thread and Overclock Hatches only fit the cross-recipe extension, not the main pattern.
        // PT: As escotilhas de Thread e de Overclock só cabem na extensão de receitas cruzadas, não no padrão
        // principal.
        // ZH: 线程仓和超频仓只能装在跨配方扩展里，主结构中没有它们的位置。
        HatchTooltips.special(b, id, "thread_hatch", "overclock_hatch");
        HatchTooltips.standardOc(b);
        b.line(own(id, "energy"), value(AdvancedFusionReactorMachine.MAX_ENERGY_HATCHES),
                value(AdvancedFusionReactorMachine.MAX_ENERGY_HATCHES))
                .warning(own(id, "not_ported"));
        return finish(b, id);
    }
}
