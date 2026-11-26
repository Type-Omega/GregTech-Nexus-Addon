// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import appeng.api.networking.IGridNode;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class WirelessTickCache {

    private WirelessTickCache() {}

    static final Map<WirelessMemberKey, IGridNode> TICK_TARGET_NODE_CACHE = new HashMap<>();
    static final Map<UUID, WirelessNetworkCoreBlockEntity> TICK_CORE_CACHE = new HashMap<>();
    static final Map<UUID, IGridNode> TICK_BRIDGE_NODE_CACHE = new HashMap<>();
    static final Map<GridNodePair, Boolean> TICK_IN_WORLD_CONNECTION_CACHE = new HashMap<>();
    static final Map<IGridNode, Set<IGridNode>> TICK_WIRED_COMPONENTS = new IdentityHashMap<>();
    static final Map<UUID, WirelessAeTopologySnapshot> TOPOLOGY_SNAPSHOTS = new HashMap<>();
    static final Map<IGridNode, WirelessAeTopologySnapshot> COMPONENT_SNAPSHOTS = new IdentityHashMap<>();
    static boolean tickCacheActive;

    static final class GridNodePair {

        private final IGridNode first;
        private final IGridNode second;

        GridNodePair(IGridNode first, IGridNode second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof GridNodePair other && this.first == other.first && this.second == other.second;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(this.first) + System.identityHashCode(this.second);
        }
    }

    static void beginTickCaches() {
        tickCacheActive = true;
        TICK_TARGET_NODE_CACHE.clear();
        TICK_CORE_CACHE.clear();
        TICK_BRIDGE_NODE_CACHE.clear();
        TICK_IN_WORLD_CONNECTION_CACHE.clear();
    }

    static void endTickCaches() {
        tickCacheActive = false;
        TICK_TARGET_NODE_CACHE.clear();
        TICK_CORE_CACHE.clear();
        TICK_BRIDGE_NODE_CACHE.clear();
        TICK_IN_WORLD_CONNECTION_CACHE.clear();
    }
}
