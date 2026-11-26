package com.raishxn.gtna.common.item.terminal.ui;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.item.terminal.plan.NexusDismantle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The text of the Dismantle warning on the Structure step (D-447): a red header, what the dismantle would take and
 * what stays. The preview walks the pattern, so it is worked out at most once a second per player; the screen asks
 * for each of its lines every tick.
 */
public final class NexusDismantleView {

    /** Lines the panel has room for. */
    public static final int LINES = 6;
    static final int REFRESH_TICKS = 20;
    /** Block kinds shown; when there are more, the last row becomes "+N other kinds". */
    static final int KIND_ROWS = 3;

    private static final Map<UUID, Cached> CACHE = new HashMap<>();

    private NexusDismantleView() {}

    private record Cached(long tick, BlockPos controller, List<String> lines) {}

    /** A block kind and its count, as shown. */
    record Row(String name, int count) {}

    /** The rows that fit, and what the rest adds up to. */
    record Top(List<Row> shown, int otherKinds, int otherBlocks) {}

    /**
     * The first rows of a list sorted by count. When there are more than {@code max}, the first {@code max - 1}
     * are kept and the rest is summed, so the "other kinds" line takes the last row's place.
     */
    static Top top(List<Row> sorted, int max) {
        if (sorted.size() <= max) return new Top(List.copyOf(sorted), 0, 0);
        List<Row> rest = sorted.subList(max - 1, sorted.size());
        int blocks = 0;
        for (Row row : rest) blocks += row.count();
        return new Top(List.copyOf(sorted.subList(0, max - 1)), rest.size(), blocks);
    }

    /** One line of the panel (0 to {@link #LINES} - 1), or an empty string. */
    public static String line(Player player, ItemStack terminal, int index) {
        List<String> lines = lines(player, terminal);
        return index < lines.size() ? lines.get(index) : "";
    }

    static List<String> lines(Player player, ItemStack terminal) {
        long now = player.level().getGameTime();
        IMultiController controller = NexusTerminalInfo.target(player);
        BlockPos pos = controller == null ? null : controller.self().getPos();
        Cached cached = CACHE.get(player.getUUID());
        if (cached != null && now - cached.tick() < REFRESH_TICKS &&
                java.util.Objects.equals(pos, cached.controller())) {
            return cached.lines();
        }
        List<String> lines = build(player, terminal, controller);
        CACHE.put(player.getUUID(), new Cached(now, pos, lines));
        return lines;
    }

    /** Drops the cached lines of a player (the mode went off). */
    public static void forget(Player player) {
        CACHE.remove(player.getUUID());
    }

    private static List<String> build(Player player, ItemStack terminal, @Nullable IMultiController controller) {
        List<String> out = new ArrayList<>();
        out.add(tr("title"));
        NexusDismantle.Preview preview = controller == null ? null :
                NexusDismantle.preview(player, controller, terminal);
        if (preview == null) {
            out.add(tr("no_target"));
            return out;
        }
        out.add(preview.remove() == 0 ? tr("nothing") : tr("removes", preview.remove()));
        out.add(tr("stays", preview.parts()) + (preview.blocked() > 0 ? tr("blocked", preview.blocked()) : ""));
        List<Row> rows = new ArrayList<>();
        for (NexusDismantle.Entry entry : preview.byBlock()) {
            rows.add(new Row(entry.block().getName().getString(), entry.count()));
        }
        Top top = top(rows, KIND_ROWS);
        for (Row row : top.shown()) out.add("§7  " + row.name() + " §fx" + row.count());
        if (top.otherKinds() > 0) out.add("§7  " + tr("others", top.otherKinds(), top.otherBlocks()));
        return out;
    }

    private static String tr(String key, Object... args) {
        return Component.translatable("gtna.terminal.nexus.ui.dismantle." + key, args).getString();
    }
}
