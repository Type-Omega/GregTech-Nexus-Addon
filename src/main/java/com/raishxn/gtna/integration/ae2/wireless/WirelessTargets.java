// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPartHost;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class WirelessTargets {

    private WirelessTargets() {}

    static final Map<Class<?>, WirelessTargetClassInfo> WIRELESS_TARGET_CLASS_CACHE = new ConcurrentHashMap<>();
    static final String GTCEU_ME_PART_PACKAGE = "com.gregtechceu.gtceu.integration.ae2.";
    static final String GTMTHINGS_ME_PART_PACKAGE = "com.hepdd.gtmthings.common.block.machine.multiblock.part.appeng.";
    static final String GTCEU_META_MACHINE_CLASS = "com.gregtechceu.gtceu.api.machine.MetaMachine";
    static final String GTCEU_MULTIBLOCK_PART_CLASS = "com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine";
    static final String GTCEU_GRID_CONNECTED_MACHINE_INTERFACE = "com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine";

    static final Set<String> WIRELESS_ME_TARGET_IDS = Set.of(
            "me_input_bus",
            "me_stocking_input_bus",
            "me_output_bus",
            "me_input_hatch",
            "me_stocking_input_hatch",
            "me_output_hatch",
            "me_pattern_buffer",
            "me_pattern_buffer_proxy");

    record WirelessTargetClassInfo(boolean knownTarget, boolean compatibleGridTarget, boolean meLikeName) {}

    static boolean canBindAsWirelessTarget(ServerLevel level, BlockPos pos) {
        return isWirelessMeTarget(level, pos) && WirelessNodes.findTargetNode(level, pos) != null;
    }

    static boolean canBindAsWirelessTarget(ServerLevel level, BlockPos pos, Direction side) {
        if (!isWirelessMeTarget(level, pos)) {
            return false;
        }
        if (side != null && WirelessNodes.findTargetNode(level, pos, side) != null) {
            return true;
        }
        return WirelessNodes.findTargetNode(level, pos) != null;
    }

    static boolean isWirelessMeTarget(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof WirelessNetworkCoreBlockEntity) return false;
        if (isAeBlockTarget(blockEntity)) return true;
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
        if (isWirelessMeTargetObject(blockEntity, blockId)) return true;
        Object metaMachine = WirelessReflection.invokeObject(blockEntity, "getMetaMachine");
        return isWirelessMeTargetObject(metaMachine, blockId) || isWirelessMeTargetId(blockId);
    }

    /**
     * AE2's own blocks and parts (interfaces, pattern providers, drives...) expose a grid node through the AE2 API.
     * A cable bus only counts when a part sits on it: the cable alone must not become a wireless bridge.
     */
    static boolean isAeBlockTarget(BlockEntity blockEntity) {
        if (blockEntity instanceof IPartHost host) {
            for (Direction direction : Direction.values()) {
                if (host.getPart(direction) != null) return true;
            }
            return false;
        }
        return blockEntity instanceof IInWorldGridNodeHost || blockEntity instanceof IActionHost;
    }

    /** GregTech's own rule for a machine with an owner: its team, its friends and operators may configure it. */
    static boolean mayConfigure(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IMachineBlockEntity holder) {
            return MachineOwner.canOpenOwnerMachine(player, holder.getMetaMachine());
        }
        return true;
    }

    static BlockPos resolveWirelessTargetPos(Level level, BlockPos pos) {
        if (level == null) {
            return pos;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        BlockPos resolved = findReferencedTargetPos(level, blockEntity);
        if (resolved != null) {
            return resolved;
        }

        Object metaMachine = WirelessReflection.invokeObject(blockEntity, "getMetaMachine");
        resolved = findReferencedTargetPos(level, metaMachine);
        return resolved == null ? pos : resolved;
    }

    static WirelessMemberKey resolveWirelessTarget(ServerLevel level, BlockPos pos, Direction side) {
        return resolveWirelessTarget(level, pos, side, null);
    }

    static WirelessMemberKey resolveWirelessTarget(ServerLevel level, BlockPos pos, Direction side,
                                                   Vec3 hitLocation) {
        BlockPos targetPos = resolveWirelessTargetPos(level, pos);
        Direction targetSide = targetPos.equals(pos) ?
                WirelessNodes.resolvePartSide(level, targetPos, side, hitLocation) : null;
        return WirelessMemberKey.of(level.dimension(), targetPos, targetSide);
    }

    static BlockPos findReferencedTargetPos(Level level, Object target) {
        if (target == null) {
            return null;
        }

        for (String methodName : new String[] { "getBufferPos", "getMainPos" }) {
            BlockPos pos = WirelessReflection.invokeBlockPos(target, methodName);
            if (isResolvableTargetPos(level, pos)) {
                return pos;
            }
        }

        for (String methodName : new String[] { "getBuffer" }) {
            Object referencedTarget = WirelessReflection.invokeObject(target, methodName);
            BlockPos pos = WirelessReflection.invokeBlockPos(referencedTarget, "getPos");
            if (isResolvableTargetPos(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    static boolean isResolvableTargetPos(Level level, BlockPos pos) {
        return pos != null && level.hasChunkAt(pos) &&
                !(level.getBlockEntity(pos) instanceof WirelessNetworkCoreBlockEntity);
    }

    static boolean isWirelessMeTargetObject(Object target, ResourceLocation blockId) {
        if (target == null) {
            return false;
        }

        WirelessTargetClassInfo classInfo = WIRELESS_TARGET_CLASS_CACHE.computeIfAbsent(
                target.getClass(),
                WirelessTargets::inspectWirelessTargetClass);
        return classInfo.knownTarget() ||
                (classInfo.compatibleGridTarget() &&
                        (classInfo.meLikeName() || isCompatibleExternalMeLikeId(blockId)));
    }

    static WirelessTargetClassInfo inspectWirelessTargetClass(Class<?> type) {
        String className = type.getName();
        // EN: Any GregTech machine that is an AE2 grid node counts, whichever mod's package it lives in.
        // PT: Qualquer máquina do GregTech que seja um nó do AE2 vale, em qualquer pacote de mod.
        // ZH: 任何属于 AE2 网格节点的 GregTech 机器都算，不论它位于哪个模组的包中。
        boolean knownTarget = className.startsWith(GTCEU_ME_PART_PACKAGE) ||
                className.startsWith(GTMTHINGS_ME_PART_PACKAGE) ||
                WirelessReflection.hasTypeName(type, GTCEU_GRID_CONNECTED_MACHINE_INTERFACE);
        boolean compatibleGridTarget = hasMachinePartMarker(type) && hasGridNodeAccess(type);
        return new WirelessTargetClassInfo(knownTarget, compatibleGridTarget, isMeLikeClassName(className));
    }

    static boolean hasMachinePartMarker(Class<?> type) {
        return WirelessReflection.hasTypeName(type, GTCEU_META_MACHINE_CLASS) ||
                WirelessReflection.hasTypeName(type, GTCEU_MULTIBLOCK_PART_CLASS) ||
                WirelessReflection.hasTypeName(type, GTCEU_GRID_CONNECTED_MACHINE_INTERFACE);
    }

    static boolean hasGridNodeAccess(Class<?> type) {
        return IManagedGridNode.class.isAssignableFrom(type) ||
                IActionHost.class.isAssignableFrom(type) ||
                IInWorldGridNodeHost.class.isAssignableFrom(type) ||
                WirelessReflection.hasTypeName(type, GTCEU_GRID_CONNECTED_MACHINE_INTERFACE) ||
                WirelessReflection.hasNoArgMethod(type, "getMainNode") ||
                WirelessReflection.hasNoArgMethod(type, "getNodeHolder") ||
                WirelessReflection.hasNoArgMethod(type, "getMETrait");
    }

    static boolean isMeLikeClassName(String className) {
        int simpleNameStart = className.lastIndexOf('.') + 1;
        String simpleName = className.substring(simpleNameStart);
        String lowerClassName = className.toLowerCase(Locale.ROOT);
        String lowerSimpleName = simpleName.toLowerCase(Locale.ROOT);
        return simpleName.startsWith("ME") ||
                simpleName.startsWith("AE") ||
                lowerClassName.contains(".appeng.") ||
                lowerSimpleName.contains("appeng") ||
                lowerSimpleName.contains("ae2") ||
                lowerSimpleName.contains("patternbuffer");
    }

    static boolean isCompatibleExternalMeLikeId(ResourceLocation blockId) {
        if (blockId == null || "minecraft".equals(blockId.getNamespace())) {
            return false;
        }
        return isMeLikePath(blockId.getPath());
    }

    static boolean isWirelessMeTargetId(ResourceLocation blockId) {
        if (blockId == null) {
            return false;
        }

        String namespace = blockId.getNamespace();
        String path = blockId.getPath();
        return WIRELESS_ME_TARGET_IDS.contains(path) ||
                ("gtmthings".equals(namespace) && isGtmthingsMeLikePath(path));
    }

    static boolean isGtmthingsMeLikePath(String path) {
        return isMeLikePath(path);
    }

    static boolean isMeLikePath(String path) {
        return path.startsWith("me_") ||
                path.contains("_me_") ||
                path.startsWith("ae_") ||
                path.contains("_ae_") ||
                path.contains("appeng") ||
                path.contains("ae2");
    }
}
