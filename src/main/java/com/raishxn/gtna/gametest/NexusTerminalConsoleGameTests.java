package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.common.item.terminal.NexusBlockPattern;
import com.raishxn.gtna.common.item.terminal.NexusBuildCheckGuard;
import com.raishxn.gtna.common.item.terminal.plan.*;
import com.raishxn.gtna.common.item.terminal.ui.BlockSelectionConfigWidget;
import com.raishxn.gtna.common.item.terminal.ui.BlockSelectionConfigWidget.BlockCategory;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory.AutoBuildSetting;

import java.util.Map;

import static com.raishxn.gtna.gametest.NexusTerminalPlanGameTests.*;

/** GameTests of the Nexus Terminal console functions (D-440): family swap, nearby machines, saved structures. */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class NexusTerminalConsoleGameTests {

    private static NexusScan.Result scan(NexusBlockPattern pattern,
                                         com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine m,
                                         net.minecraft.world.item.ItemStack terminal) {
        var plans = pattern.plan(m.getMultiblockState(), AutoBuildSetting.getSetting(terminal), terminal);
        return new NexusScan.Result(plans, Map.of(), Map.of(), true, false);
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void familySwapChangesEveryCoilOfTheStructure(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(8, 8, 8));
        var raw = FactoryBlockPattern.start().aisle("~SSS")
                .where('~', Predicates.controller(Predicates.blocks(machine.getBlockState().getBlock())))
                .where('S', Predicates.heatingCoils()).build();
        var pattern = NexusBlockPattern.fromBlockPattern(raw);
        var player = player(helper, GameType.CREATIVE);
        var terminal = terminal(0);
        var coils = BlockSelectionConfigWidget.getEntries(BlockCategory.COILS);
        helper.assertTrue(coils.size() > 2, "fixture: at least three coil tiers");
        BlockSelectionConfigWidget.select(terminal, BlockCategory.COILS, coils.get(0));
        var state = machine.getMultiblockState();
        NexusBuildCheckGuard.run(state,
                () -> pattern.autoBuild(player, state, AutoBuildSetting.getSetting(terminal), terminal));
        var families = NexusFamilySwap.families(scan(pattern, machine, terminal));
        helper.assertTrue(families.stream().anyMatch(f -> f.category() == BlockCategory.COILS && f.cells() == 3 &&
                f.current().is(coils.get(0).getItem())), "the coil family is found with its tier: " + families);
        var target = coils.get(2);
        helper.assertTrue(NexusFamilySwap.swap(player, machine, pattern, terminal, BlockCategory.COILS, target),
                "swap runs");
        NexusBuildJobs.drain(player);
        var block = ((BlockItem) target.getItem()).getBlock();
        for (int i = 1; i <= 3; i++) helper.assertTrue(helper.getLevel().getBlockState(machine.getPos().west(i))
                .is(block), "every coil is the new tier");
        helper.assertTrue(!terminal.getOrCreateTag().getBoolean("ReplaceMode"),
                "the terminal's replace setting is restored");
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 20)
    public static void nearbyListFindsControllersInRange(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(8, 8, 8));
        var player = player(helper, GameType.CREATIVE);
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8, 8, 2)));
        var found = NexusNearby.find(player, NexusNearby.RADIUS);
        helper.assertTrue(found.stream().anyMatch(e -> e.pos().equals(machine.getPos())), "the controller is listed");
        player.moveTo(player.position().add(200, 0, 0));
        helper.assertTrue(NexusNearby.find(player, NexusNearby.RADIUS).stream()
                .noneMatch(e -> e.pos().equals(machine.getPos())), "a controller out of range is not listed");
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void savedStructurePastesTurnedToTheTarget(GameTestHelper helper) {
        var a = controller(helper, new BlockPos(4, 4, 4));
        var raw = FactoryBlockPattern.start().aisle("~SS")
                .where('~', Predicates.controller(Predicates.blocks(a.getBlockState().getBlock())))
                .where('S', Predicates.blocks(Blocks.GOLD_BLOCK, Blocks.IRON_BLOCK)).build();
        var pattern = NexusBlockPattern.fromBlockPattern(raw);
        var player = player(helper, GameType.CREATIVE);
        var terminal = terminal(0);
        helper.getLevel().setBlock(a.getPos().west(1), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        helper.getLevel().setBlock(a.getPos().west(2), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        var data = new NexusBlueprints();
        var saved = data.save(player, "qa", a, scan(pattern, a, terminal));
        helper.assertTrue(saved != null && saved.size() == 2, "two blocks saved");
        var b = controller(helper, new BlockPos(11, 4, 11));
        b.setFrontFacing(Direction.EAST);
        var result = NexusBlueprints.paste(player, b, pattern, terminal, saved);
        NexusBuildJobs.drain(player);
        helper.assertTrue(result == NexusBlueprints.PasteResult.DONE, "paste runs");
        // Facing north the pattern runs west; turned to face east it runs north.
        helper.assertTrue(helper.getLevel().getBlockState(b.getPos().north(1)).is(Blocks.IRON_BLOCK) &&
                helper.getLevel().getBlockState(b.getPos().north(2)).is(Blocks.GOLD_BLOCK),
                "the saved layout comes back turned with the controller");
        helper.assertTrue(data.find(player, "qa") != null && data.delete(player, "qa") && data.list(player).isEmpty(),
                "find and delete by name");
        helper.succeed();
    }
}
