package com.raishxn.gtna.client.hud;

import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.raishxn.gtna.config.ConfigHolder;
import com.typeomega.lib.ui.hud.MoveableHud;

/**
 * Wireless steam network HUD registered with TypeOmega Lib's shared HUD editor.
 */
@OnlyIn(Dist.CLIENT)
public final class WirelessSteamHudOverlay implements MoveableHud {

    public static final WirelessSteamHudOverlay INSTANCE = new WirelessSteamHudOverlay();
    public static final String HUD_ID = "gtna:wireless_steam";

    private static final int GRAPH_WIDTH = 96;
    private static final int GRAPH_HEIGHT = 24;
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;

    private static final int COLOR_BACKGROUND = 0x8A404040;
    private static final int COLOR_BORDER = 0xCC000000;
    private static final int COLOR_GRAPH_BACKGROUND = 0x40000000;
    private static final int COLOR_GRAPH_LINE = 0xFF4FC3F7;
    private static final int COLOR_GRAPH_FILL = 0x402ECC71;
    private static final int COLOR_TEXT = 0xFFFFFF;

    private WirelessSteamHudOverlay() {}

    @Override
    public String id() {
        return HUD_ID;
    }

    @Override
    public Component displayName() {
        return Component.translatable("gtna.hud.wireless_steam.name");
    }

    @Override
    public int defaultXPercent() {
        return 5;
    }

    @Override
    public int defaultYPercent() {
        return 75;
    }

    @Override
    public int width() {
        Font font = Minecraft.getInstance().font;
        if (font == null) return 0;
        return contentWidth(font, lines()) + PADDING * 2;
    }

    @Override
    public int height() {
        ConfigHolder config = ConfigHolder.INSTANCE;
        if (config == null) return 0;
        return panelHeight(config, lines().length);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        ConfigHolder config = ConfigHolder.INSTANCE;
        if (config == null) return;
        Font font = Minecraft.getInstance().font;
        Component[] lines = lines();
        int contentWidth = Math.max(GRAPH_WIDTH, width - PADDING * 2);
        boolean drawGraph = config.client.wirelessSteamHudHistorySeconds > 0;

        graphics.fill(x, y, x + width, y + height, COLOR_BACKGROUND);
        MoveableHud.drawOutline(graphics, x, y, width, height, COLOR_BORDER);

        int textY = y + PADDING;
        for (Component line : lines) {
            graphics.drawString(font, line, x + PADDING, textY, COLOR_TEXT, true);
            textY += LINE_HEIGHT;
        }

        if (drawGraph) {
            int graphX = x + PADDING;
            int graphY = textY + 2;
            graphics.fill(graphX, graphY, graphX + contentWidth, graphY + GRAPH_HEIGHT, COLOR_GRAPH_BACKGROUND);
            drawSparkline(graphics, graphX, graphY, contentWidth, GRAPH_HEIGHT,
                    WirelessSteamHudState.history(config.client.wirelessSteamHudHistorySeconds));
            MoveableHud.drawOutline(graphics, graphX, graphY, contentWidth, GRAPH_HEIGHT, COLOR_BORDER);
        }
    }

    @Override
    public boolean isEnabled() {
        return ConfigHolder.INSTANCE != null && ConfigHolder.INSTANCE.client.wirelessSteamHud;
    }

    @Override
    public void setEnabled(boolean enabled) {
        HudConfigValues.set("wirelessSteamHud", enabled);
    }

    @Override
    public boolean shouldRenderInGame() {
        return WirelessSteamHudState.hasData() && WirelessSteamHudState.hasNetwork();
    }

    private static Component[] lines() {
        Component title = Component.translatable("gtna.hud.wireless_steam.balance",
                Component.literal(FormattingUtil.formatNumbers(WirelessSteamHudState.getBalance()))
                        .withStyle(ChatFormatting.AQUA));
        Component flow = Component.translatable("gtna.hud.wireless_steam.flow",
                Component.literal(FormattingUtil.formatNumbers(WirelessSteamHudState.getAddedPerSecond()))
                        .withStyle(ChatFormatting.GREEN),
                Component.literal(FormattingUtil.formatNumbers(WirelessSteamHudState.getConsumedPerSecond()))
                        .withStyle(ChatFormatting.RED));
        Component hatches = Component.translatable("gtna.hud.wireless_steam.hatches",
                WirelessSteamHudState.getInputHatches(), WirelessSteamHudState.getOutputHatches());
        return new Component[] { title, flow, hatches };
    }

    private static int contentWidth(Font font, Component[] lines) {
        int textWidth = 0;
        for (Component line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        return Math.max(GRAPH_WIDTH, textWidth);
    }

    private static int panelHeight(ConfigHolder config, int lineCount) {
        int graphHeight = config.client.wirelessSteamHudHistorySeconds > 0 ? GRAPH_HEIGHT + 2 : 0;
        return lineCount * LINE_HEIGHT + graphHeight + PADDING * 2;
    }

    private static void drawSparkline(GuiGraphics graphics, int x, int y, int width, int height, long[] samples) {
        if (samples.length < 2 || width <= 0 || height <= 0) return;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (long value : samples) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        long range = max - min;
        int bottom = y + height - 1;
        int columns = samples.length;
        for (int i = 0; i < columns; i++) {
            int columnX = x + (int) ((long) i * (width - 1) / Math.max(1, columns - 1));
            int nextX = i == columns - 1 ? x + width :
                    x + (int) ((long) (i + 1) * (width - 1) / Math.max(1, columns - 1)) + 1;
            int valueY = range <= 0 ? y + height / 2 :
                    bottom - (int) ((samples[i] - min) * (height - 1) / range);
            graphics.fill(columnX, valueY, Math.max(columnX + 1, nextX), bottom, COLOR_GRAPH_FILL);
            graphics.fill(columnX, valueY, Math.max(columnX + 1, nextX), valueY + 1, COLOR_GRAPH_LINE);
        }
    }
}
