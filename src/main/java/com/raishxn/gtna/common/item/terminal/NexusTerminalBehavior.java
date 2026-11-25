package com.raishxn.gtna.common.item.terminal;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NexusTerminalBehavior implements IItemUIFactory, IAddInformation {

    public static final NexusTerminalBehavior INSTANCE = new NexusTerminalBehavior();

    protected NexusTerminalBehavior() {}

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder playerInventoryHolder, Player entityPlayer) {
        return new NexusTerminalUIFactory(playerInventoryHolder, entityPlayer).createModularUI();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            HeldItemUIFactory.INSTANCE.openUI(serverPlayer, usedHand);
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        ItemStack terminalStack = player.getItemInHand(context.getHand());

        // ── ME link: Shift+click any active ME node (no range or dimension limit, GTIA-D-439) ──
        if (player.isShiftKeyDown() && NexusAE2Link.isLinkable(level, blockPos)) {
            if (!level.isClientSide()) {
                NexusAE2Link.link(terminalStack, level, blockPos);
                player.displayClientMessage(Component.translatable("gtna.terminal.nexus.ae2.linked"), true);
                level.playSound(null, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.5f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // ── A running build job is cancelled by Shift+clicking a controller again ──
        if (player.isShiftKeyDown() && !level.isClientSide() &&
                com.raishxn.gtna.common.item.terminal.plan.NexusBuildJobs.cancel(player)) {
            return InteractionResult.SUCCESS;
        }

        // ── Shift+Click on controller: auto-build / replace / build module ────
        if (player.isShiftKeyDown()) {
            if (MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller) {
                if (terminalStack.getOrCreateTag().getBoolean("DismantleMode")) {
                    if (!level.isClientSide()) {
                        var result = com.raishxn.gtna.common.item.terminal.plan.NexusDismantle.run(player, controller,
                                terminalStack);
                        player.displayClientMessage(
                                com.raishxn.gtna.common.item.terminal.plan.NexusDismantle.message(result), true);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                NexusTerminalUIFactory.AutoBuildSetting setting = NexusTerminalUIFactory.AutoBuildSetting
                        .getSetting(terminalStack);
                // Build when the controller is unformed, when replace mode is on, or when a module
                // build is requested (the module must be addable to an already-formed multiblock).
                boolean buildModule = setting.getModuleBuild() > 0;
                if (!controller.isFormed() || buildModule || setting.isReplaceMode() ||
                        setting.isDemolitionMode() || setting.isMirrorBuild()) {
                    if (!level.isClientSide()) {
                        long started = System.nanoTime();
                        NexusAutoBuilder.autoBuild(player, controller, terminalStack);
                        GTNACORE.LOGGER.info("Nexus Terminal built {} in {} ms",
                                controller.self().getDefinition().getId(),
                                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        // ── Usage instructions ────────────────────────────────────────────────
        tooltipComponents.add(Component.translatable("item.gtna.nexus_structure_terminal.tooltip.use")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.gtna.nexus_structure_terminal.tooltip.shift_use")
                .withStyle(ChatFormatting.GRAY));

        // ── Replace mode indicator ────────────────────────────────────────────
        NexusTerminalUIFactory.AutoBuildSetting settings = NexusTerminalUIFactory.AutoBuildSetting.getSetting(stack);
        if (settings.isReplaceMode()) {
            tooltipComponents.add(Component.translatable(
                    "item.gtna.nexus_structure_terminal.tooltip.replace_mode_active")
                    .withStyle(ChatFormatting.GOLD));
        }

        // ── AE2 Network status ────────────────────────────────────────────────
        {
            tooltipComponents.add(Component.literal("")); // spacer

            if (NexusAE2Link.isLinked(stack)) {
                // Show linked status
                GlobalPos linkedPos = NexusAE2Link.getLinkedPosition(stack);
                if (linkedPos != null) {
                    tooltipComponents.add(Component.translatable(
                            "gtna.terminal.nexus.ae2.tooltip.linked",
                            linkedPos.pos().getX(),
                            linkedPos.pos().getY(),
                            linkedPos.pos().getZ())
                            .withStyle(ChatFormatting.GREEN));
                }

            } else {
                tooltipComponents.add(Component.translatable("gtna.terminal.nexus.ae2.tooltip.not_linked")
                        .withStyle(ChatFormatting.RED));
                tooltipComponents.add(Component.translatable("gtna.terminal.nexus.ae2.tooltip.how_to_link")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
