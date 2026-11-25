package com.raishxn.gtna.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAItems;
import com.raishxn.gtna.common.data.GTNANanoForge;
import com.raishxn.gtna.common.item.terminal.NexusBlockPattern;
import com.raishxn.gtna.common.item.terminal.plan.NexusBuildPlan;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory;
import com.raishxn.gtna.common.machine.multiblock.electric.NanoForgeMachine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Nano Forge as the Nexus Terminal builds it per tier. Tier 4 is a structure of its own (GTNH builds it instead of
 * the tower and its extensions), so a "module build" that reaches it must lay it alone: the tower's blocks and the
 * tier-4
 * blocks claim different blocks in the same cells.
 */
@com.typeomega.lib.test.HeavyTest
@GameTestHolder(GTNACORE.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NexusNanoForgeTierGameTests {

    private NexusNanoForgeTierGameTests() {}

    private static List<NexusBuildPlan> plans(NanoForgeMachine controller, int modules) {
        var terminal = GTNAItems.NEXUS_STRUCTURE_TERMINAL.asStack();
        terminal.getOrCreateTag().putInt("ModuleBuild", modules);
        var setting = NexusTerminalUIFactory.AutoBuildSetting.getSetting(terminal);
        return NexusBlockPattern.fromBlockPattern(controller.getPattern()).plan(controller.getMultiblockState(),
                setting, terminal);
    }

    private static Set<BlockPos> owned(List<NexusBuildPlan> plans) {
        Set<BlockPos> cells = new HashSet<>();
        for (var plan : plans) for (var cell : plan.cells) cells.add(cell.pos);
        return cells;
    }

    private static NanoForgeMachine build(GameTestHelper helper, int modules, int lane) {
        return (NanoForgeMachine) AutoBuildFormationGameTests.autoBuild(helper, GTNANanoForge.NANO_FORGE, 1, modules,
                lane, Direction.NORTH);
    }

    /** The tower and its two extensions are modules; the tier-4 structure is the one alternative after them. */
    @GameTest(template = "empty_12", timeoutTicks = 40)
    public static void tierFourIsAnAlternativeNotAModule(GameTestHelper helper) {
        var modules = com.raishxn.gtna.api.machine.multiblock.GTNASubPatterns.get(GTNANanoForge.NANO_FORGE);
        helper.assertTrue(modules.size() == 2, "tier 2 and 3 are the modules, got " + modules.size());
        helper.assertTrue(com.raishxn.gtna.api.machine.multiblock.IAlternativeStructureMachine.class
                .isAssignableFrom(NanoForgeMachine.class), "the Nano Forge offers alternative structures");
        helper.succeed();
    }

    /** Asking for module 4 builds the tier-4 structure alone, and it forms as tier 4 with no tower block left. */
    @GameTest(template = "empty_16", timeoutTicks = 600, batch = "gtna_nano_tier4_build")
    public static void tierFourBuildFormsAloneAsTierFour(GameTestHelper helper) {
        var controller = build(helper, 4, 8);
        helper.assertTrue(plans(controller, 4).size() == 1, "module 4 is one structure, not the tower plus modules");
        var tierFour = plans(controller, 3).get(0);
        helper.assertTrue(
                tierFour.count(NexusBuildPlan.Kind.PLACE) == 0 && tierFour.count(NexusBuildPlan.Kind.BLOCKED) == 0,
                "every tier-4 cell is satisfied after the build: place " + tierFour.count(NexusBuildPlan.Kind.PLACE) +
                        ", blocked " + tierFour.count(NexusBuildPlan.Kind.BLOCKED));
        boolean formed = controller.isFormed() || controller.checkPatternWithLock();
        helper.assertTrue(formed, "the tier-4 build must form: " + controller.getMultiblockState().error);
        helper.assertTrue(controller.getStructureTier() == 4, "structure tier 4, got " + controller.getStructureTier());

        // The tower's own cells that tier 4 does not own must still be empty: nothing of the lower tiers was laid.
        var inTierFour = owned(List.of(tierFour));
        int stray = 0;
        for (var cell : plans(controller, 2).stream().flatMap(plan -> plan.cells.stream()).toList()) {
            if (!inTierFour.contains(cell.pos) && !cell.existing.isAir()) stray++;
        }
        helper.assertTrue(stray == 0, stray + " blocks of the tower or its extensions were laid next to tier 4");
        helper.succeed();
    }

    /** Negative: a tier-4 structure missing one block does not form, and does not fall back to a lower tier. */
    @GameTest(template = "empty_16", timeoutTicks = 600, batch = "gtna_nano_tier4_hole")
    public static void tierFourMissingBlockDoesNotForm(GameTestHelper helper) {
        var controller = build(helper, 4, 9);
        helper.assertTrue(controller.isFormed() || controller.checkPatternWithLock(), "tier 4 forms before the hole");
        var gap = controller.getMultiblockState().getCache().stream()
                .filter(pos -> !pos.equals(controller.getPos()) && helper.getLevel().getBlockEntity(pos) == null &&
                        !helper.getLevel().getBlockState(pos).isAir())
                .findFirst().orElseThrow();
        helper.getLevel().removeBlock(gap, false);
        helper.assertFalse(controller.checkPatternWithLock(), "a hole in the tier-4 structure must not form");
        helper.assertTrue(controller.getStructureTier() < 4, "no tier 4 without its structure");
        helper.succeed();
    }

    /** Tiers 1-3 keep building as the tower plus the extensions, and without the tier-3 extension it is tier 2. */
    @GameTest(template = "empty_16", timeoutTicks = 600, batch = "gtna_nano_tier3_build")
    public static void towerWithOneExtensionIsTierTwoNotFour(GameTestHelper helper) {
        var controller = build(helper, 1, 10);
        helper.assertTrue(plans(controller, 1).size() == 2, "module 1 is the tower plus the tier-2 extension");
        helper.assertTrue(controller.isFormed() || controller.checkPatternWithLock(),
                "the tower with tier 2 forms: " + controller.getMultiblockState().error);
        helper.assertTrue(controller.getStructureTier() == 2, "structure tier 2, got " + controller.getStructureTier());
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 600, batch = "gtna_nano_tier3_full")
    public static void towerWithBothExtensionsIsTierThree(GameTestHelper helper) {
        var controller = build(helper, 2, 11);
        helper.assertTrue(plans(controller, 2).size() == 3, "module 2 is the tower plus tiers 2 and 3");
        helper.assertTrue(controller.isFormed() || controller.checkPatternWithLock(),
                "the tower with tiers 2 and 3 forms: " + controller.getMultiblockState().error);
        helper.assertTrue(controller.getStructureTier() == 3, "structure tier 3, got " + controller.getStructureTier());
        helper.succeed();
    }
}
