package com.raishxn.gtna.common.data.tooltip;

import net.minecraft.network.chat.Component;

import com.raishxn.gtna.common.machine.multiblock.steam.LargeSteamSpecs;
import com.raishxn.gtna.common.machine.multiblock.steam.SteamCactusWonder;
import com.raishxn.gtna.common.machine.multiblock.steam.SteamCracking;
import com.raishxn.gtna.common.machine.multiblock.steam.SteamItemVaultMachine;
import com.raishxn.gtna.common.machine.multiblock.steam.SteamManufacturer;

import java.util.List;

import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.finish;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.own;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.shared;
import static com.raishxn.gtna.common.data.tooltip.GTNATooltips.start;
import static com.raishxn.gtna.common.data.tooltip.LargeSteamTooltips.parallels;
import static com.raishxn.gtna.common.data.tooltip.LargeSteamTooltips.rules;
import static com.typeomega.lib.tooltip.TooltipArg.configurable;
import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * Steam utility multiblocks ported from GT: Not Leisure (rock breaker, superheater, woodcutter, lava maker,
 * manufacturer, mega compressor, cracker, item vault, cactus wonder). Lines start from GTNL's controller tooltips;
 * numbers come from the machine classes and {@link LargeSteamSpecs}.
 */
final class SteamUtilityTooltips {

    private SteamUtilityTooltips() {}

    static void register() {
        // Their casings have no steel tier, so high pressure never applies.
        fixedCasing("steam_cobbler", "rock_breaker", LargeSteamSpecs.COBBLER);
        fixedCasing("stone_superheater", "superheater", LargeSteamSpecs.SUPERHEATER);
        fixedCasing("steam_woodcutter", "woodcutter", LargeSteamSpecs.WOODCUTTER);
        fixedCasing("steam_lava_maker", "lava_maker", LargeSteamSpecs.LAVA_MAKER);
        GTNATooltips.register("steam_manufacturer", SteamUtilityTooltips::manufacturer);
        GTNATooltips.register("steam_mega_compressor", SteamUtilityTooltips::megaCompressor);
        GTNATooltips.register("steam_cracking", SteamUtilityTooltips::cracking);
        GTNATooltips.register("steam_item_vault", SteamUtilityTooltips::itemVault);
        GTNATooltips.register("steam_cactus_wonder", SteamUtilityTooltips::cactusWonder);
    }

    private static void fixedCasing(String id, String type, LargeSteamSpecs.Spec spec) {
        GTNATooltips.register(id, () -> {
            var b = start(type).line(own(id, "purpose"))
                    .line(shared("steam.parallel"), parallels(spec.maxParallel()))
                    .line(shared("steam.parallel.buttons"), configurable("[+]"), configurable("[-]"))
                    .separator()
                    .line(shared("steam.fixed_casing"))
                    .line(shared("steam.hatch"), value(1));
            return finish(b, id);
        });
    }

    private static List<Component> manufacturer() {
        String id = "steam_manufacturer";
        var spec = LargeSteamSpecs.MANUFACTURER;
        var b = start("hydraulic_manufacturing").line(own(id, "purpose"))
                .line(shared("steam.parallel"), parallels(spec.maxParallel()))
                .line(shared("steam.parallel.buttons"), configurable("[+]"), configurable("[-]"))
                .line(own(id, "steam"), value(SteamManufacturer.STEAM_PER_PARALLEL + " mB"))
                .separator()
                .line(shared("steam.fixed_casing"))
                .line(shared("steam.hatch"), value(1));
        return finish(b, id);
    }

    private static List<Component> megaCompressor() {
        String id = "steam_mega_compressor";
        var spec = LargeSteamSpecs.MEGA_COMPRESSOR;
        var b = start("compressor").line(own(id, "purpose"))
                .line(shared("steam.faster"), GTNATooltips.balanced(LargeSteamTooltips::percent,
                        v -> LargeSteamTooltips.SINGLE_BLOCK_DURATION / v.duration(spec.durationMultiplier()) - 1))
                .line(shared("steam.parallel"), parallels(spec.maxParallel()))
                .separator();
        return finish(rules(b), id);
    }

    private static List<Component> cracking() {
        String id = "steam_cracking";
        var b = start("cracker").line(own(id, "purpose"))
                .line(own(id, "parallel"), parallels(SteamCracking.BRONZE_PARALLEL),
                        parallels(SteamCracking.STEEL_PARALLEL))
                .separator();
        return finish(rules(b), id);
    }

    private static List<Component> itemVault() {
        String id = "steam_item_vault";
        var b = start("item_storage")
                .line(own(id, "purpose"), value(SteamItemVaultMachine.SLOTS), value(SteamItemVaultMachine.SLOT_LIMIT))
                .line(own(id, "total"), value((long) SteamItemVaultMachine.SLOTS * SteamItemVaultMachine.SLOT_LIMIT))
                .separator()
                .line(shared("steam.hatch"), value(1));
        return finish(b, id);
    }

    private static List<Component> cactusWonder() {
        String id = "steam_cactus_wonder";
        var b = start("steam_producer").line(own(id, "purpose"))
                .line(own(id, "offer"), value(SteamCactusWonder.CYCLE_TICKS / 20 + " s"))
                .separator()
                .warning(own(id, "cacti"));
        return finish(b, id);
    }
}
