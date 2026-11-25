package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The last scan of each player, so the terminal's Materials tab and the report show it without scanning again. */
public final class NexusLastScan {

    public record Entry(GlobalPos controller, NexusScan.Result result) {}

    private static final Map<UUID, Entry> LAST = new HashMap<>();

    private NexusLastScan() {}

    public static void put(Player player, IMultiController controller, NexusScan.Result result) {
        LAST.put(player.getUUID(), new Entry(GlobalPos.of(controller.self().getLevel().dimension(),
                controller.self().getPos()), result));
    }

    @Nullable
    public static Entry get(Player player) {
        return LAST.get(player.getUUID());
    }

    public static void clear(Player player) {
        LAST.remove(player.getUUID());
    }
}
