package com.raishxn.gtna.integration.ae2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import org.jetbrains.annotations.Nullable;

/**
 * ME link of the Nexus Structure Terminal (D-439): the terminal remembers one ME node (any active grid node:
 * interface, controller, drive, access point...) and reaches its grid from any distance and any dimension. AE2 is a
 * hard dependency of GTNA, so the API is called directly.
 */
public final class NexusAE2Link {

    private static final String NBT_ACCESS_POINT = "accessPoint";

    private NexusAE2Link() {}

    /** Whether the block at {@code pos} hosts an AE2 grid node the terminal can link to. */
    public static boolean isLinkable(Level level, BlockPos pos) {
        return node(level, pos) != null;
    }

    @Nullable
    private static IGridNode node(Level level, BlockPos pos) {
        var host = GridHelper.getNodeHost(level, pos);
        if (host == null) return null;
        IGridNode node = host.getGridNode(null);
        if (node != null) return node;
        for (var side : net.minecraft.core.Direction.values()) {
            node = host.getGridNode(side);
            if (node != null) return node;
        }
        return null;
    }

    public static void link(ItemStack stack, Level level, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, GlobalPos.of(level.dimension(), pos)).result()
                .ifPresent(nbt -> tag.put(NBT_ACCESS_POINT, nbt));
    }

    public static void unlink(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) tag.remove(NBT_ACCESS_POINT);
    }

    public static boolean isLinked(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_ACCESS_POINT);
    }

    @Nullable
    public static GlobalPos getLinkedPosition(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_ACCESS_POINT)) return null;
        return GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get(NBT_ACCESS_POINT)).result().orElse(null);
    }

    // EN: no range or dimension limit (D-439 T8); reading the node loads its chunk if needed.
    // PT: sem limite de alcance nem de dimensão (D-439 T8); ler o nó carrega o chunk dele se preciso.
    // ZH: 没有距离和维度限制（D-439 T8）；读取节点时会按需加载其区块。
    /** The linked grid, or null when unlinked, the node is gone or the grid is offline. */
    @Nullable
    public static IGrid grid(ItemStack terminal, @Nullable MinecraftServer server) {
        GlobalPos at = getLinkedPosition(terminal);
        if (at == null || server == null) return null;
        ServerLevel level = server.getLevel(at.dimension());
        if (level == null) return null;
        IGridNode node = node(level, at.pos());
        if (node == null || !node.isActive()) return null;
        return node.getGrid();
    }

    /** One ME session for a whole build: the storage and action source are resolved once. */
    @Nullable
    public static Session open(ItemStack terminal, Player player) {
        IGrid grid = grid(terminal, player.getServer());
        if (grid == null) return null;
        return new Session(grid, grid.getStorageService().getInventory(), IActionSource.ofPlayer(player));
    }

    public record Session(IGrid grid, MEStorage storage, IActionSource source) {

        public long available(AEItemKey key) {
            return storage.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, source);
        }

        public long extract(AEItemKey key, long amount) {
            return amount <= 0 ? 0 : storage.extract(key, amount, Actionable.MODULATE, source);
        }

        /** Inserts what fits; returns how much went in. */
        public long insert(AEItemKey key, long amount, boolean simulate) {
            return amount <= 0 ? 0 :
                    storage.insert(key, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
        }
    }
}
