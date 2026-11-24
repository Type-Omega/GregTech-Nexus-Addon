package com.raishxn.gtna.common.data.tooltip;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.api.machine.feature.OverclockHatchMath;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;
import com.typeomega.lib.tooltip.StructureInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.times;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * Mechanic lines shared by many GTNA multiblocks: overclock rules, the special hatches the pattern accepts (Thread,
 * Accelerate, Overclock, Output Boost, Parallel) and the energy hatch limits.
 */
final class HatchTooltips {

    private HatchTooltips() {}

    /** GTCEu standard overclock: 2x faster for 4x EU per voltage tier above the recipe. */
    static MultiblockTooltipBuilder standardOc(MultiblockTooltipBuilder b) {
        return b.line(shared("oc.standard"), value(times(OverclockingLogic.STD_DURATION_FACTOR_INV)),
                value(times(OverclockingLogic.STD_VOLTAGE_FACTOR)));
    }

    /** GTCEu perfect overclock: 4x faster for 4x EU per voltage tier above the recipe. */
    static MultiblockTooltipBuilder perfectOc(MultiblockTooltipBuilder b) {
        return b.line(shared("oc.perfect"), value(times(OverclockingLogic.PERFECT_DURATION_FACTOR_INV)),
                value(times(OverclockingLogic.STD_VOLTAGE_FACTOR)));
    }

    /** A machine whose recipes always run at their own EU/t and time. */
    static MultiblockTooltipBuilder noOc(MultiblockTooltipBuilder b) {
        return b.line(shared("oc.none"));
    }

    static String tier(int tier) {
        return GTValues.VN[tier];
    }

    static String eut(int tier) {
        return num(GTValues.V[tier]) + " EU/t (" + GTValues.VN[tier] + ")";
    }

    /**
     * One line per special hatch the machine's pattern accepts, plus {@code extra} abilities that only sub-patterns
     * (extensions) add. Parallel Hatch first, then Thread, Accelerate, Overclock and Output Boost.
     */
    static MultiblockTooltipBuilder special(MultiblockTooltipBuilder b, String id, String... extra) {
        Set<String> abilities = new HashSet<>(abilities(id));
        abilities.addAll(List.of(extra));
        if (abilities.contains("parallel_hatch")) b.line(shared("hatch.parallel"));
        if (abilities.contains("thread_hatch")) b.line(shared("hatch.thread"));
        if (abilities.contains("accelerate_hatch")) b.line(shared("hatch.accelerate"));
        if (abilities.contains("overclock_hatch")) {
            b.line(shared("hatch.overclock"), value(OverclockHatchMath.MIN_DIVISOR),
                    value(OverclockHatchMath.TIER_OFFSET), value(times(OverclockingLogic.STD_DURATION_FACTOR_INV)));
        }
        if (abilities.contains("output_boost_hatch")) b.line(shared("hatch.output_boost"));
        return b;
    }

    /** Part ability names of the machine's main pattern, empty when it can't be read. */
    static Set<String> abilities(String id) {
        if (!(GTRegistries.MACHINES
                .get(new ResourceLocation(GTNACORE.MOD_ID, id)) instanceof MultiblockMachineDefinition definition)) {
            return Set.of();
        }
        StructureInfo info = GTNATooltips.structure(definition);
        return info == null ? Set.of() : info.abilities();
    }
}
