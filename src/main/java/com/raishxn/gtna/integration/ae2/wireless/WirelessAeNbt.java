// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Saved form of the positions and member keys of {@link WirelessAeSavedData}. */
final class WirelessAeNbt {

    private static final String TAG_DIMENSION = "dimension";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";
    private static final String TAG_SIDE = "side";

    private WirelessAeNbt() {}

    static CompoundTag writeGlobalPos(GlobalPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_DIMENSION, pos.dimension().location().toString());
        tag.putInt(TAG_X, pos.pos().getX());
        tag.putInt(TAG_Y, pos.pos().getY());
        tag.putInt(TAG_Z, pos.pos().getZ());
        return tag;
    }

    static GlobalPos readGlobalPos(CompoundTag tag) {
        if (!tag.contains(TAG_DIMENSION)) {
            return null;
        }

        try {
            ResourceKey<Level> dimension = ResourceKey.create(
                    Registries.DIMENSION,
                    new ResourceLocation(tag.getString(TAG_DIMENSION)));
            BlockPos pos = new BlockPos(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z));
            return GlobalPos.of(dimension, pos);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static CompoundTag writeMemberKey(WirelessMemberKey member) {
        CompoundTag tag = writeGlobalPos(member.pos());
        if (member.side() != null) {
            tag.putString(TAG_SIDE, member.side().getName());
        }
        return tag;
    }

    static WirelessMemberKey readMemberKey(CompoundTag tag) {
        GlobalPos pos = readGlobalPos(tag);
        if (pos == null) {
            return null;
        }

        Direction side = null;
        if (tag.contains(TAG_SIDE)) {
            side = Direction.byName(tag.getString(TAG_SIDE));
            // An invalid face must not silently become a whole-block binding.
            if (side == null) return null;
        }
        return new WirelessMemberKey(pos, side);
    }
}
