package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAOriginals;
import com.raishxn.gtna.config.ClientConfig;
import com.raishxn.gtna.preview.HologramBuild;
import com.raishxn.gtna.preview.HologramLimits;
import com.raishxn.gtna.preview.HologramScan;
import com.raishxn.gtna.preview.PatternParts;
import com.raishxn.gtna.preview.PatternPreviewScan;
import com.typeomega.lib.data.BlockPosHashMap;
import com.typeomega.lib.preview.PreviewLayers;
import com.typeomega.lib.test.Previews;

import java.util.HashMap;
import java.util.Map;

/**
 * The data side of the multiblock preview work (EMI page and in-world hologram): the maps hold real blocks only, the
 * optional block cap (off by default, so every size starts with all layers) picks the controller's layer when the
 * player sets one, and the EMI scan equals the old one minus air. The client mixins that use these classes are checked
 * against the GTCEu bytecode by {@code PreviewMixinContractTest}. The Stellar Containment Chamber (about 22 000
 * blocks) and the Kinetic Orbital Lance (about 171 000) are the small and the big structure of these tests.
 */
@GameTestHolder(GTNACORE.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PreviewPerformanceGameTests {

    private static final String TEMPLATE = "empty_12";

    private PreviewPerformanceGameTests() {}

    private static BlockInfo[][][] shape(MultiblockMachineDefinition definition) {
        return definition.getMatchingShapes().get(0).getBlocks();
    }

    /** Counted the plain way, without the helpers under test. */
    private static int solidCells(BlockInfo[][][] shape) {
        int count = 0;
        for (BlockInfo[][] aisle : shape)
            for (BlockInfo[] column : aisle)
                for (BlockInfo info : column)
                    if (info != null && !info.getBlockState().isAir()) count++;
        return count;
    }

    private static int solidCellsInLayer(BlockInfo[][][] shape, int layer) {
        int count = 0;
        for (BlockInfo[][] aisle : shape)
            if (layer < aisle.length)
                for (BlockInfo info : aisle[layer])
                    if (info != null && !info.getBlockState().isAir()) count++;
        return count;
    }

    /** The preview level of the EMI page, reduced to what the parts list reads: the scanned blocks. */
    private record ScanGetter(Map<BlockPos, BlockInfo> blocks) implements BlockGetter {

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            BlockInfo info = blocks.get(pos);
            return info == null ? null : info.getBlockEntity(pos);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            BlockInfo info = blocks.get(pos);
            return info == null ? Blocks.AIR.defaultBlockState() : info.getBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 256;
        }

        @Override
        public int getMinBuildHeight() {
            return 0;
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void hologramMapHasNoAirAndTheRealBlockCount(GameTestHelper h) {
        var definition = GTNAOriginals.STELLAR_CONTAINMENT_CHAMBER;
        var shape = shape(definition);
        HologramScan scan = HologramScan.of(shape);
        h.assertTrue(scan.solid() == solidCells(shape), "scan counted " + scan.solid() + ", plain count " +
                solidCells(shape));
        h.assertTrue(scan.controller() != null, "controller not found in the shape");
        BlockPos origin = new BlockPos(100, 64, 100);
        var built = HologramBuild.build(shape, scan.controller(), Direction.NORTH, Direction.NORTH, origin, -1,
                h.getLevel());
        for (var entry : built.blocks().entrySet()) {
            h.assertFalse(entry.getValue().getBlockState().isAir(), "air at " + entry.getKey());
        }
        h.assertTrue(built.controller() != null, "the controller was not bound");
        h.assertTrue(built.blocks().size() + 1 == scan.solid(), "map has " + built.blocks().size() +
                " blocks, expected " + (scan.solid() - 1) + " (every real block but the controller)");
        // Facing north with the default up the hologram is the shape moved by the controller's cell.
        BlockPos cell = scan.controller();
        for (int x = 0; x < shape.length; x++)
            for (int y = 0; y < shape[x].length; y++)
                for (int z = 0; z < shape[x][y].length; z++) {
                    BlockInfo info = shape[x][y][z];
                    if (info == null || info.getBlockState().isAir() || (x == cell.getX() && y == cell.getY() &&
                            z == cell.getZ()))
                        continue;
                    BlockPos real = origin.offset(x - cell.getX(), y - cell.getY(), z - cell.getZ());
                    BlockInfo placed = built.blocks().get(real);
                    h.assertTrue(placed != null && placed.getBlockState().getBlock() == info.getBlockState().getBlock(),
                            "block missing or wrong at " + real);
                }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void hologramRotationKeepsEveryBlock(GameTestHelper h) {
        var shape = shape(GTNAOriginals.STELLAR_CONTAINMENT_CHAMBER);
        HologramScan scan = HologramScan.of(shape);
        for (Direction front : new Direction[] { Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            var built = HologramBuild.build(shape, scan.controller(), front, Direction.NORTH,
                    new BlockPos(100, 64, 100), -1, h.getLevel());
            h.assertTrue(built.blocks().size() + 1 == scan.solid(), front + ": " + built.blocks().size() +
                    " blocks, expected " + (scan.solid() - 1));
            for (var info : built.blocks().values())
                h.assertFalse(info.getBlockState().isAir(), front + ": air in the map");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void hologramShowsEveryLayerFirstByDefault(GameTestHelper h) {
        h.assertTrue(new ClientConfig().hologramBlockCap == HologramLimits.DEFAULT_CAP &&
                HologramLimits.DEFAULT_CAP == 0, "the default must be no cap");
        int cap = HologramLimits.DEFAULT_CAP;

        for (var definition : new MultiblockMachineDefinition[] { GTNAOriginals.STELLAR_CONTAINMENT_CHAMBER,
                GTNAOriginals.KINETIC_ORBITAL_LANCE }) {
            var shape = shape(definition);
            HologramScan scan = HologramScan.of(shape);
            h.assertTrue(scan.solid() == solidCells(shape), definition.getId() + " count " + scan.solid());
            boolean over = PreviewLayers.overCap(scan.solid(), cap);
            h.assertFalse(over, definition.getId() + " (" + scan.solid() + " blocks) is over the default cap");
            h.assertTrue(PreviewLayers.firstLayer(over, scan.controller().getY()) == PreviewLayers.ALL,
                    definition.getId() + " does not start with every layer");
            var built = HologramBuild.build(shape, scan.controller(), Direction.NORTH, Direction.NORTH,
                    new BlockPos(100, 64, 100), PreviewLayers.ALL, h.getLevel());
            h.assertTrue(built.blocks().size() + 1 == scan.solid(), definition.getId() + " first view has " +
                    built.blocks().size() + " blocks, expected " + (scan.solid() - 1));
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void anOptionalCapStillFallsBackToTheControllerLayer(GameTestHelper h) {
        int cap = 32768;
        var bigShape = shape(GTNAOriginals.KINETIC_ORBITAL_LANCE);
        HologramScan big = HologramScan.of(bigShape);
        h.assertTrue(PreviewLayers.overCap(big.solid(), cap), "the lance (" + big.solid() + ") is over " + cap);
        h.assertFalse(PreviewLayers.overCap(big.solid(), 0), "a cap of 0 means no limit");
        int layer = PreviewLayers.firstLayer(true, big.controller().getY());
        h.assertTrue(layer == big.controller().getY(), "over a set cap the first layer is the controller's");
        var built = HologramBuild.build(bigShape, big.controller(), Direction.NORTH, Direction.NORTH,
                new BlockPos(100, 64, 100), layer, h.getLevel());
        int expected = solidCellsInLayer(bigShape, layer) - 1;
        h.assertTrue(built.blocks().size() == expected && expected > 0 && expected < cap,
                "the controller's layer has " + built.blocks().size() + " blocks, expected " + expected);

        int steps = 0;
        boolean[] seen = new boolean[big.height()];
        for (int current = layer; steps < big.height(); steps++) {
            h.assertTrue(current >= 0, "the capped cycle never shows all layers");
            seen[current] = true;
            current = PreviewLayers.nextLayer(current, big.height(), true);
        }
        for (boolean s : seen) h.assertTrue(s, "the capped cycle skipped a layer");
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void withoutACapTheClicksCycleAllThenEachLayer(GameTestHelper h) {
        int height = 7;
        int current = PreviewLayers.firstLayer(false, 3);
        h.assertTrue(current == PreviewLayers.ALL, "the first state is all layers");
        boolean[] seen = new boolean[height];
        for (int i = 0; i <= height; i++) {
            current = PreviewLayers.nextLayer(current, height, false);
            if (current >= 0) seen[current] = true;
        }
        for (boolean s : seen) h.assertTrue(s, "the uncapped cycle skipped a layer");
        h.assertTrue(PreviewLayers.nextLayer(height - 1, height, false) == PreviewLayers.ALL,
                "after the last layer the cycle returns to all layers");
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void emiPatternScanIsTheOldScanMinusAir(GameTestHelper h) {
        var definition = GTNAOriginals.STELLAR_CONTAINMENT_CHAMBER;
        var shape = shape(definition);
        BlockPos origin = new BlockPos(512, 50, 512);

        // The scan GTCEu 7.5.3 did, with the air filter GTNA put after it.
        Map<BlockPos, BlockInfo> old = new HashMap<>();
        for (int x = 0; x < shape.length; x++)
            for (int y = 0; y < shape[x].length; y++)
                for (int z = 0; z < shape[x][y].length; z++)
                    old.put(origin.offset(x, y, z), BlockInfo.fromBlockState(shape[x][y][z].getBlockState()));
        old.values().removeIf(info -> info.getBlockState().isAir() && !info.hasBlockEntity());

        var scan = PatternPreviewScan.scan(shape, origin, h.getLevel());
        h.assertTrue(scan.blocks().size() == old.size(), "scan has " + scan.blocks().size() + " blocks, the old one " +
                old.size());
        for (var entry : old.entrySet()) {
            BlockInfo now = scan.blocks().get(entry.getKey());
            BlockState before = entry.getValue().getBlockState();
            h.assertTrue(now != null && now.getBlockState() == before &&
                    now.hasBlockEntity() == entry.getValue().hasBlockEntity(),
                    "block differs at " + entry.getKey() + ": " + now + " vs " + before);
        }
        for (var info : scan.blocks().values()) h.assertFalse(info.getBlockState().isAir(), "air in the scan");
        h.assertTrue(scan.controller() != null && scan.controller().self().getDefinition() == definition,
                "the controller of the page is the chamber");

        var parts = PatternParts.gather(scan.blocks(), new ScanGetter(scan.blocks()));
        h.assertFalse(parts.parts().isEmpty(), "no parts listed");
        h.assertTrue(parts.parts().get(0).get(0).getItem() == definition.asStack().getItem(),
                "the controller is the first part");
        h.assertFalse(parts.drops().isEmpty(), "no drop keys");
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void savedPredicatesUsePositionHashing(GameTestHelper h) {
        try (var placed = Previews.place(h, GTMultiMachines.ELECTRIC_BLAST_FURNACE, new BlockPos(1, 1, 1))) {
            h.assertTrue(placed.controller() != null, "the blast furnace preview has no controller");
            var state = placed.controller().getMultiblockState();
            h.assertTrue(placed.controller().getPattern().checkPatternAt(state, true), "the pattern did not match: " +
                    placed.error());
            Object predicates = state.getMatchContext().get("predicates");
            h.assertTrue(predicates instanceof BlockPosHashMap<?> map && !map.isEmpty(),
                    "the saved predicates are " + (predicates == null ? "missing" : predicates.getClass()));
        }
        h.succeed();
    }
}
