package com.raishxn.gtna.common.data.tooltip;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import com.mojang.logging.LogUtils;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.api.machine.multiblock.GTNAPartAbility;
import com.raishxn.gtna.api.steam.GTNASteamBalance;
import com.raishxn.gtna.common.data.GTNASources;
import com.typeomega.lib.lang.Tr;
import com.typeomega.lib.tooltip.MultiblockTooltip;
import com.typeomega.lib.tooltip.MultiblockTooltipBuilder;
import com.typeomega.lib.tooltip.StructureInfo;
import com.typeomega.lib.tooltip.structure.GtceuPatterns;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Supplier<List<Component>>> TOOLTIPS = new LinkedHashMap<>();
    private static final Set<String> UNREADABLE = ConcurrentHashMap.newKeySet();
    private static boolean initialised;

    private GTNATooltips() {}

    /** Registers every migrated family once. */
    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        GtceuPatterns.registerAbilities(GTNAPartAbility.class);
        LargeSteamTooltips.register();
        SteamUtilityTooltips.register();
        SteamReactorTooltips.register();
        EndgameTooltips.register();
        CoilTooltips.register();
        GodforgeTooltips.register();
        ForgeTooltips.register();
        BECTooltips.register();
        ReactorTooltips.register();
        SteamElevatorTooltips.register();
        MegaTooltips.register();
        ProcessTooltips.register();
        GtoMiscTooltips.register();
        GtlTooltips.register();
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

    /** The modes and the "Supports ...!" line read from the machine, then the humour, origin and mod lines. */
    public static List<Component> finish(MultiblockTooltipBuilder builder, String id) {
        machine(builder, id);
        String source = GTNASources.sourceOf(id);
        builder.humor(own(id, "humor"));
        if (source != null) builder.line(GTNATooltipLang.tr("gtna.tooltip.source"), GTNASources.addonName(source));
        return builder.mod(MOD_NAME).build();
    }

    /**
     * Lines every GTNA multiblock gets from its definition: the modes when it switches recipe types and the
     * "Supports ...!" line read from its pattern. No structure section and no pollution line (GTIA-D-431).
     */
    public static MultiblockTooltipBuilder machine(MultiblockTooltipBuilder builder, String id) {
        if (!(GTRegistries.MACHINES
                .get(new ResourceLocation(GTNACORE.MOD_ID, id)) instanceof MultiblockMachineDefinition definition)) {
            return builder;
        }
        builder.modes(GtceuPatterns.modes(definition));
        StructureInfo info = structure(definition);
        // EN: Only the "Supports" line is taken from the pattern; the structure section is off (GTIA-D-431).
        // PT: Só a linha "Suporta" vem do padrão; a seção de estrutura está desligada (GTIA-D-431).
        // ZH: 只从结构模式读取“支持”一行；结构部分已关闭（GTIA-D-431）。
        if (info != null) builder.hatches(info);
        return builder;
    }

    /** The lines {@link #machine} adds, for machines that keep the older numbered lines. */
    public static List<Component> machineSection(String id) {
        return machine(MultiblockTooltip.section(), id).build();
    }

    /** The pattern summary, or {@code null} when the pattern can't be read (logged once). */
    public static StructureInfo structure(MultiblockMachineDefinition definition) {
        String id = definition.getId().toString();
        if (UNREADABLE.contains(id)) return null;
        try {
            return GtceuPatterns.of(definition);
        } catch (RuntimeException e) {
            if (UNREADABLE.add(id)) LOGGER.warn("Cannot read the structure of {} for its tooltip", id, e);
            return null;
        }
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
