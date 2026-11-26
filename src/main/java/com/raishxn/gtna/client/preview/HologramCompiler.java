package com.raishxn.gtna.client.preview;

import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.preview.PreviewTiming;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Compiles one hologram {@link HologramSession.View} off the render thread: one buffer per render layer, uploaded on
 * the render thread, then the block entities that have a renderer. Same work as GTCEu's {@code prepareBuffers}, but
 * it fills the view's own buffers, stops when the view is closed, releases what it can no longer upload and logs
 * what it cost.
 */
public final class HologramCompiler {

    /** Draws {@code blocks} of one render layer into the buffer (GTCEu's private {@code renderBlocks}). */
    @FunctionalInterface
    public interface LayerRenderer {

        void render(TrackedDummyWorld level, PoseStack poseStack, BlockRenderDispatcher dispatcher, RenderType layer,
                    WorldSceneRenderer.VertexConsumerWrapper buffer, Collection<BlockPos> blocks);
    }

    private HologramCompiler() {}

    public static void start(HologramSession.View view, LayerRenderer renderer, Consumer<HologramSession.View> done) {
        Thread thread = new Thread(() -> compile(view, renderer, done), "GTNA hologram compile");
        thread.setDaemon(true);
        view.bind(thread);
        thread.start();
    }

    private static void compile(HologramSession.View view, LayerRenderer renderer,
                                Consumer<HologramSession.View> done) {
        long start = PreviewTiming.now();
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        List<RenderType> layers = RenderType.chunkBufferLayers();
        long vertices = 0;
        ModelBlockRenderer.enableCaching();
        try {
            PoseStack poseStack = new PoseStack();
            for (int i = 0; i < layers.size(); i++) {
                if (Thread.interrupted() || view.closed()) return;
                RenderType layer = layers.get(i);
                BufferBuilder buffer = new BufferBuilder(layer.bufferSize());
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                renderer.render(view.level, poseStack, dispatcher, layer,
                        new WorldSceneRenderer.VertexConsumerWrapper(buffer), view.blocks);
                BufferBuilder.RenderedBuffer rendered = buffer.end();
                vertices += rendered.drawState().vertexCount();
                VertexBuffer target = view.buffers()[i];
                RenderSystem.recordRenderCall(() -> upload(target, rendered));
            }
        } finally {
            ModelBlockRenderer.clearCache();
        }
        Set<BlockPos> withRenderer = new HashSet<>();
        for (BlockPos pos : view.entityCandidates) {
            if (Thread.interrupted() || view.closed()) return;
            BlockEntity tile = view.level.getBlockEntity(pos);
            if (tile != null && mc.getBlockEntityRenderDispatcher().getRenderer(tile) != null) withRenderer.add(pos);
        }
        view.blockEntities = withRenderer;
        view.compiled = true;
        done.accept(view);
        PreviewTiming.log("hologram compile: layer={}, blocks={}, vertices={} (~{} quads), {} ms",
                view.layer, view.blocks.size(), vertices, vertices / 4, PreviewTiming.millisSince(start));
    }

    private static void upload(VertexBuffer target, BufferBuilder.RenderedBuffer rendered) {
        try {
            if (target.isInvalid()) {
                rendered.release();
                return;
            }
            target.bind();
            target.upload(rendered);
            VertexBuffer.unbind();
        } catch (RuntimeException e) {
            GTNACORE.LOGGER.error("Hologram buffer upload failed", e);
        }
    }
}
