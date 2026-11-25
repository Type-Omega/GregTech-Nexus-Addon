package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import appeng.api.stacks.AEItemKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Puts a plan into the world, one cell at a time so a job can spread it over ticks (GTIA-D-439 P0.4, P0.5). A plain
 * block gets its facing before it is placed, so it costs one {@code setBlock}; machines (hatches) still go through
 * {@link BlockItem#place} so their block entity is set up, and are turned once at the end. Placement sound plays once
 * per build, not per block.
 */
public final class NexusBuildExecutor {

    private static final Direction[] FACING_ORDER = { Direction.SOUTH, Direction.NORTH, Direction.WEST,
            Direction.EAST, Direction.UP, Direction.DOWN };

    public final List<NexusBuildPlan> plans;
    private final Player player;
    private final Level level;
    private final NexusMaterials materials;
    @Nullable
    private final Direction front;
    private int planIndex, cellIndex;
    private final List<BlockPos> machinesToTurn = new ArrayList<>();
    private final List<NexusBuildPlan> machinePlans = new ArrayList<>();
    public int placed, replaced, cleared, skipped;

    public NexusBuildExecutor(List<NexusBuildPlan> plans, Player player, NexusMaterials materials,
                              @Nullable Direction front) {
        this.plans = plans;
        this.player = player;
        this.level = player.level();
        this.materials = materials;
        this.front = front;
    }

    public boolean done() {
        return planIndex >= plans.size();
    }

    /** Cells walked so far, of {@link #total()}. */
    public int progress() {
        int n = cellIndex;
        for (int i = 0; i < planIndex && i < plans.size(); i++) n += plans.get(i).cells.size();
        return n;
    }

    public int total() {
        int n = 0;
        for (NexusBuildPlan plan : plans) n += plan.cells.size();
        return n;
    }

    /**
     * Works through cells until {@code maxWork} world changes were made or {@code deadline} (nanoTime) passed.
     * Returns true when the whole plan is done.
     */
    public boolean step(int maxWork, long deadline) {
        int work = 0;
        while (!done()) {
            NexusBuildPlan plan = plans.get(planIndex);
            if (cellIndex >= plan.cells.size()) {
                planIndex++;
                cellIndex = 0;
                continue;
            }
            NexusBuildPlan.Cell cell = plan.cells.get(cellIndex);
            cellIndex++;
            if (apply(plan, cell)) work++;
            if (work >= maxWork || (work & 63) == 0 && System.nanoTime() > deadline) return done();
        }
        return true;
    }

    /** Runs the turn pass for machines and the single placement sound. */
    public void finish() {
        for (int i = 0; i < machinesToTurn.size(); i++) {
            BlockPos pos = machinesToTurn.get(i);
            NexusBuildPlan plan = machinePlans.get(i);
            MetaMachine machine = MetaMachine.getMachine(level, pos);
            if (machine == null || machine instanceof IMultiController) continue;
            BlockState now = level.getBlockState(pos);
            BlockState turned = orient(plan, pos, now, side -> machine.isFacingValid(side));
            if (turned != now) level.setBlock(pos, turned, 3);
        }
        if (placed + replaced > 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1f, 1f);
        }
    }

    /** Returns true when the cell changed the world. */
    private boolean apply(NexusBuildPlan plan, NexusBuildPlan.Cell cell) {
        switch (cell.kind) {
            case CLEAR -> {
                if (clear(cell.pos)) {
                    cleared++;
                    return true;
                }
                return false;
            }
            case PLACE, REPLACE -> {
                if (cell.chosen == null) {
                    skipped++;
                    return false;
                }
                return place(plan, cell);
            }
            case SATISFIED -> {
                // Parts already there are turned towards an open side, as the GTCEu auto-build does.
                if (level.getBlockState(cell.pos).getBlock() instanceof MetaMachineBlock) {
                    machinesToTurn.add(cell.pos);
                    machinePlans.add(plan);
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean clear(BlockPos pos) {
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.getDestroySpeed(level, pos) < 0) return false;
        if (!level.mayInteract(player, pos) || !player.mayBuild()) return false;
        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, existing, player))) return false;
        return level.destroyBlock(pos, !player.isCreative(), player);
    }

    private boolean place(NexusBuildPlan plan, NexusBuildPlan.Cell cell) {
        BlockPos pos = cell.pos;
        AEItemKey key = cell.chosen;
        if (!level.mayInteract(player, pos) || !player.mayBuild()) {
            skipped++;
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        boolean replacing = cell.kind == NexusBuildPlan.Kind.REPLACE;
        if (replacing != !existing.isAir()) { // the world changed since the scan
            skipped++;
            return false;
        }
        AEItemKey old = replacing ? AEItemKey.of(existing.getBlock().asItem()) : null;
        if (old != null && !materials.canStore(old, 1)) {
            skipped++; // nowhere to put the block being replaced
            return false;
        }
        if (!materials.take(key)) {
            skipped++;
            return false;
        }
        if (replacing) level.removeBlock(pos, false);
        ItemStack stack = key.toStack();
        boolean ok;
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof MetaMachineBlock) {
            BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack,
                    BlockHitResult.miss(player.getEyePosition(0), Direction.UP, pos));
            ok = item.place(context).consumesAction();
            if (ok) {
                machinesToTurn.add(pos);
                machinePlans.add(plan);
            }
        } else if (stack.getItem() instanceof BlockItem item) {
            BlockState target = orient(plan, pos, item.getBlock().defaultBlockState(), side -> true);
            ok = target.canSurvive(level, pos) && level.setBlock(pos, target, 3);
            if (ok) item.getBlock().setPlacedBy(level, pos, target, player, stack);
        } else {
            ok = false;
        }
        if (!ok) {
            materials.putBack(key);
            // EN: put the old block back so a failed swap leaves the structure as it was (GTIA-D-439 T5).
            // PT: recoloca o bloco antigo para que uma troca que falhou deixe a estrutura como estava (GTIA-D-439
            // PT: T5).
            // ZH: 放回旧方块，使失败的替换让结构保持原样（GTIA-D-439 T5）。
            if (replacing) level.setBlock(pos, existing, 3);
            skipped++;
            return false;
        }
        if (old != null) {
            materials.store(old, 1);
            replaced++;
        } else {
            placed++;
        }
        return true;
    }

    /** {@code blockState} turned towards the first side of the structure that stays open, as GTCEu does. */
    private BlockState orient(NexusBuildPlan plan, BlockPos pos, BlockState blockState, Predicate<Direction> valid) {
        DirectionProperty property;
        boolean horizontal;
        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            property = BlockStateProperties.FACING;
            horizontal = false;
        } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            property = BlockStateProperties.HORIZONTAL_FACING;
            horizontal = true;
        } else {
            return blockState;
        }
        List<Direction> order = new ArrayList<>(7);
        if (front != null && !(horizontal && front.getAxis() == Direction.Axis.Y)) order.add(front);
        for (Direction side : FACING_ORDER) {
            if (!(horizontal && side.getAxis() == Direction.Axis.Y)) order.add(side);
        }
        Direction chosen = Direction.NORTH;
        for (Direction side : order) {
            if (plan.isOpen(pos.relative(side)) && valid.test(side) && property.getPossibleValues().contains(side)) {
                chosen = side;
                break;
            }
        }
        if (!property.getPossibleValues().contains(chosen)) return blockState;
        return blockState.getValue(property) == chosen ? blockState : blockState.setValue(property, chosen);
    }
}
