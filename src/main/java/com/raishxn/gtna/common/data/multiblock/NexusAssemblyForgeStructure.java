package com.raishxn.gtna.common.data.multiblock;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.world.item.DyeColor;

import com.raishxn.gtna.common.data.GTNABlocks;
import com.raishxn.gtna.common.data.GTNAMachines2;
import com.raishxn.gtna.common.data.GTNAMaterials;

import static com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.EXPORT_ITEMS;
import static com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.INPUT_ENERGY;
import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/**
 * Nexus Assembly Forge, "Orbital Forge of Crossed Rings" (D-437, proposal A): a chamfered cross base, two
 * crossed rings with an equatorial band and an octahedral core on a europium spindle. Hatches only go on the 5x5
 * front panel ({@code D}); the plain zirconia behind it ({@code d}) shares the panel's zirconia predicate, so both
 * count toward the {@link #MIN_ZIRCONIA} minimum.
 */
public final class NexusAssemblyForgeStructure {

    public static final String RESOURCE = "nexus_molecular_forge";
    public static final int MIN_ZIRCONIA = 20;

    private NexusAssemblyForgeStructure() {}

    public static BlockPattern create(MultiblockMachineDefinition definition) {
        // EN: one predicate instance: GTCEu counts global minimums per SimplePredicate, so 'd' counts toward the 20.
        // PT: uma instância só: o GTCEu conta mínimos globais por SimplePredicate, então o 'd' conta para os 20.
        // ZH: 同一个谓词实例：GTCEu 按 SimplePredicate 统计全局最小数量，因此 'd' 也计入这 20 个。
        TraceabilityPredicate zirconia = blocks(
                GTNABlocks.ZIRCONIA_CERAMIC_HIGH_STRENGTH_BENDING_RESISTANCE_MECHANICAL_BLOCK.get())
                .setMinGlobalLimited(MIN_ZIRCONIA);
        var panel = zirconia.or(abilities(INPUT_ENERGY).setMaxGlobalLimited(2)).or(abilities(EXPORT_ITEMS));
        if (GTNAMachines2.ME_CRAFT_PATTERN_HATCH != null) {
            panel = panel.or(blocks(GTNAMachines2.ME_CRAFT_PATTERN_HATCH.getBlock()));
        }
        return TextAisles.into(FactoryBlockPattern.start(), RESOURCE)
                .where('B', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTNAMaterials.HastelloyN)))
                .where('C', blocks(GTNABlocks.OXIDATION_RESISTANT_HASTELLOY_N_MECHANICAL_CASING.get()))
                .where('D', panel)
                .where('d', zirconia)
                .where('E', blocks(GTNABlocks.NAQUADAH_BOROSILICATE_GLASS.get()))
                .where('F', blocks(GTNABlocks.MAGTECH_CASING.get()))
                .where('G', blocks(GTNABlocks.PROCESS_MACHINE_CASING.get()))
                .where('H', blocks(GTBlocks.CASING_ASSEMBLY_LINE.get()))
                .where('I', blocks(GTBlocks.HIGH_POWER_CASING.get()))
                .where('J', blocks(GTNABlocks.COMPRESSOR_CONTROLLER_CASING.get()))
                .where('K', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Europium)))
                .where('L', blocks(GTNABlocks.EXTREME_DENSITY_CASING.get()))
                .where('M', controller(blocks(definition.get())))
                .where('P', blocks(GTBlocks.LAMPS.get(DyeColor.PURPLE).get()))
                .where(' ', any())
                .build();
    }
}
