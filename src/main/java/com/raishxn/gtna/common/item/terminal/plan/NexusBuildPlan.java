package com.raishxn.gtna.common.item.terminal.plan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.stacks.AEItemKey;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The scan of one structure (D-439 P0.1): every cell the pattern owns, classified once, and reused by the
 * build, the world highlight, the materials list and the report.
 */
public final class NexusBuildPlan {

    public enum Kind {
        /** The block already there fits the cell. */
        SATISFIED,
        /** Empty cell: a block will be placed. */
        PLACE,
        /** A wrong block (replace mode on): it is taken out and returned, then the right one placed. */
        REPLACE,
        /** A block that must become air (demolition mode). */
        CLEAR,
        /** A wrong block left alone: replace mode is off, or the block is a part or a controller. */
        BLOCKED,
        /** The controller itself. */
        CONTROLLER
    }

    /**
     * One cell. {@code candidates} are the items that may go there (after limits, no-hatch mode and the terminal's
     * picks); {@code hatch} marks cells whose candidates are multiblock parts; {@code chosen} is the item the
     * materials step assigned (null when nothing was available).
     */
    public static final class Cell {

        public final BlockPos pos;
        public final Kind kind;
        public final BlockState existing;
        public final List<ItemStack> candidates;
        public final boolean hatch;
        @Nullable
        public AEItemKey chosen;

        Cell(BlockPos pos, Kind kind, BlockState existing, List<ItemStack> candidates, boolean hatch) {
            this.pos = pos;
            this.kind = kind;
            this.existing = existing;
            this.candidates = candidates;
            this.hatch = hatch;
        }

        public boolean needsItem() {
            return kind == Kind.PLACE || kind == Kind.REPLACE;
        }
    }

    public final List<Cell> cells = new ArrayList<>();
    /** What each owned position will hold once built (planned block, or null for air): orientation reads it. */
    final Map<BlockPos, Boolean> occupied = new HashMap<>();
    @Nullable
    public final BlockPos controller;

    NexusBuildPlan(@Nullable BlockPos controller) {
        this.controller = controller;
    }

    public int count(Kind kind) {
        int n = 0;
        for (Cell cell : cells) if (cell.kind == kind) n++;
        return n;
    }

    /** Cells the build has to touch in the world. */
    public int work() {
        int n = 0;
        for (Cell cell : cells) if (cell.needsItem() || cell.kind == Kind.CLEAR) n++;
        return n;
    }

    /** True when the neighbour {@code pos} is outside the structure or will be air. */
    boolean isOpen(BlockPos pos) {
        Boolean filled = occupied.get(pos);
        return filled == null || !filled;
    }
}
