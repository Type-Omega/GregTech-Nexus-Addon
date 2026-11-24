package com.raishxn.gtna.client.renderer.machine;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.api.machine.feature.rift.RiftCore;
import com.raishxn.gtna.client.renderer.machine.fx.ColliderFx;
import com.raishxn.gtna.client.renderer.machine.fx.LanceFx;
import com.raishxn.gtna.client.renderer.machine.fx.RiftFx;
import com.raishxn.gtna.common.machine.multiblock.originals.ColliderRingMachine;
import com.raishxn.gtna.common.machine.multiblock.originals.LanceMachine;
import com.raishxn.gtna.common.machine.multiblock.originals.OriginalMachine;
import com.raishxn.gtna.common.machine.multiblock.originals.RiftGateMachine;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Effects of the original multiblocks, drawn after translucent blocks like the Forge of Gods: the collider's two
 * counter-rotating beams and pulsing core, the rift's swirling event horizon, and the lance's muzzle flash and bolt.
 * The structures themselves are real blocks.
 */
public final class OriginalsRenderer {

    private static final Map<BlockPos, OriginalMachine> QUEUE = new LinkedHashMap<>();

    private OriginalsRenderer() {}

    /** Free-standing rift horizon for {@code /gtna_rift_preview}; null when off. */
    @org.jetbrains.annotations.Nullable
    public static Vec3 debugRift;
    /** Seed, frequency, phase and amplitude (0..1) of the debug horizon. */
    public static float[] debugRiftParams = { 0.3f, 0.5f, 0.5f, 0.5f };

    public static void onRenderLevelStage(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() == net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS &&
                debugRift != null)
            debugRift(event);
        if (event.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS ||
                QUEUE.isEmpty())
            return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        float time = (float) ((net.minecraft.Util.getMillis() % 3_600_000L) / 1000.0);
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        for (var m : QUEUE.values()) {
            if (m instanceof ColliderRingMachine c && c.isCaptured() && !c.isInValid()) {
                pose.pushPose();
                spinningParts(c, pose, cam, projection, time);
                pose.popPose();
            } else if (m instanceof LanceMachine l && l.isCaptured() && !l.isInValid()) {
                pose.pushPose();
                turret(l, pose, cam, projection);
                pose.popPose();
            }
        }
        try {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            for (var m : QUEUE.values()) {
                if (m.isInValid() || !m.isFormed()) continue;
                // EN: effects only; the captured parts above are blocks and draw at every level (GTIA-D-434, U11).
                // PT: só os efeitos; as peças capturadas acima são blocos e aparecem em todo nível (GTIA-D-434, U11).
                // ZH: 这里只画特效；上面捕获的部件是方块，任何等级都会绘制（GTIA-D-434，U11）。
                int reach = m instanceof LanceMachine ? 768 : 384;
                if (!MachineRenders.inReach(m.getPos(), cam, reach)) continue;
                pose.pushPose();
                if (m instanceof ColliderRingMachine c && (c.isBeamOn() || MachineRenders.SHOWCASE)) ColliderFx.draw(c,
                        frame(c, pose, cam, c.offset(ColliderRingMachine.CENTER_UP, ColliderRingMachine.CENTER_BACK)),
                        projection, time);
                else if (m instanceof RiftGateMachine r && r.isRiftOpen()) rift(r, pose, cam, projection);
                else if (m instanceof LanceMachine l) lance(l, pose, cam, projection, time);
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

    /** Pose at {@code point}, with +Z along the machine's back direction. */
    private static Matrix4f frame(OriginalMachine m, PoseStack pose, Vec3 cam, BlockPos point) {
        pose.translate(point.getX() + 0.5 - cam.x, point.getY() + 0.5 - cam.y, point.getZ() + 0.5 - cam.z);
        Direction back = m.getFrontFacing().getOpposite();
        var rot = new Quaternionf().rotationTo(new Vector3f(0, 0, 1),
                new Vector3f(back.getStepX(), back.getStepY(), back.getStepZ()));
        return new Matrix4f(pose.last().pose()).rotate(rot);
    }

    @org.jetbrains.annotations.Nullable
    private static com.mojang.blaze3d.vertex.VertexBuffer horizon;

    /**
     * The rift's event horizon: the 3D preview's swirling disc (stellar_fx mode 5) filling the ring, tinted with the
     * biome's colour, brighter with stability.
     */
    private static void rift(RiftGateMachine r, PoseStack pose, Vec3 cam, Matrix4f projection) {
        Matrix4f m = frame(r, pose, cam, r.offset(RiftGateMachine.CENTER_UP, RiftGateMachine.CENTER_BACK));
        float[] col = biomeColour(r.biome());
        float strength = (float) Math.max(0.35, r.stability() / 100);
        float time = (float) ((net.minecraft.Util.getMillis() % 3_600_000L) / 1000.0) * (1 + (float) r.resonance());
        double[] v = r.view();
        float seed = 0.5f, freq = 0.5f, phase = 0.5f, amp = 0.5f;
        if (v.length >= 10) {
            long bits = Double.doubleToRawLongBits(v[9]);
            seed = (float) ((bits * 0x9E3779B97F4A7C15L >>> 40) / (double) (1L << 24));
            freq = (float) Math.min(1, Math.max(0, v[0] / 1000));
            phase = (float) (((v[1] % 360) + 360) % 360 / 360);
            amp = (float) Math.min(1, Math.max(0, v[2] / 100));
        }
        drawHorizon(m, projection, col, strength, time, seed, freq, phase, amp);
        RiftFx.draw(r, new Matrix4f(m), projection, col, strength, time);
    }

    private static void drawHorizon(Matrix4f m, Matrix4f projection, float[] col, float strength, float time,
                                    float seed, float freq, float phase, float amp) {
        var shader = StellarShaders.fxAdditive();
        if (shader == null) return;
        if (horizon == null) {
            BufferBuilder b = new BufferBuilder(6 * 20);
            b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            float[][] q = { { -1, -1, 0, 0 }, { 1, -1, 1, 0 }, { 1, 1, 1, 1 }, { -1, 1, 0, 1 } };
            for (int k : new int[] { 0, 1, 2, 0, 2, 3 }) b.vertex(q[k][0], q[k][1], 0).uv(q[k][2], q[k][3]).endVertex();
            horizon = new com.mojang.blaze3d.vertex.VertexBuffer(com.mojang.blaze3d.vertex.VertexBuffer.Usage.STATIC);
            horizon.bind();
            horizon.upload(b.end());
            com.mojang.blaze3d.vertex.VertexBuffer.unbind();
        }
        shader.safeGetUniform("Time").set(time);
        shader.safeGetUniform("Mode").set(5f);
        shader.safeGetUniform("Tint").set(col[0], col[1], col[2], strength);
        shader.safeGetUniform("Rift").set(seed, freq, phase, amp);
        RenderSystem.disableCull();
        horizon.bind();
        horizon.drawWithShader(new Matrix4f(m).scale((float) RiftGateMachine.INNER_RADIUS), projection, shader);
        com.mojang.blaze3d.vertex.VertexBuffer.unbind();
    }

    private static void debugRift(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        var shader = StellarShaders.fxAdditive();
        if (shader == null) return;
        var pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(debugRift.x - cam.x, debugRift.y - cam.y, debugRift.z - cam.z);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        float[] col = biomeColour(RiftCore.Biome.CRYSTAL_PLAINS);
        drawHorizon(new Matrix4f(pose.last().pose()), new Matrix4f(event.getProjectionMatrix()), col, 1f,
                (float) ((net.minecraft.Util.getMillis() % 3_600_000L) / 1000.0), debugRiftParams[0],
                debugRiftParams[1], debugRiftParams[2],
                debugRiftParams[3]);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        pose.popPose();
    }

    private static float[] biomeColour(RiftCore.Biome biome) {
        return switch (biome) {
            case CRYSTAL_PLAINS -> new float[] { 0.5f, 0.9f, 1f };
            case MERCURY_OCEAN -> new float[] { 0.75f, 0.78f, 0.85f };
            case FORGE_WORLD -> new float[] { 1f, 0.45f, 0.15f };
            case SPORE_FOREST -> new float[] { 0.4f, 1f, 0.45f };
            case ANTIMATTER_ABYSS -> new float[] { 1f, 0.2f, 0.5f };
            case CIVILISATION_RELIC -> new float[] { 1f, 0.85f, 0.4f };
            case VOID -> new float[] { 0.35f, 0.15f, 0.65f };
        };
    }

    private static void lance(LanceMachine l, PoseStack pose, Vec3 cam, Matrix4f projection, float time) {
        int since = l.secondsSinceShot();
        double[] v = l.view();
        double charge = v.length > 1 && v[1] > 0 ? v[0] / v[1] : 0;
        double landing = v.length > 0 ? v[v.length - 1] : -1;
        if (landing >= 0) {
            pose.pushPose();
            BlockPos ground = l.offset(0, LanceMachine.MUZZLE_BACK / 2);
            pose.translate(ground.getX() + 0.5 - cam.x, ground.getY() + 0.5 - cam.y, ground.getZ() + 0.5 - cam.z);
            LanceFx.capsule(new Matrix4f(pose.last().pose()), projection, landing, time);
            pose.popPose();
        }
        BlockPos muzzle = l.offset(LanceMachine.MUZZLE_UP, LanceMachine.MUZZLE_BACK);
        BlockPos pivot = PIVOTS.get(l.getPos());
        float[] yaw = YAW.get(l.getPos());
        Matrix4f m;
        if (l.isCaptured() && pivot != null && yaw != null) {
            // Follow the slewing gun: turn the muzzle and the barrel axis about the turret bearing.
            pose.translate(pivot.getX() + 0.5 - cam.x, pivot.getY() + 0.5 - cam.y, pivot.getZ() + 0.5 - cam.z);
            pose.mulPose(new Quaternionf().rotationY(yaw[0]));
            pose.translate(muzzle.getX() - pivot.getX(), muzzle.getY() - pivot.getY(), muzzle.getZ() - pivot.getZ());
            Direction back = l.getFrontFacing().getOpposite();
            m = new Matrix4f(pose.last().pose()).rotate(new Quaternionf().rotationTo(new Vector3f(0, 0, 1),
                    new Vector3f(back.getStepX(), back.getStepY(), back.getStepZ())));
        } else m = frame(l, pose, cam, muzzle);
        if (since <= 2) LanceFx.shot(m, since, time);
        else LanceFx.charge(m, charge, time);
    }

    // ------------------------------------------------------------------ captured parts

    private static final Map<BlockPos, com.mojang.blaze3d.vertex.VertexBuffer[]> MESHES = new java.util.HashMap<>();

    /** The collider's main ring and gyroscopes, taken into the controller, spinning in opposite directions. */
    private static void spinningParts(ColliderRingMachine c, PoseStack pose, Vec3 cam, Matrix4f projection,
                                      float time) {
        var level = c.getLevel();
        if (level == null) return;
        BlockPos center = c.offset(ColliderRingMachine.CENTER_UP, ColliderRingMachine.CENTER_BACK);
        if (!retryDue(c.getPos())) return;
        var meshes = MESHES.computeIfAbsent(c.getPos().immutable(), k -> new com.mojang.blaze3d.vertex.VertexBuffer[] {
                mesh(c, "ring", center), mesh(c, "gyro", center) });
        if (meshes[0] == null && meshes[1] == null) {
            // Parts not found yet (chunks still loading): try again in a few seconds, not every frame.
            MESHES.remove(c.getPos());
            RETRY.put(c.getPos().immutable(), net.minecraft.Util.getMillis() + 5000);
            return;
        }
        var shader = GameRenderer.getRendertypeCutoutShader();
        if (shader == null) return;
        pose.translate(center.getX() + 0.5 - cam.x, center.getY() + 0.5 - cam.y, center.getZ() + 0.5 - cam.z);
        Direction back = c.getFrontFacing().getOpposite();
        var axis = new Vector3f(back.getStepX(), back.getStepY(), back.getStepZ());
        RenderSystem.setShaderTexture(0, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        net.minecraft.client.Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        float[] spin = { time * 0.12f, -time * 0.5f };
        for (int i = 0; i < 2; i++) {
            if (meshes[i] == null) continue;
            Matrix4f model = new Matrix4f(pose.last().pose()).rotate(spin[i], axis).translate(-0.5f, -0.5f, -0.5f);
            meshes[i].bind();
            meshes[i].drawWithShader(model, projection, shader);
        }
        com.mojang.blaze3d.vertex.VertexBuffer.unbind();
    }

    private static final Map<BlockPos, BlockPos> PIVOTS = new java.util.HashMap<>();
    private static final Map<BlockPos, Long> RETRY = new java.util.HashMap<>();

    private static boolean retryDue(BlockPos key) {
        Long at = RETRY.get(key);
        if (at == null) return true;
        if (net.minecraft.Util.getMillis() < at) return false;
        RETRY.remove(key);
        return true;
    }

    private static final Map<BlockPos, float[]> YAW = new java.util.HashMap<>();

    /** The lance's gun, taken into the controller, slewing about the vertical axis to the selected body. */
    private static void turret(LanceMachine l, PoseStack pose, Vec3 cam, Matrix4f projection) {
        if (l.getLevel() == null) return;
        BlockPos key = l.getPos().immutable();
        if (!retryDue(key)) return;
        BlockPos pivot = PIVOTS.computeIfAbsent(key, k -> pivot(l));
        if (pivot == null) {
            PIVOTS.remove(key);
            RETRY.put(key, net.minecraft.Util.getMillis() + 5000);
            return;
        }
        if (!retryDue(key)) return;
        var meshes = MESHES.computeIfAbsent(key,
                k -> new com.mojang.blaze3d.vertex.VertexBuffer[] { mesh(l, "kinetic_orbital_lance", "gun", pivot) });
        if (meshes[0] == null) {
            MESHES.remove(key);
            RETRY.put(key, net.minecraft.Util.getMillis() + 5000);
            return;
        }
        var shader = GameRenderer.getRendertypeCutoutShader();
        if (shader == null || meshes[0] == null) return;
        // Slew towards the target at about 6 degrees a second.
        float[] yaw = YAW.computeIfAbsent(key, k -> new float[] { (float) l.yaw(), net.minecraft.Util.getMillis() });
        float dt = Math.min(1, (net.minecraft.Util.getMillis() - yaw[1]) / 1000f);
        yaw[1] = net.minecraft.Util.getMillis();
        float diff = (float) l.yaw() - yaw[0];
        yaw[0] += Math.signum(diff) * Math.min(Math.abs(diff), 0.1f * dt);
        pose.translate(pivot.getX() + 0.5 - cam.x, pivot.getY() + 0.5 - cam.y, pivot.getZ() + 0.5 - cam.z);
        RenderSystem.setShaderTexture(0, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        net.minecraft.client.Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        Matrix4f model = new Matrix4f(pose.last().pose()).rotate(yaw[0], new Vector3f(0, 1, 0))
                .translate(-0.5f, -0.5f, -0.5f);
        meshes[0].bind();
        meshes[0].drawWithShader(model, projection, shader);
        com.mojang.blaze3d.vertex.VertexBuffer.unbind();
    }

    /** Horizontal centroid of the gun at the height of its lowest block: the turret bearing. */
    private static BlockPos pivot(LanceMachine l) {
        var parts = com.raishxn.gtna.common.machine.multiblock.originals.CapturedParts.positions(l.getLevel(),
                "kinetic_orbital_lance", "gun", l.getPos(), l.getFrontFacing(), l.getUpwardsFacing(), l.isFlipped());
        if (parts.isEmpty()) return null;
        long x = 0, z = 0;
        int y = Integer.MAX_VALUE;
        for (BlockPos p : parts.keySet()) {
            x += p.getX();
            z += p.getZ();
            y = Math.min(y, p.getY());
        }
        return new BlockPos((int) (x / parts.size()), y, (int) (z / parts.size()));
    }

    private static com.mojang.blaze3d.vertex.VertexBuffer mesh(ColliderRingMachine c, String tag, BlockPos center) {
        return mesh(c, "antimatter_collider_ring", tag, center);
    }

    private static com.mojang.blaze3d.vertex.VertexBuffer mesh(OriginalMachine c, String name, String tag,
                                                               BlockPos center) {
        var parts = com.raishxn.gtna.common.machine.multiblock.originals.CapturedParts.positions(c.getLevel(),
                name, tag, c.getPos(), c.getFrontFacing(), c.getUpwardsFacing(), c.isFlipped());
        if (parts.isEmpty()) return null;
        long t0 = System.nanoTime();
        // Membership by packed long: BlockPos's own hash collides badly over a structure this size.
        var occupied = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(parts.size());
        for (BlockPos p : parts.keySet()) occupied.add(p.asLong());
        var states = new java.util.HashMap<Character, net.minecraft.world.level.block.state.BlockState>();
        float[] shades = new float[6];
        for (Direction side : Direction.values()) shades[side.ordinal()] = quadShade(c.getLevel(), side);
        var dispatcher = net.minecraft.client.Minecraft.getInstance().getBlockRenderer();
        var random = net.minecraft.util.RandomSource.create(42);
        var builder = new BufferBuilder(
                Math.min(parts.size() * 4 * 4, 1 << 18) * DefaultVertexFormat.BLOCK.getVertexSize());
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        PoseStack ps = new PoseStack();
        for (var e : parts.entrySet()) {
            BlockPos p = e.getKey();
            var state = states.computeIfAbsent(e.getValue(),
                    k -> com.raishxn.gtna.common.machine.multiblock.originals.CapturedParts.blockFor(name, k)
                            .defaultBlockState());
            var model = dispatcher.getBlockModel(state);
            ps.pushPose();
            ps.translate(p.getX() - center.getX(), p.getY() - center.getY(), p.getZ() - center.getZ());
            for (Direction side : Direction.values()) {
                if (occupied.contains(BlockPos.offset(p.asLong(), side))) continue;
                random.setSeed(42);
                // Match the blocks still in the world: vanilla face shading and the light at that spot (packed
                // light reads the live lightmap, so day and night still apply).
                float shade = shades[side.ordinal()];
                int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(c.getLevel(), p.relative(side));
                for (var quad : model.getQuads(state, side, random,
                        net.minecraftforge.client.model.data.ModelData.EMPTY, null))
                    builder.putBulkData(ps.last(), quad, quad.isShade() ? shade : 1f, quad.isShade() ? shade : 1f,
                            quad.isShade() ? shade : 1f, light,
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            }
            ps.popPose();
        }
        var mesh = new com.mojang.blaze3d.vertex.VertexBuffer(com.mojang.blaze3d.vertex.VertexBuffer.Usage.STATIC);
        mesh.bind();
        mesh.upload(builder.end());
        com.mojang.blaze3d.vertex.VertexBuffer.unbind();
        com.raishxn.gtna.GTNACORE.LOGGER.info("[GTNA perf] {} {} mesh: {} blocks in {} ms", name, tag, parts.size(),
                (System.nanoTime() - t0) / 1_000_000);
        return mesh;
    }

    private static float quadShade(net.minecraft.world.level.Level level, Direction side) {
        return level.getShade(side, true);
    }

    // ------------------------------------------------------------------ the three dynamic renders

    abstract static class Base<M extends OriginalMachine, R extends Base<M, R>> extends DynamicRender<M, R> {

        @Override
        public boolean shouldRenderOffScreen(M machine) {
            return true;
        }

        @Override
        public int getViewDistance() {
            return 384;
        }

        @Override
        public void render(M machine, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light,
                           int overlay) {
            if (machine.getLevel() == null) return;
            OriginalsSound.track(machine);
            if (machine.isFormed() || machine instanceof LanceMachine l && l.isCaptured())
                QUEUE.put(machine.getPos().immutable(), machine);
        }
    }

    public static final class ColliderRender extends Base<ColliderRingMachine, ColliderRender> {

        public static final Codec<ColliderRender> CODEC = Codec.unit(ColliderRender::new);
        public static final DynamicRenderType<ColliderRingMachine, ColliderRender> TYPE = DynamicRenderManager
                .register(GTNACORE.id("collider/render"), new DynamicRenderType<>(CODEC));

        @Override
        public DynamicRenderType<ColliderRingMachine, ColliderRender> getType() {
            return TYPE;
        }

        @Override
        public boolean shouldRender(ColliderRingMachine machine, Vec3 cameraPos) {
            return machine.isBeamOn() || machine.isCaptured() || MachineRenders.SHOWCASE && machine.isFormed();
        }

        @Override
        public AABB getRenderBoundingBox(ColliderRingMachine machine) {
            return new AABB(machine.offset(ColliderRingMachine.CENTER_UP, ColliderRingMachine.CENTER_BACK)).inflate(70);
        }
    }

    public static final class RiftRender extends Base<RiftGateMachine, RiftRender> {

        public static final Codec<RiftRender> CODEC = Codec.unit(RiftRender::new);
        public static final DynamicRenderType<RiftGateMachine, RiftRender> TYPE = DynamicRenderManager
                .register(GTNACORE.id("rift/render"), new DynamicRenderType<>(CODEC));

        @Override
        public DynamicRenderType<RiftGateMachine, RiftRender> getType() {
            return TYPE;
        }

        @Override
        public boolean shouldRender(RiftGateMachine machine, Vec3 cameraPos) {
            return machine.isRiftOpen();
        }

        @Override
        public AABB getRenderBoundingBox(RiftGateMachine machine) {
            return new AABB(machine.offset(RiftGateMachine.CENTER_UP, RiftGateMachine.CENTER_BACK)).inflate(60);
        }
    }

    public static final class LanceRender extends Base<LanceMachine, LanceRender> {

        public static final Codec<LanceRender> CODEC = Codec.unit(LanceRender::new);
        public static final DynamicRenderType<LanceMachine, LanceRender> TYPE = DynamicRenderManager
                .register(GTNACORE.id("lance/render"), new DynamicRenderType<>(CODEC));

        @Override
        public DynamicRenderType<LanceMachine, LanceRender> getType() {
            return TYPE;
        }

        @Override
        public boolean shouldRender(LanceMachine machine, Vec3 cameraPos) {
            return machine.isFormed() || machine.isCaptured();
        }

        @Override
        public AABB getRenderBoundingBox(LanceMachine machine) {
            return new AABB(machine.offset(LanceMachine.MUZZLE_UP, LanceMachine.MUZZLE_BACK)).inflate(700);
        }
    }
}
