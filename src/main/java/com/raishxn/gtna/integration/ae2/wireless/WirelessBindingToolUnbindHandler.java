package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.raishxn.gtna.GTNACORE;

/**
 * Sneak + left-click on a bound block with the tool in the main hand unlinks it. The click is cancelled, so the
 * block is not broken.
 */
@Mod.EventBusSubscriber(modid = GTNACORE.MOD_ID)
public final class WirelessBindingToolUnbindHandler {

    private WirelessBindingToolUnbindHandler() {}

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getEntity().isShiftKeyDown() ||
                !(event.getEntity().getMainHandItem().getItem() instanceof WirelessNetworkBindingToolItem)) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START &&
                event.getEntity() instanceof ServerPlayer player && !event.getLevel().isClientSide) {
            WirelessLinks.unbindWithTool(player, event.getPos());
        }
    }
}
