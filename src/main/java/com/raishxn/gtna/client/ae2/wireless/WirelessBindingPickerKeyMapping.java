// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.client.ae2.wireless;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.blaze3d.platform.InputConstants;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.integration.ae2.wireless.WirelessAePackets;
import com.raishxn.gtna.integration.ae2.wireless.WirelessNetworkBindingToolItem;
import com.raishxn.gtna.network.GTNANetworkHandler;
import org.lwjgl.glfw.GLFW;

/**
 * Opens the wireless network picker with a key press, so the binding tool can be used while it sits
 * in the Curios slot instead of the main hand.
 */
@Mod.EventBusSubscriber(modid = GTNACORE.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WirelessBindingPickerKeyMapping {

    public static final KeyMapping OPEN_PICKER = new KeyMapping(
            "key.gtna.wireless_binding_picker", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN, "key.categories.gtna");

    private WirelessBindingPickerKeyMapping() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PICKER);
    }

    @Mod.EventBusSubscriber(modid = GTNACORE.MOD_ID, value = Dist.CLIENT)
    static final class TickHandler {

        private TickHandler() {}

        /** Left-click in the air with the tool in the main hand clears its saved network. */
        @SubscribeEvent
        public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
            if (event.getEntity().getMainHandItem().getItem() instanceof WirelessNetworkBindingToolItem) {
                GTNANetworkHandler.CHANNEL.sendToServer(new WirelessAePackets.ClearBindingToolPacket());
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            while (OPEN_PICKER.consumeClick()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null && minecraft.screen == null) {
                    GTNANetworkHandler.CHANNEL.sendToServer(new WirelessAePackets.OpenBindingPickerPacket());
                }
            }
        }
    }
}
