package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNABlocks;
import com.raishxn.gtna.common.data.GTNAMachines;
import com.raishxn.gtna.common.data.GTNAMachines2;
import com.raishxn.gtna.common.data.multiblock.MEStorageStructure;
import com.raishxn.gtna.common.data.multiblock.TextAisles;

/** ME Storage "Data Conduit" (D-437): forms with its cores and one access hatch, and keeps the 768 contract. */
@GameTestHolder(GTNACORE.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MEStorageStructureGameTests {

    private MEStorageStructureGameTests() {}

    static TextAislePlacer.Placed build(GameTestHelper helper, int slices, boolean accessHatch) {
        var placed = TextAislePlacer.place(helper, MEStorageStructure.RESOURCE, new BlockPos(2, 1, 2), false, slices,
                'E', (symbol, pos) -> block(symbol));
        if (accessHatch) helper.setBlock(placed.of('D').get(0), GTNAMachines2.ME_STORAGE_ACCESS_HATCH.getBlock());
        controller(helper, placed).self().setFrontFacing(Direction.NORTH);
        return placed;
    }

    private static Block block(char symbol) {
        return switch (symbol) {
            case 'A' -> GTBlocks.COMPUTER_HEAT_VENT.get();
            case 'B', 'D' -> GTBlocks.COMPUTER_CASING.get();
            case 'C' -> GTNABlocks.ABS_CYAN_CASING.get();
            case 'E' -> GTNAMachines.ME_STORAGE.getBlock();
            case 'F' -> ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Titanium);
            case 'G' -> GTBlocks.HIGH_POWER_CASING.get();
            case 'H' -> GTNABlocks.LITHIUM_OXIDE_CERAMIC_HEAT_RESISTANT_SHOCK_RESISTANT_MECHANICAL_CUBE.get();
            case 'L' -> GTBlocks.LAMPS.get(DyeColor.CYAN).get();
            case 'V' -> GTBlocks.CASING_LAMINATED_GLASS.get();
            case 'c' -> GTNABlocks.T1_ME_STORAGE_CORE.get();
            default -> null;
        };
    }

    static IMultiController controller(GameTestHelper helper, TextAislePlacer.Placed placed) {
        return (IMultiController) ((MetaMachineBlockEntity) helper.getBlockEntity(placed.controller()))
                .getMetaMachine();
    }

    @GameTest(template = "empty_48", timeoutTicks = 100)
    public static void dataConduitFormsWithCoresAndOneAccessHatch(GameTestHelper helper) {
        if (GTNAMachines2.ME_STORAGE_ACCESS_HATCH == null) {
            helper.succeed();
            return;
        }
        var placed = build(helper, 4, true);
        helper.assertTrue(placed.of('c').size() == 4 * MEStorageStructure.CORES_PER_SLICE,
                "each slice holds 6 cores, got " + placed.of('c').size());
        helper.assertTrue(controller(helper, placed).checkPatternWithLock(), "the four-slice Data Conduit forms");
        helper.succeed();
    }

    @GameTest(template = "empty_48", timeoutTicks = 100)
    public static void dataConduitNeedsItsAccessHatchAndCores(GameTestHelper helper) {
        if (GTNAMachines2.ME_STORAGE_ACCESS_HATCH == null) {
            helper.succeed();
            return;
        }
        var placed = build(helper, 2, false);
        var controller = controller(helper, placed);
        helper.assertFalse(controller.checkPatternWithLock(), "without an access hatch the conduit must not form");
        helper.setBlock(placed.of('D').get(0), GTNAMachines2.ME_STORAGE_ACCESS_HATCH.getBlock());
        helper.setBlock(placed.of('c').get(0), GTBlocks.COMPUTER_CASING.get());
        helper.assertFalse(controller.checkPatternWithLock(), "a missing storage core breaks the slice");
        helper.succeed();
    }

    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void dataConduitKeepsThe768CoreContract(GameTestHelper helper) {
        int cores = 0;
        for (var aisle : TextAisles.load(MEStorageStructure.RESOURCE)) {
            if (!aisle.repeats()) {
                for (String row : aisle.rows()) helper.assertTrue(row.indexOf('c') < 0, "cores only in the slice");
                continue;
            }
            helper.assertTrue(aisle.min() == 1 && aisle.max() == MEStorageStructure.MAX_SLICES,
                    "the slice repeats 1 to 128 times");
            for (String row : aisle.rows()) cores += (int) row.chars().filter(ch -> ch == 'c').count();
        }
        helper.assertTrue(cores == MEStorageStructure.CORES_PER_SLICE && cores * MEStorageStructure.MAX_SLICES == 768,
                "6 cores per slice, 768 at most; got " + cores);
        helper.succeed();
    }
}
