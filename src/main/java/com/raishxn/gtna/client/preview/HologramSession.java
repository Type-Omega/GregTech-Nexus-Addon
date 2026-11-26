package com.raishxn.gtna.client.preview;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The compiled holograms of the controller the player is looking at: one {@link View} per layer ({@code -1} is the
 * whole structure). A click on the same controller moves to another layer; a layer that was compiled is shown again
 * without compiling, and nothing is compiled twice. Closing the session stops the compile threads and frees the
 * vertex buffers on the render thread, which GTCEu's {@code cleanPreview} never did.
 */
public final class HologramSession {

    /** What identifies "the same hologram": controller, shape and orientation. */
    public record Key(BlockPos pos, Object definition, Direction front, Direction up, Object shape) {}

    public static final class View {

        public final int layer;
        public final TrackedDummyWorld level;
        public final Collection<BlockPos> blocks;
        public final List<BlockPos> entityCandidates;
        @Nullable
        public final IMultiController controller;
        private final VertexBuffer[] buffers;
        private volatile Thread thread;
        private volatile boolean closed;
        public volatile boolean compiled;
        @Nullable
        public volatile Set<BlockPos> blockEntities;

        View(int layer, TrackedDummyWorld level, Collection<BlockPos> blocks, List<BlockPos> entityCandidates,
             @Nullable IMultiController controller) {
            this.layer = layer;
            this.level = level;
            this.blocks = blocks;
            this.entityCandidates = entityCandidates;
            this.controller = controller;
            List<RenderType> layers = RenderType.chunkBufferLayers();
            this.buffers = new VertexBuffer[layers.size()];
            for (int i = 0; i < buffers.length; i++) buffers[i] = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }

        public VertexBuffer[] buffers() {
            return buffers;
        }

        public boolean closed() {
            return closed;
        }

        void bind(Thread thread) {
            this.thread = thread;
        }

        void close() {
            closed = true;
            Thread running = thread;
            if (running != null) running.interrupt();
            Runnable free = () -> {
                for (VertexBuffer buffer : buffers) buffer.close();
            };
            if (RenderSystem.isOnRenderThread()) free.run();
            else RenderSystem.recordRenderCall(free::run);
        }
    }

    private static volatile HologramSession current;
    private static volatile View active;

    private final Key key;
    private final Map<Integer, View> views = new HashMap<>();

    private HologramSession(Key key) {
        this.key = key;
    }

    @Nullable
    public static HologramSession current() {
        return current;
    }

    /** The view the world renderer draws, or null when no hologram is compiled or compiling. */
    @Nullable
    public static View activeView() {
        return active;
    }

    /** Starts a session for {@code key}, closing the previous one. */
    public static HologramSession begin(Key key) {
        closeCurrent();
        HologramSession session = new HologramSession(key);
        current = session;
        return session;
    }

    public static void closeCurrent() {
        HologramSession old = current;
        current = null;
        active = null;
        if (old != null) old.views.values().forEach(View::close);
    }

    public Key key() {
        return key;
    }

    @Nullable
    public View view(int layer) {
        return views.get(layer);
    }

    public View add(int layer, TrackedDummyWorld level, Collection<BlockPos> blocks, List<BlockPos> entities,
                    @Nullable IMultiController controller) {
        View view = new View(layer, level, blocks, entities, controller);
        views.put(layer, view);
        return view;
    }

    public static void activate(View view) {
        active = view;
    }

    public int viewCount() {
        return views.size();
    }
}
