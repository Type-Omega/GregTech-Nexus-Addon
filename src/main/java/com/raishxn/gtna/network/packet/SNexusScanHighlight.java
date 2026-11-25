package com.raishxn.gtna.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.raishxn.gtna.client.renderer.terminal.NexusScanHighlights;

import java.util.function.Supplier;

/** Coloured boxes of a Nexus Terminal scan (D-439 P1.1): packed positions, ARGB colours and a lifetime. */
public final class SNexusScanHighlight {

    private final long[] positions;
    private final int[] colors;
    private final int ticks;

    public SNexusScanHighlight(long[] positions, int[] colors, int ticks) {
        this.positions = positions;
        this.colors = colors;
        this.ticks = ticks;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(ticks);
        buf.writeVarInt(positions.length);
        for (int i = 0; i < positions.length; i++) {
            buf.writeLong(positions[i]);
            buf.writeInt(colors[i]);
        }
    }

    public static SNexusScanHighlight decode(FriendlyByteBuf buf) {
        int ticks = buf.readVarInt();
        int n = Math.min(buf.readVarInt(), 1 << 16);
        long[] positions = new long[n];
        int[] colors = new int[n];
        for (int i = 0; i < n; i++) {
            positions[i] = buf.readLong();
            colors[i] = buf.readInt();
        }
        return new SNexusScanHighlight(positions, colors, ticks);
    }

    public static void handle(SNexusScanHighlight msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> NexusScanHighlights.show(msg.positions, msg.colors, msg.ticks));
        ctx.get().setPacketHandled(true);
    }
}
