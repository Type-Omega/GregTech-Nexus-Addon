package com.raishxn.gtna.client.renderer.machine.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.raishxn.gtna.client.renderer.machine.MachineRenders;
import com.raishxn.gtna.client.renderer.machine.StellarShaders;
import com.raishxn.gtna.common.data.multiblock.MEStorageStructure;
import com.raishxn.gtna.common.data.multiblock.NexusAssemblyForgeStructure;
import com.raishxn.gtna.common.data.multiblock.NexusFluxMatrixStructure;
import com.raishxn.gtna.common.machine.multiblock.energy.MEStorageMachine;
import com.raishxn.gtna.common.machine.multiblock.energy.NexusFluxMatrixMachine;
import com.raishxn.gtna.common.machine.multiblock.energy.NexusMolecularForgeMachine;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Effects of the redesigned Nexus structures (GTIA-D-434, D-437). Every frame {@code at} is centred on the
 * controller with world axes; the positions of the parts come from the structure's text aisles.
 */
public final class StructureFx {

    /** GT voltage colours from LV upwards, for the plasma column. */
    private static final float[][] TIER = { { 0.86f, 0.86f, 0.86f }, { 1f, 0.55f, 0.1f }, { 0.25f, 0.6f, 1f },
            { 0.7f, 0.25f, 1f }, { 0.2f, 0.9f, 0.9f }, { 1f, 0.25f, 0.6f }, { 0.3f, 1f, 0.4f }, { 0.35f, 0.4f, 1f },
            { 1f, 0.85f, 0.2f }, { 1f, 0.3f, 0.3f }, { 0.3f, 1f, 0.8f }, { 0.9f, 0.4f, 1f }, { 1f, 1f, 1f },
            { 1f, 0.2f, 0.9f } };

    private StructureFx() {}

    private static Vector3f local(NexusFluxMatrixMachine m, BlockPos p) {
        return new Vector3f(p.getX() - m.getPos().getX(), p.getY() - m.getPos().getY(), p.getZ() - m.getPos().getZ());
    }

    // ------------------------------------------------------------------ Flux Matrix

    /**
     * Plasma column inside the capacitor floors, filled to the network charge and coloured by the average capacitor
     * tier, with arcs jumping from the top of the column to the toroid's nodes (more arcs with more charge).
     */
    public static void flux(NexusFluxMatrixMachine m, Matrix4f at, Matrix4f projection, float time) {
        int floors = m.renderFloors() > 0 ? m.renderFloors() : MachineRenders.SHOWCASE ? 8 : 0;
        if (floors <= 0) return;
        var layout = AisleLayout.of(NexusFluxMatrixStructure.RESOURCE, true, floors, 'B');
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        Vector3f centre = new Vector3f();
        var capacitors = layout.offsets('D');
        for (int[] off : capacitors) {
            Vector3f p = local(m, AisleLayout.at(m, off));
            centre.add(p);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        if (capacitors.isEmpty()) return;
        centre.div(capacitors.size());
        float fill = MachineRenders.SHOWCASE ? 0.6f + 0.3f * (float) Math.sin(time * 0.4) : m.renderFill();
        float[] col = TIER[Math.max(0, Math.min(TIER.length - 1, m.renderTier() - 1))];
        if (MachineRenders.SHOWCASE && m.renderTier() <= 1) col = TIER[7];
        float height = maxY - minY + 1;
        List<Vector3f> axis = List.of(new Vector3f(centre.x, minY - 0.5f, centre.z),
                new Vector3f(centre.x, maxY + 0.5f, centre.z));
        var column = FxDraw.fx(9, time, col[0], col[1], col[2], 0.9f, Math.max(0.04f, fill), height * 0.6f, 0, 0);
        if (column == null) return;
        FxDraw.ribbon(column, at, axis, 4.6f, 1 / height);
        if (MachineRenders.high()) {
            var inner = FxDraw.fx(9, time * 1.7f, 1f, 1f, 1f, 0.6f, Math.max(0.04f, fill), height * 1.3f, 0, 0);
            FxDraw.ribbon(inner, at, axis, 1.8f, 1 / height);
        }

        // Arcs from the top of the plasma to the toroid's nodes (the orange lamps above the column).
        Vector3f top = new Vector3f(centre.x, minY - 0.5f + height * fill, centre.z);
        List<Vector3f> nodes = new ArrayList<>();
        for (int[] off : layout.offsets('L')) {
            Vector3f p = local(m, AisleLayout.at(m, off));
            if (p.y > maxY + 1) nodes.add(p);
        }
        int arcs = Math.round(nodes.size() * Math.min(1, fill * 1.4f));
        if (arcs == 0 || fill < 0.05f) return;
        // EN: a new arc shape eight times a second, as real discharges flicker.
        // PT: um formato de arco novo oito vezes por segundo, como descargas reais tremulam.
        // ZH: 电弧形状每秒刷新八次，模拟真实放电的闪烁。
        int frame = (int) (time * 8);
        var arc = FxDraw.fx(6, time, Math.min(1, col[0] + 0.4f), Math.min(1, col[1] + 0.4f), 1f, 0.95f, 4, 1, 0.3f,
                0);
        int segments = MachineRenders.detail(14, 6);
        for (int i = 0; i < arcs; i++) {
            Random rnd = new Random(frame * 131L + i * 7919L);
            if (rnd.nextFloat() < 0.25f) continue;
            Vector3f end = nodes.get((i + frame) % nodes.size());
            List<Vector3f> path = new ArrayList<>();
            for (int s = 0; s <= segments; s++) {
                float t = (float) s / segments;
                Vector3f p = new Vector3f(top).lerp(end, t);
                float jag = (float) Math.sin(Math.PI * t) * 1.6f;
                p.add((rnd.nextFloat() - 0.5f) * jag, (rnd.nextFloat() - 0.5f) * jag + (float) Math.sin(Math.PI * t) *
                        3, (rnd.nextFloat() - 0.5f) * jag);
                path.add(p);
            }
            FxDraw.ribbon(arc, at, path, 0.45f, 0.2f);
        }
    }

    // ------------------------------------------------------------------ ME Storage

    /**
     * Status lights on the glass side of every storage core, blinking with the storage use, and a capacity ring
     * around the front of the conduit that fills with the used share.
     */
    public static void storage(MEStorageMachine m, Matrix4f at, Matrix4f projection, float time) {
        int cores = m.renderCores() > 0 ? m.renderCores() : MachineRenders.SHOWCASE ? 24 : 0;
        if (cores <= 0) return;
        int slices = Math.max(1, cores / MEStorageStructure.CORES_PER_SLICE);
        var layout = AisleLayout.of(MEStorageStructure.RESOURCE, false, slices, 'E');
        float use = MachineRenders.SHOWCASE ? 0.6f : m.renderUsage();
        boolean online = MachineRenders.SHOWCASE || m.renderOnline();
        var additive = StellarShaders.additiveColor();
        if (additive == null) return;
        Direction front = m.getFrontFacing();
        Vector3f f = new Vector3f(front.getStepX(), 0, front.getStepZ());
        Vector3f side = new Vector3f(front.getClockWise().getStepX(), 0, front.getClockWise().getStepZ());
        List<Vector3f> positions = new ArrayList<>();
        Vector3f centre = new Vector3f();
        for (int[] off : layout.offsets('c')) {
            BlockPos p = AisleLayout.at(m, off);
            Vector3f v = new Vector3f(p.getX() - m.getPos().getX(), p.getY() - m.getPos().getY(),
                    p.getZ() - m.getPos().getZ());
            positions.add(v);
            centre.add(v);
        }
        if (positions.isEmpty()) return;
        centre.div(positions.size());
        RenderSystem.setShader(() -> additive);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int tick = (int) (time * 6);
        Vector3f up = new Vector3f(0, 1, 0);
        for (int i = 0; i < positions.size(); i++) {
            Vector3f p = positions.get(i);
            float sign = Math.signum(new Vector3f(p).sub(centre).dot(side));
            Vector3f face = new Vector3f(p).add(new Vector3f(side).mul(sign * 0.53f));
            for (int led = 0; led < 6; led++) {
                Random rnd = new Random(i * 977L + led * 31L + tick / (1 + led % 3));
                boolean lit = online && rnd.nextFloat() < 0.15f + 0.85f * use;
                float[] c = !online ? new float[] { 0.8f, 0.1f, 0.1f } :
                        rnd.nextFloat() < use * 0.6f ? new float[] { 1f, 0.65f, 0.15f } :
                                new float[] { 0.2f, 1f, 0.55f };
                float a = lit ? 0.95f : 0.12f;
                Vector3f centreLed = new Vector3f(face).add(new Vector3f(up).mul(-0.3f + 0.3f * (led % 3)))
                        .add(new Vector3f(f).mul(led < 3 ? -0.18f : 0.18f));
                square(buf, at, centreLed, up, f, 0.07f, c, a);
            }
        }
        Tesselator.getInstance().end();

        // Capacity ring around the front of the conduit.
        float front0 = -Float.MAX_VALUE;
        for (Vector3f p : positions) front0 = Math.max(front0, p.dot(f));
        Vector3f ringCentre = new Vector3f(centre).add(new Vector3f(f).mul(front0 - centre.dot(f) + 0.9f));
        Matrix4f ringFrame = new Matrix4f(at).translate(ringCentre)
                .rotate(new org.joml.Quaternionf().rotationTo(new Vector3f(0, 0, 1), f));
        var ring = FxDraw.fx(6, time, use > 0.85f ? 1f : 0.3f, use > 0.85f ? 0.5f : 0.9f, 1f, 0.9f, 1.5f, 1, 0.5f,
                0);
        if (ring == null || use <= 0) return;
        double start = Math.PI / 2;
        FxDraw.ribbon(ring, ringFrame, FxDraw.arc(3.3f, start, start - 2 * Math.PI * use,
                Math.max(4, (int) (64 * use))), 0.35f, 1);
    }

    private static void square(BufferBuilder b, Matrix4f m, Vector3f c, Vector3f u, Vector3f v, float s, float[] col,
                               float a) {
        Vector3f[] q = { new Vector3f(c).sub(new Vector3f(u).mul(s)).sub(new Vector3f(v).mul(s)),
                new Vector3f(c).add(new Vector3f(u).mul(s)).sub(new Vector3f(v).mul(s)),
                new Vector3f(c).add(new Vector3f(u).mul(s)).add(new Vector3f(v).mul(s)),
                new Vector3f(c).sub(new Vector3f(u).mul(s)).add(new Vector3f(v).mul(s)) };
        for (int k : new int[] { 0, 1, 2, 0, 2, 3 })
            b.vertex(m, q[k].x, q[k].y, q[k].z).color(col[0], col[1], col[2], a).endVertex();
    }

    // ------------------------------------------------------------------ Assembly Forge

    /**
     * Hologram of the item being forged, floating in the core: the item itself, large and full-bright, a scan plane
     * sweeping through it and a swarm of nanites orbiting it.
     */
    public static void forge(NexusMolecularForgeMachine m, PoseStack pose, Matrix4f at, Matrix4f projection,
                             float time) {
        ItemStack stack = m.renderItem();
        if (stack.isEmpty() && MachineRenders.SHOWCASE) stack = new ItemStack(Items.NETHER_STAR);
        if (stack.isEmpty()) return;
        var layout = AisleLayout.of(NexusAssemblyForgeStructure.RESOURCE, false, 1, 'M');
        Vector3f c = layout.centre(m, 'E');
        float size = 7;

        // The item, drawn with normal blending and depth so the core's glass shell holds it.
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        pose.pushPose();
        pose.translate(c.x, c.y + 0.3f * (float) Math.sin(time * 1.3), c.z);
        pose.mulPose(new org.joml.Quaternionf().rotationY(time * 0.7f));
        pose.scale(size, size, size);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 0xF000F0,
                OverlayTexture.NO_OVERLAY, pose, buffers, m.getLevel(), 0);
        buffers.endBatch();
        pose.popPose();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        Matrix4f centre = new Matrix4f(at).translate(c);
        float scanY = (float) Math.sin(time * 1.1) * size * 0.45f;
        var scan = FxDraw.fx(8, time, 0.4f, 0.95f, 1f, 0.75f, 40, 0, 0, 0);
        if (scan != null) FxMeshes.draw(FxMeshes.quad(), new Matrix4f(centre).translate(0, scanY, 0)
                .rotateX((float) (Math.PI / 2)).scale(size * 0.6f), projection, scan);
        var glow = FxDraw.fx(8, time, 0.55f, 0.85f, 1f, 0.35f, 10, 0, 0, 0);
        if (glow != null) FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(centre).scale(size * 0.62f), projection,
                glow);
        var additive = StellarShaders.additiveColor();
        if (additive == null) return;
        RenderSystem.setShader(() -> additive);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int nanites = MachineRenders.detail(90, 30);
        for (int i = 0; i < nanites; i++) {
            double a = i * 2.39996 + time * (0.6 + (i % 5) * 0.15);
            float y = (float) Math.sin(i * 1.7 + time * 0.9) * size * 0.4f;
            float r = size * (0.45f + 0.12f * (float) Math.sin(i + time * 2));
            Vector4f p = new Vector4f((float) Math.cos(a) * r, y, (float) Math.sin(a) * r, 1);
            FxDraw.billboard(buf, centre, p.x, p.y, p.z, 0.06f, 0.6f, 0.95f, 1f, 0.85f);
        }
        Tesselator.getInstance().end();
    }
}
