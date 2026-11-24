package com.raishxn.gtna.common.data;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.tooltip.GTNATooltips;

/**
 * Tooltip of every GTNA multiblock: the GTNH-style layout of the lib built by {@link GTNATooltips} (GTIA-D-410,
 * D-432). Runs after every other tooltip pass and replaces the builder, so the multiblock shows only this layout. A
 * multiblock without a lib tooltip gets none, and {@code everyMultiblockUsesTheStandardTooltip} fails.
 */
public final class GTNAMultiblockTooltips {

    private GTNAMultiblockTooltips() {}

    public static void applyAll() {
        for (MachineDefinition definition : GTRegistries.MACHINES) {
            if (!(definition instanceof MultiblockMachineDefinition) ||
                    !GTNACORE.MOD_ID.equals(definition.getId().getNamespace())) {
                continue;
            }
            String path = definition.getId().getPath();
            if (GTNATooltips.has(path)) {
                definition.setTooltipBuilder((stack, components) -> components.addAll(GTNATooltips.build(path)));
            }
        }
    }

    /** Whether a GTNA multiblock registry path has a tooltip from this class. */
    public static boolean has(String path) {
        return GTNATooltips.has(path);
    }
}
