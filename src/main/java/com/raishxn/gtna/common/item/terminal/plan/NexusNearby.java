package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The multiblock controllers near the player (GTIA-D-440), nearest first, for the terminal's Machines tab: pick one
 * and it becomes the terminal's target without walking to it. Only loaded chunks are read, from their block entity
 * maps, so the cost is the number of block entities nearby, not the volume.
 * <p>
 * The nearby list follows the Multi-Structure Manager of GregTech Terminals (BgameXL, MIT), rewritten for GTNA.
 */
public final class NexusNearby {

    public static final int RADIUS = 32;
    public static final int MAX = 48;

    public record Entry(BlockPos pos, IMultiController controller, double distance) {}

    private NexusNearby() {}

    public static List<Entry> find(Player player, int radius) {
        Level level = player.level();
        BlockPos at = player.blockPosition();
        List<Entry> out = new ArrayList<>();
        int c0x = SectionPos.blockToSectionCoord(at.getX() - radius),
                c1x = SectionPos.blockToSectionCoord(at.getX() + radius);
        int c0z = SectionPos.blockToSectionCoord(at.getZ() - radius),
                c1z = SectionPos.blockToSectionCoord(at.getZ() + radius);
        double r2 = (double) radius * radius;
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (var be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof IMachineBlockEntity holder) ||
                            !(holder.getMetaMachine() instanceof IMultiController controller)) {
                        continue;
                    }
                    double d = be.getBlockPos().distSqr(at);
                    if (d <= r2) out.add(new Entry(be.getBlockPos().immutable(), controller, Math.sqrt(d)));
                }
            }
        }
        out.sort(Comparator.comparingDouble(Entry::distance));
        return out.size() > MAX ? out.subList(0, MAX) : out;
    }
}
