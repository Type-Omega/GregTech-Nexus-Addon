// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AECableType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

/**
 * The AE2 node of a Wireless ME Network Core. It has one cable face (the block's front); the block next to that
 * face is the "bridge" every bound member connects to.
 */
public class WirelessNetworkCoreBlockEntity extends BlockEntity implements IActionHost, IInWorldGridNodeHost {

    private static final String TAG_FREQUENCY = "frequency";
    private static final String NODE_TAG = "wireless_core";

    private static final IGridNodeListener<WirelessNetworkCoreBlockEntity> NODE_LISTENER = new IGridNodeListener<>() {

        @Override
        public void onSaveChanges(WirelessNetworkCoreBlockEntity host, IGridNode node) {
            host.setChanged();
        }
    };

    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, NODE_LISTENER)
            .setFlags(GridFlags.DENSE_CAPACITY)
            .setIdlePowerUsage(0.0D)
            .setInWorldNode(true)
            .setExposedOnSides(getExposedSides())
            .setTagName(NODE_TAG);

    private UUID frequency;

    public WirelessNetworkCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.mainNode.setVisualRepresentation(WirelessAeContent.WIRELESS_NETWORK_CORE.asItem());
        refreshExposedSides();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel serverLevel) {
            ensureFrequency();
            refreshExposedSides();
            createMainNode(serverLevel);
            WirelessAeSavedData data = WirelessAeSavedData.get(serverLevel.getServer());
            data.setCore(this.frequency, GlobalPos.of(serverLevel.dimension(), this.worldPosition));
            UUID owner = this.mainNode.getNode().getOwningPlayerProfileId();
            if (owner != null) {
                data.setNetworkOwner(this.frequency, owner);
            }
            WirelessAeNetworkRuntime.requestReconnect(this.frequency);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.frequency != null) {
            WirelessAeNetworkRuntime.disconnectFrequency(this.frequency);
        }
        this.mainNode.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.frequency != null) {
            WirelessAeNetworkRuntime.disconnectFrequency(this.frequency);
        }
        this.mainNode.destroy();
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID(TAG_FREQUENCY)) {
            this.frequency = tag.getUUID(TAG_FREQUENCY);
        }
        this.mainNode.loadFromNBT(tag);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID(TAG_FREQUENCY, ensureFrequency());
        this.mainNode.saveToNBT(tag);
    }

    @Override
    public IGridNode getActionableNode() {
        return this.mainNode.getNode();
    }

    @Override
    public IGridNode getGridNode(Direction direction) {
        refreshExposedSides();
        return isConnectionSide(direction) ? this.mainNode.getNode() : null;
    }

    @Override
    public AECableType getCableConnectionType(Direction direction) {
        refreshExposedSides();
        return isConnectionSide(direction) ? AECableType.SMART : AECableType.NONE;
    }

    public UUID getFrequency() {
        return ensureFrequency();
    }

    public void setOwner(Player player) {
        this.mainNode.setOwningPlayer(player);
        if (this.level instanceof ServerLevel serverLevel) {
            WirelessAeSavedData.get(serverLevel.getServer()).setNetworkOwner(getFrequency(), player.getUUID());
        }
    }

    /** A core saved before owners were recorded belongs to the first player who opens it. */
    public boolean migrateLegacyOwner(Player player) {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return false;
        }

        WirelessAeSavedData data = WirelessAeSavedData.get(serverLevel.getServer());
        UUID frequency = getFrequency();
        if (data.getNetworkOwner(frequency) != null || this.mainNode.getNode().getOwningPlayerProfileId() != null) {
            return false;
        }

        setOwner(player);
        return true;
    }

    public boolean isLinkedToAeNetwork() {
        return WirelessNodes.findBridgeNode(this) != null;
    }

    public Direction getConnectionSide() {
        BlockState state = getBlockState();
        if (state.hasProperty(WirelessNetworkCoreBlock.FACING)) {
            return state.getValue(WirelessNetworkCoreBlock.FACING);
        }
        return Direction.NORTH;
    }

    public void removeNetwork(MinecraftServer server) {
        if (this.frequency == null) {
            return;
        }
        WirelessAeSavedData.get(server).removeNetwork(this.frequency);
        WirelessAeNetworkRuntime.disconnectFrequency(this.frequency);
    }

    /** The block was turned: move the cable face, drop the old link and let the members connect again. */
    public void refreshConnectionSide(Direction oldSide, Direction newSide) {
        Direction connectionSide = newSide == null ? getConnectionSide() : newSide;
        this.mainNode.setExposedOnSides(EnumSet.noneOf(Direction.class));
        this.mainNode.setExposedOnSides(EnumSet.of(connectionSide));
        setChanged();
        if (this.frequency != null) {
            WirelessAeNetworkRuntime.disconnectFrequency(this.frequency);
            WirelessAeNetworkRuntime.requestReconnect(this.frequency);
        }
        BlockState state = getBlockState();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
        notifyConnectionNeighbors(oldSide, connectionSide, state);
    }

    private UUID ensureFrequency() {
        if (this.frequency == null) {
            this.frequency = UUID.randomUUID();
            setChanged();
        }
        return this.frequency;
    }

    private void createMainNode(ServerLevel serverLevel) {
        if (!this.mainNode.isReady()) {
            this.mainNode.create(serverLevel, this.worldPosition);
        }
    }

    private boolean isConnectionSide(Direction direction) {
        return direction != null && direction == getConnectionSide();
    }

    private EnumSet<Direction> getExposedSides() {
        return EnumSet.of(getConnectionSide());
    }

    private void refreshExposedSides() {
        Direction connectionSide = getConnectionSide();
        this.mainNode.setExposedOnSides(EnumSet.of(connectionSide));

        IGridNode node = this.mainNode.getNode();
        if (node == null) {
            return;
        }

        try {
            for (Map.Entry<Direction, IGridConnection> entry : new ArrayList<>(
                    node.getInWorldConnections().entrySet())) {
                if (entry.getKey() != connectionSide) {
                    entry.getValue().destroy();
                }
            }
        } catch (RuntimeException ignored) {
            // AE2 may update the connection map while neighboring nodes are changing.
        }
    }

    private void notifyConnectionNeighbors(Direction oldSide, Direction newSide, BlockState state) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        this.level.updateNeighborsAt(this.worldPosition, state.getBlock());
        notifyConnectionNeighbor(oldSide, state);
        if (newSide != oldSide) {
            notifyConnectionNeighbor(newSide, state);
        }
    }

    private void notifyConnectionNeighbor(Direction side, BlockState state) {
        if (side == null) {
            return;
        }

        BlockPos neighborPos = this.worldPosition.relative(side);
        this.level.neighborChanged(neighborPos, state.getBlock(), this.worldPosition);
        this.level.updateNeighborsAt(neighborPos, state.getBlock());
    }
}
