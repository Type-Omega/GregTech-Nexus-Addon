package com.raishxn.gtna.preview;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.typeomega.lib.data.BlockPosHashing;
import com.typeomega.lib.preview.BlockInfoCache;
import com.typeomega.lib.preview.PreviewCells;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import org.jetbrains.annotations.Nullable;

/**
 * The block map of one EMI/JEI multiblock preview page: the shape's real blocks at their place in the preview level.
 * GTCEu's {@code PatternPreviewWidget.initializePattern} visits every cell of the bounding box and builds a position
 * and a {@link BlockInfo} for each before anything tests for air; for the 6.7 million cells of the Stellar Engine that
 * is 98% waste on the main thread. Here air is skipped first and the rest is stored in a position-hashed map.
 */
public final class PatternPreviewScan {

    /** @param controller the multiblock controller found in the shape, bound to the preview level, or null */
    public record Result(Object2ObjectLinkedOpenCustomHashMap<BlockPos, BlockInfo> blocks,
                         @Nullable IMultiController controller) {}

    private PatternPreviewScan() {}

    public static Result scan(BlockInfo[][][] shape, BlockPos origin, Level level) {
        Object2ObjectLinkedOpenCustomHashMap<BlockPos, BlockInfo> blocks = BlockPosHashing.newMap();
        IMultiController[] controller = { null };
        PreviewCells.forEachSolid(shape, -1, (x, y, z, state, info) -> {
            BlockPos pos = origin.offset(x, y, z);
            if (info.hasBlockEntity() && info.getBlockEntity(pos) instanceof IMachineBlockEntity holder &&
                    holder.getMetaMachine() instanceof IMultiController found) {
                holder.getSelf().setLevel(level);
                controller[0] = found;
            }
            blocks.put(pos, BlockInfoCache.of(state));
        });
        return new Result(blocks, controller[0]);
    }
}
