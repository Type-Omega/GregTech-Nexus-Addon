package com.raishxn.gtna.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import com.typeomega.lib.ui.report.Report;

import java.util.function.Supplier;

/** Opens a Nexus Terminal report on the client (D-439 P1.3). The lib caps the lines, so the packet is bounded. */
public final class SNexusReport {

    private final Report report;

    public SNexusReport(Report report) {
        this.report = report;
    }

    public void encode(FriendlyByteBuf buf) {
        report.write(buf);
    }

    public static SNexusReport decode(FriendlyByteBuf buf) {
        return new SNexusReport(Report.read(buf));
    }

    public static void handle(SNexusReport msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.raishxn.gtna.client.renderer.terminal.NexusReportClient.open(msg.report)));
        ctx.get().setPacketHandled(true);
    }
}
