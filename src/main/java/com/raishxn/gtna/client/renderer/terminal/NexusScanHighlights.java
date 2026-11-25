package com.raishxn.gtna.client.renderer.terminal;

import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.raishxn.gtna.GTNACORE;

/**
 * Draws the coloured boxes of the last Nexus Terminal scan through terrain, so a hidden missing block is found
 * (GTIA-D-439 P1.1). One batch for all faces and one for all frames.
 */
@Mod.EventBusSubscriber(modid = GTNACORE.MOD_ID, value = Dist.CLIENT)
public final class NexusScanHighlights {

    private static long[] positions = new long[0];
    private static int[] colors = new int[0];
    private static int ticksLeft;

    private NexusScanHighlights() {}

    public static void show(long[] newPositions, int[] newColors, int ticks) {
        positions = newPositions;
        colors = newColors;
        ticksLeft = ticks;
    }

    public static int count() {
        return ticksLeft > 0 ? positions.length : 0;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticksLeft > 0) ticksLeft--;
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS || ticksLeft <= 0 ||
                positions.length == 0 || Minecraft.getInstance().level == null) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        Vec3 camera = event.getCamera().getPosition();
        pose.translate(-camera.x, -camera.y, -camera.z);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        // Fade out over the last second.
        float fade = Math.min(1f, ticksLeft / 20f);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for (int i = 0; i < positions.length; i++) {
            BlockPos p = BlockPos.of(positions[i]);
            int c = colors[i];
            RenderBufferUtils.renderCubeFace(pose, buffer, p.getX() + 0.02f, p.getY() + 0.02f, p.getZ() + 0.02f,
                    p.getX() + 0.98f, p.getY() + 0.98f, p.getZ() + 0.98f, (c >> 16 & 255) / 255f,
                    (c >> 8 & 255) / 255f, (c & 255) / 255f, 0.22f * fade, true);
        }
        tesselator.end();
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(2.5f);
        for (int i = 0; i < positions.length; i++) {
            BlockPos p = BlockPos.of(positions[i]);
            int c = colors[i];
            RenderBufferUtils.drawCubeFrame(pose, buffer, p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1,
                    p.getZ() + 1, (c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, 0.85f * fade);
        }
        tesselator.end();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        pose.popPose();
    }
}
