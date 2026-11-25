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
 * Antimatter Collider Ring effects (GTIA-D-434): two counter-rotating beams on shaded ribbons inside the ring, with a
 * glow that reads through the casing; at both interaction points a collision every 0.4 s with a white-hot flash, a
 * shock disc across the beam and bubble-chamber tracks curling out (tracks on HIGH only).
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
        Vector3f cam = FxDraw.cameraIn(m);
        RenderSystem.disableCull();
        // Proton beam blue-white, antiproton beam magenta; streaming in opposite directions.
        for (int beam = 0; beam < 2; beam++) {
            float dir = beam == 0 ? 1 : -1;
            float[] col = beam == 0 ? new float[] { 0.45f, 0.7f, 1f } : new float[] { 1f, 0.35f, 0.85f };
            var shader = FxDraw.fx(6, time, col[0], col[1], col[2], 0.95f, speed, dir, 0.35f, 0);
            if (shader == null) return;
            // Two beams side by side, half a block apart, so the crossing at the detectors reads.
            float off = beam == 0 ? 0.45f : -0.45f;
            var path = FxDraw.arc(r, 0, 2 * Math.PI, segments);
            FxDraw.ribbon(shader, new Matrix4f(m).translate(0, 0, off), path, 2.5f, 1f);
            // EN: the glow that crosses the casing: the beam path moved towards the camera past the tube wall, so the
            // EN: ring hides its own far side but the beam reads from outside; terrain in front still hides it.
            // PT: o brilho que atravessa o casing: o caminho do feixe trazido para a câmera além da parede do tubo,
            // PT: então o anel esconde o próprio lado de trás mas o feixe aparece de fora; terreno na frente ainda
            // tapa.
            // ZH: 穿透外壳的辉光：把光束路径朝相机方向移出管壁，环仍遮挡自身背面，但从外面能看到光束；前方地形照样遮挡。
            var lifted = lift(path, new Vector3f(cam).sub(0, 0, off), TUBE_LIFT);
            // EN: three layers, hot core, glow and wide halo, sized for a ring ~80 blocks across seen from 200+
            // EN: blocks away: at the old widths (2.4 and 11) the beams vanished in a distant view. LOW keeps all
            // EN: three, a little narrower.
            // PT: três camadas, núcleo quente, brilho e halo largo, dimensionadas para um anel de ~80 blocos visto a
            // PT: mais de 200 blocos: com as larguras antigas (2,4 e 11) os feixes sumiam de longe. O LOW mantém as
            // PT: três, um pouco mais estreitas.
            // ZH: 三层：炽热核心、辉光和宽光晕，按约 80 格宽、在 200 格外观看的环来设定；旧宽度（2.4 和 11）远看时
            // ZH: 光束会消失。LOW 保留三层，只是略窄。
            var core = FxDraw.fx(6, time, 1f, 1f, 1f, 1f, speed, dir, 0.6f, 0);
            FxDraw.ribbon(core, new Matrix4f(m).translate(0, 0, off), lifted, 1.8f, 1f);
            var glow = FxDraw.fx(6, time, col[0], col[1], col[2], 1f, speed, dir, 0.35f, 0);
            FxDraw.ribbon(glow, new Matrix4f(m).translate(0, 0, off), lifted, high ? 6f : 5f, 1f);
            var halo = FxDraw.fx(6, time, col[0], col[1], col[2], high ? 0.5f : 0.42f, speed * 0.5f, dir, 0.08f, 0);
            FxDraw.ribbon(halo, new Matrix4f(m).translate(0, 0, off * 4), lifted, high ? 22f : 16f, 1f);
            var haze = FxDraw.fx(6, time, col[0], col[1], col[2], 0.18f, speed * 0.3f, dir, 0.03f, 0);
            FxDraw.ribbon(haze, new Matrix4f(m).translate(0, 0, off * 4), lifted, high ? 44f : 30f, 1f);
        }
        // A collision event every 0.4 s at each detector: a sharp flash that decays, and a shock disc across the beam.
        float rate = 2.5f;
        int event = (int) Math.floor(time * rate);
        float age = time * rate - event;
        float burst = (1 - age) * (1 - age) * (1 - age);
        for (double a : IP) {
            Vector3f ip = new Vector3f((float) Math.cos(a) * r, (float) Math.sin(a) * r, 0);
            Vector3f out = lift(ip, cam, DETECTOR_LIFT);
            Matrix4f at = new Matrix4f(m).translate(out);
            var flash = FxDraw.fx(7, time, 1f, 0.6f, 0.95f, 0.55f + 0.45f * burst, 0, 0, 0, 0);
            if (flash == null) continue;
            FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(at).scale(3f + 9f * burst * (0.5f + energy)), projection,
                    flash);
            var corona = FxDraw.fx(7, time, 0.6f, 0.5f, 1f, 0.18f + 0.2f * burst, 0, 0, 0, 0);
            FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(at).scale(9f + 6f * energy), projection, corona);
        }
        var additive = StellarShaders.additiveColor();
        if (additive == null) return;
        RenderSystem.setShader(() -> additive);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int ip = 0; ip < IP.length; ip++) {
            Vector3f origin = new Vector3f((float) Math.cos(IP[ip]) * r, (float) Math.sin(IP[ip]) * r, 0);
            Vector3f out = lift(origin, cam, DETECTOR_LIFT);
            // The shock disc lies across the beam: the ring's tangent at both detectors is the Y axis.
            Matrix4f disc = new Matrix4f(m).translate(out).rotateX((float) Math.PI / 2);
            float r0 = 1.5f + age * 18f;
            FxDraw.ring(buf, disc, r0, r0 + 3f + 4f * age, 0, 1f, 0.75f, 0.95f, 0.7f * (1 - age));
            FxDraw.sphereGlow(buf, new Matrix4f(m).translate(out), 6f + 10f * burst, 1f, 0.85f, 1f, 0.5f * burst);
            if (high) tracks(buf, m, out, event * 31 + ip * 7919, age, energy);
        }
        Tesselator.getInstance().end();
    }

    // EN: half the ring tube's cross-section plus a block, and the detector's half-size: how far the glow is moved
    // EN: towards the camera to clear the casing.
    // PT: metade da seção do tubo do anel mais um bloco, e a metade do detector: quanto o brilho vem para a câmera
    // PT: para passar à frente do casing.
    // ZH: 环管截面的一半再加一格，以及探测器的一半尺寸：辉光朝相机移动这么远以越过外壳。
    private static final float TUBE_LIFT = 4f, DETECTOR_LIFT = 7f;

    /** {@code p} moved {@code by} towards {@code cam}, never more than half the way. */
    private static Vector3f lift(Vector3f p, Vector3f cam, float by) {
        Vector3f to = new Vector3f(cam).sub(p);
        float d = to.length();
        if (d < 1e-4f) return new Vector3f(p);
        return new Vector3f(p).add(to.mul(Math.min(by, d * 0.5f) / d));
    }

    private static List<Vector3f> lift(List<Vector3f> path, Vector3f cam, float by) {
        var out = new ArrayList<Vector3f>(path.size());
        for (var p : path) out.add(lift(p, cam, by));
        return out;
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
