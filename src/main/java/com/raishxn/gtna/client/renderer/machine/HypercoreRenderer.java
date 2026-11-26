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
 * Holographic core of the Nexus ME Hypercore (D-434, U8), inside the glass chamber: a glowing sheath around the
 * central conduit, one cube per installed crafting module orbiting it in a helix per tier, data streams running to
 * every crafting CPU interface, and a pulse while crafting jobs
 * run (a strong one when a new job starts). In transcendent mode a gold fractal shell wraps the core.
 */
public final class HypercoreRenderer extends DynamicRender<NexusMEHyperCoreMachine, HypercoreRenderer> {

    public static final Codec<HypercoreRenderer> CODEC = Codec.unit(HypercoreRenderer::new);
    public static final DynamicRenderType<NexusMEHyperCoreMachine, HypercoreRenderer> TYPE = DynamicRenderManager
            .register(GTNACORE.id("nexus_me_hypercore/render"), new DynamicRenderType<>(CODEC));

    private static final int REACH = 160;
    /** Cube half-size and colour of tiers I to IV. */
    private static final float[] SIZE = { 0.16f, 0.2f, 0.24f, 0.28f };
    // EN: the open ring between the central conduit column (3 blocks from the chamber axis) and the crafting cores
    // EN: (4.5 blocks away): module cubes orbit there, so the whole hologram stays inside the glass chamber.
    // PT: o anel livre entre a coluna central (a 3 blocos do eixo da câmara) e os núcleos de crafting (a 4,5 blocos):
    // PT: os cubos dos módulos orbitam ali, então o holograma inteiro fica dentro da câmara de vidro.
    // ZH: 中央导管柱（距腔室轴 3 格）与合成核心（距 4.5 格）之间的空环：模块立方体在此环绕，使整个全息影像留在玻璃腔内。
    private static final float ORBIT = 3.75f, SHEATH = 3.3f;
    private static final float[][] TIER_COLOUR = { { 0.35f, 0.85f, 1f }, { 0.4f, 1f, 0.6f }, { 1f, 0.75f, 0.3f },
            { 0.95f, 0.4f, 1f } };

    private static final Map<BlockPos, NexusMEHyperCoreMachine> QUEUE = new LinkedHashMap<>();
    private static final Map<BlockPos, float[]> PULSES = new HashMap<>();
    /**
     * Glass chamber centre as two (up, left, forward) offsets from the controller whose midpoint is the exact centre
     * (the chamber is an even number of blocks wide), its half height, and the interface positions.
     */
    private static int[] centre, centreHi;
    private static float halfHeight = 8;
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
        // EN: the hologram lives inside the laminated-glass chamber ('J') around the crafting cores, seen through
        // EN: the glass (author's request, D-434): its centre is the middle of the glass box.
        // PT: o holograma fica dentro da câmara de vidro laminado ('J') em volta dos núcleos, visto através do vidro
        // PT: (pedido do autor, D-434): o centro é o meio da caixa de vidro.
        // ZH: 全息影像位于合成核心周围的夹层玻璃腔（'J'）内，透过玻璃可见（作者要求，D-434）：中心为玻璃箱中点。
        int[] lo = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE }, hi = { -1, -1, -1 };
        for (int a = 0; a < aisles.length; a++)
            for (int r = 0; r < aisles[a].length; r++)
                for (int c = 0; c < aisles[a][r].length(); c++) {
                    if (aisles[a][r].charAt(c) != 'J') continue;
                    int[] p = { r, c, a };
                    for (int i = 0; i < 3; i++) {
                        lo[i] = Math.min(lo[i], p[i]);
                        hi[i] = Math.max(hi[i], p[i]);
                    }
                }
        if (hi[0] < 0) {
            for (int[] p : cores) for (int i = 0; i < 3; i++) {
                lo[i] = Math.min(lo[i], p[i]);
                hi[i] = Math.max(hi[i], p[i]);
            }
        }
        centre = new int[3];
        centreHi = new int[3];
        for (int i = 0; i < 3; i++) {
            centre[i] = Math.floorDiv(lo[i] + hi[i], 2) - q[i];
            centreHi[i] = Math.floorDiv(lo[i] + hi[i] + 1, 2) - q[i];
        }
        halfHeight = Math.max(2, (hi[0] - lo[0]) / 2f - 1.5f);
        interfaces = new ArrayList<>();
        for (int[] p : ports) interfaces.add(new int[] { p[0] - q[0], p[1] - q[1], p[2] - q[2] });
    }

    private static BlockPos at(NexusMEHyperCoreMachine m, int[] off) {
        return RelativeDirection.offsetPos(m.getPos(), m.getFrontFacing(), m.getUpwardsFacing(), m.isFlipped(),
                off[0], off[1], off[2]);
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // EN: drawn after block entities and before translucent blocks: the chamber glass is then blended over the
        // EN: hologram, so it shows through the glass; after the translucent pass the glass depth hid it entirely.
        // PT: desenhado depois das block entities e antes dos blocos translúcidos: o vidro da câmara é misturado por
        // PT: cima do holograma, que aparece através dele; depois do passe translúcido a profundidade do vidro o
        // PT: escondia por inteiro.
        // ZH: 在方块实体之后、半透明方块之前绘制：腔室玻璃随后叠加混合在全息影像上，使其透过玻璃可见；若在半透明阶段之后
        // ZH: 绘制，玻璃的深度会将其完全遮住。
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || QUEUE.isEmpty()) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        float time = (float) ((net.minecraft.Util.getMillis() % 3_600_000L) / 1000.0);
        try {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            MachineRenders.depthTested();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            for (var m : QUEUE.values()) {
                if (m.isInValid() || !m.isFormed() && !MachineRenders.SHOWCASE) continue;
                BlockPos c = at(m, centre());
                if (!MachineRenders.inReach(c, cam, REACH)) continue;
                BlockPos c2 = at(m, centreHi);
                pose.pushPose();
                pose.translate((c.getX() + c2.getX()) / 2.0 + 0.5 - cam.x, (c.getY() + c2.getY()) / 2.0 + 0.5 - cam.y,
                        (c.getZ() + c2.getZ()) / 2.0 + 0.5 - cam.z);
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
        // A glowing sheath around the central conduit column, the height of the chamber.
        float breathe = 1 + 0.04f * (float) Math.sin(time * 3);
        FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(centre).rotateY(time * 0.4f).scale(SHEATH * breathe,
                halfHeight + 1, SHEATH * breathe), projection, core);
        if (transcendent && MachineRenders.high()) {
            var shell = FxDraw.fx(8, time, 1f, 0.8f, 0.35f, 0.6f, 14, 0, 0, 0);
            for (int k = 0; k < 3; k++)
                FxMeshes.draw(FxMeshes.cube(), new Matrix4f(centre).rotateY(time * 0.3f * (k + 1))
                        .scale(ORBIT + 0.3f * k, halfHeight * (0.6f + 0.15f * k), ORBIT + 0.3f * k), projection, shell);
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
                // A helix per tier, stacked bottom (I) to top (IV) in the ring around the column.
                float band = 2 * halfHeight / 4;
                float y = -halfHeight + band * (tier + (i + 0.5f) / count);
                double a = i * 2.39996 + time * speed * (tier % 2 == 0 ? 1 : -1);
                float rad = ORBIT + 0.15f * (float) Math.sin(time * 2 + i);
                Vector3f p = new Vector3f((float) Math.cos(a) * rad, y, (float) Math.sin(a) * rad);
                FxMeshes.draw(FxMeshes.cube(), new Matrix4f(centre).translate(p)
                        .rotateXYZ(time + i, time * 0.7f + i, 0).scale(SIZE[tier]), projection, cube);
            }
        }

        if (busy > 0 || since < 2) {
            var wave = FxDraw.fx(7, time, 0.5f, 0.9f, 1f, 0, 0, 0, 0, 0);
            float t = busy > 0 ? (time % 1.6f) / 1.6f : 1;
            if (wave != null && busy > 0) {
                wave.safeGetUniform("Tint").set(0.5f, 0.9f, 1f, 0.35f * (1 - t));
                FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(centre).scale(3 + t * 4.5f, halfHeight, 3 + t * 4.5f),
                        projection, wave);
            }
            if (wave != null && since < 2) {
                wave.safeGetUniform("Tint").set(1f, 1f, 1f, 0.8f * (1 - since / 2));
                FxMeshes.draw(FxMeshes.sphere(), new Matrix4f(centre).scale(3 + since * 2.5f, halfHeight,
                        3 + since * 2.5f), projection, wave);
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
