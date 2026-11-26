// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class WirelessTopology {

    private WirelessTopology() {}

    static final int MAX_WIRED_NODES_PER_SEARCH = 4096;

    static boolean connectionMatches(IGridConnection connection, IGridNode coreNode, IGridNode targetNode) {
        if (connection == null) {
            return false;
        }
        try {
            return connection.getOtherSide(coreNode) == targetNode;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    static boolean areConnected(IGridNode coreNode, IGridNode targetNode) {
        try {
            for (IGridConnection connection : coreNode.getConnections()) {
                if (connection.getOtherSide(coreNode) == targetNode) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return false;
    }

    static boolean areConnectedInWorld(IGridNode coreNode, IGridNode targetNode) {
        if (coreNode == null || targetNode == null) {
            return false;
        }
        if (WirelessTickCache.tickCacheActive) {
            WirelessTickCache.GridNodePair pair = new WirelessTickCache.GridNodePair(coreNode, targetNode);
            Boolean cached = WirelessTickCache.TICK_IN_WORLD_CONNECTION_CACHE.get(pair);
            if (cached != null) {
                return cached;
            }

            boolean connected = areConnectedInWorldUncached(coreNode, targetNode);
            WirelessTickCache.TICK_IN_WORLD_CONNECTION_CACHE.put(pair, connected);
            return connected;
        }

        return areConnectedInWorldUncached(coreNode, targetNode);
    }

    static boolean areConnectedInWorldUncached(IGridNode coreNode, IGridNode targetNode) {
        if (coreNode == targetNode) {
            return true;
        }
        WirelessAeTopologySnapshot snapshot = WirelessTickCache.tickCacheActive ?
                WirelessTickCache.COMPONENT_SNAPSHOTS.get(targetNode) : null;
        if (snapshot != null) {
            if (snapshot.contains(coreNode)) {

                return true;
            }
            if (snapshot.complete()) {

                return false;
            }
        }
        Set<IGridNode> component = WirelessTickCache.tickCacheActive ?
                WirelessTickCache.TICK_WIRED_COMPONENTS.get(targetNode) : null;
        if (component != null) {

            return component.contains(coreNode);
        }

        Set<IGridNode> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean complete = true;
        ArrayDeque<IGridNode> pending = new ArrayDeque<>();
        // In-world edges are bidirectional. Wireless targets usually have a much
        // smaller wired component than the core, often no wired edges at all.
        visited.add(targetNode);
        pending.add(targetNode);

        while (!pending.isEmpty()) {
            if (visited.size() >= MAX_WIRED_NODES_PER_SEARCH) {
                complete = false;

                break;
            }
            IGridNode node = pending.removeFirst();

            Iterable<IGridConnection> connections;
            try {
                connections = node.getConnections();
            } catch (RuntimeException ignored) {
                complete = false;
                continue;
            }

            for (IGridConnection connection : connections) {
                IGridNode otherNode;
                try {
                    if (connection == null || !connection.isInWorld()) continue;
                    otherNode = connection.getOtherSide(node);
                } catch (RuntimeException ignored) {
                    complete = false;
                    continue;
                }

                if (otherNode == coreNode) {
                    return true;
                }
                if (otherNode != null && visited.add(otherNode)) {
                    pending.add(otherNode);
                }
            }
        }
        // Only cache an exhausted, exception-free search. An early positive
        // result has not discovered the whole component and must not be cached.
        if (WirelessTickCache.tickCacheActive && complete) {
            WirelessTickCache.TICK_WIRED_COMPONENTS.put(targetNode, visited);
            WirelessAeTopologySnapshot stored = WirelessTickCache.COMPONENT_SNAPSHOTS.computeIfAbsent(targetNode,
                    ignored -> new WirelessAeTopologySnapshot());
            stored.replace(visited);

        }
        return false;
    }

    static boolean isSameGrid(IGridNode coreNode, IGridNode targetNode) {
        try {
            return coreNode.getGrid() != null && coreNode.getGrid() == targetNode.getGrid();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    static void destroyQuietly(IGridConnection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.destroy();

        } catch (RuntimeException ignored) {

            // AE2 may already have destroyed the connection while unloading a node.
        } finally {
            WirelessTickCache.TICK_WIRED_COMPONENTS.clear();
            WirelessTickCache.TICK_IN_WORLD_CONNECTION_CACHE.clear();

        }
    }
}
