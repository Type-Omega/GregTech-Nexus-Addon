package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAItems;
import com.raishxn.gtna.common.data.GTNAMachines;
import com.raishxn.gtna.common.item.terminal.NexusBlockPattern;
import com.raishxn.gtna.common.item.terminal.NexusBuildCheckGuard;
import com.raishxn.gtna.common.item.terminal.plan.*;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory.AutoBuildSetting;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** GameTests of the Nexus Terminal v2 foundation (D-439 P0): plan, batch materials, per-tick job. */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class NexusTerminalPlanGameTests {

    /** A controller (the Eye of Harmony block, with a test pattern) facing north at {@code local}. */
    static MultiblockControllerMachine controller(GameTestHelper helper, BlockPos local) {
        helper.setBlock(local, GTNAMachines.EYE_OF_HARMONY.getBlock());
        var machine = (MultiblockControllerMachine) ((IMachineBlockEntity) helper.getBlockEntity(local))
                .getMetaMachine();
        machine.setFrontFacing(Direction.NORTH);
        return machine;
    }

    static Player player(GameTestHelper helper, GameType mode) {
        var player = FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "NexusPlanQA"));
        player.setGameMode(mode);
        player.getInventory().clearContent();
        return player;
    }

    /** An 11 x 11 block of {@code block} per aisle, the controller in the middle of the first one. */
    static NexusBlockPattern cube(MultiblockControllerMachine machine, Block block) {
        String full = "SSSSSSSSSSS";
        String[] first = new String[11], rest = new String[11];
        for (int i = 0; i < 11; i++) {
            first[i] = i == 5 ? "SSSSS~SSSSS" : full;
            rest[i] = full;
        }
        var raw = FactoryBlockPattern.start().aisle(first).aisle(rest).setRepeatable(1, 64)
                .where('~', Predicates.controller(Predicates.blocks(machine.getBlockState().getBlock())))
                .where('S', Predicates.blocks(block)).build();
        return NexusBlockPattern.fromBlockPattern(raw);
    }

    static ItemStack terminal(int repetitions) {
        ItemStack terminal = GTNAItems.NEXUS_STRUCTURE_TERMINAL.asStack();
        terminal.getOrCreateTag().putInt("Repetitions", repetitions);
        return terminal;
    }

    // EN: 11 x 11 x 41 = 4961 cells, above the instant limit: the build must go through the per-tick job.
    // EN: Ceiling of 2.5 s for scan and the whole job on the CI machine; the old builder took 1.9 s for ~20k cells
    // EN: in one tick, so the plan and job must not be slower than that per cell.
    // PT: 11 x 11 x 41 = 4961 células, acima do limite instantâneo: o build tem de passar pelo job por tick.
    // PT: Teto de 2,5 s para a varredura e o job inteiro na máquina de CI; o builder antigo levava 1,9 s para
    // PT: ~20 mil células num tick, então o plano e o job não podem ser mais lentos que isso por célula.
    // ZH: 11 x 11 x 41 = 4961 格，超过即时上限：必须走逐刻任务。CI 机器上扫描加整个任务上限 2.5 秒；
    // ZH: 旧建造器一刻内处理约 2 万格用时 1.9 秒，所以每格不能比它慢。
    @GameTest(template = "empty_48", timeoutTicks = 400)
    public static void bigBuildRunsAsAJobWithinTheTimeCeiling(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(24, 12, 45));
        var pattern = cube(machine, Blocks.STONE);
        var player = player(helper, GameType.CREATIVE);
        var terminal = terminal(40);
        var setting = AutoBuildSetting.getSetting(terminal);
        var state = machine.getMultiblockState();
        int work = pattern.plan(state, setting, terminal).get(0).work();
        helper.assertTrue(work == 4960, "fixture: 4960 empty cells, found " + work);
        long started = System.nanoTime();
        boolean[] ok = new boolean[1];
        NexusBuildCheckGuard.run(state, () -> ok[0] = pattern.autoBuild(player, state, setting, terminal));
        helper.assertTrue(ok[0], "the build must not be refused (origin " + machine.getPos() + ")");
        helper.assertTrue(NexusBuildJobs.isRunning(player), "a build above 4096 cells must be a job");
        NexusBuildJobs.drain(player);
        long ms = (System.nanoTime() - started) / 1_000_000;
        GTNACORE.LOGGER.info("[GTNA perf] Nexus Terminal job of 4961 cells in {} ms", ms);
        helper.assertTrue(ms < 2500, "plan and job of 4961 cells took " + ms + " ms (ceiling 2500)");
        var check = pattern.plan(state, setting, terminal).get(0);
        helper.assertTrue(check.count(NexusBuildPlan.Kind.SATISFIED) == 4961 - 1,
                "every cell must be built: " + check.count(NexusBuildPlan.Kind.PLACE) + " still empty");
        helper.succeed();
    }

    @GameTest(template = "empty_48", timeoutTicks = 200)
    public static void cancelledJobStopsAndReturnsTheRest(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(24, 12, 45));
        var pattern = cube(machine, Blocks.STONE);
        var player = player(helper, GameType.CREATIVE);
        var terminal = terminal(40);
        var state = machine.getMultiblockState();
        NexusBuildCheckGuard.run(state,
                () -> pattern.autoBuild(player, state, AutoBuildSetting.getSetting(terminal), terminal));
        helper.assertTrue(NexusBuildJobs.cancel(player) && !NexusBuildJobs.isRunning(player),
                "a running job must be cancellable");
        var left = pattern.plan(state, AutoBuildSetting.getSetting(terminal), terminal).get(0);
        helper.assertTrue(left.count(NexusBuildPlan.Kind.PLACE) > 0, "a job cancelled before its first tick " +
                "must leave cells unbuilt");
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void survivalBuildTakesFromTheInventoryInOneBatch(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(8, 8, 8));
        var raw = FactoryBlockPattern.start().aisle("~SSS")
                .where('~', Predicates.controller(Predicates.blocks(machine.getBlockState().getBlock())))
                .where('S', Predicates.blocks(Blocks.GOLD_BLOCK)).build();
        var pattern = NexusBlockPattern.fromBlockPattern(raw);
        var player = player(helper, GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(Items.GOLD_BLOCK, 2));
        var terminal = terminal(0);
        var state = machine.getMultiblockState();
        NexusBuildCheckGuard.run(state,
                () -> pattern.autoBuild(player, state, AutoBuildSetting.getSetting(terminal), terminal));
        var plan = pattern.plan(state, AutoBuildSetting.getSetting(terminal), terminal).get(0);
        helper.assertTrue(plan.count(NexusBuildPlan.Kind.SATISFIED) == 2 &&
                plan.count(NexusBuildPlan.Kind.PLACE) == 1, "two gold blocks for three cells: two placed, one left");
        helper.assertTrue(player.getInventory().countItem(Items.GOLD_BLOCK) == 0,
                "the whole inventory stock must be used");
        helper.succeed();
    }

    /** A plain ME storage in memory that counts its calls, standing in for a grid. */
    static class CountingStorage implements MEStorage {

        final Map<AEKey, Long> items = new HashMap<>();
        int extracts, simulations;

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            long have = items.getOrDefault(what, 0L), got = Math.min(have, amount);
            if (mode == Actionable.SIMULATE) simulations++;
            else {
                extracts++;
                items.put(what, have - got);
            }
            return got;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (mode == Actionable.MODULATE) items.merge(what, amount, Long::sum);
            return amount;
        }

        @Override
        public Component getDescription() {
            return Component.literal("test");
        }
    }

    @GameTest(template = "empty_48", timeoutTicks = 200)
    public static void meReservationIsOneExtractionPerItem(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(24, 12, 45));
        var pattern = cube(machine, Blocks.STONE);
        var player = player(helper, GameType.SURVIVAL);
        var storage = new CountingStorage();
        AEItemKey stone = AEItemKey.of(Items.STONE);
        storage.items.put(stone, 1000L);
        var me = new NexusAE2Link.Session(null, storage, IActionSource.ofPlayer(player));
        var terminal = terminal(4); // 11 x 11 x 5 = 605 cells, built at once
        var state = machine.getMultiblockState();
        var plans = pattern.plan(state, AutoBuildSetting.getSetting(terminal), terminal);
        var materials = new NexusMaterials(player, me);
        materials.survey(plans);
        var need = materials.assign(plans);
        materials.reserve(plans);
        helper.assertTrue(need.get(stone) == 604 && storage.extracts == 1 && storage.simulations == 1,
                "604 stone must cost one simulation and one extraction, got " + storage.simulations + " / " +
                        storage.extracts);
        var executor = new NexusBuildExecutor(plans, player, materials, Direction.NORTH);
        NexusBuildJobs.start(player, state, executor, materials, plans.get(0).work());
        helper.assertTrue(executor.placed == 604 && storage.items.get(stone) == 396,
                "all cells placed from the ME, the rest stays there: placed " + executor.placed);
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void scanColoursCellsWithoutTouchingTheWorld(GameTestHelper helper) {
        var machine = controller(helper, new BlockPos(8, 8, 8));
        var raw = FactoryBlockPattern.start().aisle("~SSSA")
                .where('~', Predicates.controller(Predicates.blocks(machine.getBlockState().getBlock())))
                .where('S', Predicates.blocks(Blocks.GOLD_BLOCK)).where('A', Predicates.air()).build();
        var player = player(helper, GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(Items.GOLD_BLOCK, 1));
        var terminal = terminal(0);
        terminal.getOrCreateTag().putBoolean("DemolitionMode", true);
        var origin = machine.getPos();
        // Pattern columns run west from the controller facing north: S at 1..3, A at 4.
        helper.getLevel().setBlock(origin.west(1), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        helper.getLevel().setBlock(origin.west(2), Blocks.DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(origin.west(4), Blocks.STONE.defaultBlockState(), 3);
        var pattern = NexusBlockPattern.fromBlockPattern(raw);
        var plans = pattern.plan(machine.getMultiblockState(), AutoBuildSetting.getSetting(terminal), terminal);
        var materials = new NexusMaterials(player, null);
        materials.survey(plans);
        materials.assign(plans);
        var result = new NexusScan.Result(plans, Map.of(), Map.of(), false, false);
        helper.assertTrue(result.count(NexusBuildPlan.Kind.SATISFIED) == 1 &&
                result.count(NexusBuildPlan.Kind.BLOCKED) == 1 && result.count(NexusBuildPlan.Kind.PLACE) == 1 &&
                result.count(NexusBuildPlan.Kind.CLEAR) == 1, "one of each: right, wrong, empty, must-be-air");
        var boxes = NexusScan.boxes(result, 1);
        helper.assertTrue(boxes != null, "boxes packet");
        helper.assertTrue(helper.getLevel().getBlockState(origin.west(2)).is(Blocks.DIRT) &&
                helper.getLevel().getBlockState(origin.west(4)).is(Blocks.STONE) &&
                player.getInventory().countItem(Items.GOLD_BLOCK) == 1, "a scan must not change the world");
        int placeColour = NexusScan.color(plans.get(0).cells.stream()
                .filter(c -> c.kind == NexusBuildPlan.Kind.PLACE).findFirst().orElseThrow());
        helper.assertTrue(placeColour == NexusScan.PLACE, "the gold in the inventory covers the empty cell");
        helper.succeed();
    }
}
