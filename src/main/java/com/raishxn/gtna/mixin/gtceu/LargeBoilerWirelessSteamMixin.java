package com.raishxn.gtna.mixin.gtceu;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;

import com.raishxn.gtna.api.machine.multiblock.GTNAPartAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Lets the GTCEu Large Boilers send their steam through a Wireless Steam Output Hatch, in the same slot as a fluid
 * output hatch. The hatch stays a steam-only part: other multiblocks still reject it.
 */
@Mixin(GTMachineUtils.class)
public abstract class LargeBoilerWirelessSteamMixin {

    // EN: Target is the pattern lambda of registerLargeBoiler in GTCEu 7.5.3; one predicate for both hatches keeps
    // GTCEu's "at least one output" limit shared instead of requiring each.
    // PT: O alvo é o lambda do pattern de registerLargeBoiler no GTCEu 7.5.3; um só predicado para os dois hatches
    // mantém o limite "ao menos uma saída" do GTCEu compartilhado, em vez de exigir cada um.
    // ZH: 目标是 GTCEu 7.5.3 中 registerLargeBoiler 的结构 lambda；两种仓共用一个谓词，使 GTCEu 的“至少一个输出”
    // 限制由两者共享，而不是各自都必须存在。
    @ModifyArg(method = "lambda$registerLargeBoiler$47",
               at = @At(value = "INVOKE",
                        target = "Lcom/gregtechceu/gtceu/api/pattern/Predicates;abilities([Lcom/gregtechceu/gtceu/api/machine/multiblock/PartAbility;)Lcom/gregtechceu/gtceu/api/pattern/TraceabilityPredicate;"),
               remap = false)
    private static PartAbility[] gtna$wirelessSteamOutput(PartAbility[] abilities) {
        if (abilities.length == 1 && abilities[0] == PartAbility.EXPORT_FLUIDS)
            return new PartAbility[] { PartAbility.EXPORT_FLUIDS, GTNAPartAbility.STEAM_EXPORT_FLUIDS };
        return abilities;
    }
}
