package com.raishxn.gtna.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** GTIA-D-386: the wireless energy and steam networks follow the FTB team; legacy worlds migrate once. */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class WirelessTeamNetworkGameTests {

    @GameTest(template = "empty_12", timeoutTicks = 20)
    public static void wirelessNetworksMigrateToTheTeamOnce(GameTestHelper h) throws ReflectiveOperationException {
        if (com.gregtechceu.gtceu.GTCEu.Mods.isFTBTeamsLoaded()) FTBWirelessTeamTests.run(h);
        h.succeed();
    }
}
