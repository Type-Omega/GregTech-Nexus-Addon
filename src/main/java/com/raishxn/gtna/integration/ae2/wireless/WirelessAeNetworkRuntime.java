// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WirelessAeNetworkRuntime {

    private WirelessAeNetworkRuntime() {}

    static final int CONNECT_INTERVAL_SMALL_TICKS = 20;
    /** Every member is re-checked this often: a reloaded part builds a new node and nothing else says so. */
    static final int REVALIDATE_INTERVAL_TICKS = 200;
    static final long MAX_MAINTENANCE_NANOS_PER_TICK = 2_000_000L;
    static final long MAX_SCAN_NANOS_PER_TICK = 500_000L;
    static final int MAX_SCAN_MEMBERS_PER_TICK = 128;
    static final int MAX_SCAN_MEMBERS_PER_NETWORK = 32;
    static final int MAX_SCAN_NETWORKS_PER_TICK = 32;
    static final Map<UUID, Long> LAST_EXPANDED_REVISION = new HashMap<>();
    static final Set<UUID> FULL_SCAN_REQUESTS = new LinkedHashSet<>();
    static final Map<UUID, ScanProgress> FULL_SCAN_PROGRESS = new HashMap<>();
    static long maintenanceTick;
    static final Set<UUID> REQUESTED_RECONNECTS = new LinkedHashSet<>();
    static final WirelessAeMemberScheduler MEMBER_SCHEDULER = new WirelessAeMemberScheduler();
    static int tickMembersProcessed;
    static long tickMaintenanceStarted;
    static int tickCounter;
    static boolean schedulerExecuting;

    /** Coalesces bursts of AE2 edge callbacks into one maintenance invalidation. */
    static boolean topologyInvalidationPending;

    static final class ScanProgress {

        private WirelessAeSavedData.MemberScan cursor;
        private final long startedTick;
        private boolean rescanRequested;

        private ScanProgress(WirelessAeSavedData.MemberScan cursor, long startedTick) {
            this.cursor = cursor;
            this.startedTick = startedTick;
        }
    }

    public static void register(IEventBus forgeBus) {
        forgeBus.addListener(WirelessAeNetworkRuntime::onBlockBreak);
        forgeBus.addListener(WirelessAeNetworkRuntime::onBlockPlace);
        forgeBus.addListener(WirelessAeNetworkRuntime::onServerTick);
        forgeBus.addListener(WirelessAeNetworkRuntime::onServerStopped);
    }

    static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        // No node references or deadlines may survive into another integrated server.
        WirelessLinks.CONNECTIONS.clear();
        WirelessLinks.NEXT_WIRED_RECHECK_TICKS.clear();
        LAST_EXPANDED_REVISION.clear();
        FULL_SCAN_REQUESTS.clear();
        FULL_SCAN_PROGRESS.clear();
        MEMBER_SCHEDULER.clear();
        topologyInvalidationPending = false;
        WirelessTickCache.TOPOLOGY_SNAPSHOTS.clear();
        WirelessTickCache.COMPONENT_SNAPSHOTS.clear();
        maintenanceTick = 0;
        REQUESTED_RECONNECTS.clear();
        WirelessTickCache.endTickCaches();
        tickCounter = 0;
        tickMembersProcessed = 0;
        tickMaintenanceStarted = 0;
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            invalidateTopology();
            WirelessTickCache.COMPONENT_SNAPSHOTS.clear();
            WirelessLinks.clearMemberBinding(serverLevel, event.getPos());
        }
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            invalidateTopology();
            WirelessTickCache.COMPONENT_SNAPSHOTS.clear();
            WirelessLinks.clearMemberBinding(serverLevel, event.getPos());
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;
        maintenanceTick++;
        WirelessTickCache.beginTickCaches();
        try {
            applyPendingTopologyInvalidation();
            scheduleMaintenance(event.getServer());
            maintainNetworks(event.getServer());

        } finally {
            WirelessTickCache.endTickCaches();
        }
    }

    static void scheduleMaintenance(MinecraftServer server) {
        if (maintenanceTick % CONNECT_INTERVAL_SMALL_TICKS == 0) {
            connectAll(server);
        }
        // EN: A part that unloads and loads again gets a new AE2 node, and no block event tells this sweep.
        // PT: Uma peça que descarrega e carrega de novo ganha um nó AE2 novo, e nenhum evento de bloco avisa a
        // varredura.
        // ZH: 卸载后重新加载的部件会得到新的 AE2 节点，而没有任何方块事件会通知这次扫描。
        if (maintenanceTick % REVALIDATE_INTERVAL_TICKS == 0) {
            MEMBER_SCHEDULER.invalidateAllNetworks();
        }

        if (!REQUESTED_RECONNECTS.isEmpty()) {
            Set<UUID> requested = new LinkedHashSet<>(REQUESTED_RECONNECTS);
            REQUESTED_RECONNECTS.clear();
            for (UUID frequency : requested) {
                FULL_SCAN_REQUESTS.add(frequency);
                ScanProgress progress = FULL_SCAN_PROGRESS.get(frequency);
                if (progress != null) progress.rescanRequested = true;
                LAST_EXPANDED_REVISION.remove(frequency);
            }
        }
    }

    static void applyPendingTopologyInvalidation() {
        if (!topologyInvalidationPending) return;
        topologyInvalidationPending = false;
        MEMBER_SCHEDULER.invalidateAllNetworks();
    }

    static void maintainNetworks(MinecraftServer server) {
        tickMaintenanceStarted = System.nanoTime();
        tickMembersProcessed = 0;
        MEMBER_SCHEDULER.beginTick(maintenanceTick);
        expandFullScans(server, tickMaintenanceStarted + MAX_SCAN_NANOS_PER_TICK);
        MEMBER_SCHEDULER.run(tickMaintenanceStarted + MAX_MAINTENANCE_NANOS_PER_TICK,
                (key, previous) -> processScheduledMember(server, key, previous),
                WirelessTopology::destroyQuietly);
        return;
    }

    static WirelessAeMemberScheduler.Outcome processScheduledMember(MinecraftServer server,
                                                                    WirelessAeMemberScheduler.Key key,
                                                                    WirelessAeMemberScheduler.State previous) {
        WirelessAeSavedData data = WirelessAeSavedData.get(server);
        GlobalPos corePos = data.getCore(key.frequency());
        if (corePos == null || !data.containsMember(key.frequency(), key.member())) {
            return WirelessAeMemberScheduler.Outcome.of(WirelessAeMemberScheduler.State.CLEANUP);
        }
        WirelessNetworkCoreBlockEntity core = WirelessNodes.getLoadedCoreCached(server, key.frequency());
        IGridNode bridge = core == null ? null : WirelessNodes.findBridgeNodeCached(key.frequency(), core);
        if (core == null || bridge == null) {
            return new WirelessAeMemberScheduler.Outcome(WirelessAeMemberScheduler.State.RETRY_WAIT,
                    "core_unavailable");
        }
        Map<WirelessMemberKey, IGridConnection> connections = WirelessLinks.CONNECTIONS.computeIfAbsent(key.frequency(),
                ignored -> new HashMap<>());
        schedulerExecuting = true;
        WirelessLinks.ConnectionResult result;
        try {
            result = WirelessLinks.connectMember(key.frequency(), corePos, key.member(), bridge, connections);
        } finally {
            schedulerExecuting = false;
        }
        return switch (result) {
            case CONNECTED -> WirelessAeMemberScheduler.Outcome.of(WirelessAeMemberScheduler.State.WIRELESS);
            case ALREADY_CONNECTED -> WirelessAeMemberScheduler.Outcome.of(WirelessAeMemberScheduler.State.WIRED);
            case TARGET_MISSING -> new WirelessAeMemberScheduler.Outcome(WirelessAeMemberScheduler.State.UNLOADED,
                    "target_unavailable");
            case FAILED -> new WirelessAeMemberScheduler.Outcome(WirelessAeMemberScheduler.State.RETRY_WAIT,
                    "connection_failed");
            default -> new WirelessAeMemberScheduler.Outcome(WirelessAeMemberScheduler.State.RETRY_WAIT,
                    result.name().toLowerCase(Locale.ROOT));
        };
    }

    public static void invalidateTopology() {
        if (topologyInvalidationPending) return;
        topologyInvalidationPending = true;
        WirelessTickCache.TICK_WIRED_COMPONENTS.clear();
        WirelessTickCache.TICK_IN_WORLD_CONNECTION_CACHE.clear();
        WirelessTickCache.COMPONENT_SNAPSHOTS.clear();
        WirelessTickCache.TOPOLOGY_SNAPSHOTS.clear();
    }

    public static void requestReconnect(UUID frequency) {
        if (frequency != null) {
            WirelessTickCache.TICK_WIRED_COMPONENTS.clear();
            WirelessTickCache.TICK_CORE_CACHE.remove(frequency);
            WirelessTickCache.TICK_BRIDGE_NODE_CACHE.remove(frequency);

            REQUESTED_RECONNECTS.add(frequency);
        }
    }

    static void enqueueFullScan(UUID frequency) {
        FULL_SCAN_REQUESTS.add(frequency);
    }

    static void expandFullScans(MinecraftServer server, long deadline) {
        WirelessAeSavedData data = WirelessAeSavedData.get(server);
        int expanded = 0;
        int visited = 0;
        int visits = Math.min(MAX_SCAN_NETWORKS_PER_TICK, FULL_SCAN_REQUESTS.size());
        while (visited < visits && expanded < MAX_SCAN_MEMBERS_PER_TICK && System.nanoTime() < deadline) {
            Iterator<UUID> pending = FULL_SCAN_REQUESTS.iterator();
            UUID frequency = pending.next();
            pending.remove();
            visited++;
            if (data.getCore(frequency) == null) {
                FULL_SCAN_PROGRESS.remove(frequency);
                LAST_EXPANDED_REVISION.remove(frequency);

                continue;
            }
            ScanProgress progress = FULL_SCAN_PROGRESS.get(frequency);
            if (progress == null) {
                progress = new ScanProgress(data.openMemberScan(frequency), maintenanceTick);
                FULL_SCAN_PROGRESS.put(frequency, progress);

            } else if (!progress.cursor.isCurrent(frequency)) {
                // Membership changed between ticks. Never resume an invalid collection iterator.
                progress.cursor = data.openMemberScan(frequency);
                progress.rescanRequested = false;

            }
            int networkExpanded = 0;
            while (progress.cursor.remaining() > 0 && networkExpanded < MAX_SCAN_MEMBERS_PER_NETWORK &&
                    expanded < MAX_SCAN_MEMBERS_PER_TICK && System.nanoTime() < deadline) {
                MEMBER_SCHEDULER.enqueue(frequency, progress.cursor.next(), WirelessAeMemberScheduler.Kind.DIRTY);
                expanded++;
                networkExpanded++;
            }

            if (progress.cursor.remaining() == 0) {
                LAST_EXPANDED_REVISION.put(frequency, progress.cursor.revision());
                FULL_SCAN_PROGRESS.remove(frequency);

                if (progress.rescanRequested) {
                    LAST_EXPANDED_REVISION.remove(frequency);
                    FULL_SCAN_REQUESTS.add(frequency);
                }
            } else {
                // Rotate unfinished networks; neither the request set nor membership is copied.
                FULL_SCAN_REQUESTS.add(frequency);
            }
        }

        if (!FULL_SCAN_REQUESTS.isEmpty()) {

        }
    }

    public static void connectNow(MinecraftServer server, UUID frequency) {
        requestReconnect(frequency);
    }

    public static void disconnectFrequency(UUID frequency) {
        WirelessTickCache.TICK_CORE_CACHE.remove(frequency);
        WirelessTickCache.TICK_BRIDGE_NODE_CACHE.remove(frequency);
        MEMBER_SCHEDULER.forgetNetwork(frequency);
        FULL_SCAN_REQUESTS.remove(frequency);
        FULL_SCAN_PROGRESS.remove(frequency);
        REQUESTED_RECONNECTS.remove(frequency);
        Map<WirelessMemberKey, IGridConnection> frequencyConnections = WirelessLinks.CONNECTIONS.remove(frequency);
        if (frequencyConnections == null) {
            return;
        }

        for (Map.Entry<WirelessMemberKey, IGridConnection> entry : frequencyConnections.entrySet()) {
            WirelessTickCache.TICK_TARGET_NODE_CACHE.remove(entry.getKey());
            WirelessLinks.forgetWiredConnectionCheck(entry.getKey());
            WirelessTopology.destroyQuietly(entry.getValue());
        }
    }

    public static String shortFrequency(UUID frequency) {
        return frequency.toString().substring(0, 8);
    }

    static void connectAll(MinecraftServer server) {
        WirelessAeSavedData data = WirelessAeSavedData.get(server);
        Set<UUID> frequencies = new HashSet<>(data.getFrequencies());
        Set<UUID> tracked = new HashSet<>(WirelessLinks.CONNECTIONS.keySet());
        tracked.addAll(LAST_EXPANDED_REVISION.keySet());
        tracked.addAll(FULL_SCAN_REQUESTS);
        for (UUID frequency : tracked) {
            if (!frequencies.contains(frequency)) disconnectFrequency(frequency);
        }
        LAST_EXPANDED_REVISION.keySet().retainAll(frequencies);
        for (UUID frequency : frequencies) {
            long revision = data.getMemberRevision(frequency);
            if (LAST_EXPANDED_REVISION.getOrDefault(frequency, Long.MIN_VALUE) != revision) {
                enqueueFullScan(frequency);
            }
        }
    }
}
