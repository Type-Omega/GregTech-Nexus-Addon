package com.raishxn.gtna.common.data.tooltip;

import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import com.raishxn.gtna.common.data.GTNANanoForge;
import com.raishxn.gtna.common.machine.multiblock.electric.MegaAlloyBlastSmelterMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.NanoForgeMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.PlasmaForgeMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.TranscendentPlasmaMixerMachine;
import com.raishxn.gtna.common.machine.multiblock.noenergy.EyeOfWoodMachine;

import java.util.List;

import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.pct;
import static com.raishxn.gtna.common.data.tooltip.Fmt.times;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.typeomega.lib.tooltip.TooltipArg.configurable;
import static com.typeomega.lib.tooltip.TooltipArg.highlight;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * Nano Forge, Dimensionally Transcendent Plasma Forge and Transcendent Plasma Mixer, from the GTNH controller
 * tooltips (GT5-Unofficial, LGPL-3.0), and the Eye of Wood from Twist Space Technology (used with permission), with
 * GTNA's numbers.
 */
final class ForgeTooltips {

    private ForgeTooltips() {}

    static void register() {
        GTNATooltips.register("nano_forge", ForgeTooltips::nanoForge);
        GTNATooltips.register("dimensionally_transcendent_plasma_forge", ForgeTooltips::plasmaForge);
        GTNATooltips.register("transcendent_plasma_mixer", ForgeTooltips::plasmaMixer);
        GTNATooltips.register("eye_of_wood", ForgeTooltips::eyeOfWood);
    }

    private static Component nanite(int tier) {
        Item item = GTNANanoForge.tierNanites()[tier - 1];
        return highlight(item.getDescription());
    }

    private static List<Component> nanoForge() {
        String id = "nano_forge";
        int max = NanoForgeMachine.MAX_TIER;
        var b = start("nanite_fabricator").line(own(id, "purpose")).separator()
                .line(own(id, "tier"))
                .line(own(id, "nanites"), nanite(1), nanite(2), nanite(3), nanite(4))
                .line(own(id, "kept"))
                .line(own(id, "perfect"), value(times(OverclockingLogic.PERFECT_DURATION_FACTOR_INV)),
                        value(times(OverclockingLogic.STD_DURATION_FACTOR_INV)))
                .warning(own(id, "low_tier"))
                .separator()
                .line(own(id, "t4"), value(max), nanite(max))
                .line(own(id, "t4_drain"))
                .line(own(id, "t4_parallel"), value(num(NanoForgeMachine.MAGMATTER_PER_PARALLEL) + " mB"),
                        value(max))
                .line(own(id, "t4_time"), value(num(NanoForgeMachine.DURATION_PER_PARALLEL)))
                .line(own(id, "t4_perfect"))
                .warning(own(id, "t4_foreign"));
        return finish(b, id);
    }

    private static List<Component> plasmaForge() {
        String id = "dimensionally_transcendent_plasma_forge";
        var b = start("plasma_forge").line(own(id, "purpose")).separator()
                .line(own(id, "runtime"), value(num(PlasmaForgeMachine.MAX_EFFICIENCY_TICKS / 72_000) + " h"),
                        value(pct(PlasmaForgeMachine.MAXIMUM_DISCOUNT)))
                .line(own(id, "catalysts"))
                .warning(own(id, "decay"), value(PlasmaForgeMachine.DECAY_PER_TICK + "x"))
                .line(own(id, "cap"), value(num(PlasmaForgeMachine.MAX_EFFICIENCY_TICKS *
                        PlasmaForgeMachine.RUNNING_TIME_CAP / 72_000) + " h"))
                .separator();
        CoilTooltips.ebfHeat(b, MegaAlloyBlastSmelterMachine.HEAT_PER_VOLTAGE_TIER,
                MegaAlloyBlastSmelterMachine.HEAT_STEP, MegaAlloyBlastSmelterMachine.EU_DISCOUNT_PER_STEP);
        b.line(own(id, "energy"), value(PlasmaForgeMachine.MAX_ENERGY_HATCHES),
                value(PlasmaForgeMachine.MAX_LASER_HATCHES))
                .warning(own(id, "no_convergence"));
        return finish(b, id);
    }

    private static List<Component> plasmaMixer() {
        String id = "transcendent_plasma_mixer";
        var b = start("plasma_mixer").line(own(id, "purpose")).separator()
                .line(shared("godforge.wireless"))
                .line(own(id, "cost"), value(TranscendentPlasmaMixerMachine.DEFAULT_EU_MULTIPLIER + "x"))
                .line(own(id, "upfront"))
                .line(own(id, "parallel"), configurable(own(id, "parallel_hatch")),
                        value(TranscendentPlasmaMixerMachine.MAX_PARALLEL_HATCHES))
                .line(own(id, "scale"))
                .line(own(id, "affordable"))
                .warning(own(id, "no_overclock"))
                .warning(own(id, "no_energy"));
        return finish(b, id);
    }

    private static List<Component> eyeOfWood() {
        String id = "eye_of_wood";
        double base = EyeOfWoodMachine.SUCCESS_BASE / 100.0;
        var b = start("happiness_master").line(own(id, "purpose")).warning(own(id, "overworld")).separator()
                .line(own(id, "store"))
                .line(own(id, "depends"))
                .line(own(id, "peak"), value(num(base) + "%"), value(num(EyeOfWoodMachine.STANDARD_WATER) + " mB"))
                .line(own(id, "formula"), value(num(EyeOfWoodMachine.SUCCESS_BASE)),
                        value(num(EyeOfWoodMachine.SUCCESS_BASE - 1)))
                .line(own(id, "base"), value(String.format(java.util.Locale.ROOT, "%.4f",
                        EyeOfWoodMachine.SUCCESS_SUBSTRATE)))
                .line(own(id, "difference"), value(num(EyeOfWoodMachine.STANDARD_WATER) + " mB"))
                .warning(own(id, "too_far"), value(num(EyeOfWoodMachine.STANDARD_WATER) + " mB"))
                .line(own(id, "time"), value(Fmt.seconds(EyeOfWoodMachine.DURATION)))
                .line(own(id, "success"))
                .warning(own(id, "failure"), value(num(EyeOfWoodMachine.FAIL_STEAM_PER_POINT) + " mB"),
                        value(num((long) EyeOfWoodMachine.FAIL_STEAM_PER_POINT * EyeOfWoodMachine.SUCCESS_BASE) +
                                " mB"));
        return finish(b, id);
    }
}
