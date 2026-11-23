package com.raishxn.gtna.common.data;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Who shares a wireless energy or steam network. With FTB Teams loaded, the network belongs to the player's
 * effective team (a solo player's team id is the player id, so a solo player keeps the network he always had);
 * without FTB Teams it belongs to the player. A team id resolves to itself.
 */
public interface WirelessNetworkScope {

    /** The network key of {@code owner}: a team id or the player id. Never null for a non-null owner. */
    UUID networkOf(UUID owner);

    /** Members of a team network other than the network id itself; empty when unknown. */
    Collection<UUID> membersOf(UUID network);

    WirelessNetworkScope PER_PLAYER = new WirelessNetworkScope() {

        @Override
        public UUID networkOf(UUID owner) {
            return owner;
        }

        @Override
        public Collection<UUID> membersOf(UUID network) {
            return List.of();
        }
    };
}
