package com.raishxn.gtna.preview;

import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;

import com.typeomega.lib.preview.PreviewCells;
import org.jetbrains.annotations.Nullable;

/**
 * One cheap pass over a preview shape for the in-world hologram: where the controller is (pattern coordinates), how
 * tall the shape is and how many cells are real blocks. Reads references only; nothing is allocated per cell.
 */
public record HologramScan(@Nullable BlockPos controller, int height, int solid) {

    public static HologramScan of(BlockInfo[][][] blocks) {
        BlockPos[] controller = { null };
        int[] solid = { 0 };
        PreviewCells.forEachSolid(blocks, -1, (x, y, z, state, info) -> {
            solid[0]++;
            if (state.getBlock() instanceof IMachineBlock machine &&
                    machine.getDefinition() instanceof MultiblockMachineDefinition) {
                controller[0] = new BlockPos(x, y, z);
            }
        });
        return new HologramScan(controller[0], PreviewCells.height(blocks), solid[0]);
    }
}
