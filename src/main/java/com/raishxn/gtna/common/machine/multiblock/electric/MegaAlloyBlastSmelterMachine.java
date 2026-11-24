package com.raishxn.gtna.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

/**
 * GTOCore Mega Alloy Blast Smelter: the coiled, parallel Alloy Blast Smelter with GTO's 0.8× EU
 * and 0.6× duration bonus.
 * <p>
 * GTO applies its bonus through the native (closed-source) gtolib
 * {@code GTORecipeModifiers.UPGRADE_GCYM_OVERCLOCKING}, which cannot be read. The GTNA port keeps
 * the documented 0.8×/0.6× multipliers, the Parallel Hatch and the standard Alloy Blast coil
 * overclock/temperature gate, which is how GTCEu's own Alloy Blast Smelter behaves.
 */
public final class MegaAlloyBlastSmelterMachine extends CoilWorkableElectricMultiblockMachine {

    // EN: GTO's bonus on every recipe; the coil rules below come from GTCEu's ebfOverclock and are mirrored here so the
    // EN: tooltip can show them (a GameTest checks them against GTCEu).
    // PT: Bônus do GTO em toda receita; as regras de bobina abaixo vêm do ebfOverclock do GTCEu e são espelhadas aqui
    // PT: para a tooltip mostrá-las (um GameTest as confere com o GTCEu).
    // ZH: GTO 对每个配方的加成；下面的线圈规则来自 GTCEu 的 ebfOverclock，在此镜像以便提示显示（GameTest 会与 GTCEu 核对）。
    public static final double EU_MULTIPLIER = 0.8;
    public static final double DURATION_MULTIPLIER = 0.6;
    /** Heat added per voltage tier above MV, on top of the coil temperature. */
    public static final int HEAT_PER_VOLTAGE_TIER = 100;
    /** Every full step of heat above the recipe gives one EU discount; two steps give one perfect overclock. */
    public static final int HEAT_STEP = OverclockingLogic.COIL_EUT_DISCOUNT_TEMPERATURE;
    public static final double EU_DISCOUNT_PER_STEP = 0.95;
    public static final int MAX_PARALLEL_HATCHES = 1;

    public MegaAlloyBlastSmelterMachine(IMachineBlockEntity holder, Object... args) {
        super(holder);
    }

    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof MegaAlloyBlastSmelterMachine)) {
            return ModifierFunction.NULL;
        }
        ModifierFunction parallel = GTRecipeModifiers.hatchParallel(machine, recipe);
        ModifierFunction overclock = GTRecipeModifiers.ebfOverclock(machine, recipe);
        ModifierFunction bonus = ModifierFunction.builder()
                .eutMultiplier(EU_MULTIPLIER)
                .durationMultiplier(DURATION_MULTIPLIER)
                .build();
        return parallel.compose(bonus).compose(overclock);
    }
}
