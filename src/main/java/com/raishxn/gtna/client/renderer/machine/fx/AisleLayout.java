package com.raishxn.gtna.client.renderer.machine.fx;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import net.minecraft.core.BlockPos;

import com.raishxn.gtna.common.data.multiblock.TextAisles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the symbols of a text-aisle structure ({@link TextAisles}) sit relative to its controller, for renders that
 * light up parts of the structure. Offsets are (up, left, forward), as {@link RelativeDirection#offsetPos} takes.
 */
public final class AisleLayout {

    private static final Map<String, AisleLayout> CACHE = new HashMap<>();

    private final Map<Character, List<int[]>> offsets = new HashMap<>();

    private AisleLayout(String resource, boolean aislesGoUp, int repetitions, char controller) {
        List<String[]> expanded = new ArrayList<>();
        for (var aisle : TextAisles.load(resource)) {
            int count = aisle.repeats() ? repetitions : 1;
            for (int i = 0; i < count; i++) expanded.add(aisle.rows());
        }
        int[] q = null;
        Map<Character, List<int[]>> raw = new HashMap<>();
        for (int a = 0; a < expanded.size(); a++)
            for (int r = 0; r < expanded.get(a).length; r++)
                for (int c = 0; c < expanded.get(a)[r].length(); c++) {
                    char ch = expanded.get(a)[r].charAt(c);
                    if (ch == ' ') continue;
                    // start(RIGHT, FRONT, UP) when aisles go up; the default start(LEFT, UP, FRONT) otherwise.
                    int[] p = aislesGoUp ? new int[] { a, -c, r } : new int[] { r, c, a };
                    if (ch == controller) q = p;
                    raw.computeIfAbsent(ch, k -> new ArrayList<>()).add(p);
                }
        if (q == null) throw new IllegalStateException(resource + " has no controller " + controller);
        for (var e : raw.entrySet()) {
            List<int[]> list = new ArrayList<>();
            for (int[] p : e.getValue()) list.add(new int[] { p[0] - q[0], p[1] - q[1], p[2] - q[2] });
            offsets.put(e.getKey(), list);
        }
    }

    public static AisleLayout of(String resource, boolean aislesGoUp, int repetitions, char controller) {
        return CACHE.computeIfAbsent(resource + "/" + repetitions,
                k -> new AisleLayout(resource, aislesGoUp, repetitions, controller));
    }

    public List<int[]> offsets(char symbol) {
        return offsets.getOrDefault(symbol, List.of());
    }

    /** World position of an offset for this machine's orientation. */
    public static BlockPos at(com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine m, int[] off) {
        return RelativeDirection.offsetPos(m.getPos(), m.getFrontFacing(), m.getUpwardsFacing(), m.isFlipped(),
                off[0], off[1], off[2]);
    }

    /** Mean position of a symbol, in blocks relative to the controller block (world axes). */
    public org.joml.Vector3f centre(com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine m,
                                    char symbol) {
        var out = new org.joml.Vector3f();
        var list = offsets(symbol);
        for (int[] off : list) {
            BlockPos p = at(m, off);
            BlockPos c = m.getPos();
            out.add(p.getX() - c.getX(), p.getY() - c.getY(), p.getZ() - c.getZ());
        }
        return list.isEmpty() ? out : out.div(list.size());
    }
}
