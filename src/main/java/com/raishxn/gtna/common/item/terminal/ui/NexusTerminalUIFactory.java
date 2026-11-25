package com.raishxn.gtna.common.item.terminal.ui;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Opens the Nexus Terminal window ({@link NexusTerminalWindow}) and reads the terminal's settings from its NBT.
 */
public class NexusTerminalUIFactory {

    private final HeldItemUIFactory.HeldItemHolder holder;
    private final Player player;
    private final ItemStack stack;

    public NexusTerminalUIFactory(HeldItemUIFactory.HeldItemHolder holder, Player player) {
        this.holder = holder;
        this.player = player;
        this.stack = holder.getHeld();
    }

    public ModularUI createModularUI() {
        var window = new NexusTerminalWindow(player, stack).build();
        return new ModularUI(new Size(NexusTerminalWindow.WIDTH, window.totalHeight()), holder, player)
                .widget(window.build());
    }

    @Getter
    @Setter
    public static class AutoBuildSetting {

        private int repetitions = 0;
        private int moduleBuild = 0;
        private boolean replaceMode = false;
        private boolean demolitionMode = false;
        private boolean useAE = false;
        private boolean mirrorBuild = false;
        private boolean noHatchMode = false;

        // Block selection indices (-1 = default/no selection)
        private int selectedCoilIndex = -1;
        private int selectedCasingIndex = -1;
        private int selectedMufflerIndex = -1;
        private int selectedRotorIndex = -1;
        private int selectedCapacitorIndex = -1;

        /**
         * Read settings from an ItemStack's NBT tag.
         */
        public static AutoBuildSetting getSetting(ItemStack stack) {
            AutoBuildSetting setting = new AutoBuildSetting();
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                setting.repetitions = Math.max(0, Math.min(1000, tag.getInt("Repetitions")));
                setting.moduleBuild = Math.max(0, Math.min(100, tag.getInt("ModuleBuild")));
                setting.replaceMode = tag.getBoolean("ReplaceMode");
                setting.demolitionMode = tag.getBoolean("DemolitionMode");
                setting.useAE = tag.getBoolean("UseAE");
                setting.mirrorBuild = tag.getBoolean("MirrorBuild");
                setting.noHatchMode = tag.getBoolean("NoHatchMode");

                // Block selections
                setting.selectedCoilIndex = tag.contains("SelectedCoil") ? tag.getInt("SelectedCoil") : -1;
                setting.selectedCasingIndex = tag.contains("SelectedCasing") ? tag.getInt("SelectedCasing") : -1;
                setting.selectedMufflerIndex = tag.contains("SelectedMuffler") ? tag.getInt("SelectedMuffler") : -1;
                setting.selectedRotorIndex = tag.contains("SelectedRotor") ? tag.getInt("SelectedRotor") : -1;
                setting.selectedCapacitorIndex = tag.contains("SelectedCapacitor") ? tag.getInt("SelectedCapacitor") :
                        -1;
            }
            return setting;
        }

        /**
         * Write settings to an ItemStack's NBT tag.
         */
        public void save(ItemStack stack) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt("Repetitions", repetitions);
            tag.putInt("ModuleBuild", moduleBuild);
            tag.putBoolean("ReplaceMode", replaceMode);
            tag.putBoolean("DemolitionMode", demolitionMode);
            tag.putBoolean("UseAE", useAE);
            tag.putBoolean("MirrorBuild", mirrorBuild);
            tag.putBoolean("NoHatchMode", noHatchMode);
            tag.putInt("SelectedCoil", selectedCoilIndex);
            tag.putInt("SelectedCasing", selectedCasingIndex);
            tag.putInt("SelectedMuffler", selectedMufflerIndex);
            tag.putInt("SelectedRotor", selectedRotorIndex);
            tag.putInt("SelectedCapacitor", selectedCapacitorIndex);
        }

        /**
         * Get the selected ItemStack for the coil category.
         */
        public ItemStack getSelectedCoil() {
            return BlockSelectionConfigWidget.getCoilEntries().size() > selectedCoilIndex && selectedCoilIndex >= 0 ?
                    BlockSelectionConfigWidget.getCoilEntries().get(selectedCoilIndex) : null;
        }
    }
}
