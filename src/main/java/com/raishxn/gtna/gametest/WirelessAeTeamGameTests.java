package com.raishxn.gtna.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** D-444 with FTB Teams: the network belongs to the effective team of the player who placed the core. */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class WirelessAeTeamGameTests {

    private WirelessAeTeamGameTests() {}

    @GameTest(template = "empty_12", timeoutTicks = 200)
    public static void wirelessNetworkBelongsToTheFtbTeamOfTheCoresOwner(GameTestHelper h)
                                                                                           throws ReflectiveOperationException {
        if (com.gregtechceu.gtceu.GTCEu.Mods.isFTBTeamsLoaded()) FTBWirelessAeTeamTests.run(h);
        h.succeed();
    }
}
