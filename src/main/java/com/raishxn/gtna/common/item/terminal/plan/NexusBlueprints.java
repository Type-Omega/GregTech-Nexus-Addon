package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import com.raishxn.gtna.common.item.terminal.NexusBlockPattern;
import com.raishxn.gtna.common.item.terminal.NexusBuildCheckGuard;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory.AutoBuildSetting;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Saved structures of the Nexus Terminal (GTIA-D-440): the blocks of a built structure, saved under a name and
 * pasted later onto another controller of the same machine, turned to its facing. A paste plans the target as usual
 * and asks each cell for the saved block when the pattern allows it, so the result always passes the structure
 * check. Kept per player in the world's saved data (a big structure is too large for the item's NBT).
 * <p>
 * Named schematics come from the Schematic Interface of GregTech Terminals (BgameXL, MIT), rewritten for GTNA.
 */
public final class NexusBlueprints extends SavedData {

    public static final int MAX_PER_PLAYER = 32;
    public static final int MAX_CELLS = 65_536;
    private static final String ID = "gtna_nexus_blueprints";

    /** One saved structure: the machine, and per cell its offset in the controller's frame (front = north). */
    public record Blueprint(String name, ResourceLocation machine, long[] offsets, int[] blocks, List<Item> palette) {

        public int size() {
            return offsets.length;
        }
    }

    private final Map<UUID, LinkedHashMap<String, Blueprint>> byPlayer = new HashMap<>();

    public static NexusBlueprints get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(NexusBlueprints::load, NexusBlueprints::new, ID);
    }

    public List<Blueprint> list(Player player) {
        return List.copyOf(byPlayer.getOrDefault(player.getUUID(), new LinkedHashMap<>()).values());
    }

    @Nullable
    public Blueprint find(Player player, String name) {
        var map = byPlayer.get(player.getUUID());
        return map == null ? null : map.get(name);
    }

    public boolean delete(Player player, String name) {
        var map = byPlayer.get(player.getUUID());
        boolean removed = map != null && map.remove(name) != null;
        if (removed) setDirty();
        return removed;
    }

    /** Saves the blocks the scan found in place. Returns the blueprint, or null when the name list is full. */
    @Nullable
    public Blueprint save(Player player, String name, IMultiController controller, NexusScan.Result result) {
        var map = byPlayer.computeIfAbsent(player.getUUID(), k -> new LinkedHashMap<>());
        if (!map.containsKey(name) && map.size() >= MAX_PER_PLAYER) return null;
        BlockPos origin = controller.self().getPos();
        Direction front = controller.self().getFrontFacing();
        List<Item> palette = new ArrayList<>();
        Map<Item, Integer> index = new HashMap<>();
        List<Long> offsets = new ArrayList<>();
        List<Integer> blocks = new ArrayList<>();
        for (NexusBuildPlan plan : result.plans()) {
            for (NexusBuildPlan.Cell cell : plan.cells) {
                if (cell.kind == NexusBuildPlan.Kind.CONTROLLER || cell.existing.isAir()) continue;
                Item item = cell.existing.getBlock().asItem();
                if (!(item instanceof BlockItem) || offsets.size() >= MAX_CELLS) continue;
                offsets.add(toLocal(cell.pos.subtract(origin), front).asLong());
                blocks.add(index.computeIfAbsent(item, i -> {
                    palette.add(i);
                    return palette.size() - 1;
                }));
            }
        }
        var blueprint = new Blueprint(name, controller.self().getDefinition().getId(),
                offsets.stream().mapToLong(Long::longValue).toArray(),
                blocks.stream().mapToInt(Integer::intValue).toArray(), List.copyOf(palette));
        map.put(name, blueprint);
        setDirty();
        return blueprint;
    }

    public enum PasteResult {
        DONE,
        WRONG_MACHINE,
        UNREADABLE
    }

    /** Pastes onto {@code controller}: a replace-mode build that asks each cell for the saved block. */
    public static PasteResult paste(Player player, IMultiController controller, @Nullable NexusBlockPattern pattern,
                                    ItemStack terminal, Blueprint blueprint) {
        if (!controller.self().getDefinition().getId().equals(blueprint.machine())) return PasteResult.WRONG_MACHINE;
        if (pattern == null) return PasteResult.UNREADABLE;
        BlockPos origin = controller.self().getPos();
        Direction front = controller.self().getFrontFacing();
        Map<BlockPos, Item> overrides = new HashMap<>();
        for (int i = 0; i < blueprint.offsets().length; i++) {
            overrides.put(origin.offset(toWorld(BlockPos.of(blueprint.offsets()[i]), front)),
                    blueprint.palette().get(blueprint.blocks()[i]));
        }
        var tag = terminal.getOrCreateTag();
        boolean replace = tag.getBoolean("ReplaceMode");
        tag.putBoolean("ReplaceMode", true);
        NexusStructurePlanner.OVERRIDES.set(overrides);
        try {
            var state = controller.getMultiblockState();
            NexusBuildCheckGuard.run(state,
                    () -> pattern.autoBuild(player, state, AutoBuildSetting.getSetting(terminal), terminal));
        } finally {
            NexusStructurePlanner.OVERRIDES.remove();
            tag.putBoolean("ReplaceMode", replace);
        }
        return PasteResult.DONE;
    }

    /** World offset to the controller's frame, where its front faces north (horizontal facings only). */
    static BlockPos toLocal(BlockPos offset, Direction front) {
        return rotate(offset, turns(front));
    }

    static BlockPos toWorld(BlockPos local, Direction front) {
        return rotate(local, (4 - turns(front)) % 4);
    }

    private static int turns(Direction front) {
        return switch (front) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    /** Rotates an offset by quarter turns counter-clockwise seen from above. */
    private static BlockPos rotate(BlockPos p, int turns) {
        int x = p.getX(), z = p.getZ();
        for (int i = 0; i < turns; i++) {
            int t = x;
            x = z;
            z = -t;
        }
        return new BlockPos(x, p.getY(), z);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        byPlayer.forEach((uuid, map) -> {
            ListTag list = new ListTag();
            for (Blueprint b : map.values()) {
                CompoundTag t = new CompoundTag();
                t.putString("name", b.name());
                t.putString("machine", b.machine().toString());
                t.putLongArray("offsets", b.offsets());
                t.putIntArray("blocks", b.blocks());
                ListTag palette = new ListTag();
                for (Item item : b.palette())
                    palette.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
                t.put("palette", palette);
                list.add(t);
            }
            players.put(uuid.toString(), list);
        });
        tag.put("players", players);
        return tag;
    }

    private static NexusBlueprints load(CompoundTag tag) {
        NexusBlueprints data = new NexusBlueprints();
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            var map = new LinkedHashMap<String, Blueprint>();
            for (Tag raw : players.getList(key, Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) raw;
                List<Item> palette = new ArrayList<>();
                for (Tag p : t.getList("palette", Tag.TAG_STRING)) {
                    palette.add(BuiltInRegistries.ITEM.get(new ResourceLocation(p.getAsString())));
                }
                var b = new Blueprint(t.getString("name"), new ResourceLocation(t.getString("machine")),
                        t.getLongArray("offsets"), t.getIntArray("blocks"), palette);
                map.put(b.name(), b);
            }
            data.byPlayer.put(UUID.fromString(key), map);
        }
        return data;
    }
}
