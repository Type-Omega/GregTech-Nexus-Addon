package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.common.data.FluxMatrices;
import com.raishxn.gtna.common.data.NexusEnergyNetwork;
import com.typeomega.lib.data.Int128;

import java.util.UUID;

/** GTIA-D-426: several Flux Matrices of one network add their capacity; one that unforms leaves the sum. */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class FluxMatrixSumGameTests {

    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void twoMatricesSumAndOneLeaves(GameTestHelper h) {
        var level = h.getLevel();
        String dimension = level.dimension().location().toString();
        UUID owner = UUID.randomUUID();
        var network = new NexusEnergyNetwork();
        GlobalPos first = GlobalPos.of(level.dimension(), new BlockPos(0, 64, 0));
        GlobalPos second = GlobalPos.of(level.dimension(), new BlockPos(100, 64, 0));

        network.putMatrix(owner, first, new FluxMatrices.Matrix(10, GTValues.LV, 0.9, new Int128(1_000),
                new Int128(100), false, dimension));
        network.putMatrix(owner, second, new FluxMatrices.Matrix(10, GTValues.LV, 0.9, new Int128(2_000),
                new Int128(100), false, dimension));
        h.assertTrue(network.getMaxCapacity(owner).equals(new Int128(3_000)) && network.getMatrixCount(owner) == 2,
                "two matrices sum their capacity instead of the last one overwriting it");
        h.assertTrue(network.getTotalCapacitors(owner) == 20 && network.isMatrixFormed(owner),
                "capacitors are summed and the network has a formed matrix");

        network.putMatrix(owner, second, new FluxMatrices.Matrix(10, GTValues.LV, 0.9, new Int128(2_000),
                new Int128(100), false, dimension));
        h.assertTrue(network.getMaxCapacity(owner).equals(new Int128(3_000)),
                "a matrix that forms again is not counted twice");

        var reloaded = new NexusEnergyNetwork(network.save(new CompoundTag()));
        h.assertTrue(reloaded.getMaxCapacity(owner).equals(new Int128(3_000)) && reloaded.getMatrixCount(owner) == 2,
                "the matrices and their sum survive a reload");

        network.setExactEnergy(owner, java.math.BigInteger.valueOf(2_500));
        network.removeMatrix(second);
        h.assertTrue(network.getMaxCapacity(owner).equals(new Int128(1_000)) && network.getMatrixCount(owner) == 1,
                "an unformed or broken matrix takes only its own capacity out of the sum");
        h.assertTrue(network.getExactEnergy(owner).intValueExact() == 2_500 && network.isMatrixFormed(owner),
                "stored EU is kept and the other matrix still holds the network");

        network.removeMatrix(first);
        h.assertTrue(!network.isMatrixFormed(owner) && network.getMaxCapacity(owner).isZero(),
                "with no matrix left the network has no matrix");
        h.succeed();
    }

    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void aMatrixMovedToAnotherOwnerLeavesTheOldNetwork(GameTestHelper h) {
        var level = h.getLevel();
        String dimension = level.dimension().location().toString();
        UUID before = UUID.randomUUID(), after = UUID.randomUUID();
        var network = new NexusEnergyNetwork();
        GlobalPos controller = GlobalPos.of(level.dimension(), new BlockPos(5, 64, 5));
        var matrix = new FluxMatrices.Matrix(4, GTValues.MV, 0.9, new Int128(500), new Int128(50), false, dimension);

        network.putMatrix(before, controller, matrix);
        network.putMatrix(after, controller, matrix);
        h.assertTrue(!network.isMatrixFormed(before) && network.getMaxCapacity(after).equals(new Int128(500)),
                "a controller counts in one network only");
        h.succeed();
    }
}
