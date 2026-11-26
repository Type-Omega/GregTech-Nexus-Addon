package com.raishxn.gtna.client.hud;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;

import com.typeomega.lib.ui.hud.HudRegistry;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Copies legacy HUD coordinates from {@code config/gtna.yaml} into TypeOmega Lib's shared
 * {@code typeomegalib-huds.json} before {@code ConfigHolder.init()} rewrites {@code gtna.yaml}
 * without the removed position fields.
 */
@OnlyIn(Dist.CLIENT)
public final class GtnaHudMigration {

    private GtnaHudMigration() {}

    public static void migrateFromLegacyConfig() {
        Path legacyFile = FMLPaths.CONFIGDIR.get().resolve("gtna.yaml");
        if (!Files.exists(legacyFile)) return;
        try {
            List<String> lines = Files.readAllLines(legacyFile, StandardCharsets.UTF_8);
            Integer energyX = null;
            Integer energyY = null;
            Integer steamX = null;
            Integer steamY = null;
            boolean inClient = false;
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (!raw.startsWith(" ") && !raw.startsWith("\t") && line.endsWith(":")) {
                    inClient = line.equals("client:");
                    continue;
                }
                if (!inClient) continue;
                if (line.startsWith("wirelessEnergyHudX:")) energyX = parseInt(line, "wirelessEnergyHudX:");
                else if (line.startsWith("wirelessEnergyHudY:")) energyY = parseInt(line, "wirelessEnergyHudY:");
                else if (line.startsWith("wirelessSteamHudX:")) steamX = parseInt(line, "wirelessSteamHudX:");
                else if (line.startsWith("wirelessSteamHudY:")) steamY = parseInt(line, "wirelessSteamHudY:");
            }
            if (energyX != null && energyY != null) {
                HudRegistry.migratePositionIfAbsent(WirelessEnergyHudOverlay.HUD_ID, energyX, energyY);
            }
            if (steamX != null && steamY != null) {
                HudRegistry.migratePositionIfAbsent(WirelessSteamHudOverlay.HUD_ID, steamX, steamY);
            }
        } catch (Exception ignored) {
            // EN: Migration is best-effort; if gtna.yaml cannot be read, the HUDs fall back to their defaults.
            // PT: A migração é melhor-esforço; se o gtna.yaml não puder ser lido, os HUDs usam as posições padrão.
            // ZH: 迁移为尽力而为；若无法读取 gtna.yaml，HUD 将回退到默认位置。
        }
    }

    private static Integer parseInt(String line, String prefix) {
        try {
            return Integer.parseInt(line.substring(prefix.length()).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
