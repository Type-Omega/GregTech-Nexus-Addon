package com.raishxn.gtna.api.machine.feature.eyeofharmony;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Serial operation rules adapted from GTNH GT5-Unofficial a3e1e11241a814c9fa0dd0973d5699548428f689,
 * MTEEyeOfHarmony and EyeOfHarmonyRecipe/Storage (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 * No world, inventories, energy account or random generator is touched while making a plan.
 * Tiers are zero based. Parallel Astral Array rules are intentionally separate.
 */
public final class EyeOfHarmonyMath {

    // EN: Balance numbers of the GTNH Eye of Harmony, kept as in upstream; the controller tooltip reads them too.
    // PT: Números de balanço do Eye of Harmony do GTNH, mantidos como no original; a tooltip do controlador os lê.
    // ZH: GTNH 鸿蒙之眼的平衡数值，与上游一致；控制器提示也读取这些值。
    /** Highest field generator tier, zero based (nine tiers). */
    public static final int MAX_FIELD_TIER = 8;
    public static final int MAX_PLANET_TIER = 9;
    /** Time factor per Spacetime Compression tier above the recipe (multiplicative). */
    public static final double COMPRESSION_TIME_FACTOR = 0.97;
    /** Time factor per Time Dilation tier (multiplicative). */
    public static final double ACCELERATION_TIME_FACTOR = 0.5;
    /** Success chance lost per Time Dilation tier (additive). */
    public static final double ACCELERATION_CHANCE_LOSS = 0.0925;
    /** Success chance gained per Stabilisation tier (additive). */
    public static final double STABILISATION_CHANCE_GAIN = 0.05;
    /** Yield lost per Stabilisation tier (additive). */
    public static final double STABILISATION_YIELD_LOSS = 0.05;
    /** Output EU lost per Stabilisation tier below the highest (additive). */
    public static final double STABILISATION_EU_LOSS = 0.05;
    /** Base chance lost per planet tier. */
    public static final double PLANET_CHANCE_LOSS = 0.05;
    public static final long BASE_SECONDS = 18_000L;
    /** Run time grows by this factor per planet tier. */
    public static final double PLANET_TIME_GROWTH = 1.4;
    /** Hydrogen and Helium each, in mB, per planet tier plus one. */
    public static final long GAS_PER_TIER = 1_000_000_000L;
    /** EU charged per tick of the base run time, on top of the plasma cost. */
    public static final long EU_PER_BASE_TICK = (1L << 19) + 100_000_000_000L;
    /** Output EU = startup EU x (RETURN_BASE + planet tier / RETURN_TIER_DIVISOR). */
    public static final double RETURN_BASE = 0.6;
    public static final double RETURN_TIER_DIVISOR = 10.0;
    public static final long FAILURE_SPACETIME = 14_400L;
    public static final double OVERFLOW_SCALE = 30;
    public static final int MAX_CIRCUIT = 24;
    /** Each circuit level multiplies the startup EU by 4 (a two-bit shift). */
    public static final int CIRCUIT_COST_FACTOR = 4;

    private EyeOfHarmonyMath() {}

    /** Plasma cost must come from the audited output catalog, not a guessed constant. */
    public record Program(int rocketTier, int requiredCompression, long baseTicks, long hydrogen,
                          long helium, long startupEU, long returnEU, double baseChance) {

        public Program {
            if (rocketTier < 0 || rocketTier > MAX_PLANET_TIER || requiredCompression < 0 ||
                    requiredCompression > MAX_FIELD_TIER ||
                    baseTicks <= 0 || hydrogen <= 0 || helium <= 0 || startupEU <= 0 || returnEU < 0 ||
                    !Double.isFinite(baseChance) || baseChance < 0 || baseChance > 1) {
                throw new IllegalArgumentException("Invalid planetary program");
            }
        }

        public static Program overworld(long plasmaEU) {
            return planet(0, plasmaEU);
        }

        /**
         * GTNH {@code EyeOfHarmonyRecipeStorage} for a dimension at {@code blockRocketTier}: 18000 s * 1.4^tier,
         * 10^9 * (tier + 1) of each gas, base chance 1 - 0.05 * tier and return efficiency 0.6 + tier / 10. The
         * recipe tier (failed SpaceTime, required compression) is tier - 1, with tier 0 kept at 0.
         */
        public static Program planet(int blockRocketTier, long plasmaEU) {
            if (plasmaEU < 0) throw new IllegalArgumentException("Negative plasma cost");
            long ticks = Math.multiplyExact(miningSeconds(blockRocketTier), 20L);
            long cost = Math.addExact(plasmaEU, Math.multiplyExact(ticks, EU_PER_BASE_TICK));
            int recipeTier = recipeTier(blockRocketTier);
            long gas = Math.multiplyExact(GAS_PER_TIER, blockRocketTier + 1L);
            // Preserve the upstream floating point multiplication followed by long truncation.
            return new Program(recipeTier, Math.min(MAX_FIELD_TIER, recipeTier), ticks, gas, gas, cost,
                    (long) (cost * (RETURN_BASE + blockRocketTier / RETURN_TIER_DIVISOR)),
                    1.0 - PLANET_CHANCE_LOSS * blockRocketTier);
        }

        public static int recipeTier(int blockRocketTier) {
            return Math.max(blockRocketTier, 1) - 1;
        }

        public static long miningSeconds(int blockRocketTier) {
            if (blockRocketTier < 0 || blockRocketTier > MAX_PLANET_TIER)
                throw new IllegalArgumentException("Rocket tier 0-9");
            // GTUtility.powInt(1.4, tier): same multiplication order, truncated like the upstream cast.
            double base = PLANET_TIME_GROWTH, result = 1;
            for (int exponent = blockRocketTier; exponent > 0; exponent >>= 1) {
                if ((exponent & 1) == 1) result *= base;
                base *= base;
            }
            return (long) (BASE_SECONDS * result);
        }
    }

    public record Fields(int compression, int acceleration, int stabilisation) {

        public Fields {
            if (compression < 0 || compression > MAX_FIELD_TIER || acceleration < 0 ||
                    acceleration > MAX_FIELD_TIER || stabilisation < 0 || stabilisation > MAX_FIELD_TIER) {
                throw new IllegalArgumentException("Unformed or invalid fields");
            }
        }
    }

    /** The GTNH success sentinel is Double.MIN_VALUE; fresh machines start with both values zero. */
    public record History(double lastChance, double pity) {

        public History {
            if (!Double.isFinite(lastChance) || lastChance < 0 || lastChance > 1 ||
                    !Double.isFinite(pity)) {
                throw new IllegalArgumentException("Invalid chance history");
            }
        }

        public static History fresh() {
            return new History(0, 0);
        }
    }

    /** Values are frozen before debit; consume ALL buffered gas once when committing this plan. */
    public record Plan(Program program, Fields fields, int circuit, long bufferedHydrogen,
                       long bufferedHelium, int durationTicks, BigInteger debitEU, BigInteger creditEU,
                       double hydrogenPenalty, double heliumPenalty, double chance, double yield,
                       History historyAtStart) {

        public BigInteger netEU() {
            return creditEU.subtract(debitEU);
        }

        public long outputAmount(long baseAmount) {
            if (baseAmount < 0) throw new IllegalArgumentException("Negative output");
            // Mirrors GTNH's compound long *= double. Apply only to successful output.
            return (long) (baseAmount * yield);
        }
    }

    /** Credit EU is owed on failure too. Store the resolved result so reload never rolls again. */
    public record Result(boolean success, long failedSpaceTime, BigInteger creditEU, History history) {}

    public static Plan plan(Program program, Fields fields, int circuitSetting, long hydrogen, long helium,
                            History history) {
        Objects.requireNonNull(program);
        Objects.requireNonNull(fields);
        Objects.requireNonNull(history);
        if (fields.compression < program.requiredCompression) {
            throw new IllegalArgumentException("Insufficient compression");
        }
        if (hydrogen < program.hydrogen || helium < program.helium) {
            throw new IllegalArgumentException("Insufficient gas");
        }
        int circuit = Math.max(0, Math.min(MAX_CIRCUIT, circuitSetting));
        double hydrogenPenalty = overflowPenalty(hydrogen, program.hydrogen);
        double heliumPenalty = overflowPenalty(helium, program.helium);
        double baseChance = program.baseChance - fields.acceleration * ACCELERATION_CHANCE_LOSS +
                fields.stabilisation * STABILISATION_CHANCE_GAIN;
        double pity = history.pity == Double.MIN_VALUE ? baseChance : history.pity;
        // Upstream compares BEFORE applying overflow and uses exact double equality.
        double chance = baseChance == history.lastChance && pity >= 1 ? 1 : baseChance;
        chance = clamp(chance - hydrogenPenalty - heliumPenalty);
        double yield = clamp(1 - fields.stabilisation * STABILISATION_YIELD_LOSS - hydrogenPenalty - heliumPenalty);
        int duration = (int) Math.max(program.baseTicks * Math.pow(ACCELERATION_TIME_FACTOR, fields.acceleration) *
                compressionDiscount(fields.compression - program.requiredCompression) * Math.pow(2, -circuit), 1);
        BigInteger debit = startupDebit(program, circuit);
        BigInteger credit = BigInteger.valueOf(
                (long) (program.returnEU * (1 - (MAX_FIELD_TIER - fields.stabilisation) * STABILISATION_EU_LOSS)));
        return new Plan(program, fields, circuit, hydrogen, helium, duration, debit, credit,
                hydrogenPenalty, heliumPenalty, chance, yield, new History(history.lastChance, pity));
    }

    /** Shared by controller quotes and execution so balance changes cannot desynchronize the UI. */
    public static BigInteger startupDebit(Program program, int circuitSetting) {
        int circuit = Math.max(0, Math.min(MAX_CIRCUIT, circuitSetting));
        // GTNH: each circuit level multiplies the startup cost by 4 (4^24 at circuit 24).
        return BigInteger.valueOf(program.startupEU).shiftLeft(2 * circuit);
    }

    public static Result resolve(Plan plan, int roll) {
        Objects.requireNonNull(plan);
        if (roll < 0 || roll >= 10_000) throw new IllegalArgumentException("Roll must be in [0, 10000)");
        boolean success = roll < (int) (10_000 * plan.chance);
        double pity = success ? Double.MIN_VALUE :
                plan.historyAtStart.lastChance == plan.chance ?
                        plan.historyAtStart.pity + (1 - plan.chance) * plan.chance : plan.chance;
        long failedSpaceTime = success ? 0 :
                (long) (plan.chance * FAILURE_SPACETIME * Math.pow(2, plan.program.rocketTier + 1));
        return new Result(success, failedSpaceTime, plan.creditEU, new History(plan.chance, pity));
    }

    public static double overflowPenalty(long stored, long required) {
        if (required <= 0 || stored < required) throw new IllegalArgumentException("Insufficient gas");
        double excess = (double) stored / required - 1;
        double scaled = OVERFLOW_SCALE * excess;
        return 1 - Math.exp(-(scaled * scaled));
    }

    private static double compressionDiscount(int exponent) {
        // Same multiplication order as GTUtility.powInt, preserving duration truncation boundaries.
        double base = COMPRESSION_TIME_FACTOR;
        double result = 1;
        while (exponent > 0) {
            if ((exponent & 1) == 1) result *= base;
            base *= base;
            exponent >>= 1;
        }
        return result;
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
