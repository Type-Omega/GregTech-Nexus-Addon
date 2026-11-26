// Ported from GTLCore (LGPL-3.0), https://github.com/AaAdoniSsS/GTLCore
package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * Finds the wireless network binding tool wherever the player carries it: the Curios slots first (when Curios is
 * installed), then the hands, then the inventory.
 */
public final class WirelessBindingToolLocator {

    private WirelessBindingToolLocator() {}

    public static ItemStack find(Player player) {
        // EN: Curios is optional: its API is only touched after the ModList check, in a class of its own.
        // PT: O Curios é opcional: a API dele só é tocada depois do teste no ModList, numa classe separada.
        // ZH: Curios 是可选依赖：只有通过 ModList 检查后，才会在单独的类中访问它的 API。
        if (ModList.get().isLoaded("curios")) {
            for (ItemStack stack : WirelessCuriosAccess.stacks(player)) {
                if (stack.getItem() instanceof WirelessNetworkBindingToolItem) {
                    return stack;
                }
            }
        }
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof WirelessNetworkBindingToolItem) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof WirelessNetworkBindingToolItem) {
            return offHand;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof WirelessNetworkBindingToolItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
