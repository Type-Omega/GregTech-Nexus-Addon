package com.raishxn.gtna.client.renderer.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.raishxn.gtna.config.ClientConfig.MachineRenderLevel;
import com.raishxn.gtna.config.ConfigHolder;
import org.lwjgl.opengl.GL11;

/**
 * The client's render level for every GTNA machine render (D-434, U11): {@code OFF} hides the effects,
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

    /**
     * Depth test on, as GL_LEQUAL, for effects drawn in a {@code RenderLevelStageEvent} stage.
     * <p>
     * EN: vanilla's {@code DepthTestStateShard.clearState} turns the depth test OFF after every render type (each
     * EN: chunk layer, each batch of block entities), so a stage handler that never enables it draws over walls.
     * EN: Call this before the glow and beam passes; they then write no depth ({@code depthMask(false)}) but are
     * EN: hidden by blocks in front of them.
     * PT: o {@code DepthTestStateShard.clearState} do vanilla DESLIGA o teste de profundidade depois de cada render
     * PT: type (cada camada de chunk, cada lote de block entities); um handler de estágio que não o liga desenha por
     * PT: cima das paredes. Chame antes das passadas de brilho e feixe: elas não escrevem profundidade, mas os
     * PT: blocos à frente as escondem.
     * ZH: 原版 {@code DepthTestStateShard.clearState} 会在每个渲染类型（每个区块层、每批方块实体）之后关闭深度测试，
     * ZH: 阶段处理器若不重新开启就会穿墙绘制。在光晕和光束绘制前调用：它们不写入深度，但会被前方方块遮挡。
     */
    public static void depthTested() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    /** Detail for loops (segments, particles): {@code high} on HIGH, {@code low} on LOW. */
    public static int detail(int high, int low) {
        return high() ? high : low;
    }
}
