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
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Kinetic Orbital Lance effects (D-434): the shot (shaded beam, muzzle flash, shock rings, then a trail fading
 * up to the sky), the capacitor glow pulsing down the bore between shots, and a return capsule re-entering in a
 * plasma sheath before it lands. The muzzle frame has +Z out of the barrel.
 */
@OnlyIn(Dist.CLIENT)
public final class LanceFx {

    /** Inner radius of the barrel and how far the bore runs back from the muzzle, in blocks. */
    public static final float BORE = 10.5f, BORE_DEPTH = 150;

    private LanceFx() {}

    /** The shot and its trail; {@code since} whole seconds since it fired (0..2). */
    public static void shot(Matrix4f m, int since, float time) {
        float t = since + (time % 1);
        float grow = Math.min(1, t / 0.15f), fade = t < 1.2f ? 1 : Math.max(0, 1 - (t - 1.2f) / 1.8f);
        if (fade <= 0) return;
        float reach = 1500 * grow;
        RenderSystem.disableCull();
        // Shaded beam: a wide sheath and a white core, streaming outwards.
        var beam = FxDraw.fx(6, time, 0.45f, 0.95f, 1f, fade, 6, 1, 0.02f, 0);
        if (beam != null) {
            List<Vector3f> axis = List.of(new Vector3f(0, 0, -BORE_DEPTH), new Vector3f(0, 0, 0),
                    new Vector3f(0, 0, reach * 0.25f), new Vector3f(0, 0, reach));
            FxDraw.ribbon(beam, m, axis, BORE * 1.9f, 1);
            if (MachineRenders.high()) FxDraw.ribbon(beam, m, axis, BORE * 0.7f, 1);
        }
        var additive = StellarShaders.additiveColor();
        if (additive == null) return;
        RenderSystem.setShader(() -> additive);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float flash = Math.max(0, 1 - t * 1.5f);
        FxDraw.sphereGlow(buf, m, BORE * (1.5f + 2.5f * flash), 0.7f, 1f, 0.9f, flash);
        int rings = MachineRenders.detail(4, 2);
        for (int k = 0; k < rings; k++) {
            float rt = t - k * 0.25f;
            if (rt <= 0) continue;
            float r = BORE + rt * 45, a = Math.max(0, 0.6f - rt * 0.35f);
            FxDraw.ring(buf, m, r - 1.5f, r + 1.5f, 2 + k * 6 + rt * 10, 0.6f, 1f, 0.9f, a);
        }
        Tesselator.getInstance().end();
    }

    /** Between shots: rings of light run down the bore towards the breech, brighter and faster with the charge. */
    public static void charge(Matrix4f m, double charge, float time) {
        if (charge <= 0.02) return;
        var additive = StellarShaders.additiveColor();
        if (additive == null) return;
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> additive);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float c = (float) Math.min(1, charge);
        int rings = MachineRenders.detail(8, 4);
        for (int k = 0; k < rings; k++) {
            float t = (float) (((time * (0.15 + 0.35 * c)) + k / (double) rings) % 1.0);
            float z = -BORE_DEPTH * t;
            float a = 0.5f * c * (float) Math.sin(Math.PI * t);
            FxDraw.ring(buf, m, BORE * 0.55f, BORE * 0.95f, z, 0.35f, 0.8f, 1f, a);
            FxDraw.ring(buf, m, BORE * 0.55f, BORE * 0.15f, z, 0.6f, 0.95f, 1f, a * 0.5f);
        }
        // Breech glow at full charge: the bank is ready.
        FxDraw.sphereGlow(buf, new Matrix4f(m).translate(0, 0, -BORE_DEPTH), BORE * (0.6f + 0.6f * c), 0.4f, 0.85f,
                1f, 0.35f * c * c);
        Tesselator.getInstance().end();
    }

    /**
     * A return capsule {@code seconds} before landing at the origin of {@code ground} (a frame with +Y up): a glowing
     * ablative sheath falling from the sky with a long plasma trail.
     */
    public static void capsule(Matrix4f ground, Matrix4f projection, double seconds, float time) {
        if (seconds < 0 || seconds > 12) return;
        float height = (float) (6 + seconds * seconds * 9);
        float heat = (float) Math.min(1, Math.max(0, (seconds - 0.5) / 4));
        Matrix4f at = new Matrix4f(ground).translate(height * 0.25f, height, 0);
        RenderSystem.disableCull();
        var sheath = FxDraw.fx(7, time, 1f, 0.45f + 0.3f * (1 - heat), 0.15f, 0.9f, 0, 0, 0, 0);
        if (sheath != null) FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(at).scale(1.4f + 1.6f * heat), projection,
                sheath);
        var trail = FxDraw.fx(6, time, 1f, 0.5f, 0.2f, 0.8f * heat, 3, -1, 0.05f, 0);
        if (trail != null) {
            Vector3f top = new Vector3f(height * 0.25f + 30, height + 120, 0);
            FxDraw.ribbon(trail, ground, List.of(new Vector3f(height * 0.25f, height, 0),
                    new Vector3f(height * 0.25f, height, 0).lerp(top, 0.3f), top), 2.5f + 3 * heat, 1);
        }
    }
}
