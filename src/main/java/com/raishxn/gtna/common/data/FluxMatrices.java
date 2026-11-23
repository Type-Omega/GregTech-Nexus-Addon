package com.raishxn.gtna.common.data;

import com.typeomega.lib.data.Int128;

import java.math.BigInteger;
import java.util.Collection;

/**
 * The Flux Matrices of one wireless network and their sum (GTIA-D-426). No Minecraft types, so it is unit-tested
 * on its own.
 */
public final class FluxMatrices {

    private FluxMatrices() {}

    /** What one formed Flux Matrix gives its network. */
    public record Matrix(long capacitors, int averageTier, double efficiency, Int128 capacity, Int128 transferLimit,
                         boolean unlimited, String dimension) {

        public Matrix {
            capacity = capacity == null ? Int128.ZERO() : capacity.copy();
            transferLimit = transferLimit == null ? Int128.ZERO() : transferLimit.copy();
            dimension = dimension == null ? "" : dimension;
        }
    }

    /**
     * EN: Capacity, capacitors and transfer limit are summed, since every matrix stores and moves its own EU. Tier
     * and efficiency are averaged by capacitor count, so a small low-tier matrix barely changes the loss of a large
     * one. One unlimited matrix makes the whole network unlimited. The dimension is the first matrix's.
     * PT: Capacidade, capacitores e limite de transferência são somados, porque cada matriz guarda e move o próprio
     * EU. Tier e eficiência são a média ponderada pelo número de capacitores, então uma matriz pequena de tier baixo
     * quase não muda a perda de uma grande. Uma matriz ilimitada deixa a rede inteira ilimitada. A dimensão é a da
     * primeira matriz.
     * ZH: 容量、电容数量和传输上限相加，因为每个矩阵都储存并传输自己的 EU。等级和效率按电容数量加权平均，
     * 因此小型低等级矩阵几乎不会改变大型矩阵的损耗。只要有一个无限矩阵，整个网络就是无限的。维度取第一个矩阵的维度。
     *
     * @return null when there is no matrix
     */
    public static Matrix sum(Collection<Matrix> matrices) {
        if (matrices.isEmpty()) return null;
        long capacitors = 0;
        BigInteger capacity = BigInteger.ZERO, transfer = BigInteger.ZERO, tierWeight = BigInteger.ZERO;
        double efficiencyWeight = 0;
        boolean unlimited = false;
        String dimension = null;
        for (Matrix matrix : matrices) {
            long weight = Math.max(1, matrix.capacitors());
            capacitors += matrix.capacitors();
            capacity = capacity.add(matrix.capacity().toBigInteger());
            transfer = transfer.add(matrix.transferLimit().toBigInteger());
            tierWeight = tierWeight.add(BigInteger.valueOf(weight).multiply(BigInteger.valueOf(matrix.averageTier())));
            efficiencyWeight += matrix.efficiency() * weight;
            unlimited |= matrix.unlimited();
            if (dimension == null) dimension = matrix.dimension();
        }
        long weights = matrices.stream().mapToLong(m -> Math.max(1, m.capacitors())).sum();
        int tier = tierWeight.divide(BigInteger.valueOf(weights)).intValue();
        return new Matrix(capacitors, tier, efficiencyWeight / weights,
                unlimited ? Int128.MAX_VALUE : saturate(capacity), unlimited ? Int128.MAX_VALUE : saturate(transfer),
                unlimited, dimension);
    }

    private static Int128 saturate(BigInteger amount) {
        return Int128.fromBigInteger(amount.max(BigInteger.ZERO).min(Int128.MAX_VALUE.toBigInteger()));
    }
}
