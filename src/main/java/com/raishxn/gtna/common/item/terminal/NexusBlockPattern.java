package com.raishxn.gtna.common.item.terminal;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.raishxn.gtna.api.machine.multiblock.GTNASubPatterns;
import com.raishxn.gtna.api.machine.multiblock.ISubPatternMachine;
import com.raishxn.gtna.common.item.terminal.plan.*;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory.AutoBuildSetting;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * A GTCEu {@link BlockPattern} as the Nexus Structure Terminal reads it, and the entry of a build: scan into
 * {@link NexusBuildPlan}s ({@link NexusStructurePlanner}), reserve the material ({@link NexusMaterials}) and place
 * ({@link NexusBuildExecutor}, spread over ticks by {@link NexusBuildJobs} when big).
 * <p>
 * The pattern-to-world transform ({@link #toWorldOffset}) follows GTCEu Modern's own {@code BlockPattern#autoBuild}
 * (LGPL-3.0), so a structure built here is the structure GTCEu checks.
 */
public final class NexusBlockPattern {

    private final TraceabilityPredicate[][][] cells; // [aisle][row][column]
    private final RelativeDirection[] axes;
    private final int[][] aisleRange;
    private final int[] centerOffset; // x, y, z, minZ, maxZ

    private NexusBlockPattern(TraceabilityPredicate[][][] cells, RelativeDirection[] axes, int[][] aisleRange,
                              int[] centerOffset) {
        this.cells = cells;
        this.axes = axes;
        this.aisleRange = aisleRange;
        this.centerOffset = centerOffset;
    }

    public TraceabilityPredicate[][][] cells() {
        return cells;
    }

    /** Controller position in the pattern: column, row, aisle, then GTCEu's min and max Z. */
    public int[] centerOffset() {
        return centerOffset;
    }

    /** Reads a GTCEu pattern; returns null if its layout cannot be read. */
    @Nullable
    public static NexusBlockPattern fromBlockPattern(@Nullable BlockPattern pattern) {
        if (pattern == null) return null;
        try {
            return new NexusBlockPattern(read(pattern, "blockMatches"), pattern.structureDir,
                    pattern.aisleRepetitions, read(pattern, "centerOffset"));
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T read(BlockPattern pattern, String name) throws ReflectiveOperationException {
        Field field = BlockPattern.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(pattern);
    }

    /** Scans the structure and its first N modules (sub-patterns) into plans, without touching the world. */
    public List<NexusBuildPlan> plan(MultiblockState state, AutoBuildSetting setting, ItemStack terminal) {
        NexusStructurePlanner planner = new NexusStructurePlanner(state, setting, terminal);
        List<NexusBuildPlan> plans = new ArrayList<>();
        plans.add(planner.scan(this));
        int wanted = setting.getModuleBuild();
        IMultiController controller = state.getController();
        if (wanted <= 0 || controller == null) return plans;
        List<BlockPattern> modules = new ArrayList<>(GTNASubPatterns.get(controller.self().getDefinition()));
        if (controller.self() instanceof ISubPatternMachine host) modules.addAll(host.gtna$getSubPatterns());
        for (BlockPattern module : modules.subList(0, Math.min(wanted, modules.size()))) {
            NexusBlockPattern sub = fromBlockPattern(module);
            if (sub != null) plans.add(planner.scan(sub));
        }
        return plans;
    }

    /**
     * Plans, reserves the material in one batch and builds: at once when small, as a per-tick job otherwise.
     * Returns false when the build was refused (a cell is outside the world's height).
     */
    public boolean autoBuild(Player player, MultiblockState state, AutoBuildSetting setting, ItemStack terminal) {
        List<NexusBuildPlan> plans = plan(state, setting, terminal);
        Level level = player.level();
        for (NexusBuildPlan plan : plans) {
            for (NexusBuildPlan.Cell cell : plan.cells) {
                if (level.isOutsideBuildHeight(cell.pos)) {
                    player.displayClientMessage(Component.translatable("gtna.terminal.nexus.too_high"), true);
                    return false;
                }
            }
        }
        NexusAE2Link.Session me = setting.isUseAE() && !player.isCreative() ? NexusAE2Link.open(terminal, player) :
                null;
        NexusMaterials materials = new NexusMaterials(player, me);
        materials.survey(plans);
        materials.assign(plans);
        // EN: what is missing and has a pattern is requested from the ME on its own (GTIA-D-439 T9); the build
        // EN: goes on with what exists, and a second build fills the rest once the crafts are done.
        // PT: o que falta e tem padrão é pedido sozinho ao ME (GTIA-D-439 T9); a construção segue com o que existe,
        // PT: e uma segunda construção completa o resto quando os crafts terminarem.
        // ZH: 缺少且有样板的物品会自动向 ME 下单（GTIA-D-439 T9）；建造先用现有材料继续，合成完成后再建一次补齐。
        if (me != null) {
            Map<appeng.api.stacks.AEItemKey, Long> missing = NexusMaterials.missing(plans);
            if (!missing.isEmpty()) NexusCrafting.request(player, me, missing);
        }
        materials.reserve(plans);
        IMultiController controller = state.getController();
        NexusBuildExecutor executor = new NexusBuildExecutor(plans, player, materials,
                controller == null ? null : controller.self().getFrontFacing());
        int work = 0;
        for (NexusBuildPlan plan : plans) work += plan.work();
        if (NexusBuildJobs.start(player, state, executor, materials, work)) {
            player.displayClientMessage(Component.translatable("gtna.terminal.nexus.job.started", work), true);
        }
        return true;
    }

    /**
     * How far below the controller the lowest block of the structure goes for this facing and these settings
     * (0 or negative): a controller placed by a command can be lifted so the build clears the ground.
     */
    public int lowestOffset(Direction front, Direction up, boolean mirrored, AutoBuildSetting setting) {
        int[] repeat = repetitions(setting);
        int lowest = 0, z = 0;
        for (int aisle = 0; aisle < centerOffset[2]; aisle++) z -= repeat[aisle];
        for (int aisle = 0; aisle < cells.length; aisle++) {
            for (int copy = 0; copy < repeat[aisle]; copy++, z++) {
                for (int row = 0; row < cells[aisle].length; row++) {
                    for (int col = 0; col < cells[aisle][row].length; col++) {
                        if (cells[aisle][row][col].common.contains(SimplePredicate.ANY)) continue;
                        lowest = Math.min(lowest, toWorldOffset(col - centerOffset[0], row - centerOffset[1], z,
                                front, up, mirrored).getY());
                    }
                }
            }
        }
        return lowest;
    }

    /** How many times each aisle is laid: the setting, clamped to the aisle's allowed range. */
    public int[] repetitions(AutoBuildSetting setting) {
        int[] counts = new int[cells.length];
        for (int i = 0; i < cells.length; i++) {
            int min = aisleRange[i][0], max = aisleRange[i][1];
            counts[i] = min == max ? min : Math.max(min, Math.min(max, setting.getRepetitions()));
        }
        return counts;
    }

    /**
     * Pattern coordinates (column, row, aisle) relative to the controller, turned into a world offset for the
     * controller's front/up facing and flip. This is the transform GTCEu's BlockPattern uses (LGPL-3.0), kept
     * identical so built structures always pass GTCEu's own check.
     */
    public BlockPos toWorldOffset(int x, int y, int z, Direction facing, Direction upwardsFacing, boolean flipped) {
        int[] local = { x, y, z };
        int[] world = new int[3];
        boolean vertical = facing == Direction.UP || facing == Direction.DOWN;
        Direction reference = vertical ? (facing == Direction.DOWN ? upwardsFacing : upwardsFacing.getOpposite()) :
                facing;
        for (int i = 0; i < 3; i++) {
            switch (axes[i].getActualDirection(reference)) {
                case UP -> world[1] = local[i];
                case DOWN -> world[1] = -local[i];
                case WEST -> world[0] = -local[i];
                case EAST -> world[0] = local[i];
                case NORTH -> world[2] = -local[i];
                case SOUTH -> world[2] = local[i];
            }
        }
        if (vertical) {
            int sx = upwardsFacing.getStepX(), sz = upwardsFacing.getStepZ();
            if (sx == 0) rotate(world, 2, sz > 0);
            else rotate(world, 0, sx > 0);
            if (flipped) world[upwardsFacing.getAxis() == Direction.Axis.Z ? 0 : 2] *= -1;
        } else {
            if (upwardsFacing.getAxis() == Direction.Axis.X) {
                Direction side = upwardsFacing == Direction.EAST ? facing.getClockWise() :
                        facing.getCounterClockWise();
                if (side.getStepX() == 0) rotate(world, 2, side.getStepZ() < 0);
                else rotate(world, 0, side.getStepX() < 0);
            } else if (upwardsFacing == Direction.SOUTH) {
                world[1] = -world[1];
                world[facing.getStepX() == 0 ? 0 : 2] *= -1;
            }
            if (flipped) {
                if (upwardsFacing.getAxis() == Direction.Axis.Z) {
                    world[facing.getAxis() == Direction.Axis.Z ? 0 : 2] *= -1;
                } else {
                    world[1] = -world[1];
                }
            }
        }
        return new BlockPos(world[0], world[1], world[2]);
    }

    /**
     * Swaps the vertical axis with horizontal axis {@code h}: positive = (h, y) -> (y, -h); otherwise (-y, h).
     */
    private static void rotate(int[] v, int h, boolean positive) {
        int horizontal = v[h];
        v[h] = positive ? v[1] : -v[1];
        v[1] = positive ? -horizontal : horizontal;
    }
}
