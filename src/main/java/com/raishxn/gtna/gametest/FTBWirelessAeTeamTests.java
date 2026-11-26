package com.raishxn.gtna.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.integration.ae2.wireless.WirelessAeContent;
import com.raishxn.gtna.integration.ae2.wireless.WirelessNetworkBindingToolItem;

import java.util.Map;
import java.util.UUID;

/** Optional FTB Teams body of {@link WirelessAeTeamGameTests}: party members share the network, outsiders do not. */
final class FTBWirelessAeTeamTests {

    private FTBWirelessAeTeamTests() {}

    @SuppressWarnings("unchecked")
    static void run(GameTestHelper h) throws ReflectiveOperationException {
        var manager = (dev.ftb.mods.ftbteams.data.TeamManagerImpl) dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api()
                .getManager();
        var knownField = manager.getClass().getDeclaredField("knownPlayers");
        knownField.setAccessible(true);
        var known = (Map<UUID, dev.ftb.mods.ftbteams.api.Team>) knownField.get(manager);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), outsider = UUID.randomUUID();
        var party = new dev.ftb.mods.ftbteams.data.PartyTeam(manager, UUID.randomUUID());
        var firstTeam = new dev.ftb.mods.ftbteams.data.PlayerTeam(manager, first);
        var secondTeam = new dev.ftb.mods.ftbteams.data.PlayerTeam(manager, second);
        firstTeam.setEffectiveTeam(party);
        secondTeam.setEffectiveTeam(party);
        known.put(first, firstTeam);
        known.put(second, secondTeam);
        known.put(outsider, new dev.ftb.mods.ftbteams.data.PlayerTeam(manager, outsider));
        try {
            var rig = new WirelessAeTestRig(h, WirelessAeTestRig.newPlayer(h, first));
            var member = WirelessAeTestRig.newPlayer(h, second);
            var stranger = WirelessAeTestRig.newPlayer(h, outsider);
            var memberTool = new ItemStack(WirelessAeContent.WIRELESS_NETWORK_BINDING_TOOL.get());
            var strangerTool = new ItemStack(WirelessAeContent.WIRELESS_NETWORK_BINDING_TOOL.get());
            rig.select(member, memberTool);
            h.assertTrue(WirelessNetworkBindingToolItem.savedNetwork(memberTool) != null,
                    "a party member must be able to use the network of the party");
            rig.select(stranger, strangerTool);
            h.assertTrue(WirelessNetworkBindingToolItem.savedNetwork(strangerTool) == null,
                    "a player outside the party must not use its network");
            // The network follows the team, not the player: leaving the party takes the access away.
            secondTeam.setEffectiveTeam(secondTeam);
            var leftTool = new ItemStack(WirelessAeContent.WIRELESS_NETWORK_BINDING_TOOL.get());
            rig.select(member, leftTool);
            h.assertTrue(WirelessNetworkBindingToolItem.savedNetwork(leftTool) == null,
                    "a player who left the party must lose the network");
        } finally {
            known.remove(first);
            known.remove(second);
            known.remove(outsider);
        }
    }
}
