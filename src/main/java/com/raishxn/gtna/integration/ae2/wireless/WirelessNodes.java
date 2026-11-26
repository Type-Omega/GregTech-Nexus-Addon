// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;

final class WirelessNodes {

    private WirelessNodes() {}

    public static WirelessNetworkCoreBlockEntity getLoadedCore(MinecraftServer server, UUID frequency) {
        WirelessAeSavedData data = WirelessAeSavedData.get(server);
        GlobalPos corePos = data.getCore(frequency);
        if (corePos == null) {
            return null;
        }

        BlockEntity coreBlockEntity = getLoadedBlockEntity(server, corePos);
        if (coreBlockEntity instanceof WirelessNetworkCoreBlockEntity core &&
                !core.isRemoved() && frequency.equals(core.getFrequency())) {
            return core;
        }
        return null;
    }

    public static IGridNode findBridgeNode(WirelessNetworkCoreBlockEntity core) {
        IGridNode coreNode = core.getActionableNode();
        if (coreNode != null) {
            try {
                Direction connectionSide = core.getConnectionSide();
                IGridConnection connection = coreNode.getInWorldConnections().get(connectionSide);
                if (connection != null) {
                    IGridNode bridgeNode = connection.getOtherSide(coreNode);
                    if (isUsableBridgeNode(coreNode, bridgeNode)) {
                        return bridgeNode;
                    }
                }
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        if (!(core.getLevel() instanceof ServerLevel level)) {
            return null;
        }

        Direction connectionSide = core.getConnectionSide();
        try {
            BlockPos neighborPos = core.getBlockPos().relative(connectionSide);
            IGridNode bridgeNode = GridHelper.getExposedNode(level, neighborPos, connectionSide.getOpposite());
            if (isUsableBridgeNode(coreNode, bridgeNode)) {
                return bridgeNode;
            }
        } catch (RuntimeException ignored) {
            // Some third-party block entities throw while their multiblock is rebuilding.
        }
        return null;
    }

    static boolean isUsableBridgeNode(IGridNode coreNode, IGridNode bridgeNode) {
        if (bridgeNode == null || bridgeNode == coreNode) {
            return false;
        }

        try {
            if (bridgeNode.getOwner() instanceof WirelessNetworkCoreBlockEntity) {
                return false;
            }
            return bridgeNode.getGrid() != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    static WirelessNetworkCoreBlockEntity getLoadedCoreCached(MinecraftServer server, UUID frequency) {
        if (!WirelessTickCache.tickCacheActive) {
            return getLoadedCore(server, frequency);
        }
        if (WirelessTickCache.TICK_CORE_CACHE.containsKey(frequency)) {
            return WirelessTickCache.TICK_CORE_CACHE.get(frequency);
        }

        WirelessNetworkCoreBlockEntity core = getLoadedCore(server, frequency);
        WirelessTickCache.TICK_CORE_CACHE.put(frequency, core);
        return core;
    }

    static IGridNode findBridgeNodeCached(UUID frequency, WirelessNetworkCoreBlockEntity core) {
        if (!WirelessTickCache.tickCacheActive) {
            return findBridgeNode(core);
        }
        if (WirelessTickCache.TICK_BRIDGE_NODE_CACHE.containsKey(frequency)) {
            return WirelessTickCache.TICK_BRIDGE_NODE_CACHE.get(frequency);
        }

        IGridNode bridgeNode = findBridgeNode(core);
        WirelessTickCache.TICK_BRIDGE_NODE_CACHE.put(frequency, bridgeNode);
        return bridgeNode;
    }

    static BlockEntity getLoadedBlockEntity(MinecraftServer server, GlobalPos pos) {
        ServerLevel level = server.getLevel(pos.dimension());
        if (level == null || !level.hasChunkAt(pos.pos())) {
            return null;
        }
        return level.getBlockEntity(pos.pos());
    }

    static IGridNode findTargetNode(MinecraftServer server, WirelessMemberKey member) {
        if (WirelessTickCache.tickCacheActive && WirelessTickCache.TICK_TARGET_NODE_CACHE.containsKey(member)) {
            return WirelessTickCache.TICK_TARGET_NODE_CACHE.get(member);
        }

        IGridNode node = findTargetNodeUncached(server, member);
        if (WirelessTickCache.tickCacheActive) {
            WirelessTickCache.TICK_TARGET_NODE_CACHE.put(member, node);

        }
        return node;
    }

    static IGridNode findTargetNodeUncached(MinecraftServer server, WirelessMemberKey member) {
        GlobalPos pos = member.pos();
        ServerLevel level = server.getLevel(pos.dimension());
        if (level == null || !level.hasChunkAt(pos.pos())) {
            return null;
        }
        return findTargetNode(level, pos.pos(), member.side());
    }

    static IGridNode findTargetNode(ServerLevel level, BlockPos pos, Direction side) {
        if (side != null) {
            IGridNode partNode = findPartNode(level, pos, side);
            if (partNode != null) {
                return partNode;
            }

            if (level.getBlockEntity(pos) instanceof IPartHost) {
                return findTargetNode(level, pos);
            }

            try {
                IGridNode exposedNode = GridHelper.getExposedNode(level, pos, side);
                if (exposedNode != null) {
                    return exposedNode;
                }
            } catch (RuntimeException ignored) {
                // Some third-party block entities throw while their multiblock is rebuilding.
            }
        }
        return findTargetNode(level, pos);
    }

    static IGridNode findTargetNode(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IPartHost) {
            for (Direction direction : Direction.values()) {
                IGridNode node = findPartNode(level, pos, direction);
                if (node != null) {
                    return node;
                }
            }
        }

        IGridNode node = findNode(blockEntity);
        if (node != null) {
            return node;
        }

        for (Direction direction : Direction.values()) {
            try {
                node = GridHelper.getExposedNode(level, pos, direction);
                if (node != null) {
                    return node;
                }
            } catch (RuntimeException ignored) {
                // Some third-party block entities throw while their multiblock is rebuilding.
            }
        }
        return null;
    }

    static IGridNode findPartNode(ServerLevel level, BlockPos pos, Direction side) {
        if (side == null) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IPartHost host)) {
            return null;
        }

        try {
            IPart part = host.getPart(side);
            if (part == null) {
                return null;
            }
            IGridNode node = part.getGridNode();
            if (node != null) {
                return node;
            }
            node = part.getExternalFacingNode();
            if (node != null) {
                return node;
            }
            return findNode(part, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static Direction resolvePartSide(ServerLevel level, BlockPos pos, Direction clickedSide, Vec3 hitLocation) {
        Direction selectedSide = findSelectedPartSide(level, pos, hitLocation);
        if (selectedSide != null) {
            return selectedSide;
        }
        if (findPartNode(level, pos, clickedSide) != null) {
            return clickedSide;
        }
        return findFirstPartSide(level, pos);
    }

    static Direction findSelectedPartSide(ServerLevel level, BlockPos pos, Vec3 hitLocation) {
        if (hitLocation == null) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IPartHost host)) {
            return null;
        }

        try {
            appeng.api.parts.SelectedPart selectedPart = host.selectPartWorld(hitLocation);
            if (selectedPart == null || selectedPart.part == null) {
                return null;
            }
            if (selectedPart.side != null && findPartNode(level, pos, selectedPart.side) != null) {
                return selectedPart.side;
            }
            for (Direction direction : Direction.values()) {
                if (host.getPart(direction) == selectedPart.part && findPartNode(level, pos, direction) != null) {
                    return direction;
                }
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    static Direction findFirstPartSide(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IPartHost host)) {
            return null;
        }

        for (Direction direction : Direction.values()) {
            try {
                if (host.getPart(direction) != null && findPartNode(level, pos, direction) != null) {
                    return direction;
                }
            } catch (RuntimeException ignored) {
                // Keep scanning other sides.
            }
        }
        return null;
    }

    static IGridNode findNode(BlockEntity blockEntity) {
        return findNode(blockEntity, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    static IGridNode findNode(Object target, Set<Object> visited, int depth) {
        if (target == null || depth > 5 || !visited.add(target)) {
            return null;
        }

        try {
            if (target instanceof IManagedGridNode managedGridNode) {
                IGridNode node = managedGridNode.getNode();
                if (node != null) {
                    return node;
                }
            }

            IGridNode clusterNode = findClusterNode(target);
            if (clusterNode != null) {
                return clusterNode;
            }

            if (target instanceof IActionHost actionHost) {
                IGridNode node = actionHost.getActionableNode();
                if (node != null) {
                    return node;
                }
            }

            if (target instanceof IInWorldGridNodeHost host) {
                for (Direction direction : Direction.values()) {
                    IGridNode node = host.getGridNode(direction);
                    if (node != null) {
                        return node;
                    }
                }
            }

            IGridNode noSideNode = invokeGridNode(target, "getGridNode");
            if (noSideNode != null) {
                return noSideNode;
            }

            IGridNode mainNode = invokeManagedGridNode(target);
            if (mainNode != null) {
                return mainNode;
            }

            IGridNode directNode = invokeGridNode(target, "getNode");
            if (directNode != null) {
                return directNode;
            }

            for (String nestedMethod : new String[] { "getMetaMachine", "getMachine", "getNodeHolder", "getMETrait" }) {
                IGridNode nestedNode = findNode(WirelessReflection.invokeObject(target, nestedMethod), visited,
                        depth + 1);
                if (nestedNode != null) {
                    return nestedNode;
                }
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    static IGridNode findClusterNode(Object target) {
        return invokeGridNode(WirelessReflection.invokeObject(target, "getCluster"), "getNode");
    }

    static IGridNode invokeGridNode(Object target, String methodName) {
        Object result = WirelessReflection.invokeObject(target, methodName);
        return result instanceof IGridNode node ? node : null;
    }

    static IGridNode invokeManagedGridNode(Object target) {
        try {
            Object result = WirelessReflection.invokeObject(target, "getMainNode");
            return result instanceof IManagedGridNode managed ? managed.getNode() : null;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }
}
