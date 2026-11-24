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
import com.raishxn.gtna.common.machine.multiblock.originals.ColliderRingMachine;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Antimatter Collider Ring effects (GTIA-D-434): two counter-rotating beams on shaded ribbons inside the ring, a
 * white-hot flash at both interaction points, and bubble-chamber tracks curling out of every collision (HIGH only).
 * The frame has the ring in its XY plane, centred at the origin.
 */
@OnlyIn(Dist.CLIENT)
public final class ColliderFx {

    /** Angles of the two interaction points (detectors) on the ring. */
    private static final double[] IP = { 0, Math.PI };

    private ColliderFx() {}

    public static void draw(ColliderRingMachine c, Matrix4f m, Matrix4f projection, float time) {
        float r = (float) ColliderRingMachine.RING_RADIUS - 2;
        float energy = (float) c.beamEnergyShare();
        float speed = 0.8f + 2.4f * energy;
        boolean high = MachineRenders.high();
        int segments = MachineRenders.detail(160, 64);
        RenderSystem.disableCull();
        // Proton beam blue-white, antiproton beam magenta; streaming in opposite directions.
        for (int beam = 0; beam < 2; beam++) {
            float dir = beam == 0 ? 1 : -1;
            float[] col = beam == 0 ? new float[] { 0.45f, 0.7f, 1f } : new float[] { 1f, 0.35f, 0.85f };
            var shader = FxDraw.fx(6, time, col[0], col[1], col[2], 0.95f, speed, dir, 0.35f, 0);
            if (shader == null) return;
            // Two beams side by side, half a block apart, so the crossing at the detectors reads.
            float off = beam == 0 ? 0.45f : -0.45f;
            FxDraw.ribbon(shader, new Matrix4f(m).translate(0, 0, off),
                    FxDraw.arc(r, 0, 2 * Math.PI, segments), 1.6f, 1f);
        }
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 9);
        for (double a : IP) {
            Matrix4f at = new Matrix4f(m).translate((float) Math.cos(a) * r, (float) Math.sin(a) * r, 0);
            var flash = FxDraw.fx(7, time, 1f, 0.6f, 0.95f, 0.7f + 0.3f * pulse, 0, 0, 0, 0);
            if (flash != null)
                FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(at).scale(1.6f + 1.4f * pulse * (0.4f + energy)),
                        projection, flash);
        }
        var additive = StellarShaders.additiveColor();
        if (!high || additive == null) return;
        RenderSystem.setShader(() -> additive);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int ip = 0; ip < IP.length; ip++) {
            Vector3f origin = new Vector3f((float) Math.cos(IP[ip]) * r, (float) Math.sin(IP[ip]) * r, 0);
            // A new event every 0.4 s at each detector; each track lives one event and fades out.
            int event = (int) Math.floor(time * 2.5);
            float age = time * 2.5f - event;
            tracks(buf, m, origin, event * 31 + ip * 7919, age, energy);
        }
        Tesselator.getInstance().end();
    }

    /**
     * Charged tracks in the detector's magnetic field: spirals that tighten as the particle loses energy, curling one
     * way or the other by charge, plus a few straight neutral tracks.
     */
    private static void tracks(BufferBuilder buf, Matrix4f m, Vector3f origin, int seed, float age, float energy) {
        var rnd = new java.util.Random(seed);
        int count = 6 + rnd.nextInt(6) + (int) (energy * 6);
        float fade = 1 - age;
        for (int k = 0; k < count; k++) {
            boolean neutral = rnd.nextFloat() < 0.2f;
            float charge = rnd.nextBoolean() ? 1 : -1;
            double heading = rnd.nextDouble() * 2 * Math.PI;
            double tilt = (rnd.nextDouble() - 0.5) * 1.2;
            float radius = 2 + rnd.nextFloat() * 7;
            float[] col = neutral ? new float[] { 1f, 0.9f, 0.5f } :
                    charge > 0 ? new float[] { 0.5f, 0.85f, 1f } : new float[] { 1f, 0.5f, 0.8f };
            List<Vector3f> pts = new ArrayList<>();
            int steps = 40;
            double ang = heading;
            float x = 0, y = 0, z = 0, step = radius * 0.12f;
            for (int i = 0; i <= steps * Math.min(1, age * 3 + 0.2); i++) {
                pts.add(new Vector3f(origin).add(x, y, z));
                if (!neutral) {
                    ang += charge * step / radius;
                    radius *= 0.965f;
                    step *= 0.975f;
                }
                x += (float) (Math.cos(ang) * Math.cos(tilt)) * step;
                y += (float) (Math.sin(ang) * Math.cos(tilt)) * step;
                z += (float) Math.sin(tilt) * step;
            }
            for (int i = 1; i < pts.size(); i++) {
                float a = 0.8f * fade * (1 - (float) i / (steps + 1) * 0.6f);
                FxDraw.line(buf, m, pts.get(i - 1), pts.get(i), 0.06f, col[0], col[1], col[2], a, a);
            }
        }
    }
}
