package com.raishxn.gtna.common.machine.multiblock.module.steamElevator;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * GTNL {@code SteamOilDrillModule} port (LGPLv3, original by ScienceNotLeisure).
 *
 * <p>
 * GTNL reads GT's per-dimension underground-oil table and outputs a random vein fluid. 1.20.1's
 * equivalent is GTCEu's bedrock fluid veins, so the module pumps the fluid of the chunk it sits in
 * ({@link BedrockFluidVeinSavedData#getFluidInChunk}) into the module structure's <b>fluid output
 * hatch</b>. Tiers I/II/III (GTNL tiers 2/3/4) increase the yield and shorten the cycle.
 */
public class SteamOilDrillModule extends SteamElevatorModuleMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SteamOilDrillModule.class, SteamElevatorModuleMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @DescSynced
    private int progress;

    public SteamOilDrillModule(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public long getSteamUpkeep() {
        return upkeepForTier(getModuleTier());
    }

    @Override
    public int getModuleProgress() {
        return progress;
    }

    @Override
    public int getModuleMaxProgress() {
        return cycleTicks();
    }

    /** GTNL uses GTValues.VP = V * 30 / 32: 120 / 480 / 1920 for tiers I/II/III. */
    public static long upkeepForTier(int tier) {
        int index = Math.min(GTValues.V.length - 1, Math.max(0, tier));
        return GTValues.V[index] * 30L / 32L;
    }

    /** Base cycle of the first tier (GTNL tier 2); higher tiers divide it. */
    public static final int BASE_CYCLE_TICKS = 1200;
    public static final int BASE_YIELD = 250;
    /** Each roll gives the base yield times 1 to this. */
    public static final int MAX_ROLL_MULTIPLIER = 4;

    public static int cycleTicksFor(int tier) {
        return Math.max(1, BASE_CYCLE_TICKS / (tier - 1));
    }

    public static int baseYieldFor(int tier) {
        return BASE_YIELD * (1 << Math.max(0, tier - 2));
    }

    /** Yield rolls per cycle: one for tier I, three for tier III. */
    public static int rollsFor(int tier) {
        return tier - 1;
    }

    private int cycleTicks() {
        return cycleTicksFor(getModuleTier());
    }

    private int baseYield() {
        return baseYieldFor(getModuleTier());
    }

    @Override
    public void onElevatorTick(SteamElevator elevator) {
        if (!consumeSteam(getSteamUpkeep())) return;
        if (!(getLevel() instanceof ServerLevel level)) return;

        if (++progress < cycleTicks()) return;
        progress = 0;

        int chunkX = SectionPos.blockToSectionCoord(getPos().getX());
        int chunkZ = SectionPos.blockToSectionCoord(getPos().getZ());
        BedrockFluidVeinSavedData data = BedrockFluidVeinSavedData.getOrCreate(level);
        Fluid fluid = data.getFluidInChunk(chunkX, chunkZ);
        if (fluid == null) return;

        int amount = 0;
        for (int i = 0; i < rollsFor(getModuleTier()); i++) {
            amount += baseYield() * (1 + level.random.nextInt(MAX_ROLL_MULTIPLIER));
        }
        FluidStack produced = new FluidStack(fluid, amount);
        if (!canInsertFluid(produced)) return;

        insertFluid(produced);
        // GTNL's underground oil depletes on extraction; mirror that at a gentler rate.
        if (level.random.nextInt(25) == 0) {
            data.depleteVein(chunkX, chunkZ, 0, false);
        }
        markDirty();
    }

    @Override
    protected Widget createModuleUIWidget() {
        WidgetGroup group = screenGroup(150, 50);
        group.addWidget(new LabelWidget(5, 5, () -> "Tier: §b" + getModuleTier() + " §r| Yield: §b" + baseYield()));
        group.addWidget(new LabelWidget(5, 18,
                () -> "Progress: §b" + (progress * 100 / cycleTicks()) + "%%"));
        group.addWidget(new LabelWidget(5, 31, () -> "§7Oil: output hatch"));
        return group;
    }
}
