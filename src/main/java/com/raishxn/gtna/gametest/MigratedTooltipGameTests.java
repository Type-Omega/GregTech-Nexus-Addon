package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.api.steam.GTNASteamBalance;
import com.raishxn.gtna.common.data.GTNASources;
import com.raishxn.gtna.common.data.tooltip.GTNATooltips;
import com.raishxn.gtna.common.machine.multiblock.steam.LargeSteamSpecs;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Multiblocks migrated to the GTNH-style tooltip of the lib (GTIA-D-410, D-428, D-429). */
@GameTestHolder("gtna")
@PrefixGameTestTemplate(false)
public final class MigratedTooltipGameTests {

    private static final String TEMPLATE = "empty_12";

    /**
     * Every migrated tooltip starts with Machine Type, credits the origin of a port once, ends with the mod name,
     * has no Hold [LSHIFT] line, and every key it uses exists in en_us, pt_br and zh_cn.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void migratedTooltipsFollowTheLibLayout(GameTestHelper h) {
        JsonObject en = lang("en_us");
        JsonObject pt = lang("pt_br");
        JsonObject zh = lang("zh_cn");
        int checked = 0;
        for (String path : GTNATooltips.paths()) {
            MachineDefinition definition = GTRegistries.MACHINES.get(new ResourceLocation(GTNACORE.MOD_ID, path));
            if (definition == null) continue;
            List<Component> tooltip = shown(definition);
            h.assertTrue(key(tooltip.get(0)).equals("typeomegalib.tooltip.machine_type"),
                    path + " must start with Machine Type, got " + tooltip.get(0));
            Component last = tooltip.get(tooltip.size() - 1);
            h.assertTrue(last.getContents() instanceof LiteralContents literal &&
                    GTNATooltips.MOD_NAME.equals(literal.text()), path + " must end with the mod name");
            long sources = tooltip.stream().filter(c -> key(c).equals("gtna.tooltip.source")).count();
            h.assertTrue(sources == (GTNASources.sourceOf(path) != null ? 1 : 0),
                    path + " must credit its origin exactly once, got " + sources);
            for (String k : keys(tooltip)) {
                h.assertFalse(k.contains("structure") || k.contains("shift"), path + " uses " + k + " (GTIA-D-428)");
                if (k.startsWith("typeomegalib.")) continue;
                h.assertTrue(en.has(k) && pt.has(k) && zh.has(k), path + ": " + k + " missing in en/pt/zh");
            }
            checked++;
        }
        h.assertTrue(checked >= 28, "expected the 28 steam multiblocks, checked " + checked);
        h.succeed();
    }

    /** The numbers come from LargeSteamSpecs and follow the pack's steam balance. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void largeSteamNumbersFollowSpecsAndBalance(GameTestHelper h) {
        MachineDefinition crusher = GTRegistries.MACHINES
                .get(new ResourceLocation(GTNACORE.MOD_ID, "large_steam_crusher"));
        if (crusher == null) {
            h.succeed();
            return;
        }
        try {
            List<String> plain = literals(shown(crusher));
            h.assertTrue(plain.contains(String.valueOf(LargeSteamSpecs.CRUSHER.maxParallel())),
                    "crusher shows its maximum parallel, got " + plain);
            // 2 / 0.5 = 4x the bronze single block: 300% faster.
            h.assertTrue(plain.contains("300%"), "crusher speed is compared with the single block, got " + plain);
            h.assertTrue(plain.contains("2x"), "high pressure shows its multipliers, got " + plain);

            GTNASteamBalance.setProvider(() -> new GTNASteamBalance.Variant("Journey", 1.5, 0.5));
            List<String> journey = literals(shown(crusher));
            h.assertTrue(journey.contains("192") && journey.contains("700%"),
                    "the tooltip shows the balanced parallel and speed, got " + journey);
            h.assertTrue(journey.stream().anyMatch(s -> s.contains("Journey")), "the variant label is shown");
        } finally {
            GTNASteamBalance.setProvider(null);
        }
        h.succeed();
    }

    /** Only structures with a steel casing tier promise high pressure (the old cobbler-family tooltips did). */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void highPressureOnlyWhereTheCasingHasASteelTier(GameTestHelper h) {
        for (String path : new String[] { "steam_cobbler", "stone_superheater", "steam_woodcutter",
                "steam_lava_maker", "steam_manufacturer", "steam_cracking", "steam_mega_compressor" }) {
            List<Component> tooltip = GTNATooltips.build(path);
            h.assertTrue(tooltip != null, path + " is migrated");
            boolean steel = path.equals("steam_cracking") || path.equals("steam_mega_compressor");
            List<String> keys = keys(tooltip);
            h.assertTrue(keys.contains("gtna.tooltip.mb.steam.high_pressure") == steel,
                    path + (steel ? " must" : " must not") + " show the high-pressure rule");
            h.assertTrue(keys.contains("gtna.tooltip.mb.steam.fixed_casing") == !steel,
                    path + " says whether its casing has a steel tier");
        }
        h.succeed();
    }

    /** A machine that was not migrated keeps the old layout and is not claimed by the lib builder. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void unmigratedMachinesKeepTheOldLayout(GameTestHelper h) {
        h.assertFalse(GTNATooltips.has("large_steam_storage_tank"), "the storage tank is not migrated yet");
        h.assertTrue(GTNATooltips.build("large_steam_storage_tank") == null, "no lib tooltip for it");
        MachineDefinition tank = GTRegistries.MACHINES
                .get(new ResourceLocation(GTNACORE.MOD_ID, "large_steam_storage_tank"));
        if (tank != null) {
            List<Component> tooltip = shown(tank);
            h.assertTrue(key(tooltip.get(0)).equals("gtna.machine.large_steam_storage_tank.tooltip.0"),
                    "the storage tank still uses its numbered lines, got " + tooltip.get(0));
        }
        h.succeed();
    }

    private static List<Component> shown(MachineDefinition definition) {
        List<Component> tooltip = new ArrayList<>();
        definition.getTooltipBuilder().accept(ItemStack.EMPTY, tooltip);
        return tooltip;
    }

    private static String key(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getKey() : "";
    }

    /** Every translation key in the components, their arguments and siblings. */
    private static List<String> keys(List<Component> lines) {
        List<String> out = new ArrayList<>();
        for (Component line : lines) walk(line, out, true);
        return out;
    }

    private static List<String> literals(List<Component> lines) {
        List<String> out = new ArrayList<>();
        for (Component line : lines) walk(line, out, false);
        return out;
    }

    private static void walk(Component c, List<String> out, boolean wantKeys) {
        if (c.getContents() instanceof TranslatableContents t) {
            if (wantKeys) out.add(t.getKey());
            for (Object arg : t.getArgs()) {
                if (arg instanceof Component argument) walk(argument, out, wantKeys);
                else if (!wantKeys) out.add(String.valueOf(arg));
            }
        } else if (c.getContents() instanceof LiteralContents l && !wantKeys) {
            out.add(l.text().trim());
        }
        for (Component sibling : c.getSiblings()) walk(sibling, out, wantKeys);
    }

    private static JsonObject lang(String locale) {
        String path = "assets/gtna/lang/" + locale + ".json";
        try (var in = MigratedTooltipGameTests.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    private MigratedTooltipGameTests() {}
}
