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

import com.raishxn.gtna.api.machine.multiblock.GTNAPartAbility;
import com.raishxn.gtna.common.data.GTNABlocks;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/**
 * ME Storage, "Data Conduit" (GTIA-D-437, proposal B): an octagonal data duct on a titanium keel, cores behind
 * laminated glass. Each repeated slice holds {@link #CORES_PER_SLICE} cores, from 1 to {@link #MAX_SLICES} slices.
 */
public final class MEStorageStructure {

    public static final String RESOURCE = "me_storage";
    public static final int CORES_PER_SLICE = 6;
    // EN: 128 slices x 6 cores = 768, the GTO ME Storage contract the machine and its tooltip keep.
    // PT: 128 fatias x 6 núcleos = 768, o contrato do ME Storage do GTO que a máquina e a tooltip mantêm.
    // ZH: 128 段 × 6 个核心 = 768，沿用 GTO ME 存储的约定，机器与提示保持一致。
    public static final int MAX_SLICES = 128;
    public static final int MAX_CORES = CORES_PER_SLICE * MAX_SLICES;

    private MEStorageStructure() {}

    public static BlockPattern create(MultiblockMachineDefinition definition) {
        return TextAisles.into(FactoryBlockPattern.start(), RESOURCE)
                .where('A', blocks(GTBlocks.COMPUTER_HEAT_VENT.get()))
                .where('B', blocks(GTBlocks.COMPUTER_CASING.get()))
                .where('C', absCasing())
                .where('D', blocks(GTBlocks.COMPUTER_CASING.get())
                        .or(abilities(GTNAPartAbility.ME_STORAGE_ACCESS).setExactLimit(1)))
                .where('E', controller(blocks(definition.get())))
                .where('F', blocks(ChemicalHelper.getBlock(TagPrefix.frameGt, GTMaterials.Titanium)))
                .where('G', blocks(GTBlocks.HIGH_POWER_CASING.get()))
                .where('H', blocks(
                        GTNABlocks.LITHIUM_OXIDE_CERAMIC_HEAT_RESISTANT_SHOCK_RESISTANT_MECHANICAL_CUBE.get()))
                .where('L', blocks(GTBlocks.LAMPS.get(DyeColor.CYAN).get()))
                .where('V', blocks(GTBlocks.CASING_LAMINATED_GLASS.get()))
                .where('c', blocks(GTNABlocks.T1_ME_STORAGE_CORE.get(), GTNABlocks.T2_ME_STORAGE_CORE.get(),
                        GTNABlocks.T3_ME_STORAGE_CORE.get(), GTNABlocks.T4_ME_STORAGE_CORE.get(),
                        GTNABlocks.T5_ME_STORAGE_CORE.get()))
                .where(' ', any())
                .build();
    }

    private static TraceabilityPredicate absCasing() {
        return blocks(GTNABlocks.ABS_BLACK_CASING.get(), GTNABlocks.ABS_BLUE_CASING.get(),
                GTNABlocks.ABS_BROWN_CASING.get(), GTNABlocks.ABS_GREEN_CASING.get(),
                GTNABlocks.ABS_GREY_CASING.get(), GTNABlocks.ABS_LIME_CASING.get(),
                GTNABlocks.ABS_ORANGE_CASING.get(), GTNABlocks.ABS_RED_CASING.get(),
                GTNABlocks.ABS_WHITE_CASING.get(), GTNABlocks.ABS_YELLOW_CASING.get(),
                GTNABlocks.ABS_CYAN_CASING.get(), GTNABlocks.ABS_MAGENTA_CASING.get(),
                GTNABlocks.ABS_PINK_CASING.get(), GTNABlocks.ABS_PURPLE_CASING.get(),
                GTNABlocks.ABS_LIGHT_BULL_CASING.get(), GTNABlocks.ABS_LIGHT_GREY_CASING.get());
    }
}
