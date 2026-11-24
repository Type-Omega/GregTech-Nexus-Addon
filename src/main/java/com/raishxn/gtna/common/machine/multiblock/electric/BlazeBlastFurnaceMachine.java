package com.raishxn.gtna.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** GTOCore Blaze Blast Furnace: 64 parallel, half duration, molten Blaze upkeep. */
public final class BlazeBlastFurnaceMachine extends CoilWorkableElectricMultiblockMachine {

    /** GTO numbers shared with the Cold Ice Freezer: parallels, time factor and the upkeep at MV, in mB/s. */
    public static final int MAX_PARALLEL = 64;
    public static final double DURATION_MULTIPLIER = 0.5;
    public static final int UPKEEP_BASE = 10;

    public BlazeBlastFurnaceMachine(IMachineBlockEntity holder, Object... args) {
        super(holder);
    }

    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof BlazeBlastFurnaceMachine)) return ModifierFunction.NULL;
        int parallel = ParallelLogic.getParallelAmount(machine, recipe, MAX_PARALLEL);
        if (parallel <= 0) return ModifierFunction.NULL;
        return ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(parallel))
                .eutMultiplier(parallel)
                .durationMultiplier(DURATION_MULTIPLIER)
                .parallels(parallel)
                .build();
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (recipe == null || recipe.data.getInt("ebf_temp") > heatingTemperature()) return false;
        return drainBlaze() && super.beforeWorking(recipe);
    }

    @Override
    public boolean onWorking() {
        if (getOffsetTimer() % 20 == 0 && !drainBlaze()) return false;
        return super.onWorking();
    }

    private int heatingTemperature() {
        return getCoilType().getCoilTemperature() +
                CoilWorkableElectricMultipleRecipesMachine.HEAT_PER_TIER * Math.max(0, getTier() - 2);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;
        int index = Math.min(2, textList.size());
        textList.add(index++, Component.translatable("gtna.ui.parallel_max",
                Component.literal("64").withStyle(ChatFormatting.LIGHT_PURPLE))
                .withStyle(ChatFormatting.GRAY));
        textList.add(index++, Component.translatable("gtna.ui.voiding_mode",
                Component.translatable(getVoidingMode().getSerializedName()).withStyle(ChatFormatting.GRAY))
                .withStyle(ChatFormatting.WHITE));
        textList.add(index++, Component.translatable("gtna.ui.heat_capacity",
                Component.literal(FormattingUtil.formatNumbers(heatingTemperature()) + "K")
                        .withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.WHITE));
        if (getRecipeLogic().isIdle() && getRecipeLogic().getLastRecipe() == null) {
            textList.add(index, Component.translatable("gtna.ui.no_recipe_found").withStyle(ChatFormatting.GRAY));
        }
    }

    /** 2^(tier − 2) × the base upkeep, paid every second. */
    public static int upkeep(int tier) {
        return (1 << Math.max(0, Math.min(20, tier - 2))) * UPKEEP_BASE;
    }

    private boolean drainBlaze() {
        int required = upkeep(getTier());
        return FluidUpkeep.consume(this, GTMaterials.Blaze.getFluid(), required);
    }
}
