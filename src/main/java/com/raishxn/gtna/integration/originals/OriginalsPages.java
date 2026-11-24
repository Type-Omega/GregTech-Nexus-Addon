package com.raishxn.gtna.integration.originals;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.api.machine.feature.collider.ColliderCore;
import com.raishxn.gtna.api.machine.feature.lance.LanceCore;
import com.raishxn.gtna.api.machine.feature.rift.RiftCore;
import com.raishxn.gtna.api.machine.feature.stellar.StellarCore;
import com.raishxn.gtna.common.data.GTNAMaterials;
import com.raishxn.gtna.common.data.GTNAOriginals;
import com.raishxn.gtna.common.data.material.OriginalsMaterials;
import com.raishxn.gtna.common.data.tooltip.GTNATooltipLang;
import com.raishxn.gtna.common.machine.multiblock.originals.LanceMachine;
import com.raishxn.gtna.common.machine.multiblock.originals.RiftGateMachine;
import com.raishxn.gtna.common.machine.multiblock.originals.StellarChamberMachine;
import com.typeomega.lib.lang.Tr;
import com.typeomega.lib.ui.InfoLines;
import com.typeomega.lib.ui.PanelLayout;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The illustrated EMI pages of the four original multiblocks (GTIA-D-435, U5): they have no GT recipe (their recipe
 * type is a dummy), so each page is built here from the rules of the machine's core and the materials the machine
 * really takes and drops. Plain data: the EMI plugin draws it, the GameTests check it without a client.
 */
public final class OriginalsPages {

    public static final ResourceLocation COLLIDER = GTNACORE.id("antimatter_collider_ring");
    public static final ResourceLocation RIFT = GTNACORE.id("dimensional_rift_gate");
    public static final ResourceLocation LANCE = GTNACORE.id("kinetic_orbital_lance");
    public static final ResourceLocation STELLAR = GTNACORE.id("stellar_containment_chamber");
    public static final List<ResourceLocation> CATEGORIES = List.of(COLLIDER, RIFT, LANCE, STELLAR);

    /** What fills one slot of the layout; an empty one draws an empty slot. */
    public record Content(List<ItemStack> items, @Nullable Fluid fluid, long amount, float chance) {

        public static final Content EMPTY = new Content(List.of(), null, 0, 1);

        public static Content items(List<ItemStack> stacks) {
            return new Content(stacks.stream().filter(s -> !s.isEmpty()).toList(), null, 0, 1);
        }

        public static Content item(ItemStack stack) {
            return items(List.of(stack));
        }

        public static Content fluid(@Nullable Fluid fluid, long amount) {
            return new Content(List.of(), fluid, amount, 1);
        }

        public Content withChance(float chance) {
            return new Content(items, fluid, amount, chance);
        }

        public boolean isEmpty() {
            return items.isEmpty() && fluid == null;
        }
    }

    /** One EMI page: its category, a stable id, the layout and one content per layout slot, and the lines. */
    public record Page(ResourceLocation category, String id, PanelLayout layout, List<Content> contents,
                       List<Component> lines) {

        public Page {
            if (contents.size() != layout.slots().size())
                throw new IllegalArgumentException(id + ": " + contents.size() + " contents for " +
                        layout.slots().size() + " slots");
        }
    }

    // ------------------------------------------------------------------ layouts, matched to the art

    /** Ring with liquid helium at its centre; beam line splitting into the particle grid. */
    public static final PanelLayout COLLIDER_LAYOUT = grid(PanelLayout.builder(160, 80)
            .art(GTNACORE.id("textures/gui/recipe/collider.png"))
            .fluid(PanelLayout.Role.INPUT, 25, 31), 88, 13, 4, 3).build();

    /** Open rift with SpaceTime at its heart; probe above and anchor below the beam; the biome's drops on the right. */
    public static final PanelLayout RIFT_LAYOUT = grid(PanelLayout.builder(160, 80)
            .art(GTNACORE.id("textures/gui/recipe/rift_gate.png"))
            .fluid(PanelLayout.Role.INPUT, 27, 31)
            .item(PanelLayout.Role.CATALYST, 76, 8)
            .item(PanelLayout.Role.CATALYST, 76, 54), 106, 22, 3, 2).build();

    /** Projectile into the breech; the capsule comes back from the target with its cargo along the bottom. */
    public static final PanelLayout LANCE_LAYOUT = PanelLayout.builder(160, 80)
            .art(GTNACORE.id("textures/gui/recipe/lance.png"))
            .item(PanelLayout.Role.INPUT, 4, 44)
            .item(PanelLayout.Role.OUTPUT, 70, 60)
            .item(PanelLayout.Role.OUTPUT, 88, 60)
            .item(PanelLayout.Role.OUTPUT, 106, 60)
            .item(PanelLayout.Role.OUTPUT, 124, 60)
            .item(PanelLayout.Role.OUTPUT, 142, 60)
            .build();

    /** Star matter piped into the bands; what the star sheds on the right. */
    public static final PanelLayout STELLAR_LAYOUT = PanelLayout.builder(160, 80)
            .art(GTNACORE.id("textures/gui/recipe/stellar_chamber.png"))
            .fluid(PanelLayout.Role.INPUT, 4, 31)
            .item(PanelLayout.Role.OUTPUT, 124, 31)
            .item(PanelLayout.Role.OUTPUT, 142, 31)
            .build();

    /** Lines each category shows, for the page height: Usage/Output, Time and the category's own lines. */
    public static final int COLLIDER_LINES = 6, RIFT_LINES = 6, LANCE_LINES = 6, STELLAR_LINES = 6;

    private static PanelLayout.Builder grid(PanelLayout.Builder b, int x0, int y0, int cols, int rows) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) b.item(PanelLayout.Role.OUTPUT, x0 + 18 * c, y0 + 18 * r);
        return b;
    }

    private OriginalsPages() {}

    public static Tr tr(String key) {
        return GTNATooltipLang.tr("gtna.originals.emi." + key);
    }

    public static int lineCount(ResourceLocation category) {
        if (category.equals(COLLIDER)) return COLLIDER_LINES;
        if (category.equals(RIFT)) return RIFT_LINES;
        if (category.equals(LANCE)) return LANCE_LINES;
        return STELLAR_LINES;
    }

    public static List<Page> all() {
        List<Page> out = new ArrayList<>();
        out.addAll(collider());
        out.addAll(rift());
        out.addAll(lance());
        out.addAll(stellar());
        return out;
    }

    private static List<Content> fill(PanelLayout layout, List<Content> given) {
        List<Content> out = new ArrayList<>(given);
        if (out.size() > layout.slots().size())
            throw new IllegalStateException("more contents than slots: " + out.size());
        while (out.size() < layout.slots().size()) out.add(Content.EMPTY);
        return out;
    }

    private static String fmt(double v, int decimals) {
        return String.format(Locale.US, "%,." + decimals + "f", v);
    }

    private static @Nullable Fluid fluid(@Nullable Material m) {
        return m == null || !m.hasFluid() ? null : m.getFluid();
    }

    // ------------------------------------------------------------------ collider

    /** Magnet tier unlocked by each proposal (tier 0 is the starting NbTi). */
    private static final ColliderCore.Proposal[] MAGNET_PROPOSAL = { null, ColliderCore.Proposal.NB3SN_MAGNET,
            ColliderCore.Proposal.REBCO_MAGNET, ColliderCore.Proposal.NEUTRONIUM_MAGNET,
            ColliderCore.Proposal.BEYOND_STANDARD_MODEL };

    static List<Page> collider() {
        List<Page> pages = new ArrayList<>();
        var helium = GTMaterials.Helium.getProperty(
                com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey.FLUID).getStorage()
                .get(FluidStorageKeys.LIQUID);
        for (int tier = 0; tier < ColliderCore.MAGNET_FIELD.length; tier++)
            pages.add(colliderPage(tier, false, helium));
        pages.add(colliderPage(0, true, helium));
        return pages;
    }

    private static Page colliderPage(int tier, boolean electrons, @Nullable Fluid helium) {
        var core = new ColliderCore();
        core.magnetTier = tier;
        core.rampSeconds = 20;
        core.electronMode = electrons;
        double ecm = core.centreOfMassGeV();
        List<Content> contents = new ArrayList<>();
        contents.add(Content.fluid(helium, Math.round(core.heliumDemand() * 1000)));
        for (var p : ColliderCore.Particle.values()) {
            if (ecm < p.thresholdGeV) continue;
            contents.add(Content.item(new ItemStack(GTNAOriginals.particle(p))));
        }
        if (!electrons) contents.add(Content.item(GTNAOriginals.ANTIPROTON_CELL.asStack()));
        // EN: magnet upkeep from the machine (2^(33 + tier) EU/t) plus the first Penning trap from the core.
        // PT: manutenção dos ímãs da máquina (2^(33 + tier) EU/t) mais a primeira armadilha de Penning do core.
        // ZH: 磁体维护功耗取自机器（2^(33 + 等级) EU/t），再加上核心中第一级潘宁阱的功耗。
        double eut = Math.pow(2, 33 + tier) + core.trapUpkeepEUt();
        var lines = InfoLines.create().usage(eut).continuous()
                .line(tr("magnets"), ColliderCore.MAGNET_NAMES[tier], fmt(ColliderCore.MAGNET_FIELD[tier], 1))
                .line(tr("collision"), fmt(ecm / 1000, ecm < 1000 ? 2 : 1))
                .line(tr("helium"), fmt(core.heliumDemand() * 1000, 0));
        var proposal = electrons ? ColliderCore.Proposal.LEPTON_COLLIDER : MAGNET_PROPOSAL[tier];
        if (proposal == null) lines.line(tr("collider.start"));
        else lines.line(tr("unlocked_by"), Component.translatable(
                "gtna.machine.antimatter_collider_ring.proposal." + proposal.name().toLowerCase(Locale.ROOT)));
        return new Page(COLLIDER, electrons ? "electrons" : "magnet_" + tier, COLLIDER_LAYOUT,
                fill(COLLIDER_LAYOUT, contents), lines.build());
    }

    // ------------------------------------------------------------------ rift

    static List<Page> rift() {
        List<Page> pages = new ArrayList<>();
        int total = Arrays.stream(RiftCore.Biome.values()).mapToInt(b -> b.weight).sum();
        for (var biome : RiftCore.Biome.values()) {
            List<Content> contents = new ArrayList<>();
            contents.add(Content.fluid(fluid(GTNAMaterials.SpaceTime), Math.round(RiftGateMachine.SPACETIME_PER_UNIT *
                    1000)));
            contents.add(Content.item(GTNAOriginals.RIFT_PROBE.asStack()));
            var anchors = new ArrayList<ItemStack>();
            anchors.add(GTNAOriginals.DEPTH_ANCHOR.asStack());
            if (biome.minDepth >= RiftGateMachine.DEEP_DEPTH)
                anchors.add(new ItemStack(GTNAOriginals.particle(ColliderCore.Particle.X_NEXUS)));
            contents.add(Content.items(anchors));
            for (var m : RiftGateMachine.biomeOutputs(biome))
                if (m != null) contents.add(Content.item(ChemicalHelper.get(TagPrefix.dust, m)));
            // EN: the singularity takes a minute held at R >= 0.98 and then 5% per minute: shown as a chance.
            // PT: a singularidade pede um minuto com R >= 0,98 e depois 5% por minuto: aparece como chance.
            // ZH: 奇点需要 R ≥ 0.98 保持一分钟，之后每分钟 5% 概率：以概率显示。
            contents.add(Content.item(GTNAOriginals.RIFT_SINGULARITY.asStack()).withChance(0.05F));
            var h = RiftCore.harmonic(seedOf(biome));
            var lines = InfoLines.create().usage(RiftGateMachine.FIELD_EUT).continuous()
                    .line(tr("biome"), Component.translatable(
                            "gtna.machine.dimensional_rift_gate.biome." + biome.name().toLowerCase(Locale.ROOT)),
                            fmt(100.0 * biome.weight / total, 0))
                    .line(tr("min_depth"), biome.minDepth == 0 ? 1 : biome.minDepth)
                    .line(tr("harmonic"), h.p + ":" + h.q)
                    .line(tr("spacetime"), fmt(RiftGateMachine.SPACETIME_PER_UNIT, 0));
            pages.add(new Page(RIFT, biome.name().toLowerCase(Locale.ROOT), RIFT_LAYOUT, fill(RIFT_LAYOUT, contents),
                    lines.build()));
        }
        return pages;
    }

    /** The first seed that draws a biome, to ask the core which harmonic the biome gives. */
    static long seedOf(RiftCore.Biome biome) {
        long seed = 0;
        while (RiftCore.biome(seed) != biome) seed++;
        return seed;
    }

    // ------------------------------------------------------------------ lance

    static List<Page> lance() {
        List<Page> pages = new ArrayList<>();
        for (var body : LanceCore.Body.values()) {
            List<Content> contents = new ArrayList<>();
            var projectiles = new ArrayList<ItemStack>();
            for (var p : LanceCore.Projectile.values()) {
                if (!LanceCore.pierces(body, p)) continue;
                var stack = projectileStack(p);
                if (!stack.isEmpty()) projectiles.add(stack);
            }
            contents.add(Content.items(projectiles));
            for (var m : LanceMachine.materials(body))
                if (m != null) contents.add(Content.item(dustOrCell(m)));
            var parents = new ArrayList<Component>();
            for (var p : body.parents) parents.add(Component.translatable(
                    "gtna.machine.kinetic_orbital_lance.body." + p.name().toLowerCase(Locale.ROOT)));
            var lines = InfoLines.create().usage(LanceMachine.CHARGE_PER_SECOND / 20)
                    .line(tr("lance.return"), fmt(LanceCore.returnSeconds(body), 0))
                    .line(tr("lance.target"), Component.translatable(
                            "gtna.machine.kinetic_orbital_lance.body." + body.name().toLowerCase(Locale.ROOT)),
                            fmt(body.distanceMm, 0))
                    .line(tr("lance.hardness"), fmt(body.hardnessMJ, 1))
                    .line(tr("lance.dust"), fmt(LanceMachine.KG_PER_DUST / 1000, 0));
            if (parents.isEmpty()) lines.line(tr("lance.open"));
            else lines.line(tr("lance.survey"), join(parents));
            pages.add(new Page(LANCE, body.name().toLowerCase(Locale.ROOT), LANCE_LAYOUT,
                    fill(LANCE_LAYOUT, contents), lines.build()));
        }
        return pages;
    }

    private static Component join(List<Component> parts) {
        var out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) out.append(" + ");
            out.append(parts.get(i));
        }
        return out;
    }

    private static ItemStack dustOrCell(Material m) {
        var dust = ChemicalHelper.get(TagPrefix.dust, m);
        return dust.isEmpty() ? ChemicalHelper.get(TagPrefix.ingot, m) : dust;
    }

    static ItemStack projectileStack(LanceCore.Projectile p) {
        Material m = switch (p) {
            case NAQUADAH_ALLOY -> GTMaterials.NaquadahAlloy;
            case TRINIUM -> GTMaterials.Trinium;
            case NEUTRONIUM -> GTMaterials.Neutronium;
            case COSMIC_NEUTRONIUM -> GTNAMaterials.CosmicNeutronium;
            case DEGENERATE -> GTNAMaterials.WhiteDwarfMatter;
            case MAGNETAR -> OriginalsMaterials.Magnetarium;
        };
        return m == null ? ItemStack.EMPTY : ChemicalHelper.get(TagPrefix.ingot, m);
    }

    // ------------------------------------------------------------------ stellar chamber

    static List<Page> stellar() {
        var core = new StellarCore();
        Material[] feed = { GTNAMaterials.MagnetohydrodynamicallyConstrainedStarMatter,
                GTNAMaterials.WhiteDwarfMatter, GTNAMaterials.RawStarMatter, GTMaterials.Neutronium };
        List<Page> pages = new ArrayList<>();
        // Feeding fluids cycle in the input tank (any of them feeds the star; the value is per litre).
        for (String mode : List.of("neutron_star", "magnetar", "collapse")) {
            List<Content> contents = new ArrayList<>();
            Material shown = switch (mode) {
                case "magnetar" -> feed[1];
                case "collapse" -> feed[0];
                default -> feed[2];
            };
            contents.add(Content.fluid(fluid(shown), 1000));
            var lines = InfoLines.create().usage(core.bandUpkeep());
            switch (mode) {
                case "neutron_star" -> {
                    contents.add(Content.item(ChemicalHelper.get(TagPrefix.dust, GTMaterials.Neutronium)));
                    if (GTNAMaterials.WhiteDwarfMatter != null)
                        contents.add(Content.item(ChemicalHelper.get(TagPrefix.dust, GTNAMaterials.WhiteDwarfMatter)));
                    lines.continuous().line(tr("stellar.cap"), InfoLines.format(core.extractionCap()),
                            InfoLines.tierName(core.extractionCap()))
                            .line(tr("stellar.tov"), fmt(StellarCore.TOV, 1))
                            .line(tr("stellar.feed"), fmt(StellarChamberMachine.feedValue(fluid(shown)) * 1e9, 0));
                }
                case "magnetar" -> {
                    contents.add(Content.item(ChemicalHelper.get(TagPrefix.gem, OriginalsMaterials.MagnetarCrystal)
                            .copyWithCount((int) StellarChamberMachine.MAGNETAR_CRYSTALS)));
                    lines.line(tr("stellar.magnetar")).line(tr("stellar.tov_exotic"), fmt(StellarCore.TOV_EXOTIC, 1))
                            .line(tr("stellar.field"), fmt(StellarCore.DIPOLE_FIELD * 4, 0))
                            .line(tr("stellar.feed"), fmt(StellarChamberMachine.feedValue(fluid(shown)) * 1e9, 0));
                }
                default -> {
                    var matter = ChemicalHelper.get(TagPrefix.dust, OriginalsMaterials.DegenerateCoreMatter);
                    contents.add(Content.item(matter.copyWithCount((int) StellarChamberMachine.CORE_MATTER)));
                    core.mode = StellarCore.Mode.BLACK_HOLE;
                    lines.line(tr("stellar.collapse"), fmt(StellarCore.TOV, 1))
                            .line(tr("stellar.cap"), InfoLines.format(core.extractionCap()),
                                    InfoLines.tierName(core.extractionCap()))
                            .line(tr("stellar.penrose"), fmt(100 * StellarCore.penroseEfficiency(StellarCore.MAX_SPIN),
                                    1))
                            .line(tr("stellar.feed"), fmt(StellarChamberMachine.feedValue(fluid(shown)) * 1e9, 0));
                    core.mode = StellarCore.Mode.NEUTRON_STAR;
                }
            }
            pages.add(new Page(STELLAR, mode, STELLAR_LAYOUT, fill(STELLAR_LAYOUT, contents), lines.build()));
        }
        return pages;
    }
}
