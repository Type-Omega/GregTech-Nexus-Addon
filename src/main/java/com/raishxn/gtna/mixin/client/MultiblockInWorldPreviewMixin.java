package com.raishxn.gtna.mixin.client;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.raishxn.gtna.client.preview.HologramCompiler;
import com.raishxn.gtna.client.preview.HologramSession;
import com.raishxn.gtna.preview.HologramBuild;
import com.raishxn.gtna.preview.HologramLimits;
import com.raishxn.gtna.preview.HologramScan;
import com.raishxn.gtna.preview.PreviewTiming;
import com.typeomega.lib.preview.PreviewLayers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * EN: The Shift + right-click hologram of GTCEu froze the game on big structures: {@code showPreview} walked the whole
 * EN: bounding box (air included, up to 6.7 million cells) with several BlockPos per cell, compiled every block with
 * EN: all neighbours hidden (no internal face culling), never stopped its thread or freed its buffers, and compiled
 * EN: again on each click. This replaces {@code showPreview} (air skipped first, position-hashed map, one compiled
 * EN: view per layer that a repeated click reuses, a block cap with a layer fallback), makes {@code cleanPreview} stop
 * EN: the threads and free the buffers on the render thread, and removes the per-block render filter so internal
 * EN: faces are culled.
 * PT: O holograma de Shift + clique direito do GTCEu travava o jogo em estruturas grandes: o {@code showPreview}
 * PT: percorria a caixa inteira (ar incluso, até 6,7 milhões de células) com vários BlockPos por célula, compilava
 * PT: cada bloco com todos os vizinhos escondidos (sem descarte de faces internas), nunca parava a thread nem
 * PT: liberava os buffers e compilava de novo a cada clique. Isto troca o {@code showPreview} (ar pulado primeiro,
 * PT: mapa com hash de posição, uma visão compilada por camada que o clique repetido reaproveita, limite de blocos
 * PT: com camada como alternativa), faz o {@code cleanPreview} parar as threads e liberar os buffers na thread de
 * PT: render e remove o filtro de render por bloco para as faces internas serem descartadas.
 * ZH: GTCEu 的 Shift + 右键全息投影会让大型结构卡死游戏：{@code showPreview} 遍历整个包围盒（含空气，最多 670 万个
 * ZH: 单元）且每个单元创建多个 BlockPos，编译每个方块时隐藏所有相邻方块（不剔除内部面），从不停止线程或释放缓冲，
 * ZH: 每次点击都重新编译。这里替换 {@code showPreview}（先跳过空气、位置哈希映射、每层一个可复用的已编译视图、
 * ZH: 带分层回退的方块上限），让 {@code cleanPreview} 停止线程并在渲染线程释放缓冲，并移除逐方块渲染过滤器以剔除内部面。
 */
@Mixin(value = MultiblockInWorldPreviewRenderer.class, remap = false)
public abstract class MultiblockInWorldPreviewMixin {

    @Unique
    private static final Object GTNA$LOCK = new Object();

    @Shadow
    private static TrackedDummyWorld LEVEL;
    @Shadow
    private static Thread THREAD;
    @Shadow
    private static Set<BlockPos> BLOCK_ENTITIES;
    @Shadow
    @Final
    private static AtomicInteger LEFT_TICK;
    @Shadow
    @Final
    @SuppressWarnings("rawtypes")
    private static AtomicReference CACHE_STATE;
    @Shadow
    private static BlockPos LAST_POS;
    @Shadow
    private static int LAST_LAYER;

    @Shadow
    private static void renderBlocks(TrackedDummyWorld level, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                     net.minecraft.client.renderer.block.BlockRenderDispatcher dispatcher,
                                     net.minecraft.client.renderer.RenderType layer,
                                     com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer.VertexConsumerWrapper buffer,
                                     java.util.Collection<BlockPos> renderedBlocks) {
        throw new AssertionError();
    }

    @Inject(method = "showPreview", at = @At("HEAD"), cancellable = true)
    private static void gtna$showPreview(BlockPos pos, MultiblockControllerMachine controller, int duration,
                                         CallbackInfo ci) {
        ci.cancel();
        if (!controller.getDefinition().isRenderWorldPreview()) return;
        long start = PreviewTiming.now();
        Direction front = controller.getFrontFacing();
        Direction up = controller.getUpwardsFacing();
        MultiblockShapeInfo shapeInfo = controller.getDefinition().getMatchingShapes().get(0);
        long shapeMs = PreviewTiming.millisSince(start);
        BlockInfo[][][] shape = shapeInfo.getBlocks();
        HologramScan scan = HologramScan.of(shape);
        if (scan.controller() == null) return;

        int cap = HologramLimits.blockCap();
        boolean overCap = PreviewLayers.overCap(scan.solid(), cap);
        HologramSession.Key key = new HologramSession.Key(pos.immutable(), controller.getDefinition(), front, up,
                shapeInfo);
        HologramSession session = HologramSession.current();
        int layer;
        if (session != null && session.key().equals(key) && pos.equals(LAST_POS)) {
            layer = PreviewLayers.nextLayer(LAST_LAYER, scan.height(), overCap);
        } else {
            session = HologramSession.begin(key);
            layer = PreviewLayers.firstLayer(overCap, scan.controller().getY());
        }
        LAST_POS = pos;
        LAST_LAYER = layer;

        HologramSession.View view = session.view(layer);
        boolean reused = view != null;
        if (view == null) {
            TrackedDummyWorld level = new TrackedDummyWorld();
            HologramBuild.Result built = HologramBuild.build(shape, scan.controller(), front, up, pos, layer, level);
            level.addBlocks(built.blocks());
            if (built.controller() != null) {
                level.setInnerBlockEntity(built.controller().self().holder.getSelf());
            }
            view = session.add(layer, level, built.blocks().keySet(), built.blockEntities(), built.controller());
        }
        gtna$activate(view, duration);
        if (!reused) {
            HologramCompiler.start(view,
                    (level, poseStack, dispatcher, renderLayer, buffer, blocks) -> renderBlocks(level, poseStack,
                            dispatcher, renderLayer, buffer, blocks),
                    done -> gtna$compiled(done, duration));
        }
        if (overCap && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("gtna.hologram.too_large", scan.solid(), cap, layer + 1, scan.height()),
                    true);
        }
        PreviewTiming.log("hologram show: {}, solid={}, cap={}, layer={}, drawn={}, reusedView={}, shape={} ms, " +
                "total={} ms (compile runs off-thread)", controller.getDefinition().getId(), scan.solid(), cap,
                layer, view.blocks.size(), reused, shapeMs, PreviewTiming.millisSince(start));
    }

    /** Points the world renderer's statics at {@code view}. */
    @Unique
    private static void gtna$activate(HologramSession.View view, int duration) {
        synchronized (GTNA$LOCK) {
            HologramSession.activate(view);
            LEVEL = view.level;
            if (view.controller != null) view.controller.self().holder.getSelf().setLevel(view.level);
            BLOCK_ENTITIES = view.compiled ? view.blockEntities : null;
            gtna$setState(view.compiled ? "COMPILED" : "COMPILING");
            LEFT_TICK.set(view.compiled ? duration : -1);
        }
    }

    @Unique
    private static void gtna$compiled(HologramSession.View view, int duration) {
        synchronized (GTNA$LOCK) {
            if (HologramSession.activeView() != view || view.closed()) return;
            BLOCK_ENTITIES = view.blockEntities;
            gtna$setState("COMPILED");
            LEFT_TICK.set(duration);
        }
    }

    // LDLib-style private enum: written by constant name.
    @Unique
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void gtna$setState(String name) {
        if (CACHE_STATE.get() instanceof Enum<?> state) {
            CACHE_STATE.set(Enum.valueOf((Class) state.getDeclaringClass(), name));
        }
    }

    @Inject(method = "cleanPreview", at = @At("HEAD"))
    private static void gtna$cleanPreview(CallbackInfo ci) {
        synchronized (GTNA$LOCK) {
            HologramSession.closeCurrent();
            Thread running = THREAD;
            THREAD = null;
            if (running != null) running.interrupt();
        }
    }

    @Inject(method = "getBUFFERS", at = @At("HEAD"), cancellable = true)
    private static void gtna$activeBuffers(CallbackInfoReturnable<VertexBuffer[]> cir) {
        HologramSession.View view = HologramSession.activeView();
        if (view != null) cir.setReturnValue(view.buffers());
    }

    @Redirect(method = "renderBlocks",
              at = @At(value = "INVOKE",
                       target = "Lcom/lowdragmc/lowdraglib/utils/TrackedDummyWorld;setRenderFilter(Ljava/util/function/Predicate;)V"),
              require = 2)
    private static void gtna$keepNeighbours(TrackedDummyWorld level, Predicate<BlockPos> filter) {}
}
