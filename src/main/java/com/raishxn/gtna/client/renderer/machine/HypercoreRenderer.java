package com.raishxn.gtna.client.renderer.machine;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.client.renderer.machine.fx.FxDraw;
import com.raishxn.gtna.client.renderer.machine.fx.FxMeshes;
import com.raishxn.gtna.common.data.multiblock.GTNAMultiBlockFileReader;
import com.raishxn.gtna.common.machine.multiblock.energy.NexusMEHyperCoreMachine;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holographic core of the Nexus ME Hypercore (GTIA-D-434, U8): one cube per installed crafting module orbiting the
 * lattice in a shell per tier, data streams running to every crafting CPU interface, and a pulse while crafting jobs
 * run (a strong one when a new job starts). In transcendent mode a gold fractal shell wraps the core.
 */
public final class HypercoreRenderer extends DynamicRender<NexusMEHyperCoreMachine, HypercoreRenderer> {

    public static final Codec<HypercoreRenderer> CODEC = Codec.unit(HypercoreRenderer::new);
    public static final DynamicRenderType<NexusMEHyperCoreMachine, HypercoreRenderer> TYPE = DynamicRenderManager
            .register(GTNACORE.id("nexus_me_hypercore/render"), new DynamicRenderType<>(CODEC));

    private static final int REACH = 160;
    /** Shell radius, cube half-size and colour of tiers I to IV. */
    private static final float[] SHELL = { 6, 8.5f, 11, 13.5f }, SIZE = { 0.22f, 0.3f, 0.38f, 0.48f };
    private static final float[][] TIER_COLOUR = { { 0.35f, 0.85f, 1f }, { 0.4f, 1f, 0.6f }, { 1f, 0.75f, 0.3f },
            { 0.95f, 0.4f, 1f } };

    private static final Map<BlockPos, NexusMEHyperCoreMachine> QUEUE = new LinkedHashMap<>();
    private static final Map<BlockPos, float[]> PULSES = new HashMap<>();
    /**
     * Hologram centre (above the lattice) and interface positions as (up, left, forward) offsets from the controller.
     */
    private static int[] centre;
    private static List<int[]> interfaces;

    @Override
    public DynamicRenderType<NexusMEHyperCoreMachine, HypercoreRenderer> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(NexusMEHyperCoreMachine machine, Vec3 cameraPos) {
        return (machine.isFormed() || MachineRenders.SHOWCASE) && MachineRenders.effects();
    }

    @Override
    public boolean shouldRenderOffScreen(NexusMEHyperCoreMachine machine) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return MachineRenders.reach(REACH);
    }

    @Override
    public AABB getRenderBoundingBox(NexusMEHyperCoreMachine machine) {
        return new AABB(at(machine, centre())).inflate(24);
    }

    @Override
    public void render(NexusMEHyperCoreMachine machine, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        if (machine.getLevel() != null && (machine.isFormed() || MachineRenders.SHOWCASE))
            QUEUE.put(machine.getPos().immutable(), machine);
    }

    private static int[] centre() {
        if (centre == null) readLayout();
        return centre;
    }

    /** Reads the lattice centre (mean of the crafting cores 'L') and the interfaces ('P') from the pattern. */
    private static void readLayout() {
        String[][] aisles = GTNAMultiBlockFileReader.loadAisles("nexus_me_hypercore");
        int[] q = null;
        List<int[]> cores = new ArrayList<>(), ports = new ArrayList<>();
        for (int a = 0; a < aisles.length; a++)
            for (int r = 0; r < aisles[a].length; r++)
                for (int c = 0; c < aisles[a][r].length(); c++) {
                    char ch = aisles[a][r].charAt(c);
                    if (ch == 'Q') q = new int[] { r, c, a };
                    else if (ch == 'L') cores.add(new int[] { r, c, a });
                    else if (ch == 'P') ports.add(new int[] { r, c, a });
                }
        if (q == null) q = new int[3];
        double[] sum = new double[3];
        for (int[] p : cores) for (int i = 0; i < 3; i++) sum[i] += p[i];
        int n = Math.max(1, cores.size());
        int top = 0;
        for (String[] aisle : aisles) top = Math.max(top, aisle.length - 1);
        // EN: the lattice is closed, so the hologram floats above its roof, over the centre of the cores.
        // PT: a treliça é fechada, então o holograma flutua acima do teto, sobre o centro dos núcleos.
        // ZH: 晶格是封闭的，因此全息影像悬浮在顶部之上、核心中心的正上方。
        centre = new int[] { top - q[0] + 16, (int) Math.round(sum[1] / n) - q[1],
                (int) Math.round(sum[2] / n) - q[2] };
        interfaces = new ArrayList<>();
        for (int[] p : ports) interfaces.add(new int[] { p[0] - q[0], p[1] - q[1], p[2] - q[2] });
    }

    private static BlockPos at(NexusMEHyperCoreMachine m, int[] off) {
        return RelativeDirection.offsetPos(m.getPos(), m.getFrontFacing(), m.getUpwardsFacing(), m.isFlipped(),
                off[0], off[1], off[2]);
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || QUEUE.isEmpty()) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        float time = (float) ((net.minecraft.Util.getMillis() % 3_600_000L) / 1000.0);
        try {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            for (var m : QUEUE.values()) {
                if (m.isInValid() || !m.isFormed() && !MachineRenders.SHOWCASE) continue;
                BlockPos c = at(m, centre());
                if (!MachineRenders.inReach(c, cam, REACH)) continue;
                pose.pushPose();
                pose.translate(c.getX() + 0.5 - cam.x, c.getY() + 0.5 - cam.y, c.getZ() + 0.5 - cam.z);
                draw(m, new Matrix4f(pose.last().pose()), projection, time, c);
                pose.popPose();
            }
        } finally {
            QUEUE.clear();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
        }
    }

    private static void draw(NexusMEHyperCoreMachine m, Matrix4f centre, Matrix4f projection, float time,
                             BlockPos centrePos) {
        long[] modules = m.modulesByTier();
        int busy = m.busyCpus();
        if (MachineRenders.SHOWCASE && modules[0] + modules[1] + modules[2] + modules[3] == 0) {
            modules = new long[] { 96, 64, 40, 24 };
            busy = (int) (time / 4) % 2 + 1;
        }
        boolean transcendent = m.isTranscendentMode();
        // Pulse state: [last busy count, start of the last pulse].
        float[] pulse = PULSES.computeIfAbsent(m.getPos().immutable(), k -> new float[] { 0, -100 });
        if (busy > pulse[0]) pulse[1] = time;
        pulse[0] = busy;
        float since = time - pulse[1];
        float activity = busy > 0 ? 1 : 0.35f;

        var core = FxDraw.fx(8, time, transcendent ? 1f : 0.4f, transcendent ? 0.85f : 0.9f, transcendent ? 0.4f : 1f,
                0.9f, 6, 0, 0, 0);
        if (core == null) return;
        FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(centre).rotateY(time * 0.4f).scale(2.6f + 0.2f *
                (float) Math.sin(time * 3)), projection, core);
        if (transcendent && MachineRenders.high()) {
            var shell = FxDraw.fx(8, time, 1f, 0.8f, 0.35f, 0.6f, 14, 0, 0, 0);
            for (int k = 0; k < 3; k++)
                FxMeshes.draw(FxMeshes.cube(), new Matrix4f(centre).rotateXYZ(time * 0.3f * (k + 1), time * 0.2f,
                        k * 0.7f).scale(3.6f + k * 0.9f), projection, shell);
        }

        // EN: one cube per installed module; LOW caps the count (LOD), the shells still show every tier.
        // PT: um cubo por módulo instalado; o LOW limita a quantidade (LOD), as camadas ainda mostram todo tier.
        // ZH: 每个已安装模块一个立方体；LOW 限制数量（LOD），各层仍显示所有等级。
        int cap = MachineRenders.detail(320, 64);
        long total = modules[0] + modules[1] + modules[2] + modules[3];
        float share = total > cap ? (float) cap / total : 1;
        for (int tier = 0; tier < 4; tier++) {
            int count = (int) Math.ceil(modules[tier] * share);
            if (count == 0) continue;
            float[] col = TIER_COLOUR[tier];
            var cube = FxDraw.fx(8, time, col[0], col[1], col[2], 0.55f + 0.45f * activity, 18, 0, 0, 0);
            float speed = (0.25f - tier * 0.04f) * (busy > 0 ? 2.2f : 1);
            for (int i = 0; i < count; i++) {
                // Fibonacci sphere: an even shell whatever the count; the shell spins about a tilted axis.
                float y = 1 - 2 * (i + 0.5f) / count;
                float ring = (float) Math.sqrt(1 - y * y);
                double a = i * 2.39996 + time * speed * (tier % 2 == 0 ? 1 : -1);
                Vector3f p = new Vector3f((float) Math.cos(a) * ring, y, (float) Math.sin(a) * ring)
                        .mul(SHELL[tier] + 0.4f * (float) Math.sin(time * 2 + i));
                FxMeshes.draw(FxMeshes.cube(), new Matrix4f(centre).rotateZ(0.35f * tier).translate(p)
                        .rotateXYZ(time + i, time * 0.7f + i, 0).scale(SIZE[tier]), projection, cube);
            }
        }

        if (busy > 0 || since < 2) {
            var wave = FxDraw.fx(7, time, 0.5f, 0.9f, 1f, 0, 0, 0, 0, 0);
            float t = busy > 0 ? (time % 1.6f) / 1.6f : 1;
            if (wave != null && busy > 0) {
                wave.safeGetUniform("Tint").set(0.5f, 0.9f, 1f, 0.35f * (1 - t));
                FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(centre).scale(3 + t * 12), projection, wave);
            }
            if (wave != null && since < 2) {
                wave.safeGetUniform("Tint").set(1f, 1f, 1f, 0.8f * (1 - since / 2));
                FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(centre).scale(2.5f + since * 14), projection, wave);
            }
        }
        streams(m, centre, time, busy, centrePos);
    }

    /** Data streams to the crafting CPU interfaces: a faint line and packets running along it. */
    private static void streams(NexusMEHyperCoreMachine m, Matrix4f centre, float time, int busy, BlockPos c) {
        var additive = StellarShaders.additiveColor();
        if (additive == null || interfaces == null) return;
        RenderSystem.setShader(() -> additive);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int packets = busy > 0 ? MachineRenders.detail(6, 3) : 2;
        float speed = busy > 0 ? 0.9f : 0.25f;
        for (int[] off : interfaces) {
            BlockPos p = at(m, off);
            Vector3f end = new Vector3f(p.getX() - c.getX(), p.getY() - c.getY(), p.getZ() - c.getZ());
            FxDraw.line(buf, centre, new Vector3f(), end, 0.05f, 0.4f, 0.85f, 1f, 0.5f, 0.15f);
            for (int k = 0; k < packets; k++) {
                float t = (time * speed + k / (float) packets) % 1;
                Vector3f q = new Vector3f(end).mul(t);
                FxDraw.billboard(buf, centre, q.x, q.y, q.z, 0.18f, 0.6f, 0.95f, 1f, 0.9f * (1 - t * 0.5f));
            }
        }
        Tesselator.getInstance().end();
    }
}
