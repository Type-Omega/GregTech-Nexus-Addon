package com.raishxn.gtna.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.raishxn.gtna.config.ConfigHolder;
import com.typeomega.lib.ui.hud.MoveableHud;

/** Moveable, client-configurable readout of the player's Nexus Flux Matrix network. */
@OnlyIn(Dist.CLIENT)
public final class WirelessEnergyHudOverlay implements MoveableHud {

    public static final WirelessEnergyHudOverlay INSTANCE = new WirelessEnergyHudOverlay();
    public static final String HUD_ID = "gtna:wireless_energy";

    private WirelessEnergyHudOverlay() {}

    @Override
    public String id() {
        return HUD_ID;
    }

    @Override
    public Component displayName() {
        return Component.translatable("gtna.hud.wireless_energy.name");
    }

    @Override
    public int defaultXPercent() {
        return 5;
    }

    @Override
    public int defaultYPercent() {
        return 60;
    }

    @Override
    public int width() {
        var font = Minecraft.getInstance().font;
        int width = 96;
        for (Component line : lines()) {
            width = Math.max(width, font.width(line));
        }
        return width + 8;
    }

    @Override
    public int height() {
        return 38;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0x8A404040);
        MoveableHud.drawOutline(graphics, x, y, width, height, 0xCC000000);
        var font = Minecraft.getInstance().font;
        Component[] lines = lines();
        for (int i = 0; i < lines.length; i++) {
            graphics.drawString(font, lines[i], x + 4, y + 4 + i * 10, 0xFFFFFF);
        }
    }

    @Override
    public boolean isEnabled() {
        return ConfigHolder.INSTANCE != null && ConfigHolder.INSTANCE.client.wirelessEnergyHud;
    }

    @Override
    public void setEnabled(boolean enabled) {
        HudConfigValues.set("wirelessEnergyHud", enabled);
    }

    @Override
    public boolean shouldRenderInGame() {
        return WirelessEnergyHudState.hasNetwork();
    }

    private static Component[] lines() {
        return new Component[] {
                Component.translatable("gtna.hud.wireless_energy.balance", WirelessEnergyHudState.balance(),
                        WirelessEnergyHudState.capacity()),
                Component.translatable("gtna.hud.wireless_energy.flow", WirelessEnergyHudState.input(),
                        WirelessEnergyHudState.output()),
                Component.translatable("gtna.hud.wireless_energy.connections", WirelessEnergyHudState.connections()) };
    }
}
