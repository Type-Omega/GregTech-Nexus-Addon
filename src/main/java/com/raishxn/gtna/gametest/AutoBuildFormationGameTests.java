package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.mojang.authlib.GameProfile;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAEnergyHatches;
import com.raishxn.gtna.common.data.GTNAItems;
import com.raishxn.gtna.common.data.GTNAMachines;
import com.raishxn.gtna.common.data.GTNAOriginals;
import com.raishxn.gtna.common.item.terminal.NexusAutoBuilder;
import com.raishxn.gtna.common.item.terminal.NexusBlockPattern;
import com.raishxn.gtna.common.item.terminal.NexusBuildCheckGuard;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory;

import java.util.UUID;

/**
 * The Nexus Terminal auto-build, as a creative player uses it: only the controller is placed by hand, the terminal
 * lays the rest, and the structure must form. One batch each, because these structures reach far past the template.
 */
@GameTestHolder(GTNACORE.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AutoBuildFormationGameTests {

    private AutoBuildFormationGameTests() {}

    /**
     * @param lane each test builds in its own lane, far from the templates and from the other lanes: blocks outside
     *             the template are never cleared between batches, and the ring's empty middle must be in loaded chunks
     *             when the pattern is checked.
     */
    static MultiblockControllerMachine autoBuild(GameTestHelper helper, MultiblockMachineDefinition definition,
                                                 int repetitions, int lane) {
        ItemStack terminal = GTNAItems.NEXUS_STRUCTURE_TERMINAL.asStack();
        terminal.getOrCreateTag().putInt("Repetitions", repetitions);
        // EN: out in the lanes there is real terrain, and the terminal never replaces it: lift the controller by what
        // EN: the pattern reaches below it, plus clear air.
        // PT: nas faixas há terreno de verdade, e o terminal nunca o substitui: ergue o controlador pelo que o padrão
        // PT: desce abaixo dele, mais uma folga de ar.
        // ZH: 测试通道里有真实地形，终端不会替换它：按结构在控制器下方延伸的高度抬高控制器，再留出空气余量。
        var pattern = NexusBlockPattern.fromBlockPattern(definition.getPatternFactory().get());
        int below = pattern == null ? 0 : -pattern.lowestOffset(Direction.NORTH, Direction.NORTH, false,
                NexusTerminalUIFactory.AutoBuildSetting.getSetting(terminal));
        var local = new BlockPos(8 + 320 * lane, 200 + below, 8);
        var centre = helper.absolutePos(local);
        for (int cx = (centre.getX() - 96) >> 4; cx <= (centre.getX() + 96) >> 4; cx++)
            for (int cz = (centre.getZ() - 160) >> 4; cz <= (centre.getZ() + 160) >> 4; cz++)
                helper.getLevel().getChunk(cx, cz);
        helper.setBlock(local, definition.getBlock());
        var controller = (MultiblockControllerMachine) ((IMachineBlockEntity) helper.getBlockEntity(local))
                .getMetaMachine();
        controller.setFrontFacing(Direction.NORTH);
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "AutoBuildQA"));
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, terminal);
        NexusAutoBuilder.autoBuild(player, controller, terminal);
        com.raishxn.gtna.common.item.terminal.plan.NexusBuildJobs.drain(player);
        NexusBuildCheckGuard.flushPending();
        return controller;
    }

    private static void assertForms(GameTestHelper helper, MultiblockMachineDefinition definition,
                                    int repetitions, int lane) {
        var controller = autoBuild(helper, definition, repetitions, lane);
        boolean formed = controller.isFormed() || controller.checkPatternWithLock();
        var error = controller.getMultiblockState().error;
        helper.assertTrue(formed, definition.getId() + " must form after the auto-build: " +
                (error == null ? "no error" : error.getPos() + " holds " +
                        helper.getLevel().getBlockState(error.getPos()) + ", controller at " + controller.getPos() +
                        " facing " + controller.getFrontFacing()));
        helper.succeed();
    }

    @GameTest(template = "empty_16", timeoutTicks = 200, batch = "gtna_autobuild_me_storage")
    public static void meStorageAutoBuildForms(GameTestHelper helper) {
        assertForms(helper, GTNAMachines.ME_STORAGE, 3, 1);
    }

    @GameTest(template = "empty_16", timeoutTicks = 200, batch = "gtna_autobuild_flux_matrix")
    public static void fluxMatrixAutoBuildForms(GameTestHelper helper) {
        assertForms(helper, GTNAEnergyHatches.NEXUS_FLUX_MATRIX, 3, 2);
    }

    @GameTest(template = "empty_16", timeoutTicks = 200, batch = "gtna_autobuild_assembly_forge")
    public static void assemblyForgeAutoBuildForms(GameTestHelper helper) {
        assertForms(helper, GTNAMachines.NEXUS_MOLECULAR_FORGE, 1, 3);
    }

    /** Hypercore and Collider reach below the controller; built from a lifted controller they form (dev rig fix). */
    @GameTest(template = "empty_16", timeoutTicks = 400, batch = "gtna_autobuild_hypercore")
    public static void hypercoreAutoBuildForms(GameTestHelper helper) {
        assertForms(helper, GTNAMachines.NEXUS_ME_HYPERCORE, 1, 4);
    }

    @GameTest(template = "empty_16", timeoutTicks = 400, batch = "gtna_autobuild_collider")
    public static void colliderAutoBuildForms(GameTestHelper helper) {
        assertForms(helper, GTNAOriginals.ANTIMATTER_COLLIDER_RING, 1, 5);
    }

    /** Negative: a structure the auto-build left one block short of does not form. */
    @GameTest(template = "empty_16", timeoutTicks = 200, batch = "gtna_autobuild_me_storage_negative")
    public static void meStorageMissingBlockDoesNotForm(GameTestHelper helper) {
        var controller = autoBuild(helper, GTNAMachines.ME_STORAGE, 2, 6);
        helper.assertTrue(controller.isFormed(), "the two-slice conduit forms before the hole is made");
        var gap = controller.getMultiblockState().getCache().stream()
                .filter(pos -> !pos.equals(controller.getPos()) && helper.getLevel().getBlockEntity(pos) == null &&
                        !helper.getLevel().getBlockState(pos).isAir())
                .findFirst().orElseThrow();
        helper.getLevel().removeBlock(gap, false);
        helper.assertFalse(controller.checkPatternWithLock(), "a hole in the auto-built conduit must not form");
        helper.succeed();
    }

    /** The EMI / multiblock preview of each text-aisle structure holds every block of its smallest build. */
    @GameTest(template = "empty_12", timeoutTicks = 200)
    public static void textAislePreviewsShowTheWholeStructure(GameTestHelper helper) {
        check(helper, GTNAMachines.ME_STORAGE, com.raishxn.gtna.common.data.multiblock.MEStorageStructure.RESOURCE);
        check(helper, GTNAEnergyHatches.NEXUS_FLUX_MATRIX,
                com.raishxn.gtna.common.data.multiblock.NexusFluxMatrixStructure.RESOURCE);
        check(helper, GTNAMachines.NEXUS_MOLECULAR_FORGE,
                com.raishxn.gtna.common.data.multiblock.NexusAssemblyForgeStructure.RESOURCE);
        helper.succeed();
    }

    private static void check(GameTestHelper helper, MultiblockMachineDefinition definition, String resource) {
        int expected = 0;
        for (var aisle : com.raishxn.gtna.common.data.multiblock.TextAisles.load(resource))
            for (String row : aisle.rows())
                expected += (int) row.chars().filter(ch -> ch != ' ').count() * aisle.min();
        var shapes = definition.getMatchingShapes();
        helper.assertTrue(!shapes.isEmpty(), definition.getId() + " has a preview");
        int shown = 0;
        for (var plane : shapes.get(0).getBlocks())
            for (var column : plane)
                for (var info : column)
                    if (info != null && !info.getBlockState().isAir()) shown++;
        helper.assertTrue(shown == expected,
                definition.getId() + " preview shows " + shown + " of " + expected + " blocks");
    }
}
