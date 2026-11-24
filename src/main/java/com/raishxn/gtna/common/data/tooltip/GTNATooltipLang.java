package com.raishxn.gtna.common.data.tooltip;

import com.google.gson.JsonParser;
import com.typeomega.lib.lang.Tr;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Texts of the GTNH-style multiblock tooltips (GTIA-D-410), in {@code assets/gtna/tooltip_lang/<family>.json}: each
 * key maps to [en_us, pt_br, zh_cn]. English goes to the generated en_us through {@code GTNALangProvider};
 * {@code tools/tooltip_lang.py} copies the other two into lang/pt_br.json and lang/zh_cn.json.
 */
public final class GTNATooltipLang {

    /** Files under tooltip_lang, without the extension, one per migrated machine family. */
    public static final String[] FILES = { "large_steam" };

    private static Map<String, String[]> texts;

    private GTNATooltipLang() {}

    /** The key as a {@link Tr} with its three texts. */
    public static Tr tr(String key) {
        String[] text = texts().get(key);
        if (text == null) throw new IllegalArgumentException("no tooltip_lang text for " + key);
        return Tr.of(key, text[0], text[1], text[2]);
    }

    /** Every key with its three texts, in file order. */
    public static synchronized Map<String, String[]> texts() {
        if (texts != null) return texts;
        Map<String, String[]> read = new LinkedHashMap<>();
        for (String name : FILES) {
            String path = "assets/gtna/tooltip_lang/" + name + ".json";
            try (var in = GTNATooltipLang.class.getClassLoader().getResourceAsStream(path)) {
                if (in == null) throw new IllegalStateException("missing " + path);
                var root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var e : root.entrySet()) {
                    var array = e.getValue().getAsJsonArray();
                    if (array.size() != 3) throw new IllegalStateException(e.getKey() + " needs en, pt and zh");
                    String[] value = { array.get(0).getAsString(), array.get(1).getAsString(),
                            array.get(2).getAsString() };
                    if (read.put(e.getKey(), value) != null) {
                        throw new IllegalStateException(e.getKey() + " declared twice in tooltip_lang");
                    }
                }
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Broken " + path, e);
            }
        }
        texts = Collections.unmodifiableMap(read);
        return texts;
    }
}
