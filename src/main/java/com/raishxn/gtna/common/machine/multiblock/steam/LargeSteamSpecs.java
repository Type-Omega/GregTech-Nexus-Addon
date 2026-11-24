package com.raishxn.gtna.common.machine.multiblock.steam;

/**
 * Parallel and speed of the steam multiblocks, read by the recipe logic and by the tooltips, so both always
 * show the same numbers. The pack's {@code GTNASteamBalance} scales them at run time.
 */
public final class LargeSteamSpecs {

    /** Parallel set when the controller is placed, the cap of the [+] button and the recipe duration multiplier. */
    public record Spec(int defaultParallel, int maxParallel, double durationMultiplier) {}

    // EN: GTNA's own numbers, not GTNL's: GTNL's percentages are measured against GT5 single blocks.
    // PT: Números do próprio GTNA, não os do GTNL: as porcentagens do GTNL comparam com as máquinas do GT5.
    // ZH: 使用 GTNA 自己的数值而非 GTNL 的：GTNL 的百分比是相对 GT5 单方块机器的。
    public static final Spec CRUSHER = new Spec(8, 128, 0.5);
    public static final Spec FURNACE = new Spec(8, 128, 0.1);
    public static final Spec ALLOY_SMELTER = new Spec(64, 64, 0.57);
    public static final Spec HAMMER = new Spec(32, 64, 0.5);
    public static final Spec COMPRESSOR = new Spec(24, 48, 0.4);
    public static final Spec EXTRACTOR = new Spec(24, 48, 0.57);
    public static final Spec ORE_WASHER = new Spec(48, 96, 0.2);
    public static final Spec MIXER = new Spec(64, 64, 0.5);
    public static final Spec CENTRIFUGE = new Spec(64, 64, 0.5);
    public static final Spec THERMAL_CENTRIFUGE = new Spec(64, 64, 0.5);
    public static final Spec BATH = new Spec(64, 64, 0.5);
    public static final Spec LATHE = new Spec(16, 16, 0.5);
    public static final Spec CUTTING = new Spec(16, 16, 0.5);
    public static final Spec BENDING = new Spec(16, 16, 0.5);
    public static final Spec EXTRUDER = new Spec(16, 16, 0.5);
    public static final Spec WIREMILL = new Spec(16, 16, 0.5);
    public static final Spec SIFTER = new Spec(16, 16, 0.5);
    public static final Spec FORMING_PRESS = new Spec(32, 32, 0.4);
    // GTNL steam utilities; recipes run at their listed time except the mega compressor.
    public static final Spec COBBLER = new Spec(16, 16, 1.0);
    public static final Spec SUPERHEATER = new Spec(32, 32, 1.0);
    public static final Spec WOODCUTTER = new Spec(64, 64, 1.0);
    public static final Spec LAVA_MAKER = new Spec(16, 16, 1.0);
    public static final Spec MANUFACTURER = new Spec(16, 16, 1.0);
    public static final Spec MEGA_COMPRESSOR = new Spec(256, 256, 0.5);

    private LargeSteamSpecs() {}
}
