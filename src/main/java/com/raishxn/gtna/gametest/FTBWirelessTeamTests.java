package com.raishxn.gtna.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;

import com.raishxn.gtna.api.capability.SteamWirelessNetworkManager;
import com.raishxn.gtna.api.capability.WirelessEnergyManager;
import com.raishxn.gtna.common.data.NexusEnergyNetwork;
import com.raishxn.gtna.common.data.SteamNetworkData;
import com.typeomega.lib.data.Int128;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/** Optional FTB Teams test body for GTIA-D-386: legacy per-player networks move to the party once. */
final class FTBWirelessTeamTests {

    private FTBWirelessTeamTests() {}

    @SuppressWarnings("unchecked")
    static void run(GameTestHelper h) throws ReflectiveOperationException {
        var manager = (dev.ftb.mods.ftbteams.data.TeamManagerImpl) dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api()
                .getManager();
        var knownField = manager.getClass().getDeclaredField("knownPlayers");
        knownField.setAccessible(true);
        var known = (Map<UUID, dev.ftb.mods.ftbteams.api.Team>) knownField.get(manager);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), solo = UUID.randomUUID();

        // Saved before anyone is in a team: the format every existing world has.
        var legacy = new NexusEnergyNetwork();
        legacy.setExactEnergy(first, BigInteger.valueOf(100));
        legacy.setExactEnergy(second, BigInteger.valueOf(50));
        legacy.setExactEnergy(solo, BigInteger.valueOf(30));
        String dimension = h.getLevel().dimension().location().toString();
        legacy.putMatrix(first, net.minecraft.core.GlobalPos.of(h.getLevel().dimension(), h.absolutePos(
                net.minecraft.core.BlockPos.ZERO)), new com.raishxn.gtna.common.data.FluxMatrices.Matrix(1, 1, 0.9,
                        new Int128(1_000), new Int128(10), false, dimension));
        legacy.putMatrix(second, net.minecraft.core.GlobalPos.of(h.getLevel().dimension(), h.absolutePos(
                new net.minecraft.core.BlockPos(1, 0, 0))), new com.raishxn.gtna.common.data.FluxMatrices.Matrix(1, 1,
                        0.9, new Int128(2_000), new Int128(10), false, dimension));
        CompoundTag energyTag = legacy.save(new CompoundTag());
        var legacySteam = new SteamNetworkData();
        legacySteam.setSteam(first, 2_000);
        legacySteam.setSteam(second, 500);
        CompoundTag steamTag = legacySteam.save(new CompoundTag());

        var party = new dev.ftb.mods.ftbteams.data.PartyTeam(manager, UUID.randomUUID());
        var firstTeam = new dev.ftb.mods.ftbteams.data.PlayerTeam(manager, first);
        var secondTeam = new dev.ftb.mods.ftbteams.data.PlayerTeam(manager, second);
        firstTeam.setEffectiveTeam(party);
        secondTeam.setEffectiveTeam(party);
        known.put(first, firstTeam);
        known.put(second, secondTeam);
        known.put(solo, new dev.ftb.mods.ftbteams.data.PlayerTeam(manager, solo));
        try {
            var energy = new NexusEnergyNetwork(energyTag);
            h.assertTrue(energy.networkOf(first).equals(party.getId()), "a party member's network is the party's");
            h.assertTrue(energy.getExactEnergy(first).equals(BigInteger.valueOf(100)),
                    "a member's network moves when he is looked up (the offline sweep needs a registered party)");
            h.assertTrue(energy.networkOf(second).equals(party.getId()) &&
                    energy.getExactEnergy(first).equals(BigInteger.valueOf(150)), "members' EU is summed");
            h.assertTrue(energy.getExactEnergy(second).equals(BigInteger.valueOf(150)), "and read by every member");
            h.assertTrue(energy.getMaxCapacity(first).equals(new Int128(3_000)) && energy.getMatrixCount(second) == 2,
                    "both members' Flux Matrices add up in the party (GTIA-D-426)");
            h.assertTrue(energy.getExactEnergy(first).equals(BigInteger.valueOf(150)), "never added twice");
            h.assertTrue(energy.networkOf(solo).equals(solo) &&
                    energy.getExactEnergy(solo).equals(BigInteger.valueOf(30)), "a solo player keeps his network");
            var reloaded = new NexusEnergyNetwork(energy.save(new CompoundTag()));
            h.assertTrue(reloaded.getExactEnergy(second).equals(BigInteger.valueOf(150)) &&
                    party.getId().equals(reloaded.migratedInto(first)), "a reload keeps the total and the record");

            var steam = new SteamNetworkData(steamTag);
            steam.networkOf(first);
            h.assertTrue(steam.getSteam(second) == 2_500 && steam.getSteam(first) == 2_500,
                    "steam is summed into the party once");

            var level = h.getLevel();
            WirelessEnergyManager.setEnergy(level, first, new Int128(777));
            h.assertTrue(WirelessEnergyManager.getEnergy(level, second).equals(new Int128(777)),
                    "the world network is shared by the party");
            SteamWirelessNetworkManager.setUserSteam(level, second, 321);
            h.assertTrue(SteamWirelessNetworkManager.getUserSteam(level, first) == 321,
                    "the world steam pool is shared by the party");

            secondTeam.setEffectiveTeam(secondTeam);
            h.assertTrue(energy.getExactEnergy(second).signum() == 0, "a player who leaves starts empty");
            h.assertTrue(energy.getExactEnergy(first).equals(BigInteger.valueOf(150)),
                    "the energy stays with the team");
            h.assertTrue(steam.getSteam(second) == 0 && steam.getSteam(first) == 2_500, "so does the steam");
        } finally {
            known.remove(first);
            known.remove(second);
            known.remove(solo);
        }
    }
}
