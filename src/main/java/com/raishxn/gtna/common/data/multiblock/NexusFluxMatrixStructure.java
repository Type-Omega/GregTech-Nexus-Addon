package com.raishxn.gtna.common.data.multiblock;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.world.item.DyeColor;

import com.raishxn.gtna.common.data.GTNABlocks;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/**
 * Nexus Flux Matrix, "Tesla Coil" (D-437, proposal B): a wide steel base, the capacitor column behind
 * borosilicate glass, loose busbars and a discharge toroid on top. Aisles go up, so the repeated aisle is one floor
 * of {@link #CAPACITORS_PER_FLOOR} capacitors, from {@link #MIN_FLOORS} to {@link #MAX_FLOORS} floors.
 */
public final class NexusFluxMatrixStructure {

    public static final String RESOURCE = "nexus_flux_matrix";
    public static final int CAPACITORS_PER_FLOOR = 25;
    public static final int MIN_FLOORS = 2;
    // EN: 30 floors x 25 capacitors = 750 = NexusFluxMatrixMachine.MAX_CAPACITORS, the unlimited threshold.
    // PT: 30 andares x 25 capacitores = 750 = NexusFluxMatrixMachine.MAX_CAPACITORS, o limiar do modo ilimitado.
    // ZH: 30 层 × 25 个电容 = 750 = NexusFluxMatrixMachine.MAX_CAPACITORS，即无限模式的门槛。
    public static final int MAX_FLOORS = 30;

    private NexusFluxMatrixStructure() {}

    public static BlockPattern create(MultiblockMachineDefinition definition) {
        var pattern = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.FRONT,
                RelativeDirection.UP);
        return TextAisles.into(pattern, RESOURCE)
                .where('A', blocks(GTBlocks.CASING_STEEL_SOLID.get()))
                .where('B', controller(blocks(definition.get())))
                .where('C', blocks(GTNABlocks.BOROSILICATE_GLASS_BLOCK.get()))
                .where('D', blocks(GTNABlocks.NEXUS_CAPACITOR_LV.get(), GTNABlocks.NEXUS_CAPACITOR_MV.get(),
                        GTNABlocks.NEXUS_CAPACITOR_HV.get(), GTNABlocks.NEXUS_CAPACITOR_EV.get(),
                        GTNABlocks.NEXUS_CAPACITOR_IV.get(), GTNABlocks.NEXUS_CAPACITOR_LUV.get(),
                        GTNABlocks.NEXUS_CAPACITOR_ZPM.get(), GTNABlocks.NEXUS_CAPACITOR_UV.get(),
                        GTNABlocks.NEXUS_CAPACITOR_UHV.get(), GTNABlocks.NEXUS_CAPACITOR_UEV.get(),
                        GTNABlocks.NEXUS_CAPACITOR_UIV.get(), GTNABlocks.NEXUS_CAPACITOR_UXV.get(),
                        GTNABlocks.NEXUS_CAPACITOR_OPV.get(), GTNABlocks.NEXUS_CAPACITOR_MAX.get())
                        // EN: capacitors first, so the preview and the auto-build show them; empty slots still form.
                        // PT: capacitores primeiro, para a preview e o auto-build mostrarem; vagas vazias ainda formam.
                        // ZH: 电容放在前面，预览与自动搭建才会显示它们；空位仍可成型。
                        .or(air()))
                .where('F', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Steel)))
                .where('G', blocks(GTBlocks.CASING_STEEL_GEARBOX.get()))
                .where('H', blocks(GTBlocks.CASING_STEEL_SOLID.get())
                        .or(abilities(PartAbility.OUTPUT_ENERGY).setPreviewCount(1))
                        .or(abilities(PartAbility.INPUT_ENERGY).setPreviewCount(1)))
                .where('L', blocks(GTBlocks.LAMPS.get(DyeColor.ORANGE).get()))
                .where('P', blocks(GTBlocks.CASING_STEEL_PIPE.get()))
                .where(' ', any())
                .build();
    }
}
