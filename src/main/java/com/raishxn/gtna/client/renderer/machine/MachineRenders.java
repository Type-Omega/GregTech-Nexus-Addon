package com.raishxn.gtna.client.renderer.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.raishxn.gtna.config.ClientConfig.MachineRenderLevel;
import com.raishxn.gtna.config.ConfigHolder;

/**
 * The client's render level for every GTNA machine render (GTIA-D-434, U11): {@code OFF} hides the effects,
 * {@code LOW} halves their reach and skips the expensive passes (LOD), {@code HIGH} draws everything.
 */
@OnlyIn(Dist.CLIENT)
public final class MachineRenders {

    /** Set by tests and the bench command; null follows the config. */
    @org.jetbrains.annotations.Nullable
    public static MachineRenderLevel override;

    /**
     * Dev showcase ({@code -Dgtna.renderShowcase=true}): renders that depend on live data (network charge, storage
     * use, forged items, crafting jobs) show sample values, for screenshots in a dev world without a factory behind.
     */
    public static final boolean SHOWCASE = Boolean.getBoolean("gtna.renderShowcase");

    private MachineRenders() {}

    public static MachineRenderLevel level() {
        if (override != null) return override;
        return ConfigHolder.INSTANCE == null ? MachineRenderLevel.HIGH : ConfigHolder.INSTANCE.client.machineRenders;
    }

    /** Whether effects are drawn at all. */
    public static boolean effects() {
        return level() != MachineRenderLevel.OFF;
    }

    /** Whether the expensive passes (shaders on many layers, particles, lenses) are drawn. */
    public static boolean high() {
        return level() == MachineRenderLevel.HIGH;
    }

    /** View distance of an effect at the current level. */
    public static int reach(int full) {
        return switch (level()) {
            case OFF -> 0;
            // EN: LOW keeps the effects only up to half the distance: far ones are a few pixels anyway.
            // PT: LOW mantém os efeitos só até metade da distância: de longe eles ocupam poucos pixels.
            // ZH: LOW 只在一半距离内绘制特效：远处的特效本来也只占几个像素。
            case LOW -> full / 2;
            case HIGH -> full;
        };
    }

    /** Whether an effect centred at {@code centre} with full reach {@code full} is drawn from {@code camera}. */
    public static boolean inReach(BlockPos centre, Vec3 camera, int full) {
        int reach = reach(full);
        return reach > 0 && Vec3.atCenterOf(centre).closerThan(camera, reach);
    }

    /** Detail for loops (segments, particles): {@code high} on HIGH, {@code low} on LOW. */
    public static int detail(int high, int low) {
        return high() ? high : low;
    }
}
