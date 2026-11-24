package com.raishxn.gtna.common.data.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.raishxn.gtna.api.steam.GTNASteamBalance;
import com.raishxn.gtna.common.data.GTNASources;
import com.typeomega.lib.lang.Tr;
import com.typeomega.lib.tooltip.MultiblockTooltip;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import static com.typeomega.lib.tooltip.TooltipArg.value;

/**
 * GTNA multiblocks already migrated to the GTNH-style tooltip of the lib (GTIA-D-410). Each family registers its
 * machines here; {@code GTNAMultiblockTooltips} uses these instead of the old generated layout. The tooltip is built
 * when it is shown, so the language and the pack's steam balance are current.
 */
public final class GTNATooltips {

    /** Shown in blue italics on the last line, as the mod list names the mod. */
    public static final String MOD_NAME = "GregTech: Nexus Addon";

    private static final Map<String, Supplier<List<Component>>> TOOLTIPS = new LinkedHashMap<>();
    private static boolean initialised;

    private GTNATooltips() {}

    /** Registers every migrated family once. */
    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        LargeSteamTooltips.register();
    }

    static void register(String path, Supplier<List<Component>> tooltip) {
        if (TOOLTIPS.put(path, tooltip) != null) throw new IllegalStateException(path + " tooltip registered twice");
    }

    public static boolean has(String path) {
        init();
        return TOOLTIPS.containsKey(path);
    }

    /** The tooltip of a migrated machine, or {@code null} when it still uses the old layout. */
    public static List<Component> build(String path) {
        init();
        Supplier<List<Component>> tooltip = TOOLTIPS.get(path);
        return tooltip == null ? null : tooltip.get();
    }

    /** Registry paths of the migrated machines. */
    public static List<String> paths() {
        init();
        return List.copyOf(TOOLTIPS.keySet());
    }

    /** Shared text {@code gtna.tooltip.mb.<key>}. */
    public static Tr shared(String key) {
        return GTNATooltipLang.tr("gtna.tooltip.mb." + key);
    }

    /** {@code gtna.machine.<id>.tooltip.<key>}. */
    public static Tr own(String id, String key) {
        return GTNATooltipLang.tr("gtna.machine." + id + ".tooltip." + key);
    }

    /** Builder with the Machine Type line already in place. */
    public static MultiblockTooltipBuilder start(String... types) {
        Tr[] trs = new Tr[types.length];
        for (int i = 0; i < types.length; i++) trs[i] = shared("type." + types[i]);
        return MultiblockTooltip.builder().machineType(trs);
    }

    /** The humour line, the origin of a port and the mod name that close every tooltip. */
    public static List<Component> finish(MultiblockTooltipBuilder builder, String id) {
        String source = GTNASources.sourceOf(id);
        builder.humor(own(id, "humor"));
        if (source != null) builder.line(GTNATooltipLang.tr("gtna.tooltip.source"), GTNASources.addonName(source));
        return builder.mod(MOD_NAME).build();
    }

    /**
     * A number that the steam balance of the pack changes: one cyan value per variant the tooltip should list, each
     * with its label (for example "Normal" and "Journey" in the main menu).
     */
    public static Component balanced(DoubleFunction<String> format,
                                     ToDoubleFunction<GTNASteamBalance.Variant> number) {
        List<GTNASteamBalance.Variant> variants = GTNASteamBalance.provider().tooltipVariants();
        MutableComponent out = Component.empty();
        for (int i = 0; i < variants.size(); i++) {
            var variant = variants.get(i);
            if (i > 0) out.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY));
            out.append(value(format.apply(number.applyAsDouble(variant))));
            if (variant.label() != null) {
                out.append(Component.literal(" (" + variant.label() + ")").withStyle(ChatFormatting.GRAY));
            }
        }
        return out;
    }
}
