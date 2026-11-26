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
 * Effects of the redesigned Nexus structures (D-434, D-437). Every frame {@code at} is centred on the
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
        // EN: the capacitors fill the column, so the plasma glows on the four faces between them and the glass.
        // PT: os capacitores enchem a coluna, então o plasma brilha nas quatro faces entre eles e o vidro.
        // ZH: 电容填满了柱体，因此等离子体在电容与玻璃之间的四个面上发光。
        var column = FxDraw.fx(9, time, col[0], col[1], col[2], 0.95f, Math.max(0.04f, fill), height * 0.6f, 0, 0);
        if (column == null) return;
        float half = 2.56f, y0 = minY - 0.5f, y1 = maxY + 0.5f;
        for (int side = 0; side < 4; side++) {
            float a = (float) (side * Math.PI / 2);
            float cx = (float) Math.cos(a), cz = (float) Math.sin(a);
            Matrix4f face = new Matrix4f(at).translate(centre.x + cx * half, (y0 + y1) / 2, centre.z + cz * half)
                    .rotateY((float) (-a + Math.PI / 2)).scale(half, (y1 - y0) / 2, 1);
            FxMeshes.draw(FxMeshes.quad(), face, projection, column);
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
     * The "Data Conduit" read at a glance (author's request, after studying gt_shanhai's renders), seen through the
     * side windows: a fill gauge along the top of each window, as long as the used share and coloured green, amber,
     * red as it fills; data packets streaming through the windows to the access hatch while the network is online;
     * and one light per storage core, lit front to back in proportion to the use. Offline, everything turns dim red.
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
        float back = Float.MAX_VALUE, front0 = -Float.MAX_VALUE, reach = 0;
        for (Vector3f p : positions) {
            back = Math.min(back, p.dot(f));
            front0 = Math.max(front0, p.dot(f));
            reach = Math.max(reach, Math.abs(new Vector3f(p).sub(centre).dot(side)));
        }
        back -= 0.5f;
        front0 += 0.5f;
        float length = front0 - back;
        // EN: the cores sit right behind the side windows (laminated glass 'V', which is not see-through enough):
        // EN: the gauge, the packets and the core lights are drawn on the outer face of those windows, 1.6 blocks
        // EN: out from the cores, as a display panel on the glass.
        // PT: os núcleos ficam logo atrás das janelas laterais (vidro laminado 'V', que não é transparente o
        // PT: bastante): o medidor, os pacotes e as luzes dos núcleos são desenhados na face externa dessas janelas,
        // PT: a 1,6 bloco dos núcleos, como um painel no vidro.
        // ZH: 存储核心紧贴侧窗（夹层玻璃 'V'，透明度不足）：量表、数据包和核心灯绘制在这些窗的外表面，距核心 1.6 格，
        // ZH: 如同玻璃上的显示面板。
        float lane = reach + 1.6f;
        // EN: gauge colour by fill: green below 60 %, amber to 85 %, red above (the network is nearly full).
        // PT: cor do medidor pelo preenchimento: verde abaixo de 60 %, âmbar até 85 %, vermelho acima (rede quase
        // PT: cheia).
        // ZH: 量表颜色随填充变化：60% 以下绿色，85% 以下琥珀色，以上红色（网络将满）。
        float[] gauge = !online ? new float[] { 0.7f, 0.1f, 0.1f } :
                use < 0.6f ? new float[] { 0.25f, 1f, 0.55f } :
                        use < 0.85f ? new float[] { 1f, 0.7f, 0.15f } : new float[] { 1f, 0.25f, 0.2f };
        var filled = FxDraw.fx(6, time, gauge[0], gauge[1], gauge[2], 0.95f, online ? 1.2f : 0, 1, 0.4f, 0);
        var guide = FxDraw.fx(6, time, gauge[0], gauge[1], gauge[2], 0.18f, 0, 1, 0.1f, 0);
        var halo = FxDraw.fx(6, time, gauge[0], gauge[1], gauge[2], 0.3f, 0.5f, 1, 0.05f, 0);
        if (filled == null) return;
        for (int sgn : new int[] { -1, 1 }) {
            // Fill gauge along the top of each window: bright up to the used share, a faint guide after it.
            Vector3f start = new Vector3f(centre).add(new Vector3f(f).mul(back - centre.dot(f)))
                    .add(new Vector3f(side).mul(sgn * lane)).add(0, 1.35f, 0);
            FxDraw.ribbon(guide, at, List.of(start, new Vector3f(start).add(new Vector3f(f).mul(length))), 0.25f, 1);
            if (use > 0) {
                List<Vector3f> part = List.of(new Vector3f(start),
                        new Vector3f(start).add(new Vector3f(f).mul(length * Math.min(1, use))));
                FxDraw.ribbon(filled, at, part, 0.42f, 1);
                FxDraw.ribbon(halo, at, part, MachineRenders.high() ? 1.4f : 1f, 1);
            }
        }

        RenderSystem.setShader(() -> additive);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        // Data packets streaming through both windows to the front (the access hatch); fewer on LOW.
        if (online) {
            int packets = Math.min(MachineRenders.detail(48, 16), 4 + 2 * slices);
            for (int sgn : new int[] { -1, 1 }) {
                for (int k = 0; k < packets; k++) {
                    float t = (time * 0.35f + k / (float) packets + (sgn > 0 ? 0.5f / packets : 0)) % 1;
                    Vector3f q = new Vector3f(centre).add(new Vector3f(f).mul(back - centre.dot(f) + t * length))
                            .add(new Vector3f(side).mul(sgn * lane)).add(0, ((k * 3) % 3 - 1) * 0.6f, 0);
                    FxDraw.billboard(buf, at, q.x, q.y, q.z, 0.14f, 0.55f, 0.9f, 1f, 0.9f * (1 - t * 0.4f));
                }
            }
        }
        // One light per storage core, on its window face, lit front to back with the use.
        List<Vector3f> ordered = new ArrayList<>(positions);
        ordered.sort((x, y) -> Float.compare(y.dot(f), x.dot(f)));
        int lit = Math.round(Math.min(1, use) * ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            Vector3f p = ordered.get(i);
            float sign = Math.signum(new Vector3f(p).sub(centre).dot(side));
            Vector3f face = new Vector3f(p).add(new Vector3f(side).mul(sign * (lane - reach)));
            boolean on = online && i < lit;
            float pulse = on ? 0.75f + 0.25f * (float) Math.sin(time * 4 + i * 0.6f) : 0.3f;
            float[] c = on ? gauge : online ? new float[] { 0.25f, 0.35f, 0.45f } : new float[] { 0.6f, 0.1f, 0.1f };
            FxDraw.billboard(buf, at, face.x, face.y, face.z, on ? 0.4f : 0.22f, c[0], c[1], c[2], pulse);
        }
        Tesselator.getInstance().end();

        // Capacity ring around the front of the conduit, the same share as the gauge.
        Vector3f ringCentre = new Vector3f(centre).add(new Vector3f(f).mul(front0 - centre.dot(f) + 0.4f));
        Matrix4f ringFrame = new Matrix4f(at).translate(ringCentre)
                .rotate(new org.joml.Quaternionf().rotationTo(new Vector3f(0, 0, 1), f));
        var ring = FxDraw.fx(6, time, gauge[0], gauge[1], gauge[2], 0.9f, 1.5f, 1, 0.5f, 0);
        if (ring == null || use <= 0) return;
        double start = Math.PI / 2;
        FxDraw.ribbon(ring, ringFrame, FxDraw.arc(3.3f, start, start - 2 * Math.PI * use,
                Math.max(4, (int) (64 * use))), 0.45f, 1);
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
        MachineRenders.depthTested(); // the item batch above ended with the depth test switched off
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
