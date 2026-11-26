package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.ILaserContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAMachines2;
import com.raishxn.gtna.common.data.GTNAMegaMachines;
import com.raishxn.gtna.common.machine.multiblock.part.DebugEnergyHatchPartMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * The Debug Energy Hatch and Debug Laser Target Hatch behave like GTMThings' Creative Energy Hatch: an input-only
 * receiver buffer that recipes drain and the tick refills, never a source that pushes energy out.
 */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtna")
public final class GTNADebugEnergyHatchGameTests {

    private GTNADebugEnergyHatchGameTests() {}

    private static DebugEnergyHatchPartMachine hatchAt(GameTestHelper helper, BlockPos pos, boolean laser) {
        helper.setBlock(pos, (laser ? GTNAMachines2.DEBUG_LASER_HATCH : GTNAMachines2.DEBUG_ENERGY_HATCH).getBlock());
        if (helper.getBlockEntity(pos) instanceof MetaMachineBlockEntity entity &&
                entity.getMetaMachine() instanceof DebugEnergyHatchPartMachine hatch) {
            return hatch;
        }
        helper.fail("the debug " + (laser ? "laser" : "energy") + " hatch did not instantiate");
        return null;
    }

    private static List<EnergyStack> draw(long eu) {
        return new ArrayList<>(List.of(new EnergyStack(eu)));
    }

    /** Voltage, amperage and the 16-tick buffer follow the settings; it is a receiver, not an emitter. */
    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void debugEnergyHatchIsAnInputOnlyBuffer(GameTestHelper helper) {
        var hatch = hatchAt(helper, new BlockPos(2, 2, 2), false);
        hatch.setVoltageTier(GTValues.IV);
        hatch.setDebugAmperage(4);
        var container = hatch.energyContainer;
        long voltage = GTValues.V[GTValues.IV];
        helper.assertTrue(container.getInputVoltage() == voltage, "voltage must follow the tier");
        helper.assertTrue(container.getInputAmperage() == 4, "amperage must follow the setting");
        helper.assertTrue(container.getEnergyCapacity() == voltage * 16 * 4,
                "the buffer is 16 ticks of voltage x amperage, got " + container.getEnergyCapacity());
        helper.assertTrue(container.getHandlerIO() == IO.IN, "the hatch must be input only");
        helper.assertTrue(container.getOutputVoltage() == 0 && container.getOutputAmperage() == 0,
                "the hatch must have no output");
        helper.assertTrue(container.getEnergyStored() == container.getEnergyCapacity(), "the buffer starts full");
        helper.succeed();
    }

    /** Recipes really drain the buffer (it is not a bottomless creative container) and the tick refills it. */
    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void debugEnergyHatchDrainsAndRefillsEveryTick(GameTestHelper helper) {
        var hatch = hatchAt(helper, new BlockPos(2, 2, 2), false);
        hatch.setVoltageTier(GTValues.HV);
        hatch.setDebugAmperage(2);
        var container = hatch.energyContainer;
        long full = container.getEnergyCapacity();
        helper.assertTrue(container.handleRecipeInner(IO.IN, null, draw(full), false) == null,
                "the whole buffer can be drawn");
        helper.assertTrue(container.getEnergyStored() == 0, "the draw must empty the buffer, got " +
                container.getEnergyStored());
        helper.assertTrue(container.handleRecipeInner(IO.IN, null, draw(1), true) != null,
                "nothing is left to draw until the next tick");
        hatch.refill();
        helper.assertTrue(container.getEnergyStored() == full, "the tick must fill the buffer again");
        var left = container.handleRecipeInner(IO.IN, null, draw(full + 100), false);
        helper.assertTrue(left != null && left.get(0).getTotalEU() == 100,
                "a draw larger than the buffer is only partly paid");
        helper.succeed();
    }

    /** The hatch the author hit: GTCEu's Creative Energy Container pushes into neighbours, this one must not. */
    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void debugEnergyHatchDoesNotPushEnergyOut(GameTestHelper helper) {
        BlockPos hatchPos = new BlockPos(2, 2, 2);
        BlockPos furnacePos = new BlockPos(3, 2, 2);
        helper.setBlock(furnacePos, GTMachines.ELECTRIC_FURNACE[GTValues.LV].getBlock());
        var hatch = hatchAt(helper, hatchPos, false);
        if (hatch == null) return;
        hatch.setVoltageTier(GTValues.MAX);
        hatch.setDebugAmperage(64);
        var furnace = ((MetaMachineBlockEntity) helper.getBlockEntity(furnacePos)).getMetaMachine();
        var furnaceContainer = furnace.getTraits().stream()
                .filter(t -> t instanceof com.gregtechceu.gtceu.api.capability.IEnergyContainer)
                .map(t -> (com.gregtechceu.gtceu.api.capability.IEnergyContainer) t)
                .findFirst().orElseThrow();
        for (Direction facing : Direction.values()) {
            if (hatch.isFacingValid(facing)) hatch.setFrontFacing(facing);
            for (Direction side : Direction.values()) {
                helper.assertTrue(!hatch.energyContainer.outputsEnergy(side),
                        "the hatch must not output on " + side + " facing " + facing);
            }
            hatch.energyContainer.serverTick();
            hatch.refill();
        }
        helper.assertTrue(furnaceContainer.getEnergyStored() == 0,
                "the neighbouring machine must get nothing, got " + furnaceContainer.getEnergyStored());
        helper.succeed();
    }

    /** Like GTMThings' hatch it is a plain receiver on every side and an overvoltage cable cannot blow it up. */
    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void debugEnergyHatchReceivesOnAnySideWithoutExploding(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        var hatch = hatchAt(helper, pos, false);
        if (hatch == null) return;
        hatch.setVoltageTier(GTValues.LV);
        hatch.setDebugAmperage(1);
        var container = hatch.energyContainer;
        for (Direction side : Direction.values()) {
            helper.assertTrue(container.inputsEnergy(side), "the hatch must accept energy on " + side);
        }
        helper.assertTrue(!(hatch instanceof com.gregtechceu.gtceu.api.machine.feature.IExplosionMachine),
                "a debug hatch never explodes");
        container.acceptEnergyFromNetwork(Direction.UP, GTValues.V[GTValues.EV], 1);
        helper.assertBlockPresent(GTNAMachines2.DEBUG_ENERGY_HATCH.getBlock(), pos);
        container.handleRecipeInner(IO.IN, null, draw(container.getEnergyCapacity()), false);
        long accepted = container.acceptEnergyFromNetwork(Direction.UP, GTValues.V[GTValues.LV], 1);
        helper.assertTrue(accepted == 1, "an emptied buffer takes an amp from a cable, got " + accepted);
        helper.succeed();
    }

    /** Laser variant: starts at MAX / 4096 A, is a laser container, holds 64 ticks and has the GTMThings limits. */
    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void debugLaserHatchIsAnInputOnlyLaserBuffer(GameTestHelper helper) {
        var hatch = hatchAt(helper, new BlockPos(2, 2, 2), true);
        if (hatch == null) return;
        var container = hatch.energyContainer;
        helper.assertTrue(container instanceof ILaserContainer, "the laser hatch must be a laser container");
        helper.assertTrue(container.getInputAmperage() == 4096, "laser hatch must start at 4096 A");
        helper.assertTrue(container.getInputVoltage() == GTValues.V[GTValues.MAX], "and at MAX voltage");
        helper.assertTrue(container.getHandlerIO() == IO.IN && container.getOutputVoltage() == 0,
                "the laser hatch must be input only");
        hatch.setVoltageTier(GTValues.LV);
        hatch.setDebugAmperage(1);
        helper.assertTrue(
                hatch.getVoltageTier() == GTValues.IV && container.getInputVoltage() == GTValues.V[GTValues.IV],
                "a laser hatch is at least IV");
        helper.assertTrue(container.getInputAmperage() == 256, "a laser hatch is at least 256 A");
        helper.assertTrue(container.getEnergyCapacity() == GTValues.V[GTValues.IV] * 64 * 256,
                "the buffer is 64 ticks, got " + container.getEnergyCapacity());
        helper.succeed();
    }

    /** A real GTCEu multiblock (EBF) with the debug hatch as its only energy input forms and runs a recipe. */
    @GameTest(template = "empty_16", timeoutTicks = 1200)
    public static void debugEnergyHatchPowersAnElectricBlastFurnace(GameTestHelper helper) {
        var rig = GTNAMultiblockRig.build(helper, GTMultiMachines.ELECTRIC_BLAST_FURNACE, true);
        rig.placeIo(GTBlocks.CASING_INVAR_HEATPROOF.get(), 1, 1, 0, 0, false, false);
        rig.form();
        assertOnlyDebugEnergy(helper, rig);
        var recipe = rig.pickRecipe(GTRecipeTypes.BLAST_RECIPES, 1, GTNAMultiblockRig.TRITANIUM_COIL_K);
        helper.assertTrue(recipe != null, "the rig must find an EBF recipe it can feed");
        rig.runAndCheck(recipe, 1, 1);
        helper.succeed();
    }

    /** The same for a GTNA multiblock: 64 coil parallels of the natural gas recipe on the debug hatch. */
    @GameTest(template = "empty_80", timeoutTicks = 1200)
    public static void debugEnergyHatchPowersAGtnaMultiblock(GameTestHelper helper) {
        var recipe = (GTRecipe) helper.getLevel().getRecipeManager()
                .byKey(GTNACORE.id("desulfurizer/natural_gas")).orElseThrow();
        var rig = GTNAMultiblockRig.build(helper, GTNAMegaMachines.DESULFURIZER, true);
        rig.placeIo(GTBlocks.CASING_STAINLESS_CLEAN.get(), 0, 2, 1, 1, false, false);
        rig.form();
        assertOnlyDebugEnergy(helper, rig);
        rig.runAndCheck(recipe, 64, 1 << (GTNAMultiblockRig.TRITANIUM_COIL_K / 900));
        helper.succeed();
    }

    /**
     * Changing the hatch in the GUI of a formed multiblock must leave the machine seeing it at once, with the new
     * voltage, without waiting for the next structure check.
     */
    @GameTest(template = "empty_16", timeoutTicks = 200)
    public static void debugEnergyHatchChangeKeepsTheMultiblockFormed(GameTestHelper helper) {
        var rig = GTNAMultiblockRig.build(helper, GTMultiMachines.ELECTRIC_BLAST_FURNACE, true);
        rig.form();
        var hatch = rig.debugHatches.get(0);
        for (var other : rig.debugHatches) {
            if (other != hatch) other.setVoltageTier(GTValues.LV);
        }
        hatch.setVoltageTier(GTValues.UV);
        helper.assertTrue(rig.controller.isFormed(), "the machine must stay formed");
        var machine = (WorkableElectricMultiblockMachine) rig.controller;
        helper.assertTrue(machine.getMaxVoltage() >= GTValues.V[GTValues.UV],
                "the machine must see the new voltage, got " + machine.getMaxVoltage());
        helper.assertTrue(energyHandlers(machine).contains(hatch.energyContainer),
                "the hatch must stay an energy input of the machine");
        hatch.setVoltageTier(GTValues.MAX);
        helper.assertTrue(machine.getMaxVoltage() == GTValues.V[GTValues.MAX],
                "and again after the next change, got " + machine.getMaxVoltage());
        helper.succeed();
    }

    private static List<?> energyHandlers(WorkableElectricMultiblockMachine machine) {
        var handlers = ((IRecipeCapabilityHolder) machine).getCapabilitiesFlat()
                .getOrDefault(IO.IN, java.util.Map.of()).get(EURecipeCapability.CAP);
        return handlers == null ? List.of() : handlers;
    }

    private static void assertOnlyDebugEnergy(GameTestHelper helper, GTNAMultiblockRig rig) {
        helper.assertTrue(rig.energy.isEmpty(), "no GTCEu energy hatch may remain in the structure");
        helper.assertTrue(!rig.debugHatches.isEmpty(), "the machine must have a debug energy hatch");
        var machine = (WorkableElectricMultiblockMachine) rig.controller;
        var handlers = energyHandlers(machine);
        for (var hatch : rig.debugHatches) {
            helper.assertTrue(PartAbility.INPUT_ENERGY.isApplicable(hatch.getDefinition().getBlock()),
                    "the debug hatch must be an energy input ability");
            helper.assertTrue(handlers.contains(hatch.energyContainer),
                    "the debug hatch buffer must be an energy input of the machine");
        }
        helper.assertTrue(machine.getMaxVoltage() == GTValues.V[GTValues.MAX],
                "the machine must see the hatch voltage, got " + machine.getMaxVoltage());
    }
}
