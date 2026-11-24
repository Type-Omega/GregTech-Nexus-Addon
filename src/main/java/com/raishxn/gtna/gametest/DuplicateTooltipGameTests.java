package com.raishxn.gtna.gametest;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raishxn.gtna.GTNACORE;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * No GTNA item, block item or machine shows the same tooltip line twice (a line added by the builder and again by
 * the lang {@code .tooltip} key or by {@code appendHoverText}).
 */
@GameTestHolder(GTNACORE.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DuplicateTooltipGameTests {

    private static final JsonObject EN = lang();

    @GameTest(template = "empty_12", timeoutTicks = 40)
    public static void noGtnaItemRepeatsATooltipLine(GameTestHelper h) {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item> entry : ForgeRegistries.ITEMS.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            if (!GTNACORE.MOD_ID.equals(id.getNamespace())) continue;
            List<Component> lines = new ArrayList<>();
            try {
                entry.getValue().appendHoverText(new ItemStack(entry.getValue()), h.getLevel(), lines,
                        TooltipFlag.Default.NORMAL);
            } catch (Throwable clientOnly) {
                // EN: Some tooltips read client-only state (key presses); the dedicated test server cannot build them.
                // PT: Algumas tooltips leem estado só do cliente (teclas); o servidor de teste não consegue montá-las.
                // ZH: 部分提示读取仅客户端的状态（按键）；专用测试服务器无法构建它们。
                continue;
            }
            checked++;
            // EN: GTCEu's MetaMachineBlock adds "<ns>.machine.<id>.tooltip" by itself when the key exists; the test
            // server may not load mod lang files, so the key is added here from the packaged en_us.
            // PT: O MetaMachineBlock do GTCEu já adiciona "<ns>.machine.<id>.tooltip" quando a chave existe; o servidor
            // de teste pode não carregar os lang dos mods, então a chave é adicionada aqui a partir do en_us
            // empacotado.
            // ZH: 当键存在时，GTCEu 的 MetaMachineBlock 会自行添加 "<ns>.machine.<id>.tooltip"；测试服务器可能不加载
            // 模组语言文件，因此这里根据打包的 en_us 补上该键。
            String mainKey = GTNACORE.MOD_ID + ".machine." + id.getPath() + ".tooltip";
            if (entry.getValue() instanceof BlockItem bi && bi.getBlock() instanceof MetaMachineBlock &&
                    EN.has(mainKey) && !Language.getInstance().has(mainKey)) {
                lines.add(Math.min(1, lines.size()), Component.translatable(mainKey));
            }
            Set<String> seen = new HashSet<>();
            for (Component line : lines) {
                String text = (line.getContents() instanceof TranslatableContents t && EN.has(t.getKey()) &&
                        !Language.getInstance().has(t.getKey())) ?
                                t.getKey() + java.util.Arrays.toString(t.getArgs()) + line.getSiblings() :
                                line.getString().trim();
                if (text.isEmpty() || isSeparator(text)) continue;
                if (!seen.add(text)) failures.add(id + ": \"" + text + "\"");
            }
        }
        h.assertTrue(checked > 100, "only " + checked + " GTNA items had a tooltip built");
        h.assertTrue(failures.isEmpty(), failures.size() + " repeated tooltip lines: " + String.join("; ", failures));
        h.succeed();
    }

    /** Ruler lines made only of dashes or box characters may repeat between sections. */
    private static boolean isSeparator(String text) {
        return text.chars().noneMatch(Character::isLetterOrDigit);
    }

    private static JsonObject lang() {
        String path = "assets/gtna/lang/en_us.json";
        try (var in = DuplicateTooltipGameTests.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    private DuplicateTooltipGameTests() {}
}
