package com.raishxn.gtna.client.renderer.machine.fx;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.raishxn.gtna.client.renderer.machine.MachineRenders;
import com.raishxn.gtna.client.renderer.machine.StellarShaders;
import com.raishxn.gtna.common.machine.multiblock.originals.RiftGateMachine;
import org.joml.Matrix4f;

/**
 * Dimensional Rift Gate extras around the horizon (D-434): a shimmering lens rim where light bends around the
 * horizon, and debris spiralling in from the rim (more on HIGH). The frame has the horizon in its XY plane.
 */
@OnlyIn(Dist.CLIENT)
public final class RiftFx {

    private RiftFx() {}

    public static void draw(RiftGateMachine r, Matrix4f m, Matrix4f projection, float[] tint, float strength,
                            float time) {
        float radius = (float) RiftGateMachine.INNER_RADIUS;
        RenderSystem.disableCull();
        var rim = FxDraw.fx(10, time, tint[0], tint[1], tint[2], 0.9f * strength, 0, 0, 0, 0);
        if (rim != null) FxMeshes.draw(FxMeshes.quad(), new Matrix4f(m).scale(radius * 1.25f), projection, rim);
        var additive = StellarShaders.additiveColor();
        if (additive == null) return;
        RenderSystem.setShader(() -> additive);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int count = MachineRenders.detail(90, 30);
        float pull = 0.08f + 0.12f * (float) r.resonance();
        for (int i = 0; i < count; i++) {
            // EN: golden-angle spread so the debris never clumps; each piece falls in over 1/pull seconds.
            // PT: espalhados pelo ângulo áureo para os detritos não se amontoarem; cada um cai em 1/pull segundos.
            // ZH: 按黄金角分布，碎片不会扎堆；每块碎片在 1/pull 秒内落入。
            double a0 = i * 2.39996;
            float t = (float) ((time * pull + i * 0.618034) % 1.0);
            double ang = a0 + t * t * 4.5;
            float rr = radius * (2.1f - 1.15f * t);
            float z = (i % 2 == 0 ? 1 : -1) * (1 - t) * 6;
            float size = 0.25f + 0.45f * (1 - t) * ((i * 37 % 10) / 10f);
            float a = Math.min(1, t * 4) * (1 - t * t) * 0.85f * strength;
            float heat = t * t;
            FxDraw.billboard(buf, m, (float) Math.cos(ang) * rr, (float) Math.sin(ang) * rr, z, size,
                    tint[0] * (1 - heat) + heat, tint[1] * (1 - heat) + heat * 0.9f, tint[2] * (1 - heat) + heat * 0.8f,
                    a);
        }
        Tesselator.getInstance().end();
    }
}
