package com.raishxn.gtna.client.renderer.machine.fx;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Static meshes (POSITION_TEX) shared by the machine effects: a unit sphere, a unit cube and a unit quad. Drawn with
 * a model matrix, so the stellar_fx shader sees object-space positions (fresnel and scanlines need them).
 */
@OnlyIn(Dist.CLIENT)
public final class FxMeshes {

    @Nullable
    private static VertexBuffer sphere, cube, quad;

    private FxMeshes() {}

    public static VertexBuffer sphere() {
        if (sphere != null) return sphere;
        int stacks = 24, slices = 32;
        var b = new BufferBuilder(stacks * slices * 6 * 20);
        b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < stacks; i++) {
            double t0 = Math.PI * i / stacks, t1 = Math.PI * (i + 1) / stacks;
            for (int j = 0; j < slices; j++) {
                double p0 = 2 * Math.PI * j / slices, p1 = 2 * Math.PI * (j + 1) / slices;
                float[][] q = { point(t0, p0, i, j, stacks, slices), point(t1, p0, i + 1, j, stacks, slices),
                        point(t1, p1, i + 1, j + 1, stacks, slices), point(t0, p1, i, j + 1, stacks, slices) };
                for (int k : new int[] { 0, 1, 2, 0, 2, 3 })
                    b.vertex(q[k][0], q[k][1], q[k][2]).uv(q[k][3], q[k][4]).endVertex();
            }
        }
        sphere = upload(b);
        return sphere;
    }

    private static float[] point(double theta, double phi, int i, int j, int stacks, int slices) {
        return new float[] { (float) (Math.sin(theta) * Math.cos(phi)), (float) Math.cos(theta),
                (float) (Math.sin(theta) * Math.sin(phi)), (float) j / slices, (float) i / stacks };
    }

    /** Cube from -1 to 1. */
    public static VertexBuffer cube() {
        if (cube != null) return cube;
        var b = new BufferBuilder(36 * 20);
        b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        int[][] faces = { { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { 2, 1 }, { 2, -1 } };
        for (int[] f : faces) {
            int axis = f[0], sign = f[1];
            float[][] c = { { -1, -1 }, { 1, -1 }, { 1, 1 }, { -1, 1 } };
            float[][] v = new float[4][];
            for (int k = 0; k < 4; k++) {
                float[] p = new float[3];
                p[axis] = sign;
                p[(axis + 1) % 3] = c[k][0];
                p[(axis + 2) % 3] = c[k][1];
                v[k] = new float[] { p[0], p[1], p[2], (c[k][0] + 1) / 2, (c[k][1] + 1) / 2 };
            }
            for (int k : new int[] { 0, 1, 2, 0, 2, 3 })
                b.vertex(v[k][0], v[k][1], v[k][2]).uv(v[k][3], v[k][4]).endVertex();
        }
        cube = upload(b);
        return cube;
    }

    /** Quad from -1 to 1 in the XY plane, UV 0..1. */
    public static VertexBuffer quad() {
        if (quad != null) return quad;
        var b = new BufferBuilder(6 * 20);
        b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        float[][] q = { { -1, -1, 0, 0 }, { 1, -1, 1, 0 }, { 1, 1, 1, 1 }, { -1, 1, 0, 1 } };
        for (int k : new int[] { 0, 1, 2, 0, 2, 3 }) b.vertex(q[k][0], q[k][1], 0).uv(q[k][2], q[k][3]).endVertex();
        quad = upload(b);
        return quad;
    }

    public static void draw(VertexBuffer mesh, Matrix4f model, Matrix4f projection, ShaderInstance shader) {
        mesh.bind();
        mesh.drawWithShader(model, projection, shader);
        VertexBuffer.unbind();
    }

    private static VertexBuffer upload(BufferBuilder b) {
        var vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        vb.upload(b.end());
        VertexBuffer.unbind();
        return vb;
    }
}
