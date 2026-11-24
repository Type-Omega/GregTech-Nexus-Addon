package com.raishxn.gtna.common.data.multiblock;

import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Readable multiblock aisles kept as text under {@code pattern/<name>.aisles}: a line {@code aisle} (or
 * {@code aisle repeat <min> <max>}) starts an aisle, each following {@code |row|} line is one row and {@code #} starts
 * a comment. Rows sit between bars so trailing spaces survive editors.
 */
public final class TextAisles {

    /** One aisle; {@code min == max == 1} when it does not repeat. */
    public record Aisle(String[] rows, int min, int max) {

        public boolean repeats() {
            return min != 1 || max != 1;
        }
    }

    private TextAisles() {}

    public static List<Aisle> load(String name) {
        String resource = "pattern/" + name + ".aisles";
        var stream = TextAisles.class.getClassLoader().getResourceAsStream(resource);
        if (stream == null) throw new IllegalStateException("Missing multiblock pattern resource: " + resource);
        List<Aisle> aisles = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            List<String> rows = null;
            int min = 1, max = 1;
            for (String line; (line = reader.readLine()) != null;) {
                if (line.isBlank() || line.startsWith("#")) continue;
                if (line.startsWith("aisle")) {
                    if (rows != null) aisles.add(new Aisle(rows.toArray(String[]::new), min, max));
                    rows = new ArrayList<>();
                    String[] words = line.trim().split("\\s+");
                    min = words.length == 4 ? Integer.parseInt(words[2]) : 1;
                    max = words.length == 4 ? Integer.parseInt(words[3]) : 1;
                } else if (rows != null && line.length() >= 2 && line.startsWith("|") && line.endsWith("|")) {
                    rows.add(line.substring(1, line.length() - 1));
                } else {
                    throw new IllegalStateException(resource + ": unexpected line '" + line + "'");
                }
            }
            if (rows != null) aisles.add(new Aisle(rows.toArray(String[]::new), min, max));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + resource, e);
        }
        return aisles;
    }

    /** Adds every aisle of the resource to the pattern, with its repetition. */
    public static FactoryBlockPattern into(FactoryBlockPattern pattern, String name) {
        for (var aisle : load(name)) {
            pattern.aisle(aisle.rows());
            if (aisle.repeats()) pattern.setRepeatable(aisle.min(), aisle.max());
        }
        return pattern;
    }
}
