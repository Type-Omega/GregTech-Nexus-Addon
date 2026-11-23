package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAMachines;

/** The GTCEu Large Boilers take the Wireless Steam Output Hatch where a fluid output hatch goes. */
@PrefixGameTestTemplate(false)
@GameTestHolder(GTNACORE.MOD_ID)
public final class LargeBoilerWirelessSteamGameTests {

    private LargeBoilerWirelessSteamGameTests() {}

    @GameTest(template = "empty_16", timeoutTicks = 40)
    public static void largeBoilerTakesWirelessSteamOutput(GameTestHelper helper) {
        for (var boiler : new com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition[] {
                GTMultiMachines.LARGE_BOILER_BRONZE, GTMultiMachines.LARGE_BOILER_STEEL }) {
            var rig = GTNAMultiblockRig.build(helper, boiler);
            helper.assertTrue(rig.controller != null && rig.controller.checkPatternWithLock(),
                    boiler.getName() + " must form from its preview");
            helper.assertTrue(!rig.fluidOutPos.isEmpty(), boiler.getName() + " preview has a fluid output hatch");
            var output = rig.fluidOutPos.get(0);
            for (var hatch : new com.gregtechceu.gtceu.api.machine.MachineDefinition[] {
                    GTNAMachines.WIRELESS_STEAM_OUTPUT_HATCH, GTNAMachines.WIRELESS_STEAM_OUTPUT_HATCH_STEEL }) {
                helper.setBlock(output, hatch.getBlock());
                helper.assertTrue(rig.controller.checkPatternWithLock(),
                        boiler.getName() + " must form with " + hatch.getName() + " as its steam output");
            }
            helper.setBlock(output, GTNAMachines.WIRELESS_STEAM_INPUT_HATCH.getBlock());
            helper.assertTrue(!rig.controller.checkPatternWithLock(),
                    boiler.getName() + " must not take a wireless steam input as its output");
        }
        helper.succeed();
    }
}
