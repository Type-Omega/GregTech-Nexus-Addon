package com.raishxn.gtna.preview;

import com.raishxn.gtna.config.ConfigHolder;

/** The block cap of the in-world hologram (client option {@code hologramBlockCap}). */
public final class HologramLimits {

    public static final int DEFAULT_CAP = 32768;

    private HologramLimits() {}

    /** Largest structure drawn whole, or 0 for no limit. */
    public static int blockCap() {
        return ConfigHolder.INSTANCE == null ? DEFAULT_CAP : ConfigHolder.INSTANCE.client.hologramBlockCap;
    }
}
