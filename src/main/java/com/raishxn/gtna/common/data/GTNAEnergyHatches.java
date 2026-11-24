package com.raishxn.gtna.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.machine.multiblock.energy.NexusFluxMatrixMachine;
import com.raishxn.gtna.common.machine.multiblock.part.energy.WirelessDynamoHatchPartMachine;
import com.raishxn.gtna.common.machine.multiblock.part.energy.WirelessEnergyHatchPartMachine;

import java.util.Locale;

import static com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.IS_FORMED;
import static com.raishxn.gtna.api.registry.GTNARegistry.REGISTRATE;

public class GTNAEnergyHatches {

    public static final MachineDefinition[][] WIRELESS_ENERGY_HATCHES = new MachineDefinition[GTValues.MAX + 1][11];
    public static final MachineDefinition[][] WIRELESS_DYNAMO_HATCHES = new MachineDefinition[GTValues.MAX + 1][11];

    public static final MachineDefinition NETWORK_INPUT_TERMINAL = registerNetworkTerminal(
            "wireless_energy_network_input_terminal", false);
    public static final MachineDefinition NETWORK_OUTPUT_TERMINAL = registerNetworkTerminal(
            "wireless_energy_network_output_terminal", true);

    private static MachineDefinition registerNetworkTerminal(String name, boolean output) {
        return REGISTRATE.machine(name,
                holder -> new com.raishxn.gtna.common.machine.multiblock.part.energy.NexusNetworkTerminalPartMachine(
                        holder, output ? com.gregtechceu.gtceu.api.capability.recipe.IO.OUT :
                                com.gregtechceu.gtceu.api.capability.recipe.IO.IN))
                .tier(GTValues.MAX).rotationState(RotationState.ALL)
                .abilities(output ? com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.OUTPUT_ENERGY :
                        com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.INPUT_ENERGY,
                        output ? com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.OUTPUT_LASER :
                                com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.INPUT_LASER)
                .langValue(output ? "Nexus Wireless Network Output Terminal" : "Nexus Wireless Network Input Terminal")
                .modelProperty(IS_FORMED, false)
                .model((ctx, prov, builder) -> {
                    var model = prov.models().nested().parent(prov.models().getExistingFile(
                            com.raishxn.gtna.GTNACORE.id("block/machine/part/wireless_energy_network_terminal")))
                            .texture("all", com.raishxn.gtna.GTNACORE.id("block/casings/wireless_terminate_casing"));
                    builder.forAllStatesModels(state -> model);
                })
                .tooltips(Component.translatable("gtna.network_terminal." + (output ? "output" : "input")),
                        Component.translatable("gtna.network_terminal.limit"),
                        Component.translatable("gtna.network_terminal.binding"))
                .register();
    }

    public static final MultiblockMachineDefinition NEXUS_FLUX_MATRIX = REGISTRATE
            .multiblock("nexus_flux_matrix", NexusFluxMatrixMachine::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .recipeType(GTRecipeTypes.DUMMY_RECIPES)
            .appearanceBlock(() -> com.gregtechceu.gtceu.common.data.GTBlocks.CASING_STEEL_SOLID.get())
            .pattern(com.raishxn.gtna.common.data.multiblock.NexusFluxMatrixStructure::create)
            .modelProperty(com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.RECIPE_LOGIC_STATUS,
                    com.gregtechceu.gtceu.api.machine.trait.RecipeLogic.Status.IDLE)
            .model(com.gregtechceu.gtceu.common.data.models.GTMachineModels.createWorkableCasingMachineModel(
                    GTCEu.id("block/casings/solid/machine_casing_solid_steel"),
                    GTCEu.id("block/multiblock/implosion_compressor"))
                    .andThen(builder -> builder.addDynamicRenderer(
                            com.raishxn.gtna.client.renderer.machine.NexusStructureRenderers.FluxRender::new)))
            .register();

    public static void init() {
        for (int tier = GTValues.LV; tier <= GTValues.MAX; tier++) {
            for (int ampExp = 0; ampExp <= 10; ampExp++) {
                int amps = (int) Math.pow(4, ampExp);
                registerWirelessHatches(tier, amps, ampExp);
            }
        }
    }

    private static void registerWirelessHatches(int tier, int amps, int ampExp) {
        String tierName = GTValues.VN[tier].toLowerCase(Locale.ROOT);

        // E.g. wireless_energy_hatch_1A_lv
        String outName = "wireless_energy_hatch_" + amps + "a_" + tierName;
        String inName = "wireless_dynamo_hatch_" + amps + "a_" + tierName;

        String upperTierName = GTValues.VN[tier];
        long capacity = GTValues.V[tier] * 64L * amps;
        String capacityStr = formatCapacity(capacity);

        WIRELESS_ENERGY_HATCHES[tier][ampExp] = REGISTRATE
                .machine(outName, holder -> new WirelessEnergyHatchPartMachine(holder, tier, amps))
                .langValue(GTValues.VNF[tier] + " " + amps + "A Wireless Energy Hatch")
                .tier(tier)
                .rotationState(RotationState.ALL)
                .abilities(com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.INPUT_ENERGY)
                .modelProperty(IS_FORMED, false)
                .model((ctx, prov, builder) -> {
                    var model = prov.models()
                            .withExistingParent(ctx.getName(), GTCEu.id("block/machine/template/part/hatch_machine"))
                            .texture("overlay",
                                    new net.minecraft.resources.ResourceLocation("gtna",
                                            "block/overlay/machine/overlay_steam_wireless_out"))
                            .texture("side", GTCEu.id("block/casings/voltage/" + tierName + "/side"))
                            .texture("top", GTCEu.id("block/casings/voltage/" + tierName + "/top"))
                            .texture("bottom", GTCEu.id("block/casings/voltage/" + tierName + "/bottom"))
                            .texture("particle", GTCEu.id("block/casings/voltage/" + tierName + "/side"));
                    builder.partialState().setModel(model);
                })
                .tooltips(
                        Component.translatable("gtna.machine.wireless_energy_hatch.tooltip"),
                        Component.translatable("gtna.machine.wireless_energy_hatch.tier_info", upperTierName,
                                String.valueOf(amps)),
                        Component.translatable("gtna.machine.wireless_hatch.capacity", capacityStr),
                        Component.translatable("gtna.machine.wireless_hatch.auto_bind"))
                .register();

        WIRELESS_DYNAMO_HATCHES[tier][ampExp] = REGISTRATE
                .machine(inName, holder -> new WirelessDynamoHatchPartMachine(holder, tier, amps))
                .langValue(GTValues.VNF[tier] + " " + amps + "A Wireless Dynamo Hatch")
                .tier(tier)
                .rotationState(RotationState.ALL)
                .abilities(com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.OUTPUT_ENERGY)
                .modelProperty(IS_FORMED, false)
                .model((ctx, prov, builder) -> {
                    var model = prov.models()
                            .withExistingParent(ctx.getName(), GTCEu.id("block/machine/template/part/hatch_machine"))
                            .texture("overlay",
                                    new net.minecraft.resources.ResourceLocation("gtna",
                                            "block/overlay/machine/overlay_steam_wireless_in"))
                            .texture("side", GTCEu.id("block/casings/voltage/" + tierName + "/side"))
                            .texture("top", GTCEu.id("block/casings/voltage/" + tierName + "/top"))
                            .texture("bottom", GTCEu.id("block/casings/voltage/" + tierName + "/bottom"))
                            .texture("particle", GTCEu.id("block/casings/voltage/" + tierName + "/side"));
                    builder.partialState().setModel(model);
                })
                .tooltips(
                        Component.translatable("gtna.machine.wireless_dynamo_hatch.tooltip"),
                        Component.translatable("gtna.machine.wireless_dynamo_hatch.tier_info", upperTierName,
                                String.valueOf(amps)),
                        Component.translatable("gtna.machine.wireless_hatch.capacity", capacityStr),
                        Component.translatable("gtna.machine.wireless_hatch.auto_bind"))
                .register();
    }

    private static String formatCapacity(long capacity) {
        if (capacity >= 1_000_000_000L) {
            return String.format("%.1fG", capacity / 1_000_000_000.0);
        } else if (capacity >= 1_000_000L) {
            return String.format("%.1fM", capacity / 1_000_000.0);
        } else if (capacity >= 1_000L) {
            return String.format("%.1fK", capacity / 1_000.0);
        }
        return String.valueOf(capacity);
    }
}
