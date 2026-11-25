package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
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
import com.raishxn.gtna.common.data.GTNAMaterials;
import com.raishxn.gtna.common.data.multiblock.NexusAssemblyForgeStructure;

/**
 * Nexus Assembly Forge "Orbital Forge of Crossed Rings" (D-437): the plain zirconia behind the panel counts
 * toward the minimum of 20, hatches only go on the front panel, and the core spindle is load-bearing.
 */
@GameTestHolder(GTNACORE.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NexusAssemblyForgeStructureGameTests {

    private NexusAssemblyForgeStructureGameTests() {}

    static TextAislePlacer.Placed build(GameTestHelper helper) {
        var placed = TextAislePlacer.place(helper, NexusAssemblyForgeStructure.RESOURCE, new BlockPos(2, 1, 2), false,
                1, 'M', (symbol, pos) -> block(symbol));
        controller(helper, placed).self().setFrontFacing(Direction.NORTH);
        return placed;
    }

    private static Block block(char symbol) {
        return switch (symbol) {
            case 'B' -> ChemicalHelper.getBlock(TagPrefix.frameGt, GTNAMaterials.HastelloyN);
            case 'C' -> GTNABlocks.OXIDATION_RESISTANT_HASTELLOY_N_MECHANICAL_CASING.get();
            case 'D', 'd' -> GTNABlocks.ZIRCONIA_CERAMIC_HIGH_STRENGTH_BENDING_RESISTANCE_MECHANICAL_BLOCK.get();
            case 'E' -> GTNABlocks.NAQUADAH_BOROSILICATE_GLASS.get();
            case 'F' -> GTNABlocks.MAGTECH_CASING.get();
            case 'G' -> GTNABlocks.PROCESS_MACHINE_CASING.get();
            case 'H' -> GTBlocks.CASING_ASSEMBLY_LINE.get();
            case 'I' -> GTBlocks.HIGH_POWER_CASING.get();
            case 'J' -> GTNABlocks.COMPRESSOR_CONTROLLER_CASING.get();
            case 'K' -> ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Europium);
            case 'L' -> GTNABlocks.EXTREME_DENSITY_CASING.get();
            case 'M' -> GTNAMachines.NEXUS_MOLECULAR_FORGE.getBlock();
            case 'P' -> GTBlocks.LAMPS.get(DyeColor.PURPLE).get();
            default -> null;
        };
    }

    static IMultiController controller(GameTestHelper helper, TextAislePlacer.Placed placed) {
        return (IMultiController) ((MetaMachineBlockEntity) helper.getBlockEntity(placed.controller()))
                .getMetaMachine();
    }

    @GameTest(template = "empty_48", timeoutTicks = 100)
    public static void crossedRingForgeFormsWithAFullHatchPanel(GameTestHelper helper) {
        var placed = build(helper);
        var controller = controller(helper, placed);
        helper.assertTrue(controller.checkPatternWithLock(), "the forge forms with a plain zirconia panel");
        helper.assertTrue(placed.of('d').size() >= NexusAssemblyForgeStructure.MIN_ZIRCONIA,
                "the plain zirconia alone meets the minimum, got " + placed.of('d').size());
        for (var pos : placed.of('D')) helper.setBlock(pos, GTMachines.ITEM_EXPORT_BUS[GTValues.LuV].getBlock());
        helper.assertTrue(controller.checkPatternWithLock(),
                "every panel slot can hold a hatch: the zirconia behind the panel counts toward the 20");
        helper.succeed();
    }

    @GameTest(template = "empty_48", timeoutTicks = 100)
    public static void crossedRingForgeRejectsHiddenHatchesAndAMissingSpindle(GameTestHelper helper) {
        var placed = build(helper);
        var controller = controller(helper, placed);
        var hidden = placed.of('d').get(0);
        helper.setBlock(hidden, GTMachines.ITEM_EXPORT_BUS[GTValues.LuV].getBlock());
        helper.assertFalse(controller.checkPatternWithLock(), "a hatch behind the panel must not form");
        helper.setBlock(hidden, block('d'));
        var spindle = placed.of('K').stream().filter(p -> p.getX() == placed.controller().getX())
                .min(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
        helper.setBlock(spindle, net.minecraft.world.level.block.Blocks.AIR);
        helper.assertFalse(controller.checkPatternWithLock(), "the core spindle is part of the structure");
        helper.succeed();
    }
}
