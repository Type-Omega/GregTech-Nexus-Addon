package com.raishxn.gtna;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import com.raishxn.gtna.client.ClientProxy;
import com.raishxn.gtna.common.CommonProxy;
import com.raishxn.gtna.common.data.condition.RestrictedItemsEnabledForgeCondition;
import com.raishxn.gtna.config.GTNAConfigBootstrap;
import com.raishxn.gtna.planner.neoforge.crafting.Ae2PlannerBridge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GTNACORE.MOD_ID)
public class GTNACORE {

    public static final String MOD_ID = "gtna";
    public static final Logger LOGGER = LogManager.getLogger();

    public GTNACORE() {
        // EN: Migrate old gtna.yaml HUD coordinates before ConfigHolder.init() strips the removed fields.
        // PT: Migra as posições antigas do gtna.yaml antes do ConfigHolder.init() remover os campos antigos.
        // ZH: 在 ConfigHolder.init() 移除旧字段前，先迁移 gtna.yaml 中的旧 HUD 坐标。
        DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> com.raishxn.gtna.client.hud.GtnaHudMigration::migrateFromLegacyConfig);
        GTNAConfigBootstrap.init();
        RestrictedItemsEnabledForgeCondition.register();
        // NOTE: the config screen factory is registered in ClientProxy, NOT here. ConfigScreenHandler
        // is client-only, and referencing it from this class (even inside a lambda) makes
        // RuntimeDistCleaner reject net.minecraft.client.Minecraft on a dedicated server, which
        // crashed mod loading outright.
        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) Ae2PlannerBridge.tick();
    }
}
