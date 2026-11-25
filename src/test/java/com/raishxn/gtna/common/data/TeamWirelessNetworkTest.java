package com.raishxn.gtna.common.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** D-386: per-player wireless networks move to the team once, without loss or duplication. */
public final class TeamWirelessNetworkTest {

    private TeamWirelessNetworkTest() {}

    /** A team table the test edits; a player with no team is his own network. */
    private static final class FakeTeams implements WirelessNetworkScope {

        final Map<UUID, UUID> teamOf = new HashMap<>();

        @Override
        public UUID networkOf(UUID owner) {
            return teamOf.getOrDefault(owner, owner);
        }

        @Override
        public Collection<UUID> membersOf(UUID network) {
            return teamOf.entrySet().stream().filter(e -> e.getValue().equals(network)).map(Map.Entry::getKey)
                    .toList();
        }
    }

    /** One network map with its migration, read the way the saved data reads it. */
    private static final class Pool {

        final Map<UUID, Long> storage = new HashMap<>();
        final TeamNetworkMigration<Long> migration;
        int merges;

        Pool(WirelessNetworkScope scope, Map<UUID, Long> legacy) {
            storage.putAll(legacy);
            migration = new TeamNetworkMigration<>(storage, scope, Long::sum, (member, team) -> merges++);
        }

        long get(UUID owner) {
            return storage.getOrDefault(migration.networkOf(owner), 0L);
        }

        void add(UUID owner, long amount) {
            storage.merge(migration.networkOf(owner), amount, Long::sum);
        }

        long total() {
            return storage.values().stream().mapToLong(Long::longValue).sum();
        }
    }

    public static void main(String[] args) {
        noTeamsKeepsPlayers();
        soloPlayerKeepsHisNetwork();
        twoMembersMergeOnce();
        offlineMemberIsSweptWithTheTeam();
        leavingStartsEmptyAndJoiningBringsTheNetwork();
        recordsSurviveARestore();
    }

    private static void noTeamsKeepsPlayers() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        var pool = new Pool(WirelessNetworkScope.PER_PLAYER, Map.of(a, 100L, b, 50L));
        check(pool.get(a) == 100 && pool.get(b) == 50, "without FTB Teams each player keeps his network");
        check(pool.merges == 0 && pool.migration.migratedInto(a) == null, "nothing is migrated without teams");
    }

    private static void soloPlayerKeepsHisNetwork() {
        UUID a = UUID.randomUUID();
        var pool = new Pool(new FakeTeams(), Map.of(a, 100L));
        check(pool.get(a) == 100 && a.equals(pool.migration.networkOf(a)), "a solo team is the player's network");
        check(pool.merges == 0, "a solo player is not migrated");
    }

    private static void twoMembersMergeOnce() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), team = UUID.randomUUID();
        var teams = new FakeTeams();
        teams.teamOf.put(a, team);
        teams.teamOf.put(b, team);
        var pool = new Pool(teams, Map.of(a, 100L, b, 50L));
        check(pool.get(a) == 150, "both members' EU is summed into the team");
        check(pool.get(b) == 150 && pool.get(team) == 150, "every member and the team id read the same network");
        for (int i = 0; i < 5; i++) pool.get(i % 2 == 0 ? a : b);
        check(pool.total() == 150 && pool.merges == 2, "repeated access never adds again");
        check(!pool.storage.containsKey(a) && !pool.storage.containsKey(b), "the member entries are gone");
        check(team.equals(pool.migration.migratedInto(a)) && team.equals(pool.migration.migratedInto(b)),
                "the move is recorded");
    }

    private static void offlineMemberIsSweptWithTheTeam() {
        UUID online = UUID.randomUUID(), offline = UUID.randomUUID(), team = UUID.randomUUID();
        var teams = new FakeTeams();
        teams.teamOf.put(online, team);
        teams.teamOf.put(offline, team);
        var pool = new Pool(teams, Map.of(online, 7L, offline, 5L));
        check(pool.get(online) == 12, "an offline member's network joins on the team's first lookup");
    }

    private static void leavingStartsEmptyAndJoiningBringsTheNetwork() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), team = UUID.randomUUID();
        var teams = new FakeTeams();
        teams.teamOf.put(a, team);
        teams.teamOf.put(b, team);
        var pool = new Pool(teams, Map.of(a, 100L, b, 50L));
        check(pool.get(a) == 150, "merged");

        teams.teamOf.remove(b);
        check(pool.get(b) == 0, "a player who leaves starts an empty network");
        check(pool.get(a) == 150, "the energy stays with the team");
        pool.add(b, 10);

        teams.teamOf.put(b, team);
        check(pool.get(b) == 160 && pool.get(a) == 160, "rejoining brings what was earned alone, once");
        check(pool.total() == 160, "nothing is duplicated across the leave and the rejoin");
    }

    private static void recordsSurviveARestore() {
        UUID a = UUID.randomUUID(), team = UUID.randomUUID();
        var teams = new FakeTeams();
        teams.teamOf.put(a, team);
        var pool = new Pool(teams, Map.of(a, 3L));
        pool.get(a);
        var restored = new Pool(teams, pool.storage);
        pool.migration.records().forEach(restored.migration::restore);
        check(team.equals(restored.migration.migratedInto(a)) && restored.get(a) == 3 && restored.merges == 0,
                "a reload keeps the record and does not merge again");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
