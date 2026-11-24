package com.raishxn.gtna.common.data.tooltip;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamApiaryModule;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamBeaconModule;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamBeeBreedingModule;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamElevator;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamEntityCrusherModule;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamFlightModule;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamGreenhouseModule;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamMonsterRepellentModule;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamOilDrillModule;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamOreProcessorModule;
import com.raishxn.gtna.common.machine.multiblock.module.steamElevator.SteamWeatherModule;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.util.List;
import java.util.StringJoiner;

import static com.raishxn.gtna.common.data.tooltip.Fmt.num;
import static com.raishxn.gtna.common.data.tooltip.Fmt.seconds;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * Steam Elevator and its modules, GTNL ports (used with permission): the numbers are the module formulas, read per
 * tier; the humour lines keep GTNL's current text where it has one.
 */
final class SteamElevatorTooltips {

    private SteamElevatorTooltips() {}

    private static final String[] ROMAN = { "", "i", "ii", "iii" };

    static void register() {
        GTNATooltips.register("steam_elevator", SteamElevatorTooltips::elevator);
        for (int t = 1; t <= 3; t++) {
            int tier = t;
            GTNATooltips.register("steam_elevator_flight_module_" + ROMAN[t], () -> flight(tier));
            GTNATooltips.register("steam_elevator_weather_module_" + ROMAN[t], () -> weather(tier));
            // EN: Oil drill I to III are module tiers 2 to 4 (GTNL tiers).
            // PT: As sondas de petróleo I a III são os tiers de módulo 2 a 4 (tiers do GTNL).
            // ZH: 石油钻机模块I至III对应模块等级2至4（GTNL等级）。
            GTNATooltips.register("steam_elevator_oil_drill_module_" + ROMAN[t], () -> oilDrill(tier + 1));
            GTNATooltips.register("steam_elevator_entity_crusher_module_" + ROMAN[t], () -> crusher(tier));
            GTNATooltips.register("steam_elevator_monster_repellent_module_" + ROMAN[t], () -> repellent(tier));
            GTNATooltips.register("steam_elevator_beacon_module_" + ROMAN[t], () -> beacon(tier));
        }
        GTNATooltips.register("steam_elevator_greenhouse_module", SteamElevatorTooltips::greenhouse);
        GTNATooltips.register("steam_elevator_ore_processor_module", SteamElevatorTooltips::oreProcessor);
        GTNATooltips.register("steam_elevator_apiary_module", SteamElevatorTooltips::apiary);
        GTNATooltips.register("steam_elevator_bee_breeding_module", SteamElevatorTooltips::beeBreeding);
    }

    private static String mb(long amount) {
        return num(amount) + " mB";
    }

    /** Builder of a module with its purpose line; {@code family} names the shared texts of tiered modules. */
    private static MultiblockTooltipBuilder module(String family) {
        return start("elevator_module").line(shared("elevator." + family + ".purpose")).separator();
    }

    /** Lines every module shares: the slot, the steam upkeep and its own IO. */
    private static MultiblockTooltipBuilder upkeep(MultiblockTooltipBuilder b, long steamPerTick) {
        return b.line(shared("elevator.attached"))
                .line(shared("elevator.upkeep"), value(mb(steamPerTick)))
                .warning(shared("elevator.no_steam"));
    }

    private static List<Component> tiered(MultiblockTooltipBuilder b, String family, int tier) {
        String id = "steam_elevator_" + family + "_module_" + ROMAN[tier];
        return finish(b, id);
    }

    private static List<Component> elevator() {
        String id = "steam_elevator";
        var b = start("steam_elevator").line(own(id, "purpose")).separator()
                .line(own(id, "slots"), value(SteamElevator.moduleSlots()))
                .line(own(id, "free"))
                .line(own(id, "pool"))
                .line(own(id, "io"))
                .line(own(id, "set_out"))
                .warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> flight(int tier) {
        var b = module("flight")
                .line(shared("elevator.flight.range"), value(num(SteamFlightModule.rangeFor(tier)) + " m"),
                        value(SteamFlightModule.RANGE))
                .line(shared("elevator.flight.revoke"))
                .line(shared("elevator.flight.safe"));
        upkeep(b, SteamFlightModule.upkeepFor(tier)).warning(shared("elevator.flight.not_ported"));
        return tiered(b, "flight", tier);
    }

    private static List<Component> weather(int tier) {
        var b = module("weather")
                .line(shared("elevator.weather.circuit"), value(SteamWeatherModule.MODE_CLEAR + 1),
                        value(SteamWeatherModule.MODE_RAIN + 1), value(SteamWeatherModule.MODE_THUNDER + 1))
                .line(shared("elevator.weather.cost"), value(mb(SteamWeatherModule.WEATHER_STEAM_COST)))
                .line(shared("elevator.weather.duration"),
                        value(num(SteamWeatherModule.durationFor(tier) / 72_000.0) + " h"))
                .line(shared("elevator.weather.keep"))
                .line(shared("elevator.attached"))
                .warning(shared("elevator.weather.not_ported"));
        return tiered(b, "weather", tier);
    }

    private static List<Component> oilDrill(int tier) {
        var b = module("oil_drill")
                .line(shared("elevator.oil_drill.cycle"), value(seconds(SteamOilDrillModule.cycleTicksFor(tier))))
                .line(shared("elevator.oil_drill.yield"), value(SteamOilDrillModule.rollsFor(tier)),
                        value(mb(SteamOilDrillModule.baseYieldFor(tier))),
                        value(SteamOilDrillModule.MAX_ROLL_MULTIPLIER))
                .line(shared("elevator.oil_drill.output"))
                .warning(shared("elevator.oil_drill.no_vein"));
        upkeep(b, SteamOilDrillModule.upkeepForTier(tier)).warning(shared("elevator.oil_drill.not_ported"));
        return tiered(b, "oil_drill", tier - 1);
    }

    private static List<Component> crusher(int tier) {
        var b = module("entity_crusher")
                .line(shared("elevator.entity_crusher.catalyst"))
                .line(shared("elevator.entity_crusher.cycle"),
                        value(seconds(SteamEntityCrusherModule.cycleTicksFor(tier))))
                .line(shared("elevator.entity_crusher.double"),
                        value(num(SteamEntityCrusherModule.BASE_DOUBLING_CHANCE) + "%"),
                        value(num(SteamEntityCrusherModule.CHANCE_PER_SPAWNER) + "%"),
                        value(num(SteamEntityCrusherModule.MAX_DOUBLING_CHANCE) + "%"))
                .warning(shared("elevator.full"))
                .line(shared("oc.none"));
        upkeep(b, SteamEntityCrusherModule.upkeepFor(tier)).warning(shared("elevator.entity_crusher.not_ported"));
        return tiered(b, "entity_crusher", tier);
    }

    private static List<Component> repellent(int tier) {
        var b = module("monster_repellent")
                .line(shared("elevator.monster_repellent.range"),
                        value(num(SteamMonsterRepellentModule.rangeFor(tier)) + " m"))
                .warning(shared("elevator.monster_repellent.running"));
        upkeep(b, SteamMonsterRepellentModule.upkeepFor(tier));
        return tiered(b, "monster_repellent", tier);
    }

    private static List<Component> beacon(int tier) {
        var b = module("beacon")
                .line(shared("elevator.beacon.range"), value(num(SteamBeaconModule.rangeFor(tier)) + " m"))
                .line(shared("elevator.beacon.level"), value(ROMAN[tier].toUpperCase(java.util.Locale.ROOT)))
                .line(shared("elevator.beacon.effects"), value(SteamBeaconModule.maxEffectsFor(tier)),
                        value(SteamBeaconModule.effectTypes()))
                .line(shared("elevator.beacon.list"))
                .line(shared("elevator.beacon.refresh"), value(seconds(SteamBeaconModule.REFRESH_TICKS)),
                        value(seconds(SteamBeaconModule.EFFECT_DURATION)))
                .line(shared("elevator.attached"))
                .line(shared("elevator.beacon.upkeep"), value(mb(SteamBeaconModule.upkeepFor(1, tier))))
                .warning(shared("elevator.no_steam"))
                .warning(shared("elevator.beacon.not_ported"));
        return tiered(b, "beacon", tier);
    }

    private static List<Component> greenhouse() {
        String id = "steam_elevator_greenhouse_module";
        var b = start("elevator_module").line(own(id, "purpose")).separator()
                .line(own(id, "cycle"), value(seconds(SteamGreenhouseModule.CYCLE_TICKS)),
                        value(mb(SteamGreenhouseModule.WATER_PER_OPERATION)), value(SteamGreenhouseModule.MAX_GROWN))
                .line(own(id, "area"), value(SteamGreenhouseModule.RANGE),
                        value(SteamGreenhouseModule.VERTICAL_RANGE))
                .line(shared("oc.none"));
        upkeep(b, SteamGreenhouseModule.STEAM_UPKEEP).warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> oreProcessor() {
        String id = "steam_elevator_ore_processor_module";
        StringJoiner times = new StringJoiner(", ");
        for (int mode = 0; mode <= SteamOreProcessorModule.MAX_MODE; mode++) {
            times.add(seconds(SteamOreProcessorModule.cycleTicksFor(mode)));
        }
        var b = start("elevator_module").line(own(id, "purpose")).separator()
                .line(own(id, "chains"))
                .line(own(id, "fluids"))
                .line(own(id, "parallel"), value(SteamOreProcessorModule.maxParallelFor(0)),
                        value(SteamOreProcessorModule.maxParallelFor(SteamOreProcessorModule.MAX_MODE)))
                .line(own(id, "times"), value(SteamOreProcessorModule.MAX_MODE), value(times.toString()))
                .line(own(id, "inputs"))
                .warning(shared("elevator.full"))
                .line(shared("elevator.attached"))
                .line(own(id, "upkeep"), value(mb(SteamOreProcessorModule.STEAM_UPKEEP)))
                .warning(shared("elevator.no_steam"));
        return finish(b, id);
    }

    private static List<Component> apiary() {
        String id = "steam_elevator_apiary_module";
        var b = start("elevator_module").line(own(id, "purpose")).separator()
                .line(own(id, "cycle"), value(seconds(SteamApiaryModule.CYCLE_TICKS)),
                        value(SteamApiaryModule.COMBS_CONSUMED), value(mb(SteamApiaryModule.WATER_PER_CYCLE)),
                        value(SteamApiaryModule.COMBS_PRODUCED), value(SteamApiaryModule.HONEY_PRODUCED))
                .line(own(id, "colony"), value(SteamApiaryModule.BASE_COLONY))
                .warning(shared("elevator.full"));
        upkeep(b, SteamApiaryModule.STEAM_UPKEEP).warning(own(id, "not_ported"));
        return finish(b, id);
    }

    private static List<Component> beeBreeding() {
        String id = "steam_elevator_bee_breeding_module";
        var b = start("elevator_module").line(own(id, "purpose")).separator()
                .line(own(id, "queen"))
                .line(own(id, "cycle"), value(seconds(SteamBeeBreedingModule.CYCLE_TICKS)),
                        value(SteamBeeBreedingModule.FEED_CONSUMED))
                .warning(own(id, "idle"))
                .warning(own(id, "requires"));
        upkeep(b, SteamBeeBreedingModule.STEAM_UPKEEP).warning(own(id, "not_ported"));
        return finish(b, id);
    }
}
