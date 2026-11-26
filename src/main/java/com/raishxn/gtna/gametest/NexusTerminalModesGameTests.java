package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.common.item.terminal.NexusBlockPattern;
import com.raishxn.gtna.common.item.terminal.NexusBuildCheckGuard;
import com.raishxn.gtna.common.item.terminal.plan.*;
import com.raishxn.gtna.common.item.terminal.plan.NexusHatchPlan.Function;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory.AutoBuildSetting;

import java.util.List;
import java.util.Map;

import static com.raishxn.gtna.gametest.NexusTerminalPlanGameTests.*;

/** GameTests of the Nexus Terminal v2 modes (D-439): swap, dismantle, hatch plan, report. */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class NexusTerminalModesGameTests {

    private static NexusBlockPattern goldRow(com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine m) {
        var raw = FactoryBlockPattern.start().aisle("~SSS")
                .where('~', Predicates.controller(Predicates.blocks(m.getBlockState().getBlock())))
                .where('S', Predicates.blocks(Blocks.GOLD_BLOCK)).build();
        return NexusBlockPattern.fromBlockPattern(raw);
    }

    private static void build(GameTestHelper helper, NexusBlockPattern pattern,
                              com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine machine,
                              net.minecraft.world.entity.player.Player player, ItemStack terminal) {
        var state = machine.getMultiblockState();
        NexusBuildCheckGuard.run(state,
                () -> pattern.autoBuild(player, state, AutoBuildSetting.getSetting(terminal), terminal));
        NexusBuildJobs.drain(player);
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void swapReturnsTheOldBlockAndPlacesTheNewOne(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(8, 8, 8));
        var origin = machine.getPos();
        helper.getLevel().setBlock(origin.west(1), Blocks.DIRT.defaultBlockState(), 3);
        var player = player(helper, GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(Items.GOLD_BLOCK, 3));
        var terminal = terminal(0);
        terminal.getOrCreateTag().putBoolean("ReplaceMode", true);
        build(helper, goldRow(machine), machine, player, terminal);
        helper.assertTrue(helper.getLevel().getBlockState(origin.west(1)).is(Blocks.GOLD_BLOCK),
                "the wrong block must be swapped for the right one");
        helper.assertTrue(player.getInventory().countItem(Items.DIRT) == 1 &&
                player.getInventory().countItem(Items.GOLD_BLOCK) == 0, "the old block comes back to the player");
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void swapWithoutRoomKeepsTheOldBlock(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(8, 8, 8));
        var origin = machine.getPos();
        helper.getLevel().setBlock(origin.west(1), Blocks.DIRT.defaultBlockState(), 3);
        var player = player(helper, GameType.SURVIVAL);
        for (int i = 0; i < player.getInventory().items.size(); i++)
            player.getInventory().items.set(i, new ItemStack(Items.COBBLESTONE, 64));
        // The gold comes from an ME that takes nothing back, and the inventory is full.
        var storage = new CountingStorage() {

            @Override
            public long insert(appeng.api.stacks.AEKey what, long amount, appeng.api.config.Actionable mode,
                               appeng.api.networking.security.IActionSource source) {
                return 0;
            }
        };
        storage.items.put(appeng.api.stacks.AEItemKey.of(Items.GOLD_BLOCK), 3L);
        var me = new com.raishxn.gtna.integration.ae2.NexusAE2Link.Session(null, storage,
                appeng.api.networking.security.IActionSource.ofPlayer(player));
        var terminal = terminal(0);
        terminal.getOrCreateTag().putBoolean("ReplaceMode", true);
        var state = machine.getMultiblockState();
        var plans = goldRow(machine).plan(state, AutoBuildSetting.getSetting(terminal), terminal);
        var materials = new NexusMaterials(player, me);
        materials.survey(plans);
        materials.assign(plans);
        materials.reserve(plans);
        var executor = new NexusBuildExecutor(plans, player, materials, Direction.NORTH);
        NexusBuildJobs.start(player, state, executor, materials, plans.get(0).work());
        helper.assertTrue(helper.getLevel().getBlockState(origin.west(1)).is(Blocks.DIRT) && executor.replaced == 0,
                "with nowhere to put the old block the swap must not happen");
        helper.assertTrue(helper.getLevel().getBlockState(origin.west(2)).is(Blocks.GOLD_BLOCK),
                "the empty cells are still built");
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void dismantleReturnsEveryStructureBlockAndKeepsTheController(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(8, 8, 8));
        var origin = machine.getPos();
        var player = player(helper, GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(Items.GOLD_BLOCK, 3));
        var terminal = terminal(0);
        build(helper, goldRow(machine), machine, player, terminal);
        var result = NexusDismantle.run(player, machine, goldRow(machine), terminal);
        helper.assertTrue(result.outcome() == NexusDismantle.Outcome.DONE && result.removed() == 3,
                "three gold blocks, not the controller: " + result);
        helper.assertTrue(helper.getLevel().getBlockState(origin.west(2)).isAir(), "the structure is gone");
        helper.assertTrue(helper.getLevel().getBlockState(origin).is(machine.getBlockState().getBlock()),
                "the controller stays");
        helper.assertTrue(player.getInventory().countItem(Items.GOLD_BLOCK) == 3, "the blocks are in the inventory");
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void dismantleIsRefusedWithoutRoom(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(8, 8, 8));
        var origin = machine.getPos();
        var player = player(helper, GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(Items.GOLD_BLOCK, 3));
        var terminal = terminal(0);
        build(helper, goldRow(machine), machine, player, terminal);
        for (int i = 0; i < player.getInventory().items.size(); i++)
            player.getInventory().items.set(i, new ItemStack(Items.COBBLESTONE, 64));
        var result = NexusDismantle.run(player, machine, goldRow(machine), terminal);
        helper.assertTrue(result.outcome() == NexusDismantle.Outcome.NO_ROOM, "full inventory must refuse");
        helper.assertTrue(helper.getLevel().getBlockState(origin.west(1)).is(Blocks.GOLD_BLOCK),
                "a refused dismantle must leave every block");
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 20)
    public static void hatchPlanPicksFiltersAndNeverEmpties(GameTestHelper helper) {
        ItemStack lvBus = GTMachines.ITEM_IMPORT_BUS[GTValues.LV].asStack();
        ItemStack hvBus = GTMachines.ITEM_IMPORT_BUS[GTValues.HV].asStack();
        ItemStack energy = GTMachines.ENERGY_INPUT_HATCH[GTValues.LV].asStack();
        ItemStack casing = new ItemStack(Items.IRON_BLOCK);
        helper.assertTrue(NexusHatchPlan.classify(lvBus.getItem()) == Function.INPUT_BUS &&
                NexusHatchPlan.classify(energy.getItem()) == Function.ENERGY &&
                NexusHatchPlan.classify(casing.getItem()) == null, "functions are read from the machine ID");
        var terminal = terminal(0);
        var candidates = List.of(lvBus, hvBus, energy, casing);
        NexusHatchPlan.set(terminal, Function.INPUT_BUS, "gtceu:hv_input_bus");
        var picked = NexusHatchPlan.read(terminal).apply(candidates);
        helper.assertTrue(picked.size() == 3 && picked.get(0).is(hvBus.getItem()),
                "a pick keeps only that bus, the rest untouched: " + picked);
        NexusHatchPlan.set(terminal, Function.INPUT_BUS, NexusHatchPlan.NONE);
        NexusHatchPlan.set(terminal, Function.ENERGY, NexusHatchPlan.NONE);
        helper.assertTrue(NexusHatchPlan.read(terminal).apply(candidates).equals(List.of(casing)),
                "None drops the function's hatches");
        helper.assertTrue(NexusHatchPlan.read(terminal).apply(List.of(energy)).equals(List.of(energy)),
                "a cell that would be left empty keeps its candidates, so required hatches are kept");
        NexusHatchPlan.set(terminal, Function.INPUT_BUS, null);
        helper.assertTrue(NexusHatchPlan.read(terminal).pick(Function.INPUT_BUS) == null, "null goes back to auto");
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void reportListsEveryKindWithPositions(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(8, 8, 8));
        var origin = machine.getPos();
        helper.getLevel().setBlock(origin.west(1), Blocks.DIRT.defaultBlockState(), 3);
        var player = player(helper, GameType.SURVIVAL);
        var terminal = terminal(0);
        var plans = goldRow(machine).plan(machine.getMultiblockState(), AutoBuildSetting.getSetting(terminal),
                terminal);
        var materials = new NexusMaterials(player, null);
        materials.survey(plans);
        var need = materials.assign(plans);
        var report = NexusReport.build(new NexusScan.Result(plans, need, Map.of(), false, false), Map.of());
        var groups = report.groups();
        helper.assertTrue(groups.get(0).total() == 2 && groups.get(1).total() == 1,
                "two cells without gold, one blocked by dirt: " + groups);
        helper.assertTrue(groups.get(1).lines().get(0).pos().equals(origin.west(1)),
                "each line keeps its position");
        helper.assertTrue(groups.get(groups.size() - 1).total() == 1, "one material line (gold)");
        helper.succeed();
    }
}
