package com.raishxn.gtna.preview;

import com.raishxn.gtna.config.ConfigHolder;

/** The optional block cap of the in-world hologram (client option {@code hologramBlockCap}, off by default). */
public final class HologramLimits {

    public static final int DEFAULT_CAP = 0;

    private HologramLimits() {}

    /** Largest structure drawn whole, or 0 for no limit. */
    public static int blockCap() {
        return ConfigHolder.INSTANCE == null ? DEFAULT_CAP : ConfigHolder.INSTANCE.client.hologramBlockCap;
    }
}
