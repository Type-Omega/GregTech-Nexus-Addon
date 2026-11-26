package com.raishxn.gtna.integration.ae2.wireless;

import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.MinecraftForge;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.api.registry.GTNARegistry;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.MenuEntry;

/**
 * Wireless ME network (D-444), ported from GTLCore: the Wireless ME Network Core, the Wireless ME Network Binding
 * Tool and their menus. Registration only; the behaviour lives in the classes next to this one.
 */
public final class WirelessAeContent {

    public static final BlockEntry<WirelessNetworkCoreBlock> WIRELESS_NETWORK_CORE = GTNARegistry.REGISTRATE
            .block("wireless_network_core", properties -> new WirelessNetworkCoreBlock())
            .lang("Wireless ME Network Core")
            .addLayer(() -> RenderType::cutout)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL, CustomTags.MINEABLE_WITH_WRENCH)
            .blockstate((context, provider) -> {})
            .loot((tables, block) -> tables.dropSelf(block))
            .item()
            .model((context, provider) -> {})
            .build()
            .register();

    public static final BlockEntityEntry<WirelessNetworkCoreBlockEntity> WIRELESS_NETWORK_CORE_BE = GTNARegistry.REGISTRATE
            .blockEntity("wireless_network_core", WirelessNetworkCoreBlockEntity::new)
            .validBlock(WIRELESS_NETWORK_CORE)
            .register();

    public static final ItemEntry<WirelessNetworkBindingToolItem> WIRELESS_NETWORK_BINDING_TOOL = GTNARegistry.REGISTRATE
            .item("wireless_network_binding_tool", properties -> new WirelessNetworkBindingToolItem())
            .lang("Wireless ME Network Binding Tool")
            .model((context, provider) -> provider.generated(context,
                    GTNACORE.id("item/wireless_network_binding_tool")))
            .register();

    public static final MenuEntry<WirelessNetworkCoreMenu> WIRELESS_NETWORK_CORE_MENU = GTNARegistry.REGISTRATE
            .menu("wireless_network_core", WirelessNetworkCoreMenu::new,
                    () -> com.raishxn.gtna.client.ae2.wireless.WirelessNetworkCoreScreen::new)
            .register();

    public static final MenuEntry<WirelessNetworkPickerMenu> WIRELESS_NETWORK_PICKER_MENU = GTNARegistry.REGISTRATE
            .menu("wireless_network_picker", WirelessNetworkPickerMenu::new,
                    () -> com.raishxn.gtna.client.ae2.wireless.WirelessNetworkPickerScreen::new)
            .register();

    private WirelessAeContent() {}

    private static boolean initialized;

    /** Safe to call more than once: both proxies reach it. */
    public static void init() {
        if (initialized) return;
        initialized = true;
        WirelessAeLang.init();
        WirelessAeNetworkRuntime.register(MinecraftForge.EVENT_BUS);
    }
}
