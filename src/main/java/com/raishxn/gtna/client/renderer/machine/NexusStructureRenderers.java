package com.raishxn.gtna.client.renderer.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.client.renderer.machine.fx.StructureFx;
import com.raishxn.gtna.common.machine.multiblock.energy.MEStorageMachine;
import com.raishxn.gtna.common.machine.multiblock.energy.NexusFluxMatrixMachine;
import com.raishxn.gtna.common.machine.multiblock.energy.NexusMolecularForgeMachine;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dynamic renders of the three redesigned Nexus structures (GTIA-D-434, D-437): the Flux Matrix plasma column, the
 * ME Storage cell lights and the Assembly Forge hologram. The block entity pass only queues the machine; drawing
 * happens after the translucent blocks ({@link StructureFx}).
 */
public final class NexusStructureRenderers {

    private static final Map<BlockPos, MultiblockControllerMachine> QUEUE = new LinkedHashMap<>();
    static final int REACH = 160;

    private NexusStructureRenderers() {}

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || QUEUE.isEmpty()) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        float time = (float) ((net.minecraft.Util.getMillis() % 3_600_000L) / 1000.0);
        try {
            for (var m : QUEUE.values()) {
                if (m.isInValid() || !m.isFormed() || !MachineRenders.inReach(m.getPos(), cam, REACH)) continue;
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
                pose.pushPose();
                BlockPos p = m.getPos();
                pose.translate(p.getX() + 0.5 - cam.x, p.getY() + 0.5 - cam.y, p.getZ() + 0.5 - cam.z);
                Matrix4f at = new Matrix4f(pose.last().pose());
                if (m instanceof NexusFluxMatrixMachine flux) StructureFx.flux(flux, at, projection, time);
                else if (m instanceof MEStorageMachine storage) StructureFx.storage(storage, at, projection, time);
                else if (m instanceof NexusMolecularForgeMachine forge)
                    StructureFx.forge(forge, pose, at, projection, time);
                pose.popPose();
            }
        } finally {
            QUEUE.clear();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
        }
    }

    /** Shared queueing render; each machine gets its own registered type. */
    abstract static class Base<M extends MultiblockControllerMachine, R extends Base<M, R>>
                              extends DynamicRender<M, R> {

        @Override
        public boolean shouldRender(M machine, Vec3 cameraPos) {
            return machine.isFormed() && MachineRenders.effects();
        }

        @Override
        public boolean shouldRenderOffScreen(M machine) {
            return true;
        }

        @Override
        public int getViewDistance() {
            return MachineRenders.reach(REACH);
        }

        @Override
        public AABB getRenderBoundingBox(M machine) {
            Direction back = machine.getFrontFacing().getOpposite();
            return new AABB(machine.getPos().relative(back, 12)).inflate(40);
        }

        @Override
        public void render(M machine, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light,
                           int overlay) {
            if (machine.getLevel() != null && machine.isFormed()) QUEUE.put(machine.getPos().immutable(), machine);
        }
    }

    public static final class FluxRender extends Base<NexusFluxMatrixMachine, FluxRender> {

        public static final Codec<FluxRender> CODEC = Codec.unit(FluxRender::new);
        public static final DynamicRenderType<NexusFluxMatrixMachine, FluxRender> TYPE = DynamicRenderManager
                .register(GTNACORE.id("nexus_flux_matrix/render"), new DynamicRenderType<>(CODEC));

        @Override
        public DynamicRenderType<NexusFluxMatrixMachine, FluxRender> getType() {
            return TYPE;
        }
    }

    public static final class StorageRender extends Base<MEStorageMachine, StorageRender> {

        public static final Codec<StorageRender> CODEC = Codec.unit(StorageRender::new);
        public static final DynamicRenderType<MEStorageMachine, StorageRender> TYPE = DynamicRenderManager
                .register(GTNACORE.id("me_storage/render"), new DynamicRenderType<>(CODEC));

        @Override
        public DynamicRenderType<MEStorageMachine, StorageRender> getType() {
            return TYPE;
        }
    }

    public static final class ForgeRender extends Base<NexusMolecularForgeMachine, ForgeRender> {

        public static final Codec<ForgeRender> CODEC = Codec.unit(ForgeRender::new);
        public static final DynamicRenderType<NexusMolecularForgeMachine, ForgeRender> TYPE = DynamicRenderManager
                .register(GTNACORE.id("nexus_molecular_forge/render"), new DynamicRenderType<>(CODEC));

        @Override
        public DynamicRenderType<NexusMolecularForgeMachine, ForgeRender> getType() {
            return TYPE;
        }
    }
}
