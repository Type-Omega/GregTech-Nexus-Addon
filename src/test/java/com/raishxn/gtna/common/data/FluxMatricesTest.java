package com.raishxn.gtna.common.data;

import com.typeomega.lib.data.Int128;

import java.util.List;

/** D-426: the Flux Matrices of one network add up; one that leaves takes only its share. */
public final class FluxMatricesTest {

    private FluxMatricesTest() {}

    public static void main(String[] args) {
        var small = new FluxMatrices.Matrix(10, 2, 0.9, new Int128(1_000), new Int128(100), false, "minecraft:a");
        var large = new FluxMatrices.Matrix(30, 6, 0.98, new Int128(5_000), new Int128(400), false, "minecraft:b");

        var both = FluxMatrices.sum(List.of(small, large));
        check(both.capacity().equals(new Int128(6_000)), "capacity is summed");
        check(both.capacitors() == 40 && both.transferLimit().equals(new Int128(500)),
                "capacitors and transfer limit are summed");
        check(both.averageTier() == 5, "tier is weighted by capacitors: (10*2 + 30*6) / 40 = 5");
        check(Math.abs(both.efficiency() - 0.96) < 1e-9, "efficiency is weighted by capacitors");
        check("minecraft:a".equals(both.dimension()) && !both.unlimited(), "first dimension, not unlimited");

        var left = FluxMatrices.sum(List.of(large));
        check(left.capacity().equals(new Int128(5_000)) && left.averageTier() == 6,
                "a matrix that leaves takes only its own capacity out");
        check(FluxMatrices.sum(List.of()) == null, "no matrix, no sum");

        var infinite = new FluxMatrices.Matrix(750, 14, 1.0, Int128.MAX_VALUE, Int128.MAX_VALUE, true, "x:y");
        var withInfinite = FluxMatrices.sum(List.of(infinite, large));
        check(withInfinite.unlimited() && withInfinite.capacity().equals(Int128.MAX_VALUE),
                "an unlimited matrix makes the network unlimited without overflowing");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
