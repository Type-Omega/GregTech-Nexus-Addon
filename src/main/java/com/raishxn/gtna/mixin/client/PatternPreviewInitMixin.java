package com.raishxn.gtna.mixin.client;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.ItemStackKey;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.core.BlockPos;

import com.raishxn.gtna.preview.PatternParts;
import com.raishxn.gtna.preview.PatternPreviewScan;
import com.raishxn.gtna.preview.PreviewTiming;
import com.typeomega.lib.data.BlockPosHashing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * EN: GTCEu's {@code initializePattern} walks every cell of the bounding box (6.7 million for the Stellar Engine,
 * EN: 98% air) and builds a BlockPos and a BlockInfo for each before the old air filter could drop it. This replaces
 * EN: the method: air is skipped first, the block map uses position hashing, and the parts list is computed from the
 * EN: real blocks only. Same result as before minus the air entries.
 * PT: O {@code initializePattern} do GTCEu percorre toda célula da caixa (6,7 milhões no Stellar Engine, 98% ar) e
 * PT: cria BlockPos e BlockInfo para cada uma antes de o filtro de ar antigo poder descartá-la. Isto substitui o
 * PT: método: o ar é pulado primeiro, o mapa usa hash de posição e a lista de peças vem só dos blocos reais.
 * ZH: GTCEu 的 {@code initializePattern} 会遍历包围盒的每个单元（恒星引擎有 670 万个，98% 是空气），并在旧的空气过滤
 * ZH: 生效之前为每个单元创建 BlockPos 和 BlockInfo。这里替换该方法：先跳过空气，映射使用位置哈希，零件列表只由真实方块得出。
 */
@Mixin(value = PatternPreviewWidget.class, remap = false)
public abstract class PatternPreviewInitMixin {

    @Shadow
    private static TrackedDummyWorld LEVEL;

    @Shadow
    private void loadControllerFormed(Collection<BlockPos> positions, IMultiController controllerBase) {
        throw new AssertionError();
    }

    @Inject(method = "initializePattern", at = @At("HEAD"), cancellable = true)
    private void gtna$initializePattern(MultiblockShapeInfo shapeInfo, HashSet<ItemStackKey> blockDrops,
                                        CallbackInfoReturnable<PatternPreviewWidget.MBPattern> cir) {
        long start = PreviewTiming.now();
        BlockPos region = PatternPreviewWidget.locateNextRegion();
        BlockInfo[][][] shape = shapeInfo.getBlocks();
        PatternPreviewScan.Result scan = PatternPreviewScan.scan(shape, region, LEVEL);
        IMultiController controller = scan.controller();
        Map<BlockPos, BlockInfo> blocks = scan.blocks();

        LEVEL.addBlocks(blocks);
        if (controller != null) LEVEL.setInnerBlockEntity(controller.self().holder.getSelf());
        PatternParts.Result parts = PatternParts.gather(blocks, LEVEL);
        blockDrops.addAll(parts.drops());

        Map<BlockPos, TraceabilityPredicate> predicates = BlockPosHashing.newMap();
        if (controller != null) {
            loadControllerFormed(new HashSet<>(), controller);
            Map<BlockPos, TraceabilityPredicate> saved = controller.getMultiblockState().getMatchContext()
                    .get("predicates");
            if (saved != null) predicates = saved;
        }
        PatternPreviewWidget.MBPattern pattern = controller == null ? null :
                new PatternPreviewWidget.MBPattern(blocks, parts.parts(), predicates, controller);
        PreviewTiming.log("EMI preview init {}: cells={}, blocks={}, {} ms",
                controller == null ? "(no controller)" : controller.self().getDefinition().getId(),
                countCells(shape), blocks.size(), PreviewTiming.millisSince(start));
        cir.setReturnValue(pattern);
    }

    @org.spongepowered.asm.mixin.Unique
    private static long countCells(BlockInfo[][][] shape) {
        long cells = 0;
        for (BlockInfo[][] aisle : shape) for (BlockInfo[] column : aisle) cells += column.length;
        return cells;
    }
}
