package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.common.cover.DebugEnergyCover;
import com.raishxn.gtna.common.data.GTNACovers;
import com.raishxn.gtna.common.data.GTNAItems;
import com.raishxn.gtna.common.data.GTNAMachines2;
import com.raishxn.gtna.common.machine.multiblock.part.DebugInputBusPartMachine;
import com.raishxn.gtna.common.machine.multiblock.part.DebugInputHatchPartMachine;

import java.util.ArrayList;
import java.util.List;

@PrefixGameTestTemplate(false)
@GameTestHolder("gtna")
public final class GTNADebugPartGameTests {

    private GTNADebugPartGameTests() {}

    /** Ghost items are offered without limit and never consumed; other items are not offered. */
    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void debugInputBusSuppliesEndlessGhostItems(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, GTNAMachines2.DEBUG_INPUT_BUS.getBlock());
        if (!(helper.getBlockEntity(pos) instanceof MetaMachineBlockEntity entity) ||
                !(entity.getMetaMachine() instanceof DebugInputBusPartMachine bus)) {
            helper.fail("the debug input bus did not instantiate");
            return;
        }
        var recipe = GTRecipeTypes.DUMMY_RECIPES.recipeBuilder("debug_bus_test").duration(1).buildRawRecipe();
        bus.setGhost(0, new ItemStack(Items.IRON_INGOT, 5));
        for (int run = 0; run < 3; run++) {
            List<Ingredient> iron = new ArrayList<>(
                    List.of(SizedIngredient.create(Ingredient.of(Items.IRON_INGOT), 1_000_000_000)));
            helper.assertTrue(bus.getInventory().handleRecipeInner(IO.IN, recipe, iron, false) == null,
                    "a ghost item must satisfy any amount, run " + run);
        }
        List<Ingredient> gold = new ArrayList<>(List.of(SizedIngredient.create(Ingredient.of(Items.GOLD_INGOT), 1)));
        helper.assertTrue(bus.getInventory().handleRecipeInner(IO.IN, recipe, gold, true) != null,
                "an item that is not marked must not be offered");
        helper.assertTrue(bus.getInventory().insertItem(1, new ItemStack(Items.DIRT), false).getCount() == 1,
                "nothing can be inserted");
        helper.succeed();
    }

    /** Ghost fluids are offered without limit and never consumed. */
    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void debugInputHatchSuppliesEndlessGhostFluids(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, GTNAMachines2.DEBUG_INPUT_HATCH.getBlock());
        if (!(helper.getBlockEntity(pos) instanceof MetaMachineBlockEntity entity) ||
                !(entity.getMetaMachine() instanceof DebugInputHatchPartMachine hatch)) {
            helper.fail("the debug input hatch did not instantiate");
            return;
        }
        var recipe = GTRecipeTypes.DUMMY_RECIPES.recipeBuilder("debug_hatch_test").duration(1).buildRawRecipe();
        var water = GTMaterials.Water.getFluid();
        hatch.setGhost(0, new FluidStack(water, 1000));
        for (int run = 0; run < 3; run++) {
            List<FluidIngredient> fluids = new ArrayList<>(List.of(FluidIngredient.of(water, 2_000_000_000)));
            helper.assertTrue(hatch.tank.handleRecipeInner(IO.IN, recipe, fluids, false) == null,
                    "a ghost fluid must satisfy any amount, run " + run);
        }
        List<FluidIngredient> lava = new ArrayList<>(List.of(FluidIngredient.of(Fluids.LAVA, 1)));
        helper.assertTrue(hatch.tank.handleRecipeInner(IO.IN, recipe, lava, true) != null,
                "a fluid that is not marked must not be offered");
        helper.succeed();
    }

    /** The cover fills an empty singleblock machine. */
    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void debugEnergyCoverKeepsMachineFull(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, GTMachines.ELECTRIC_FURNACE[GTValues.LV].getBlock());
        if (!(helper.getBlockEntity(pos) instanceof MetaMachineBlockEntity entity)) {
            helper.fail("the furnace did not instantiate");
            return;
        }
        var machine = entity.getMetaMachine();
        boolean placed = machine.getCoverContainer().placeCoverOnSide(Direction.UP,
                new ItemStack(GTNAItems.DEBUG_ENERGY_COVER.get()), GTNACovers.DEBUG_ENERGY_COVER, null);
        helper.assertTrue(placed, "the cover must attach to an electric machine");
        var container = machine.getTraits().stream()
                .filter(t -> t instanceof com.gregtechceu.gtceu.api.capability.IEnergyContainer)
                .map(t -> (com.gregtechceu.gtceu.api.capability.IEnergyContainer) t)
                .findFirst().orElseThrow();
        helper.assertTrue(container.getEnergyStored() == 0, "the furnace must start empty");
        // Natural block-entity ticks are not reliable in the GameTest level (see the wireless steam test), so the
        // cover's tick body is called directly; it is subscribed like GTCEu's Infinite Water Cover.
        var cover = (DebugEnergyCover) machine.getCoverContainer().getCoverAtSide(Direction.UP);
        cover.update();
        helper.assertTrue(container.getEnergyStored() == container.getEnergyCapacity(),
                "the machine must be full, got " + container.getEnergyStored());
        container.changeEnergy(-container.getEnergyCapacity());
        cover.update();
        helper.assertTrue(container.getEnergyStored() == container.getEnergyCapacity(), "the cover must refill it");
        helper.succeed();
    }
}
