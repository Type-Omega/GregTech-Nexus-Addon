package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
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
import com.raishxn.gtna.common.data.GTNAMultiblockTooltips;
import com.raishxn.gtna.common.data.GTNASources;
import com.raishxn.gtna.common.data.tooltip.GTNATooltips;
import com.raishxn.gtna.common.machine.multiblock.steam.LargeSteamSpecs;
import com.typeomega.lib.tooltip.StructureInfo;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Multiblocks migrated to the GTNH-style tooltip of the lib (GTIA-D-410, D-428, D-429, D-431). */
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
                h.assertFalse(k.contains("shift"), path + " uses " + k + " (GTIA-D-428)");
                if (!k.startsWith("gtna.")) continue;
                h.assertTrue(en.has(k) && pt.has(k) && zh.has(k), path + ": " + k + " missing in en/pt/zh");
            }
            checked++;
        }
        h.assertTrue(checked >= 31, "expected the 31 steam multiblocks, checked " + checked);
        h.succeed();
    }

    /**
     * No GTNA multiblock shows a structure section or a pollution line (GTIA-D-431): dimensions, controller, block
     * counts and hatch counts stay out of the tooltip. The "Supports ...!" line and the modes stay.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void noMultiblockShowsItsStructure(GameTestHelper h) {
        List<String> shownStructure = new ArrayList<>();
        int checked = 0;
        for (MachineDefinition definition : GTRegistries.MACHINES) {
            if (!(definition instanceof MultiblockMachineDefinition) ||
                    !GTNACORE.MOD_ID.equals(definition.getId().getNamespace()) ||
                    !GTNAMultiblockTooltips.has(definition.getId().getPath())) {
                continue;
            }
            for (String k : keys(shown(definition))) {
                if (k.startsWith("typeomegalib.tooltip.structure.") || k.equals("typeomegalib.tooltip.pollution")) {
                    shownStructure.add(definition.getId().getPath() + " " + k);
                }
            }
            checked++;
        }
        dumpPreview();
        h.assertTrue(shownStructure.isEmpty(), "multiblocks showing a structure or pollution line: " + shownStructure);
        h.assertTrue(checked >= 160, "expected about 170 GTNA multiblocks, checked " + checked);
        h.succeed();
    }

    /** The Large Steam Macerator keeps the "Supports" line from its pattern, but no structure section. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void largeSteamCrusherHasNoStructureSection(GameTestHelper h) {
        MachineDefinition crusher = GTRegistries.MACHINES
                .get(new ResourceLocation(GTNACORE.MOD_ID, "large_steam_crusher"));
        if (!(crusher instanceof MultiblockMachineDefinition multi)) {
            h.succeed();
            return;
        }
        StructureInfo info = GTNATooltips.structure(multi);
        h.assertTrue(info != null && info.abilities().contains("steam"), "crusher pattern is still readable");
        List<String> keys = keys(shown(crusher));
        h.assertFalse(keys.contains("typeomegalib.tooltip.structure.header") ||
                keys.contains("typeomegalib.tooltip.structure.dimensions"), "no structure section, got " + keys);
        h.assertTrue(keys.contains("gtna.machine.large_steam_crusher.tooltip.humor"), "humour line stays");
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

    /** The Infernal Coke Oven reads its ramp from the machine: +1% every 5 s up to 10x, 8 to 256 parallels. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void infernalCokeOvenShowsItsRamp(GameTestHelper h) {
        List<String> plain = literals(GTNATooltips.build("infernal_coke_oven"));
        for (String expected : new String[] { "5 s", "1%", "10x", "8", "16", "10 min", "256", "320 mB", "80 mB" }) {
            h.assertTrue(plain.contains(expected), "coke oven tooltip lacks " + expected + ": " + plain);
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

    /** Machines of the tooltip preview page (design/core in the pack workspace). */
    private static final String[] PREVIEW = { "large_steam_crusher", "steam_manufacturer", "mega_alloy_blast_smelter",
            "nano_forge", "eye_of_harmony", "forge_of_gods", "kinetic_orbital_lance", "large_naquadah_reactor",
            "cracker_hub" };

    /**
     * Writes the shown lines of the preview machines, as text with colour per segment, to
     * {@code tooltip-preview.json} in the run folder, for the HTML preview. The run folder is not versioned.
     */
    private static void dumpPreview() {
        com.google.gson.JsonObject out = new com.google.gson.JsonObject();
        for (String path : PREVIEW) {
            MachineDefinition definition = GTRegistries.MACHINES.get(new ResourceLocation(GTNACORE.MOD_ID, path));
            if (definition == null) continue;
            com.google.gson.JsonArray lines = new com.google.gson.JsonArray();
            for (Component line : shown(definition)) {
                com.google.gson.JsonArray segments = new com.google.gson.JsonArray();
                line.visit((style, text) -> {
                    com.google.gson.JsonObject seg = new com.google.gson.JsonObject();
                    seg.addProperty("text", text);
                    if (style.getColor() != null) seg.addProperty("color", style.getColor().serialize());
                    if (style.isItalic()) seg.addProperty("italic", true);
                    if (style.isBold()) seg.addProperty("bold", true);
                    segments.add(seg);
                    return java.util.Optional.empty();
                }, net.minecraft.network.chat.Style.EMPTY);
                lines.add(segments);
            }
            out.add(path, lines);
        }
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("tooltip-preview.json"), out.toString());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
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
