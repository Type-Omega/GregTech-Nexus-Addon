// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** One wireless member: a block position and, for a cable-bus part, the side of that part. */
public record WirelessMemberKey(GlobalPos pos, Direction side) {

    public static WirelessMemberKey of(ResourceKey<Level> dimension, BlockPos pos, Direction side) {
        return new WirelessMemberKey(GlobalPos.of(dimension, pos), side);
    }

    public BlockPos blockPos() {
        return this.pos.pos();
    }
}
