package com.raishxn.gtna.client.renderer.terminal;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.typeomega.lib.ui.report.Report;
import com.typeomega.lib.ui.report.ReportScreen;

import java.util.ArrayList;
import java.util.List;

/** Opens the report screen; its "show boxes" button lights the listed positions in the group colours. */
@OnlyIn(Dist.CLIENT)
public final class NexusReportClient {

    private NexusReportClient() {}

    public static void open(Report report) {
        Minecraft.getInstance().setScreen(new ReportScreen(report, NexusReportClient::show));
    }

    static void show(List<Report.Group> groups) {
        List<Long> positions = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (Report.Group group : groups) {
            for (Report.Line line : group.lines()) {
                if (line.pos() == null || positions.size() >= Report.MAX_LINES) continue;
                positions.add(line.pos().asLong());
                colors.add(group.colour());
            }
        }
        NexusScanHighlights.show(positions.stream().mapToLong(Long::longValue).toArray(),
                colors.stream().mapToInt(Integer::intValue).toArray(), 30 * 20);
    }
}
