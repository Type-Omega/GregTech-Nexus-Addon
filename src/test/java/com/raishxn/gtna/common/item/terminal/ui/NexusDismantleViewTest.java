package com.raishxn.gtna.common.item.terminal.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * The ranking of the Dismantle preview's block kinds: the rows that fit the panel and the sum of the rest.
 * GTLCore-style: {@code main()} + asserts, no JUnit.
 */
public final class NexusDismantleViewTest {

    private NexusDismantleViewTest() {}

    public static void main(String[] args) {
        fewKindsAreAllShown();
        manyKindsKeepTheBiggestAndSumTheRest();
        anEmptyListShowsNothing();
        theShownRowsAndTheRestAddUpToTheTotal();
        System.out.println("[NexusDismantleViewTest] all cases passed");
    }

    private static List<NexusDismantleView.Row> rows(int... counts) {
        List<NexusDismantleView.Row> out = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) out.add(new NexusDismantleView.Row("block" + i, counts[i]));
        return out;
    }

    private static void fewKindsAreAllShown() {
        var top = NexusDismantleView.top(rows(30, 12, 4), 3);
        require(top.shown().size() == 3, "three kinds fit three rows");
        require(top.otherKinds() == 0 && top.otherBlocks() == 0, "nothing is folded");
    }

    private static void manyKindsKeepTheBiggestAndSumTheRest() {
        var top = NexusDismantleView.top(rows(30, 12, 4, 3, 1), 3);
        require(top.shown().size() == 2, "the last row's place goes to the 'other kinds' line");
        require(top.shown().get(0).name().equals("block0") && top.shown().get(1).name().equals("block1"),
                "the biggest kinds stay");
        require(top.otherKinds() == 3, "three kinds folded");
        require(top.otherBlocks() == 8, "their blocks are summed");
    }

    private static void anEmptyListShowsNothing() {
        var top = NexusDismantleView.top(List.of(), 3);
        require(top.shown().isEmpty() && top.otherKinds() == 0, "empty in, empty out");
    }

    private static void theShownRowsAndTheRestAddUpToTheTotal() {
        var all = rows(50, 20, 10, 9, 8, 7, 6);
        var top = NexusDismantleView.top(all, 3);
        int shown = top.shown().stream().mapToInt(NexusDismantleView.Row::count).sum();
        int total = all.stream().mapToInt(NexusDismantleView.Row::count).sum();
        require(shown + top.otherBlocks() == total, "the counts add up to " + total);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
