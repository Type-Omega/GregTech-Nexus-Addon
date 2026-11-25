package com.raishxn.gtna.common.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

/**
 * Lazy move of per-player wireless networks into team networks (D-386). No Minecraft types, so it is
 * unit-tested on its own.
 *
 * <p>
 * Every lookup goes through {@link #networkOf}. When a player's network key is a team and the player still has a
 * network of his own, that network is removed and merged into the team's in one step, so the same EU or mB can
 * never be counted twice. The first lookup of a team in a session also merges the legacy networks of its other
 * members (offline players). Each player-to-team move is kept as a record and saved by the owner of the map.
 *
 * <p>
 * A player who leaves keeps nothing (the energy stays with the team) and starts an empty network; a player who
 * joins brings his own network into the new team.
 */
public final class TeamNetworkMigration<S> {

    private final Map<UUID, S> storage;
    private final WirelessNetworkScope scope;
    private final BinaryOperator<S> merge;
    private final BiConsumer<UUID, UUID> onMerge;
    private final Map<UUID, UUID> migrated = new HashMap<>();
    private final Set<UUID> swept = new HashSet<>();

    /**
     * @param merge   receives (team, member) and returns the value stored under the team
     * @param onMerge called with (member, team) after a merge, to mark the data dirty and log
     */
    public TeamNetworkMigration(Map<UUID, S> storage, WirelessNetworkScope scope, BinaryOperator<S> merge,
                                BiConsumer<UUID, UUID> onMerge) {
        this.storage = storage;
        this.scope = scope;
        this.merge = merge;
        this.onMerge = onMerge;
    }

    public UUID networkOf(UUID owner) {
        if (owner == null) return null;
        UUID network = scope.networkOf(owner);
        if (network == null) network = owner;
        if (!network.equals(owner)) absorb(owner, network);
        if (swept.add(network)) {
            for (UUID member : scope.membersOf(network)) {
                if (!member.equals(network) && network.equals(scope.networkOf(member))) absorb(member, network);
            }
        }
        return network;
    }

    /** The team a player's own network was last merged into, or null. */
    public UUID migratedInto(UUID player) {
        return migrated.get(player);
    }

    /** Saved player-to-team records, for the owner's save. */
    public Map<UUID, UUID> records() {
        return Collections.unmodifiableMap(migrated);
    }

    public void restore(UUID player, UUID network) {
        if (player != null && network != null) migrated.put(player, network);
    }

    private void absorb(UUID member, UUID network) {
        S legacy = storage.remove(member);
        if (legacy == null) return;
        storage.merge(network, legacy, merge);
        migrated.put(member, network);
        onMerge.accept(member, network);
    }
}
