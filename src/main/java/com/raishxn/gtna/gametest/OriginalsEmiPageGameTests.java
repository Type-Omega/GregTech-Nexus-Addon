package com.raishxn.gtna.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.api.machine.feature.collider.ColliderCore;
import com.raishxn.gtna.api.machine.feature.lance.LanceCore;
import com.raishxn.gtna.api.machine.feature.rift.RiftCore;
import com.raishxn.gtna.integration.emi.OriginalsEmiPlugin;
import com.raishxn.gtna.integration.originals.OriginalsPages;
import com.typeomega.lib.ui.PanelLayout;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Illustrated EMI pages of the originals (GTIA-D-435): the four categories are registered, every page fits its
 * picture and every number comes from the machine's core.
 */
@GameTestHolder(GTNACORE.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OriginalsEmiPageGameTests {

    private static final String TEMPLATE = "empty_12";

    private OriginalsEmiPageGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void everyLayoutFitsItsPicture(GameTestHelper h) {
        for (var layout : List.of(OriginalsPages.COLLIDER_LAYOUT, OriginalsPages.RIFT_LAYOUT,
                OriginalsPages.LANCE_LAYOUT, OriginalsPages.STELLAR_LAYOUT)) {
            h.assertTrue(layout.problems().isEmpty(), "layout " + layout.art() + ": " + layout.problems());
            h.assertTrue(OriginalsEmiPageGameTests.class.getResource("/assets/gtna/" +
                    layout.art().getPath()) != null, "missing art " + layout.art());
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void pagesFollowTheCores(GameTestHelper h) {
        var pages = OriginalsPages.all();
        int expected = ColliderCore.MAGNET_FIELD.length + 1 + RiftCore.Biome.values().length +
                LanceCore.Body.values().length + 3;
        h.assertTrue(pages.size() == expected, "pages " + pages.size() + ", expected " + expected);
        Set<String> ids = new HashSet<>();
        for (var page : pages) {
            h.assertTrue(ids.add(page.category() + "/" + page.id()), "repeated page " + page.id());
            h.assertTrue(page.lines().size() <= OriginalsPages.lineCount(page.category()),
                    page.id() + " has more lines than its category reserves");
            boolean output = false, input = false;
            for (int i = 0; i < page.contents().size(); i++) {
                var role = page.layout().slots().get(i).role();
                if (page.contents().get(i).isEmpty()) continue;
                output |= role == PanelLayout.Role.OUTPUT;
                input |= role == PanelLayout.Role.INPUT;
            }
            h.assertTrue(input && output, page.id() + " shows what goes in and what comes out");
        }
        // EN: electron beams stop at 2 x 100 GeV, so the e+e- page has no Higgs, top or X(Nexus).
        // PT: feixes de elétrons param em 2 x 100 GeV, então a página e+e- não tem Higgs, top nem X(Nexus).
        // ZH: 电子束上限为 2 × 100 GeV，因此正负电子页面没有希格斯、顶夸克和 X(Nexus)。
        var electrons = pages.stream().filter(p -> p.id().equals("electrons")).findFirst().orElseThrow();
        long particles = electrons.contents().stream().filter(c -> !c.isEmpty()).count() - 1;
        long below = java.util.Arrays.stream(ColliderCore.Particle.values())
                .filter(p -> p.thresholdGeV <= 2 * ColliderCore.ELECTRON_BEAM_GEV).count();
        h.assertTrue(particles == below, "e+e- page shows " + particles + " particles, the core allows " + below);
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void pluginRegistersTheFourCategories(GameTestHelper h) {
        // EN: the GameTest server has no EMI; the check runs where EMI is loaded (dev client, packs).
        // PT: o servidor de GameTest não tem EMI; a checagem roda onde o EMI está carregado (cliente de dev, packs).
        // ZH: GameTest 服务端没有 EMI；此检查在加载了 EMI 的环境中运行（开发客户端、整合包）。
        if (!ModList.get().isLoaded("emi")) {
            h.succeed();
            return;
        }
        EmiProbe.check(h);
        h.succeed();
    }

    /** Touches EMI types only when EMI is present, so the test class loads without it. */
    private static final class EmiProbe {

        static void check(GameTestHelper h) {
            List<ResourceLocation> categories = new ArrayList<>();
            List<ResourceLocation> workstations = new ArrayList<>();
            List<EmiRecipe> recipes = new ArrayList<>();
            var registry = (EmiRegistry) Proxy.newProxyInstance(EmiRegistry.class.getClassLoader(),
                    new Class<?>[] { EmiRegistry.class }, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "addCategory" -> categories.add(((EmiRecipeCategory) args[0]).getId());
                            case "addWorkstation" -> workstations.add(((EmiRecipeCategory) args[0]).getId());
                            case "addRecipe" -> recipes.add((EmiRecipe) args[0]);
                            default -> {}
                        }
                        return null;
                    });
            new OriginalsEmiPlugin().register(registry);
            h.assertTrue(categories.equals(OriginalsPages.CATEGORIES), "categories " + categories);
            h.assertTrue(workstations.equals(OriginalsPages.CATEGORIES), "each machine is its category's workstation");
            h.assertTrue(recipes.size() == OriginalsPages.all().size(),
                    "every page becomes an EMI recipe: " + recipes.size());
            for (var recipe : recipes) {
                h.assertTrue(!recipe.getInputs().isEmpty() && !recipe.getOutputs().isEmpty(),
                        recipe.getId() + " lists its inputs and outputs to EMI");
                h.assertTrue(recipe.getDisplayWidth() == OriginalsPages.COLLIDER_LAYOUT.panelWidth(),
                        recipe.getId() + " is as wide as the panel");
            }
        }
    }
}
