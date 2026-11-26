package com.raishxn.gtna.integration.ae2.wireless;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/** The stacks in a player's Curios slots. Only loaded when Curios is installed. */
final class WirelessCuriosAccess {

    private WirelessCuriosAccess() {}

    static List<ItemStack> stacks(Player player) {
        List<ItemStack> found = new ArrayList<>();
        try {
            CuriosApi.getCuriosInventory(player).resolve().ifPresent(inventory -> {
                for (var handler : inventory.getCurios().values()) {
                    var slots = handler.getStacks();
                    for (int i = 0; i < slots.getSlots(); i++) {
                        ItemStack stack = slots.getStackInSlot(i);
                        if (!stack.isEmpty()) found.add(stack);
                    }
                }
            });
        } catch (RuntimeException | LinkageError ignored) {
            // A Curios version without this API: the tool is then found in the hands and the inventory only.
        }
        return found;
    }
}
