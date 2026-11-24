package com.raishxn.gtna.common.data.tooltip;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Number formats shared by the GTNH-style tooltips: percentages with their sign, factors and big numbers. */
final class Fmt {

    private Fmt() {}

    /** 0.0925 → "9.25%", 0.05 → "5%". */
    static String pct(double fraction) {
        return new BigDecimal(Double.toString(fraction * 100)).setScale(4, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString() + "%";
    }

    /** Signed percentage: -0.03 → "-3%", 0.05 → "+5%". */
    static String signed(double fraction) {
        return (fraction > 0 ? "+" : "") + pct(fraction);
    }

    /** The change a factor makes: 0.97 → "-3%", 1.5 → "+50%". */
    static String change(double factor) {
        return signed(factor - 1);
    }

    /** 2.0 → "2x", 0.25 → "0.25x". */
    static String times(double factor) {
        return LargeSteamTooltips.times(factor);
    }

    static String num(long n) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(n);
    }

    static String num(double n) {
        return n == Math.rint(n) && Math.abs(n) < 1e15 ? num((long) n) :
                new BigDecimal(Double.toString(n)).stripTrailingZeros().toPlainString();
    }

    static String seconds(long ticks) {
        return num(ticks / 20.0) + " s";
    }
}
