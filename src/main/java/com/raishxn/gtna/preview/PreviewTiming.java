package com.raishxn.gtna.preview;

import net.minecraftforge.fml.loading.FMLEnvironment;

import com.raishxn.gtna.GTNACORE;

/**
 * Timing of the multiblock preview paths (EMI page and in-world hologram), for comparing before and after on a client.
 * Lines start with {@code [GTNA perf]}; they are logged at INFO when the JVM flag {@code -Dgtna.previewTiming=true} is
 * set or in a development run, and at DEBUG otherwise.
 */
public final class PreviewTiming {

    private static final boolean LOUD = Boolean.getBoolean("gtna.previewTiming") || !FMLEnvironment.production;

    private PreviewTiming() {}

    public static long now() {
        return System.nanoTime();
    }

    public static long millisSince(long start) {
        return (System.nanoTime() - start) / 1_000_000L;
    }

    public static void log(String format, Object... args) {
        if (LOUD) GTNACORE.LOGGER.info("[GTNA perf] " + format, args);
        else GTNACORE.LOGGER.debug("[GTNA perf] " + format, args);
    }
}
