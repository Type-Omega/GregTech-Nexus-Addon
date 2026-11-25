package com.raishxn.gtna;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Every GTNA multiblock uses the GTNH-style tooltip of the lib (D-410, D-432): each registered multiblock id has
 * its humour line in {@code tooltip_lang} (which {@code GTNATooltips.finish} always adds), and no numbered line of the
 * old generated layout ({@code gtna.machine.<id>.tooltip.N}) of a multiblock is left in pt_br. GTLCore style:
 * {@code main()} + asserts.
 */
public final class MultiblockTooltipStandardTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final Path PT_BR = Path.of("src/main/resources/assets/gtna/lang/pt_br.json");
    private static final Path TOOLTIP_LANG = Path.of("src/main/resources/assets/gtna/tooltip_lang");
    private static final Pattern MULTIBLOCK = Pattern
            .compile("(?:\\.multiblock|registerElevatorModule)\\(\\s*\"([a-z0-9_]+)\"");
    private static final Pattern OLD_KEY = Pattern
            .compile("\"(gtna\\.machine\\.([a-z0-9_]+)\\.tooltip\\.\\d+)\"\\s*:");

    private MultiblockTooltipStandardTest() {}

    private static Set<String> migrated() throws IOException {
        Set<String> ids = new TreeSet<>();
        Pattern humor = Pattern.compile("\"gtna\\.machine\\.([a-z0-9_]+)\\.tooltip\\.humor\"");
        try (Stream<Path> files = Files.list(TOOLTIP_LANG)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                Matcher m = humor.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) ids.add(m.group(1));
            }
        }
        return ids;
    }

    public static void main(String[] args) throws IOException {
        List<String> errors = new ArrayList<>();
        Set<String> registered = new TreeSet<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher matcher = MULTIBLOCK.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) registered.add(matcher.group(1));
            }
        }
        Set<String> migrated = migrated();
        for (String id : registered) {
            if (!migrated.contains(id)) errors.add(id + ": still on the old tooltip layout (no lib tooltip)");
        }
        Matcher old = OLD_KEY.matcher(Files.readString(PT_BR, StandardCharsets.UTF_8));
        while (old.find()) {
            // Hatches and parts keep numbered lines; only multiblocks moved to the lib layout.
            if (registered.contains(old.group(2))) {
                errors.add("pt_br keeps an old numbered tooltip line: " + old.group(1));
            }
        }
        if (registered.size() < 100) errors.add("found only " + registered.size() + " registered multiblocks");
        if (!errors.isEmpty()) {
            throw new AssertionError(errors.size() + " multiblock tooltip problem(s):\n" + String.join("\n", errors));
        }
        System.out.println("[MultiblockTooltipStandardTest] " + registered.size() + " multiblocks, all on the lib " +
                "layout (" + migrated.size() + " tooltips)");
    }
}
