package com.raishxn.gtna.common.item.terminal.plan;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import appeng.api.stacks.AEItemKey;
import com.raishxn.gtna.network.GTNANetworkHandler;
import com.raishxn.gtna.network.packet.SNexusReport;
import com.typeomega.lib.ui.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The report of the last scan (D-439 P1.3), shown in the lib's {@link com.typeomega.lib.ui.report.ReportScreen}:
 * one group per kind of cell with its position, the materials and the ME crafting status. Opened from the link at
 * the end of the scan summary or from the terminal window.
 */
public final class NexusReport {

    public static final String COMMAND = "/gtna nexus_report";

    private NexusReport() {}

    public static Report build(NexusScan.Result result, Map<AEItemKey, NexusCrafting.Status> crafting) {
        List<Report.Line> missing = new ArrayList<>(), blocked = new ArrayList<>(), replace = new ArrayList<>(),
                clear = new ArrayList<>(), hatches = new ArrayList<>(), place = new ArrayList<>();
        for (NexusBuildPlan plan : result.plans()) {
            for (NexusBuildPlan.Cell cell : plan.cells) {
                switch (cell.kind) {
                    case PLACE, REPLACE -> {
                        Component what = cell.chosen != null ? cell.chosen.getDisplayName() :
                                cell.candidates.isEmpty() ? Component.literal("?") :
                                        cell.candidates.get(0).getHoverName();
                        Report.Line line = new Report.Line(at(what, cell.pos), cell.pos);
                        if (cell.chosen == null) missing.add(line);
                        else if (cell.hatch) hatches.add(line);
                        else if (cell.kind == NexusBuildPlan.Kind.REPLACE) replace.add(line);
                        else place.add(line);
                    }
                    case BLOCKED -> blocked.add(new Report.Line(
                            at(cell.existing.getBlock().getName(), cell.pos), cell.pos));
                    case CLEAR -> clear.add(new Report.Line(
                            at(cell.existing.getBlock().getName(), cell.pos), cell.pos));
                    default -> {}
                }
            }
        }
        List<Report.Line> materials = new ArrayList<>();
        result.need().forEach((key, need) -> {
            long[] stock = result.stock().getOrDefault(key, new long[2]);
            long have = result.creative() ? need : Math.min(need, stock[0] + stock[1]);
            NexusCrafting.Status status = crafting.get(key);
            MutableComponent text = Component.translatable("gtna.terminal.nexus.report.material",
                    key.getDisplayName(), need, stock[0], stock[1], need - have);
            if (status != null) text.append(" ").append(Component.translatable(
                    "gtna.terminal.nexus.craft." + status.name().toLowerCase(java.util.Locale.ROOT)));
            materials.add(new Report.Line(text, null));
        });
        return Report.builder(Component.translatable("gtna.terminal.nexus.report.title"))
                .group(Component.translatable("gtna.terminal.nexus.report.missing"), NexusScan.MISSING, missing)
                .group(Component.translatable("gtna.terminal.nexus.report.blocked"), NexusScan.BLOCKED, blocked)
                .group(Component.translatable("gtna.terminal.nexus.report.replace"), NexusScan.REPLACE, replace)
                .group(Component.translatable("gtna.terminal.nexus.report.clear"), NexusScan.CLEAR, clear)
                .group(Component.translatable("gtna.terminal.nexus.report.hatches"), NexusScan.HATCH, hatches)
                .group(Component.translatable("gtna.terminal.nexus.report.place"), NexusScan.PLACE, place)
                .group(Component.translatable("gtna.terminal.nexus.report.materials"), 0x2B2B2B, materials)
                .build();
    }

    private static Component at(Component what, BlockPos pos) {
        return Component.empty().append(what).append(" @ " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    /** Sends the report of the player's last scan; false when there is none. */
    public static boolean send(ServerPlayer player) {
        NexusLastScan.Entry last = NexusLastScan.get(player);
        if (last == null) return false;
        GTNANetworkHandler.sendToPlayer(new SNexusReport(build(last.result(), NexusCrafting.statuses(player))),
                player);
        return true;
    }

    /** The clickable "[report]" put after a scan summary. */
    public static Component link() {
        return Component.translatable("gtna.terminal.nexus.report.link").withStyle(style -> style
                .withColor(0x55FFFF).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, COMMAND))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("gtna.terminal.nexus.report.title"))));
    }
}
