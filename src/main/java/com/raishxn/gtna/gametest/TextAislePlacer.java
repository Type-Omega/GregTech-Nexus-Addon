package com.raishxn.gtna.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;

import com.raishxn.gtna.common.data.multiblock.TextAisles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Builds a text-aisle multiblock ({@link TextAisles}) in a GameTest with the controller facing north, with a chosen
 * number of repetitions. Every structure placed this way is symmetric left to right, so the column direction does
 * not matter.
 */
final class TextAislePlacer {

    /** Where each symbol went, and the controller. */
    record Placed(BlockPos controller, Map<Character, List<BlockPos>> positions) {

        List<BlockPos> of(char symbol) {
            return positions.getOrDefault(symbol, List.of());
        }
    }

    private TextAislePlacer() {}

    /**
     * @param aislesGoUp       true for {@code start(RIGHT, FRONT, UP)} (aisles stack up, rows go to the front), false
     *                         for
     *                         the default {@code start()} (aisles go to the front, rows go up)
     * @param controllerSymbol the symbol of the controller in the resource
     * @param blocks           the block for each symbol; a missing symbol (or {@code null}) is left as air
     */
    static Placed place(GameTestHelper helper, String resource, BlockPos origin, boolean aislesGoUp,
                        int repetitions, char controllerSymbol, BiFunction<Character, BlockPos, Block> blocks) {
        List<String[]> expanded = new ArrayList<>();
        for (var aisle : TextAisles.load(resource)) {
            int count = aisle.repeats() ? repetitions : 1;
            for (int i = 0; i < count; i++) expanded.add(aisle.rows());
        }
        int depth = aislesGoUp ? expanded.get(0).length : expanded.size();
        Map<Character, List<BlockPos>> positions = new java.util.HashMap<>();
        BlockPos controller = null;
        for (int a = 0; a < expanded.size(); a++) {
            String[] rows = expanded.get(a);
            for (int r = 0; r < rows.length; r++) {
                for (int c = 0; c < rows[r].length(); c++) {
                    char symbol = rows[r].charAt(c);
                    if (symbol == ' ') continue;
                    int y = aislesGoUp ? a : r;
                    int front = aislesGoUp ? r : a;
                    // EN: north is the front, so the front-most aisle or row gets the smallest z.
                    // PT: o norte é a frente, então a fileira mais à frente fica com o menor z.
                    // ZH: 北为正面，因此最靠前的一层或一行 z 最小。
                    BlockPos pos = origin.offset(c, y, depth - 1 - front);
                    positions.computeIfAbsent(symbol, k -> new ArrayList<>()).add(pos);
                    if (symbol == controllerSymbol) controller = pos;
                    Block block = blocks.apply(symbol, pos);
                    if (block != null) helper.setBlock(pos, block);
                }
            }
        }
        return new Placed(controller, positions);
    }
}
