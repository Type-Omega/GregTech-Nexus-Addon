package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.api.machine.feature.eyeofharmony.EyeOfHarmonyMath;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeData;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeMath;
import com.raishxn.gtna.api.machine.feature.godforge.GodforgeModuleStats;
import com.raishxn.gtna.common.data.tooltip.GTNATooltips;
import com.raishxn.gtna.common.machine.multiblock.electric.MegaAlloyBlastSmelterMachine;
import com.raishxn.gtna.common.machine.multiblock.electric.PlasmaForgeMachine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The mechanic lines of the GTNH-style tooltips (GTIA-D-432): each migrated machine keeps at least as many lines as
 * the GTNH or GTNL original gives, and a few numbers are checked against the code that applies them.
 */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class MechanicTooltipGameTests {

    private static final String TEMPLATE = "empty_12";

    /** Minimum mechanic lines per machine: the count of the original's mechanic lines, not counting structure. */
    static final Map<String, Integer> MINIMUM = new LinkedHashMap<>();

    static {
        MINIMUM.put("eye_of_harmony", 22);
        MINIMUM.put("mega_alloy_blast_smelter", 7);
        MINIMUM.put("forge_of_gods", 16);
        MINIMUM.put("godforge_smelting_module", 13);
        MINIMUM.put("godforge_molten_module", 13);
        MINIMUM.put("godforge_plasma_module", 11);
        MINIMUM.put("godforge_exotic_module", 12);
        MINIMUM.put("nano_forge", 12);
        MINIMUM.put("dimensionally_transcendent_plasma_forge", 12);
        MINIMUM.put("transcendent_plasma_mixer", 9);
        MINIMUM.put("eye_of_wood", 12);
        MINIMUM.put("bec_assembler", 9);
        MINIMUM.put("bec_storage", 8);
        MINIMUM.put("bec_generator", 6);
        MINIMUM.put("bec_io_node", 15);
        MINIMUM.put("bec_diode", 7);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void mechanicLinesMeetTheMinimum(GameTestHelper h) {
        StringBuilder short_ = new StringBuilder();
        for (var e : MINIMUM.entrySet()) {
            List<Component> tooltip = GTNATooltips.build(e.getKey());
            h.assertTrue(tooltip != null, e.getKey() + " is not migrated to the lib layout");
            int lines = mechanicLines(tooltip);
            if (lines < e.getValue()) short_.append(e.getKey()).append(' ').append(lines).append('<')
                    .append(e.getValue()).append("; ");
        }
        h.assertTrue(short_.isEmpty(), "tooltips with fewer mechanic lines than the original: " + short_);
        h.succeed();
    }

    /** The Mega Alloy Blast Smelter shows GTCEu's coil discount, and the mirrored numbers match GTCEu. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void megaAlloyBlastSmelterCoilBonusMatchesGtceu(GameTestHelper h) {
        int step = MegaAlloyBlastSmelterMachine.HEAT_STEP;
        double discount = OverclockingLogic.getCoilEUtDiscount(1000, 1000 + step);
        h.assertTrue(discount == MegaAlloyBlastSmelterMachine.EU_DISCOUNT_PER_STEP,
                "one heat step must give the mirrored discount, GTCEu gives " + discount);
        h.assertTrue(OverclockingLogic.getCoilEUtDiscount(1000, 1000 + 2 * step) == discount * discount,
                "the discount stacks multiplicatively");
        h.assertTrue(OverclockingLogic.getCoilEUtDiscount(1000, 1000 + step - 1) == 1,
                "less than one step gives no discount");
        String text = text("mega_alloy_blast_smelter");
        for (String expected : new String[] { "-5% EU usage per 900 K", "1,800 K", "4x faster instead of 2x",
                "-20% EU usage and -40% recipe time" }) {
            h.assertTrue(text.contains(expected), "Mega Alloy Blast Smelter tooltip lacks '" + expected + "': " + text);
        }
        h.succeed();
    }

    /** The Eye of Harmony field numbers in the tooltip are the ones the plan applies. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void eyeOfHarmonyFieldNumbersMatchThePlan(GameTestHelper h) {
        var program = EyeOfHarmonyMath.Program.overworld(0);
        var history = EyeOfHarmonyMath.History.fresh();
        var base = EyeOfHarmonyMath.plan(program, new EyeOfHarmonyMath.Fields(0, 0, 0), 0, program.hydrogen(),
                program.helium(), history);
        var dilated = EyeOfHarmonyMath.plan(program, new EyeOfHarmonyMath.Fields(0, 1, 1), 0, program.hydrogen(),
                program.helium(), history);
        h.assertTrue(dilated.durationTicks() == (int) (base.durationTicks() *
                EyeOfHarmonyMath.ACCELERATION_TIME_FACTOR), "one dilation tier halves the time");
        double expected = 1 - EyeOfHarmonyMath.ACCELERATION_CHANCE_LOSS + EyeOfHarmonyMath.STABILISATION_CHANCE_GAIN;
        h.assertTrue(Math.abs(dilated.chance() - expected) < 1e-9, "chance " + dilated.chance() + " != " + expected);
        String text = text("eye_of_harmony");
        for (String needed : new String[] { "-3% time per tier", "-50% time per tier", "-9.25% success chance",
                "+5% success chance", "-40% at the lowest tier", "0-24", "14,400" }) {
            h.assertTrue(text.contains(needed), "Eye of Harmony tooltip lacks '" + needed + "': " + text);
        }
        h.succeed();
    }

    /** The base module numbers in the tooltips are what GodforgeMath gives a Forge with no upgrades. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void godforgeModuleNumbersMatchTheMath(GameTestHelper h) {
        var data = new GodforgeData();
        var smelting = new GodforgeModuleStats(GodforgeModuleStats.Type.SMELTING);
        GodforgeMath.maxHeat(smelting, data);
        GodforgeMath.maxParallel(smelting, data);
        h.assertTrue(smelting.heat == GodforgeMath.BASE_HEAT, "fuel factor 1 gives the base heat");
        String text = text("godforge_smelting_module");
        for (String needed : new String[] { "Base parallels: " + String.format(java.util.Locale.ROOT, "%,d",
                smelting.calculatedMaxParallel), "12,601 K", "-5% EU per 900 K", "15,000 K",
                "2,000,000,000 EU/t" }) {
            h.assertTrue(text.contains(needed), "Smelting module tooltip lacks '" + needed + "': " + text);
        }
        h.assertTrue(text("forge_of_gods").contains("Holds " + GodforgeMath.maxModuleCount(data) + " modules"),
                "the Forge lists the module slots GodforgeMath allows");
        h.succeed();
    }

    /** Plasma Forge discount and Eye of Wood peak come from the machine code. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void plasmaForgeAndEyeOfWoodNumbersMatchTheCode(GameTestHelper h) {
        h.assertTrue(PlasmaForgeMachine.discount((long) PlasmaForgeMachine.MAX_EFFICIENCY_TICKS) ==
                PlasmaForgeMachine.MAXIMUM_DISCOUNT, "full run time gives the maximum discount");
        String forge = text("dimensionally_transcendent_plasma_forge");
        for (String needed : new String[] { "after 8 h the catalysts cost up to 50% less", "100x", "16 h" }) {
            h.assertTrue(forge.contains(needed), "Plasma Forge tooltip lacks '" + needed + "': " + forge);
        }
        String wood = text("eye_of_wood");
        for (String needed : new String[] { "75% with exactly 256,000 mB", "60 s", "up to 270,000,000 mB",
                "Source:" }) {
            h.assertTrue(wood.contains(needed), "Eye of Wood tooltip lacks '" + needed + "': " + wood);
        }
        h.succeed();
    }

    /** Lines from the machine's own or the shared mechanic texts; humour, type, origin and mod do not count. */
    static int mechanicLines(List<Component> tooltip) {
        int n = 0;
        for (Component line : tooltip) {
            if (!(line.getContents() instanceof TranslatableContents t)) continue;
            String key = t.getKey();
            if (!key.startsWith("gtna.") || key.endsWith(".humor") || key.equals("gtna.tooltip.source")) continue;
            n++;
        }
        return n;
    }

    private static String text(String path) {
        StringBuilder out = new StringBuilder();
        for (Component line : GTNATooltips.build(path)) out.append(line.getString()).append('\n');
        return out.toString();
    }

    private MechanicTooltipGameTests() {}
}
