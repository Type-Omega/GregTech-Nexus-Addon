// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class WirelessLinks {

    private WirelessLinks() {}

    static final int WIRED_RECHECK_INTERVAL_TICKS = 100;
    static final Map<UUID, Map<WirelessMemberKey, IGridConnection>> CONNECTIONS = new HashMap<>();
    static final Map<WirelessMemberKey, Integer> NEXT_WIRED_RECHECK_TICKS = new HashMap<>();

    public enum ConnectionResult {
        CONNECTED,
        ALREADY_CONNECTED,
        CORE_MISSING,
        CORE_NOT_CONNECTED,
        TARGET_MISSING,
        FAILED
    }

    public static ConnectionResult connectMemberNow(MinecraftServer server, UUID frequency, GlobalPos member) {
        return connectMemberNow(server, frequency, new WirelessMemberKey(member, null));
    }

    public static ConnectionResult connectMemberNow(MinecraftServer server, UUID frequency,
                                                    WirelessMemberKey member) {
        WirelessAeSavedData data = WirelessAeSavedData.get(server);
        GlobalPos corePos = data.getCore(frequency);
        if (corePos == null) {
            WirelessAeNetworkRuntime.disconnectFrequency(frequency);
            return ConnectionResult.CORE_MISSING;
        }

        WirelessNetworkCoreBlockEntity core = WirelessNodes.getLoadedCoreCached(server, frequency);
        if (core == null) {
            WirelessAeNetworkRuntime.disconnectFrequency(frequency);
            return ConnectionResult.CORE_MISSING;
        }

        IGridNode bridgeNode = WirelessNodes.findBridgeNodeCached(frequency, core);
        if (bridgeNode == null) {
            WirelessAeNetworkRuntime.disconnectFrequency(frequency);
            return ConnectionResult.CORE_NOT_CONNECTED;
        }

        Map<WirelessMemberKey, IGridConnection> frequencyConnections = CONNECTIONS.computeIfAbsent(frequency,
                ignored -> new HashMap<>());
        clearMemberRetry(member);
        forgetWiredConnectionCheck(member);
        WirelessTickCache.TICK_IN_WORLD_CONNECTION_CACHE.clear();
        WirelessTickCache.TICK_WIRED_COMPONENTS.clear();
        WirelessTickCache.TICK_TARGET_NODE_CACHE.remove(member);
        ConnectionResult result = connectMember(frequency, corePos, member, bridgeNode, frequencyConnections);
        removeEmptyConnectionSet(frequency, frequencyConnections);
        return result;
    }

    public static void disconnectMember(UUID frequency, GlobalPos member) {
        disconnectMember(frequency, new WirelessMemberKey(member, null));
    }

    public static void disconnectMember(UUID frequency, WirelessMemberKey member) {
        clearMemberRetry(member);
        WirelessTickCache.TICK_TARGET_NODE_CACHE.remove(member);
        Map<WirelessMemberKey, IGridConnection> frequencyConnections = CONNECTIONS.get(frequency);
        if (frequencyConnections == null) {
            forgetWiredConnectionCheck(member);
            return;
        }

        IGridConnection connection = frequencyConnections.remove(member);
        forgetWiredConnectionCheck(member);
        WirelessTopology.destroyQuietly(connection);
        if (frequencyConnections.isEmpty()) {
            CONNECTIONS.remove(frequency);
        }
    }

    public static boolean isMemberConnected(MinecraftServer server, UUID frequency, WirelessMemberKey member) {
        Map<WirelessMemberKey, IGridConnection> frequencyConnections = CONNECTIONS.get(frequency);
        if (hasStoredConnection(frequencyConnections, member)) {
            return true;
        }

        WirelessNetworkCoreBlockEntity core = WirelessNodes.getLoadedCore(server, frequency);
        if (core == null) {
            return false;
        }

        IGridNode bridgeNode = WirelessNodes.findBridgeNode(core);
        IGridNode targetNode = WirelessNodes.findTargetNode(server, member);
        return bridgeNode != null && targetNode != null && (WirelessTopology.areConnected(bridgeNode, targetNode) ||
                WirelessTopology.isSameGrid(bridgeNode, targetNode));
    }

    public static boolean hasWirelessConnection(UUID frequency, WirelessMemberKey member) {
        return hasStoredConnection(CONNECTIONS.get(frequency), member);
    }

    public static UUID findConnectedNetworkFrequency(MinecraftServer server, WirelessMemberKey member) {
        UUID wiredFrequency = findWiredNetworkFrequency(server, member);
        if (wiredFrequency != null) {
            return wiredFrequency;
        }

        WirelessAeSavedData data = WirelessAeSavedData.get(server);
        UUID frequency = data.getMemberNetwork(member);
        if (frequency != null && isMemberConnected(server, frequency, member)) {
            return frequency;
        }

        return null;
    }

    public static UUID findWiredNetworkFrequency(MinecraftServer server, WirelessMemberKey member) {
        return findWiredNetworkFrequency(server, member, true);
    }

    static UUID findWiredNetworkFrequency(MinecraftServer server, WirelessMemberKey member,
                                          boolean includeWirelessBackedCable) {
        IGridNode targetNode = WirelessNodes.findTargetNode(server, member);
        if (targetNode == null) {
            return null;
        }

        WirelessAeSavedData data = WirelessAeSavedData.get(server);
        for (WirelessNetworkInfo network : data.getNetworkInfo(server)) {
            WirelessNetworkCoreBlockEntity core = WirelessNodes.getLoadedCoreCached(server, network.frequency());
            if (core == null) {
                continue;
            }

            IGridNode bridgeNode = WirelessNodes.findBridgeNodeCached(network.frequency(), core);
            if (WirelessTopology.areConnectedInWorld(bridgeNode, targetNode)) {
                return network.frequency();
            }
            if (includeWirelessBackedCable && isConnectedThroughWirelessBackedCable(server, network.frequency(),
                    bridgeNode, member, targetNode)) {
                return network.frequency();
            }
        }
        return null;
    }

    static boolean isConnectedThroughWirelessBackedCable(MinecraftServer server, UUID frequency,
                                                         IGridNode bridgeNode,
                                                         WirelessMemberKey member,
                                                         IGridNode targetNode) {
        Map<WirelessMemberKey, IGridConnection> frequencyConnections = CONNECTIONS.get(frequency);
        if (frequencyConnections == null || frequencyConnections.isEmpty()) {
            return false;
        }

        for (Map.Entry<WirelessMemberKey, IGridConnection> entry : frequencyConnections.entrySet()) {
            WirelessMemberKey connectedMember = entry.getKey();
            if (connectedMember.pos().equals(member.pos())) {
                continue;
            }

            IGridNode connectedNode = WirelessNodes.findTargetNode(server, connectedMember);
            if (connectedNode == null ||
                    !WirelessTopology.connectionMatches(entry.getValue(), bridgeNode, connectedNode)) {
                continue;
            }

            if (WirelessTopology.areConnectedInWorld(connectedNode, targetNode)) {
                return true;
            }
        }
        return false;
    }

    public static void unbindWithTool(ServerPlayer player, BlockPos clickedPos) {
        ServerLevel level = player.serverLevel();
        if (!player.isShiftKeyDown() || !player.mayBuild() || player.isSpectator() ||
                !(player.getMainHandItem().getItem() instanceof WirelessNetworkBindingToolItem) ||
                !level.hasChunkAt(clickedPos) || !level.mayInteract(player, clickedPos))
            return;
        BlockPos pos = WirelessTargets.resolveWirelessTargetPos(level, clickedPos);
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) ||
                !WirelessTargets.isWirelessMeTarget(level, pos))
            return;
        GlobalPos member = GlobalPos.of(level.dimension(), pos);
        WirelessAeSavedData data = WirelessAeSavedData.get(level.getServer());
        Set<UUID> frequencies = new HashSet<>();
        for (UUID frequency : data.getFrequencies()) {
            if (data.getMembers(frequency).stream().anyMatch(key -> key.pos().equals(member)))
                frequencies.add(frequency);
        }
        for (var entry : CONNECTIONS.entrySet()) {
            if (entry.getValue().keySet().stream().anyMatch(key -> key.pos().equals(member)))
                frequencies.add(entry.getKey());
        }
        if (frequencies.stream().anyMatch(frequency -> !WirelessAccess.canAccessNetwork(player, frequency))) {
            player.displayClientMessage(Component.translatable("gtna.wireless.message.tool.no_access"), true);
            return;
        }
        clearMemberBinding(level, pos);
        for (UUID frequency : frequencies) disconnectMembersAt(frequency, member);
        WirelessAeNetworkRuntime.invalidateTopology();
        WirelessTickCache.COMPONENT_SNAPSHOTS.clear();
        player.displayClientMessage(Component.translatable(frequencies.isEmpty() ?
                "gtna.wireless.message.tool.not_bound" : "gtna.wireless.message.target.disconnected"), true);
    }

    static void clearMemberBinding(ServerLevel level, BlockPos pos) {
        GlobalPos member = GlobalPos.of(level.dimension(), pos);
        clearMemberRetries(key -> key.pos().equals(member));
        forgetWiredConnectionChecksAt(member);
        WirelessTickCache.TICK_TARGET_NODE_CACHE.keySet().removeIf(key -> key.pos().equals(member));
        WirelessAeSavedData data = WirelessAeSavedData.get(level.getServer());
        for (UUID frequency : data.removeMembersAt(member)) {
            disconnectMembersAt(frequency, member);
        }
    }

    public static void disconnectMembersAt(UUID frequency, GlobalPos member) {
        clearMemberRetries(key -> key.pos().equals(member));
        NEXT_WIRED_RECHECK_TICKS.keySet().removeIf(key -> key.pos().equals(member));
        WirelessTickCache.TICK_TARGET_NODE_CACHE.keySet().removeIf(key -> key.pos().equals(member));
        Map<WirelessMemberKey, IGridConnection> frequencyConnections = CONNECTIONS.get(frequency);
        if (frequencyConnections == null) {
            return;
        }

        Iterator<Map.Entry<WirelessMemberKey, IGridConnection>> iterator = frequencyConnections.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<WirelessMemberKey, IGridConnection> entry = iterator.next();
            if (entry.getKey().pos().equals(member)) {
                forgetWiredConnectionCheck(entry.getKey());
                WirelessTopology.destroyQuietly(entry.getValue());
                iterator.remove();
            }
        }
        if (frequencyConnections.isEmpty()) {
            CONNECTIONS.remove(frequency);
        }
    }

    static ConnectionResult connectMember(UUID frequency, GlobalPos corePos,
                                          WirelessMemberKey member,
                                          IGridNode bridgeNode,
                                          Map<WirelessMemberKey, IGridConnection> frequencyConnections) {
        try {
            ConnectionResult result = connectMemberMeasured(frequency, corePos, member, bridgeNode,
                    frequencyConnections);
            if (result == ConnectionResult.CONNECTED || result == ConnectionResult.ALREADY_CONNECTED) {
                clearMemberRetry(member);
            }

            return result;
        } catch (RuntimeException error) {

            forgetWiredConnectionCheck(member);
            WirelessTopology.destroyQuietly(frequencyConnections.remove(member));
            scheduleMemberRetry(frequency, member);
            return ConnectionResult.FAILED;
        }
    }

    static ConnectionResult connectMemberMeasured(UUID frequency, GlobalPos corePos,
                                                  WirelessMemberKey member, IGridNode bridgeNode,
                                                  Map<WirelessMemberKey, IGridConnection> frequencyConnections) {
        MinecraftServer server = bridgeNode.getLevel().getServer();
        ServerLevel targetLevel = server.getLevel(member.pos().dimension());
        if (targetLevel == null || !targetLevel.hasChunkAt(member.blockPos())) {

            WirelessTickCache.TICK_TARGET_NODE_CACHE.remove(member);
            forgetWiredConnectionCheck(member);
            WirelessTopology.destroyQuietly(frequencyConnections.remove(member));
            scheduleMemberRetry(frequency, member);
            return ConnectionResult.TARGET_MISSING;
        }
        if (!WirelessTargets.isWirelessMeTarget(targetLevel, member.blockPos())) {

            forgetWiredConnectionCheck(member);
            WirelessTopology.destroyQuietly(frequencyConnections.remove(member));
            scheduleMemberRetry(frequency, member);
            return ConnectionResult.TARGET_MISSING;
        }

        IGridNode targetNode = WirelessNodes.findTargetNode(server, member);
        if (targetNode == null) {

            forgetWiredConnectionCheck(member);
            WirelessTopology.destroyQuietly(frequencyConnections.remove(member));
            scheduleMemberRetry(frequency, member);
            return ConnectionResult.TARGET_MISSING;
        }

        IGridConnection currentConnection = frequencyConnections.get(member);
        boolean currentConnectionMatches = WirelessTopology.connectionMatches(currentConnection, bridgeNode,
                targetNode);
        if (currentConnectionMatches && !shouldRecheckWiredConnection(member)) {
            return ConnectionResult.ALREADY_CONNECTED;
        }

        if (currentConnectionMatches) {
            if (findWiredNetworkFrequency(server, member, false) != null) {
                forgetWiredConnectionCheck(member);
                WirelessTopology.destroyQuietly(currentConnection);
                frequencyConnections.remove(member);
                return ConnectionResult.ALREADY_CONNECTED;
            }
            markWiredConnectionChecked(member);
            return ConnectionResult.ALREADY_CONNECTED;
        }

        if (targetNode == bridgeNode) {
            forgetWiredConnectionCheck(member);
            WirelessTopology.destroyQuietly(frequencyConnections.remove(member));
            return ConnectionResult.ALREADY_CONNECTED;
        }

        if ((WirelessTopology.areConnected(bridgeNode, targetNode) ||
                WirelessTopology.isSameGrid(bridgeNode, targetNode))) {
            forgetWiredConnectionCheck(member);
            WirelessTopology.destroyQuietly(frequencyConnections.remove(member));
            return ConnectionResult.ALREADY_CONNECTED;
        }

        if (findWiredNetworkFrequency(server, member, true) != null) {
            forgetWiredConnectionCheck(member);
            WirelessTopology.destroyQuietly(currentConnection);
            frequencyConnections.remove(member);
            return ConnectionResult.ALREADY_CONNECTED;
        }
        markWiredConnectionChecked(member);

        WirelessTopology.destroyQuietly(currentConnection);
        frequencyConnections.remove(member);

        try {
            try {
                frequencyConnections.put(member, GridHelper.createConnection(bridgeNode, targetNode));
            } finally {
                WirelessTickCache.TICK_WIRED_COMPONENTS.clear();
                WirelessTickCache.TICK_IN_WORLD_CONNECTION_CACHE.clear();
            }
            return ConnectionResult.CONNECTED;
        } catch (RuntimeException error) {

            scheduleMemberRetry(frequency, member);
            return ConnectionResult.FAILED;
        }
    }

    static void scheduleMemberRetry(UUID frequency, WirelessMemberKey member) {
        if (WirelessAeNetworkRuntime.schedulerExecuting) return;
        WirelessAeNetworkRuntime.MEMBER_SCHEDULER.enqueue(frequency, member, WirelessAeMemberScheduler.Kind.RETRY_DUE);
    }

    static void clearMemberRetry(WirelessMemberKey member) {
        if (WirelessAeNetworkRuntime.schedulerExecuting) return;
        WirelessAeNetworkRuntime.MEMBER_SCHEDULER.cancelMember(member);
    }

    static void clearMemberRetries(java.util.function.Predicate<WirelessMemberKey> predicate) {
        WirelessAeNetworkRuntime.MEMBER_SCHEDULER.forgetAt(predicate);
    }

    static boolean hasStoredConnection(Map<WirelessMemberKey, IGridConnection> frequencyConnections,
                                       WirelessMemberKey member) {
        if (frequencyConnections == null) {
            return false;
        }
        if (frequencyConnections.get(member) != null) {
            return true;
        }
        if (member.side() != null) {
            return frequencyConnections.get(new WirelessMemberKey(member.pos(), null)) != null;
        }
        for (Map.Entry<WirelessMemberKey, IGridConnection> entry : frequencyConnections.entrySet()) {
            if (entry.getValue() != null && entry.getKey().pos().equals(member.pos())) {
                return true;
            }
        }
        return false;
    }

    static void removeEmptyConnectionSet(UUID frequency,
                                         Map<WirelessMemberKey, IGridConnection> frequencyConnections) {
        if (frequencyConnections.isEmpty()) {
            CONNECTIONS.remove(frequency);
        }
    }

    static boolean shouldRecheckWiredConnection(WirelessMemberKey member) {
        Integer nextCheckTick = NEXT_WIRED_RECHECK_TICKS.get(member);
        return nextCheckTick == null || WirelessAeNetworkRuntime.tickCounter - nextCheckTick >= 0;
    }

    static void markWiredConnectionChecked(WirelessMemberKey member) {
        NEXT_WIRED_RECHECK_TICKS.put(member, WirelessAeNetworkRuntime.tickCounter + WIRED_RECHECK_INTERVAL_TICKS);
    }

    static void forgetWiredConnectionCheck(WirelessMemberKey member) {
        NEXT_WIRED_RECHECK_TICKS.remove(member);
    }

    static void forgetWiredConnectionChecksAt(GlobalPos pos) {
        NEXT_WIRED_RECHECK_TICKS.keySet().removeIf(member -> member.pos().equals(pos));
    }
}
