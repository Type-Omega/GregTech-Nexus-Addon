package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAMachines;
import com.raishxn.gtna.common.data.GTNARecipeType;

/** Steam Void Ore "portal house" (D-361): forms from its preview, breaks without a firebox, has its own type. */
@PrefixGameTestTemplate(false)
@GameTestHolder(GTNACORE.MOD_ID)
public final class SteamVoidOreGameTests {

    private SteamVoidOreGameTests() {}

    static IMultiController build(GameTestHelper h, Block remove) {
        var def = GTNAMachines.VOID_MINER_STEAM_GATE_AGED;
        var shape = def.getMatchingShapes().get(0).getBlocks();
        IMultiController controller = null;
        boolean removed = remove == null;
        for (int x = 0; x < shape.length; x++)
            for (int y = 0; y < shape[x].length; y++)
                for (int z = 0; z < shape[x][y].length; z++) {
                    var info = shape[x][y][z];
                    BlockPos pos = new BlockPos(x + 1, y + 1, z + 1);
                    var state = info == null ? Blocks.AIR.defaultBlockState() : info.getBlockState();
                    if (!removed && state.is(remove)) {
                        state = Blocks.AIR.defaultBlockState();
                        removed = true;
                    }
                    h.setBlock(pos, state);
                    if (info != null && info.getBlockState().is(def.getBlock()) &&
                            h.getBlockEntity(pos) instanceof IMachineBlockEntity be &&
                            be.getMetaMachine() instanceof IMultiController c)
                        controller = c;
                }
        return controller;
    }

    @GameTest(template = "empty_48", timeoutTicks = 60)
    public static void steamVoidOreForms(GameTestHelper h) {
        var c = build(h, null);
        h.assertTrue(c != null && c.checkPattern(), "the portal house must form from its preview");
        h.assertTrue(GTNAMachines.VOID_MINER_STEAM_GATE_AGED.getRecipeTypes()[0] == GTNARecipeType.STEAM_VOID_ORE,
                "it runs the steam_void_ore type, not the dummy one");
        h.succeed();
    }

    @GameTest(template = "empty_48", timeoutTicks = 60)
    public static void steamVoidOreNeedsItsFireboxes(GameTestHelper h) {
        var c = build(h, GTBlocks.FIREBOX_BRONZE.get());
        h.assertTrue(c != null && !c.checkPattern(), "a portal house without a firebox must not form");
        h.succeed();
    }
}
