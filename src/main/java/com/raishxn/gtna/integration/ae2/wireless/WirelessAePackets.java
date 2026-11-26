// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

/** The client-to-server messages of the wireless ME network: picker, saved network and core name. */
public final class WirelessAePackets {

    private WirelessAePackets() {}

    public static void register(SimpleChannel channel, java.util.function.IntSupplier ids) {
        channel.messageBuilder(ClearBindingToolPacket.class, ids.getAsInt(), NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new ClearBindingToolPacket())
                .consumerMainThread(ClearBindingToolPacket::handle)
                .add();
        channel.messageBuilder(OpenBindingPickerPacket.class, ids.getAsInt(), NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new OpenBindingPickerPacket())
                .consumerMainThread(OpenBindingPickerPacket::handle)
                .add();
        channel.messageBuilder(PickBindingToolNetworkPacket.class, ids.getAsInt(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(PickBindingToolNetworkPacket::encode)
                .decoder(PickBindingToolNetworkPacket::decode)
                .consumerMainThread(PickBindingToolNetworkPacket::handle)
                .add();
        channel.messageBuilder(RenameNetworkPacket.class, ids.getAsInt(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RenameNetworkPacket::encode)
                .decoder(RenameNetworkPacket::decode)
                .consumerMainThread(RenameNetworkPacket::handle)
                .add();
    }

    /** Left-click in the air with the tool: forget the saved network. */
    public record ClearBindingToolPacket() {

        private void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player != null && !player.isSpectator()) {
                WirelessNetworkBindingToolItem.clearSavedNetwork(player);
            }
        }
    }

    /** Sent by the picker key, so the tool can be used from a Curios slot or anywhere in the inventory. */
    public record OpenBindingPickerPacket() {

        private void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player != null && player.mayBuild()) {
                WirelessNetworkBindingToolItem.openPickerFor(player);
            }
        }
    }

    public record PickBindingToolNetworkPacket(int containerId, UUID frequency) {

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeUUID(this.frequency);
        }

        private static PickBindingToolNetworkPacket decode(FriendlyByteBuf buffer) {
            return new PickBindingToolNetworkPacket(buffer.readVarInt(), buffer.readUUID());
        }

        private void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player != null && player.containerMenu.containerId == this.containerId) {
                WirelessNetworkBindingToolItem.applyPickerChoice(player, this.frequency);
            }
        }
    }

    public record RenameNetworkPacket(BlockPos corePos, String name) {

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(this.corePos);
            buffer.writeUtf(this.name);
        }

        private static RenameNetworkPacket decode(FriendlyByteBuf buffer) {
            return new RenameNetworkPacket(buffer.readBlockPos(), buffer.readUtf(32));
        }

        private void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player == null || !player.canReach(this.corePos, 0)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            if (!(level.getBlockEntity(this.corePos) instanceof WirelessNetworkCoreBlockEntity core)) {
                return;
            }
            UUID frequency = core.getFrequency();
            if (!WirelessAccess.canAccessNetwork(player, frequency)) {
                return;
            }
            WirelessAeSavedData data = WirelessAeSavedData.get(level.getServer());
            data.setCore(frequency, GlobalPos.of(level.dimension(), this.corePos));
            data.setNetworkName(frequency, this.name);
            WirelessAeNetworkRuntime.requestReconnect(frequency);
            player.displayClientMessage(
                    Component.translatable("gtna.wireless.message.core.name_saved", data.getNetworkName(frequency)),
                    true);
        }
    }
}
