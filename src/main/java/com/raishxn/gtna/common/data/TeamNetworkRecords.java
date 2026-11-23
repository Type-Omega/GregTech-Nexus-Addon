package com.raishxn.gtna.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.UUID;

/** Save format and log line of the team migration records (GTIA-D-386). */
final class TeamNetworkRecords {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "TeamMigrations";

    private TeamNetworkRecords() {}

    static void log(String network, UUID member, UUID team) {
        LOGGER.info("GTNA wireless {} network of {} merged into team network {} (GTIA-D-386)", network, member, team);
    }

    static void load(CompoundTag tag, TeamNetworkMigration<?> migration) {
        if (!tag.contains(TAG, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("Player") && entry.hasUUID("Network")) {
                migration.restore(entry.getUUID("Player"), entry.getUUID("Network"));
            }
        }
    }

    static void save(CompoundTag tag, TeamNetworkMigration<?> migration) {
        if (migration.records().isEmpty()) return;
        ListTag list = new ListTag();
        migration.records().forEach((player, network) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", player);
            entry.putUUID("Network", network);
            list.add(entry);
        });
        tag.put(TAG, list);
    }
}
