package com.raishxn.gtna.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

public class DimensionallyTranscendentSteamOvenMachine extends FixedThreadSteamParallelMachine {

    /** GTL numbers: parallels, the time factor (100x faster than a furnace) and two fixed threads. */
    public static final int MAX_PARALLEL = 524288;
    public static final double DURATION = 0.01;
    public static final int THREADS = 2;

    public DimensionallyTranscendentSteamOvenMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, GTRecipeTypes.FURNACE_RECIPES, MAX_PARALLEL, MAX_PARALLEL, DURATION, THREADS, args);
    }
}
