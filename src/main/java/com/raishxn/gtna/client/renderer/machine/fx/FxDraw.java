package com.raishxn.gtna.client.renderer.machine.fx;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.raishxn.gtna.client.renderer.machine.StellarShaders;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Drawing helpers of the machine effects: stellar_fx uniforms, camera-facing ribbons for beams and columns, and the
 * flat colour primitives (billboards, glows, rings, cylinders) drawn with the additive colour shader.
 */
@OnlyIn(Dist.CLIENT)
public final class FxDraw {

    private FxDraw() {}

    /** Sets the uniforms of the additive stellar_fx shader; null when shaders are not loaded. */
    @org.jetbrains.annotations.Nullable
    public static ShaderInstance fx(int mode, float time, float r, float g, float b, float a, float p0, float p1,
                                    float p2, float p3) {
        var shader = StellarShaders.fxAdditive();
        if (shader == null) return null;
        shader.safeGetUniform("Time").set(time);
        shader.safeGetUniform("Mode").set((float) mode);
        shader.safeGetUniform("Tint").set(r, g, b, a);
        shader.safeGetUniform("Rift").set(p0, p1, p2, p3);
        return shader;
    }

    /** Camera position in the frame of {@code m} (m maps local space to view space, where the camera is at 0). */
    public static Vector3f cameraIn(Matrix4f m) {
        return new Matrix4f(m).invert().transformPosition(new Vector3f());
    }

    /**
     * A ribbon along {@code path} (local points), always turned to the camera; uv.x goes across 0..1, uv.y is the
     * length along the path times {@code vScale}. Drawn with the shader already set up by {@link #fx}.
     */
    public static void ribbon(ShaderInstance shader, Matrix4f m, List<Vector3f> path, float width, float vScale) {
        if (path.size() < 2) return;
        Vector3f cam = cameraIn(m);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        float v = 0;
        Vector3f[] prev = null;
        float prevV = 0;
        for (int i = 0; i < path.size(); i++) {
            Vector3f p = path.get(i);
            Vector3f tangent = new Vector3f(path.get(Math.min(i + 1, path.size() - 1)))
                    .sub(path.get(Math.max(i - 1, 0)));
            Vector3f side = new Vector3f(tangent).cross(new Vector3f(cam).sub(p));
            if (side.lengthSquared() < 1e-8f) side.set(0, 1, 0);
            side.normalize(width / 2);
            if (i > 0) v += path.get(i).distance(path.get(i - 1)) * vScale;
            Vector3f[] edge = { new Vector3f(p).sub(side), new Vector3f(p).add(side) };
            if (prev != null) {
                buf.vertex(m, prev[0].x, prev[0].y, prev[0].z).uv(0, prevV).endVertex();
                buf.vertex(m, prev[1].x, prev[1].y, prev[1].z).uv(1, prevV).endVertex();
                buf.vertex(m, edge[1].x, edge[1].y, edge[1].z).uv(1, v).endVertex();
                buf.vertex(m, prev[0].x, prev[0].y, prev[0].z).uv(0, prevV).endVertex();
                buf.vertex(m, edge[1].x, edge[1].y, edge[1].z).uv(1, v).endVertex();
                buf.vertex(m, edge[0].x, edge[0].y, edge[0].z).uv(0, v).endVertex();
            }
            prev = edge;
            prevV = v;
        }
        RenderSystem.setShader(() -> shader);
        Tesselator.getInstance().end();
    }

    /** Points on a circle of radius r in the XY plane, from angle a0 to a1. */
    public static List<Vector3f> arc(float r, double a0, double a1, int segments) {
        var out = new java.util.ArrayList<Vector3f>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            double a = a0 + (a1 - a0) * i / segments;
            out.add(new Vector3f((float) (Math.cos(a) * r), (float) (Math.sin(a) * r), 0));
        }
        return out;
    }

    // ------------------------------------------------------------------ flat colour primitives

    public static void billboard(BufferBuilder b, Matrix4f m, float x, float y, float z, float s, float r, float g,
                                 float bl, float a) {
        for (int plane = 0; plane < 3; plane++) {
            float[][] v = switch (plane) {
                case 0 -> new float[][] { { -s, -s, 0 }, { s, -s, 0 }, { s, s, 0 }, { -s, s, 0 } };
                case 1 -> new float[][] { { -s, 0, -s }, { s, 0, -s }, { s, 0, s }, { -s, 0, s } };
                default -> new float[][] { { 0, -s, -s }, { 0, s, -s }, { 0, s, s }, { 0, -s, s } };
            };
            for (int k : new int[] { 0, 1, 2, 0, 2, 3 })
                b.vertex(m, x + v[k][0], y + v[k][1], z + v[k][2]).color(r, g, bl, a).endVertex();
        }
    }

    public static void sphereGlow(BufferBuilder b, Matrix4f m, float radius, float r, float g, float bl, float a) {
        int seg = 32;
        for (int plane = 0; plane < 3; plane++) {
            for (int i = 0; i < seg; i++) {
                double a0 = 2 * Math.PI * i / seg, a1 = 2 * Math.PI * (i + 1) / seg;
                float[] p0 = onRing(plane, a0, radius), p1 = onRing(plane, a1, radius);
                b.vertex(m, 0, 0, 0).color(r, g, bl, a).endVertex();
                b.vertex(m, p0[0], p0[1], p0[2]).color(r, g, bl, 0f).endVertex();
                b.vertex(m, p1[0], p1[1], p1[2]).color(r, g, bl, 0f).endVertex();
            }
        }
    }

    private static float[] onRing(int plane, double a, float radius) {
        float c = (float) Math.cos(a) * radius, s = (float) Math.sin(a) * radius;
        return switch (plane) {
            case 0 -> new float[] { c, s, 0 };
            case 1 -> new float[] { c, 0, s };
            default -> new float[] { 0, c, s };
        };
    }

    /** Open cylinder along +Z. */
    public static void cylinder(BufferBuilder buf, Matrix4f m, float r, float z0, float z1, float red, float green,
                                float blue, float alpha) {
        int seg = 32;
        for (int i = 0; i < seg; i++) {
            double a0 = 2 * Math.PI * i / seg, a1 = 2 * Math.PI * (i + 1) / seg;
            float x0 = (float) Math.cos(a0) * r, y0 = (float) Math.sin(a0) * r;
            float x1 = (float) Math.cos(a1) * r, y1 = (float) Math.sin(a1) * r;
            buf.vertex(m, x0, y0, z0).color(red, green, blue, alpha).endVertex();
            buf.vertex(m, x1, y1, z0).color(red, green, blue, alpha).endVertex();
            buf.vertex(m, x1, y1, z1).color(red, green, blue, alpha * 0.4f).endVertex();
            buf.vertex(m, x0, y0, z0).color(red, green, blue, alpha).endVertex();
            buf.vertex(m, x1, y1, z1).color(red, green, blue, alpha * 0.4f).endVertex();
            buf.vertex(m, x0, y0, z1).color(red, green, blue, alpha * 0.4f).endVertex();
        }
    }

    /** Flat ring facing +Z at depth {@code z}, fading outwards. */
    public static void ring(BufferBuilder buf, Matrix4f m, float r0, float r1, float z, float red, float green,
                            float blue, float alpha) {
        int seg = 48;
        for (int i = 0; i < seg; i++) {
            double a0 = 2 * Math.PI * i / seg, a1 = 2 * Math.PI * (i + 1) / seg;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1),
                    s1 = (float) Math.sin(a1);
            buf.vertex(m, c0 * r0, s0 * r0, z).color(red, green, blue, alpha).endVertex();
            buf.vertex(m, c1 * r0, s1 * r0, z).color(red, green, blue, alpha).endVertex();
            buf.vertex(m, c1 * r1, s1 * r1, z).color(red, green, blue, 0).endVertex();
            buf.vertex(m, c0 * r0, s0 * r0, z).color(red, green, blue, alpha).endVertex();
            buf.vertex(m, c1 * r1, s1 * r1, z).color(red, green, blue, 0).endVertex();
            buf.vertex(m, c0 * r1, s0 * r1, z).color(red, green, blue, 0).endVertex();
        }
    }

    /** A thin line drawn as a camera-agnostic cross of two quads, for tracks and data streams. */
    public static void line(BufferBuilder b, Matrix4f m, Vector3f p0, Vector3f p1, float w, float r, float g,
                            float bl, float a0, float a1) {
        Vector3f d = new Vector3f(p1).sub(p0);
        Vector3f u = Math.abs(d.y) < 0.9f * d.length() ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f s1 = new Vector3f(d).cross(u).normalize(w), s2 = new Vector3f(d).cross(s1).normalize(w);
        for (Vector3f s : new Vector3f[] { s1, s2 }) {
            b.vertex(m, p0.x - s.x, p0.y - s.y, p0.z - s.z).color(r, g, bl, a0).endVertex();
            b.vertex(m, p0.x + s.x, p0.y + s.y, p0.z + s.z).color(r, g, bl, a0).endVertex();
            b.vertex(m, p1.x + s.x, p1.y + s.y, p1.z + s.z).color(r, g, bl, a1).endVertex();
            b.vertex(m, p0.x - s.x, p0.y - s.y, p0.z - s.z).color(r, g, bl, a0).endVertex();
            b.vertex(m, p1.x + s.x, p1.y + s.y, p1.z + s.z).color(r, g, bl, a1).endVertex();
            b.vertex(m, p1.x - s.x, p1.y - s.y, p1.z - s.z).color(r, g, bl, a1).endVertex();
        }
    }
}
