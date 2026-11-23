package com.raishxn.gtna.integration.ftb;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Loaded only when FTB Teams is installed: the members of a team, for the wireless network migration. */
public final class FTBWirelessTeams {

    private FTBWirelessTeams() {}

    public static Collection<UUID> members(UUID team) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) return List.of();
        return FTBTeamsAPI.api().getManager().getTeamByID(team)
                .filter(found -> found.isPartyTeam())
                .map(found -> (Collection<UUID>) Set.copyOf(found.getMembers()))
                .orElse(List.of());
    }
}
