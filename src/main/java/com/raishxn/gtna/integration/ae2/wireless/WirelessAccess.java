package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import appeng.api.networking.IGridNode;
import com.raishxn.gtna.common.data.FtbAwareNetworkScope;

import java.util.List;
import java.util.UUID;

/**
 * Who may use a wireless ME network. A network belongs to the team of the player who placed its core (D-386:
 * the effective FTB team, or the player without FTB Teams or without a team), so a party member can use it and
 * anyone outside the team cannot.
 */
final class WirelessAccess {

    private WirelessAccess() {}

    static boolean canAccessNetwork(ServerPlayer player, UUID frequency) {
        if (player == null || frequency == null) {
            return false;
        }

        MinecraftServer server = player.server;
        WirelessAeSavedData data = WirelessAeSavedData.get(server);
        UUID owner = data.getNetworkOwner(frequency);
        if (owner == null) {
            WirelessNetworkCoreBlockEntity core = WirelessNodes.getLoadedCore(server, frequency);
            if (core != null) {
                IGridNode coreNode = core.getActionableNode();
                owner = coreNode == null ? null : coreNode.getOwningPlayerProfileId();
                if (owner != null) {
                    data.setNetworkOwner(frequency, owner);
                }
            }
        }
        return owner != null && sameTeam(owner, player.getUUID());
    }

    static boolean sameTeam(UUID first, UUID second) {
        // EN: The team is looked up on every call: a player who joins a party takes his networks along.
        // PT: O time é consultado a cada chamada: quem entra num grupo leva as redes junto.
        // ZH: 每次调用都会重新查询队伍：加入队伍的玩家会带着自己的网络一起过去。
        UUID firstTeam = FtbAwareNetworkScope.INSTANCE.networkOf(first);
        return firstTeam != null && firstTeam.equals(FtbAwareNetworkScope.INSTANCE.networkOf(second));
    }

    static List<WirelessNetworkInfo> getAccessibleNetworkInfo(ServerPlayer player) {
        MinecraftServer server = player.server;
        return WirelessAeSavedData.get(server).getNetworkInfo(server).stream()
                .filter(network -> canAccessNetwork(player, network.frequency()))
                .toList();
    }
}
