package com.raishxn.gtna.common.data.tooltip;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.machine.multiMachineBase.SteamMultiMachineBase;
import com.raishxn.gtna.common.machine.multiblock.steam.LargeSteamCircuitAssembler;
import com.raishxn.gtna.common.machine.multiblock.steam.LargeSteamSpecs;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.balanced;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.typeomega.lib.tooltip.TooltipArg.configurable;
import static com.typeomega.lib.tooltip.TooltipArg.highlight;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * Large steam multiblocks ported from GT: Not Leisure. The lines start from GTNL's ("N% faster than single-block
 * machines of the same tier", "Can process up to N items at once", the high-pressure rule), with GTNA's numbers from
 * {@link LargeSteamSpecs} and the steam balance of the pack.
 */
final class LargeSteamTooltips {

    // EN: GTCEu runs a bronze single-block steam machine at twice the recipe time (SimpleSteamMachine), and the
    // EN: high-pressure ones at the recipe time; both halve again in a steel multiblock, so the ratio is 2 / d.
    // PT: O GTCEu roda a máquina a vapor de bloco único de bronze no dobro do tempo da receita (SimpleSteamMachine)
    // PT: e a de alta pressão no tempo da receita; as duas caem pela metade numa multi de aço, então a razão é 2 / d.
    // ZH: GTCEu 的青铜单方块蒸汽机以配方时间的两倍运行（SimpleSteamMachine），高压版以配方时间运行；
    // ZH: 钢制多方块同样再减半，所以比例为 2 / d。
    static final double SINGLE_BLOCK_DURATION = 2.0;

    private LargeSteamTooltips() {}

    static void register() {
        // Machines with a GTCEu steam single block compare against it, the others against the recipe time.
        adjustable("large_steam_crusher", "macerator", LargeSteamSpecs.CRUSHER, true);
        adjustable("large_steam_furnace", "furnace", LargeSteamSpecs.FURNACE, true);
        adjustable("large_steam_alloy_smelter", "alloy_smelter", LargeSteamSpecs.ALLOY_SMELTER, true);
        adjustable("large_steam_hammer", "forge_hammer", LargeSteamSpecs.HAMMER, true);
        adjustable("large_steam_compressor", "compressor", LargeSteamSpecs.COMPRESSOR, true);
        adjustable("large_steam_extractor", "extractor", LargeSteamSpecs.EXTRACTOR, true);
        adjustable("large_steam_ore_washer", "ore_washer", LargeSteamSpecs.ORE_WASHER, false, "water");
        adjustable("large_steam_mixer", "mixer", LargeSteamSpecs.MIXER, false);
        adjustable("large_steam_centrifuge", "centrifuge", LargeSteamSpecs.CENTRIFUGE, false);
        adjustable("large_steam_thermal_centrifuge", "thermal_centrifuge", LargeSteamSpecs.THERMAL_CENTRIFUGE,
                false);
        adjustable("large_steam_bath", "chemical_bath", LargeSteamSpecs.BATH, false);
        adjustable("large_steam_lathe", "lathe", LargeSteamSpecs.LATHE, false);
        adjustable("large_steam_cutting", "cutter", LargeSteamSpecs.CUTTING, false);
        adjustable("large_steam_bending", "bender", LargeSteamSpecs.BENDING, false);
        adjustable("large_steam_extruder", "extruder", LargeSteamSpecs.EXTRUDER, false);
        adjustable("large_steam_wiremill", "wiremill", LargeSteamSpecs.WIREMILL, false);
        adjustable("large_steam_sifter", "sifter", LargeSteamSpecs.SIFTER, false);
        adjustable("large_steam_forming_press", "forming_press", LargeSteamSpecs.FORMING_PRESS, false);
        GTNATooltips.register("large_steam_circuit_assembler", LargeSteamTooltips::circuitAssembler);
    }

    private static void adjustable(String id, String type, LargeSteamSpecs.Spec spec, boolean hasSingleBlock,
                                   String... extraLines) {
        GTNATooltips.register(id, () -> {
            var b = start(type).line(own(id, "purpose"));
            double reference = hasSingleBlock ? SINGLE_BLOCK_DURATION : 1.0;
            b.line(shared(hasSingleBlock ? "steam.faster" : "steam.faster_recipe"),
                    balanced(LargeSteamTooltips::percent,
                            v -> reference / v.duration(spec.durationMultiplier()) - 1));
            b.line(shared("steam.parallel"), parallels(spec.maxParallel()));
            b.line(shared("steam.parallel.buttons"), configurable("[+]"), configurable("[-]"));
            for (String extra : extraLines) b.line(own(id, extra));
            return finish(rules(b.separator()), id);
        });
    }

    private static List<Component> circuitAssembler() {
        String id = "large_steam_circuit_assembler";
        var b = start("circuit_assembler")
                .line(own(id, "purpose"))
                .line(own(id, "lock"), value(LargeSteamCircuitAssembler.ENGRAVING_NEEDED),
                        highlight(Component.translatable("gtna.machine." + id + ".engrave_circuit")))
                .line(own(id, "parallel"), parallels(LargeSteamCircuitAssembler.MAX_PARALLELS))
                .line(own(id, "multiply"), value(times(LargeSteamCircuitAssembler.OUTPUT_MULTIPLIER)),
                        value(times(LargeSteamCircuitAssembler.DURATION_MULTIPLIER)))
                .separator();
        return finish(rules(b), id);
    }

    /** High pressure and the steam hatch, shared by every large steam multiblock. */
    private static MultiblockTooltipBuilder rules(MultiblockTooltipBuilder b) {
        return b.line(shared("steam.high_pressure"), highlight(shared("steam.steel")),
                value(times(1 / SteamMultiMachineBase.HIGH_PRESSURE_DURATION_MULTIPLIER)),
                value(times(SteamMultiMachineBase.HIGH_PRESSURE_CONVERSION_MULTIPLIER)))
                .line(shared("steam.hatch"), value(1));
    }

    private static Component parallels(int base) {
        return balanced(n -> NumberFormat.getIntegerInstance(Locale.ROOT).format((long) n),
                v -> v.parallels(base));
    }

    private static String percent(double fraction) {
        return Math.round(fraction * 100) + "%";
    }

    private static String times(double factor) {
        return (factor == Math.rint(factor) ? String.valueOf((long) factor) :
                String.format(Locale.ROOT, "%.2f", factor)) + "x";
    }
}
