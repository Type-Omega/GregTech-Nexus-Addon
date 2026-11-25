package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import appeng.api.stacks.AEItemKey;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAItems;
import com.raishxn.gtna.common.item.terminal.NexusBlockPattern;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory.AutoBuildSetting;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;
import com.raishxn.gtna.network.GTNANetworkHandler;
import com.raishxn.gtna.network.packet.SNexusScanHighlight;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Read-only scan of a structure (GTIA-D-439 P1.1): plans it with the terminal's settings, works out which item each
 * cell would get, and shows the result as a chat summary and coloured boxes in the world. Nothing is changed.
 * Left-clicking a controller with the terminal scans it (once per click: the client repeats the attack while the
 * button is held).
 */
@Mod.EventBusSubscriber(modid = GTNACORE.MOD_ID)
public final class NexusScan {

    public static final int MAX_BOXES = 4096;
    public static final int BOX_TICKS = 15 * 20;

    /** Box colours by what the build would do. */
    public static final int PLACE = 0x55FF55, MISSING = 0xFF55FF, REPLACE = 0xFFAA00, BLOCKED = 0xFF5555,
            CLEAR = 0xFFFF55, HATCH = 0x55FFFF;

    private static final Map<UUID, Long> LAST_SCAN = new HashMap<>();

    private NexusScan() {}

    /** A scan: the plans, the need per item and what the inventory and the ME hold of each. */
    public record Result(List<NexusBuildPlan> plans, Map<AEItemKey, Long> need, Map<AEItemKey, long[]> stock,
                         boolean creative, boolean me) {

        public int count(NexusBuildPlan.Kind kind) {
            int n = 0;
            for (NexusBuildPlan plan : plans) n += plan.count(kind);
            return n;
        }

        /** Cells that need an item and got none. */
        public int missing() {
            int n = 0;
            for (NexusBuildPlan plan : plans)
                for (NexusBuildPlan.Cell cell : plan.cells) if (cell.needsItem() && cell.chosen == null) n++;
            return n;
        }

        public int hatches() {
            int n = 0;
            for (NexusBuildPlan plan : plans)
                for (NexusBuildPlan.Cell cell : plan.cells) if (cell.hatch && cell.needsItem()) n++;
            return n;
        }
    }

    @Nullable
    public static Result scan(Player player, IMultiController controller, ItemStack terminal) {
        NexusBlockPattern pattern = NexusBlockPattern.fromBlockPattern(controller.getPattern());
        if (pattern == null) return null;
        AutoBuildSetting setting = AutoBuildSetting.getSetting(terminal);
        List<NexusBuildPlan> plans = pattern.plan(controller.getMultiblockState(), setting, terminal);
        NexusAE2Link.Session me = setting.isUseAE() && !player.isCreative() ? NexusAE2Link.open(terminal, player) :
                null;
        NexusMaterials materials = new NexusMaterials(player, me);
        materials.survey(plans);
        Map<AEItemKey, Long> need = materials.assign(plans);
        Map<AEItemKey, long[]> stock = new LinkedHashMap<>();
        for (AEItemKey key : need.keySet()) stock.put(key, materials.stockOf(key).clone());
        return new Result(plans, need, stock, player.isCreative(), me != null);
    }

    public static int color(NexusBuildPlan.Cell cell) {
        return switch (cell.kind) {
            case PLACE, REPLACE -> cell.chosen == null ? MISSING : cell.hatch ? HATCH :
                    cell.kind == NexusBuildPlan.Kind.PLACE ? PLACE : REPLACE;
            case BLOCKED -> BLOCKED;
            case CLEAR -> CLEAR;
            default -> 0;
        };
    }

    /** The boxes of a scan, at most {@code limit}: problems first (missing, blocked), then the rest. */
    public static SNexusScanHighlight boxes(Result result, int limit) {
        List<NexusBuildPlan.Cell> shown = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            for (NexusBuildPlan plan : result.plans) {
                for (NexusBuildPlan.Cell cell : plan.cells) {
                    int c = color(cell);
                    if (c == 0) continue;
                    boolean problem = c == MISSING || c == BLOCKED;
                    if (problem == (pass == 0) && shown.size() < limit) shown.add(cell);
                }
            }
        }
        long[] positions = new long[shown.size()];
        int[] colors = new int[shown.size()];
        for (int i = 0; i < shown.size(); i++) {
            positions[i] = shown.get(i).pos.asLong();
            colors[i] = color(shown.get(i));
        }
        return new SNexusScanHighlight(positions, colors, BOX_TICKS);
    }

    public static void scanAndShow(ServerPlayer player, IMultiController controller, ItemStack terminal) {
        Result result = scan(player, controller, terminal);
        if (result == null) return;
        player.sendSystemMessage(Component.empty().append(summary(result)).append(" ").append(NexusReport.link()));
        GTNANetworkHandler.sendToPlayer(boxes(result, MAX_BOXES), player);
        NexusLastScan.put(player, controller, result);
    }

    public static Component summary(Result result) {
        int place = result.count(NexusBuildPlan.Kind.PLACE), replace = result.count(NexusBuildPlan.Kind.REPLACE);
        if (place + replace + result.count(NexusBuildPlan.Kind.BLOCKED) + result.count(NexusBuildPlan.Kind.CLEAR) ==
                0) {
            return Component.translatable("gtna.terminal.nexus.scan.complete").withStyle(ChatFormatting.GREEN);
        }
        return Component.translatable("gtna.terminal.nexus.scan.summary",
                Component.literal(String.valueOf(place)).withStyle(ChatFormatting.GREEN),
                Component.literal(String.valueOf(replace)).withStyle(ChatFormatting.GOLD),
                Component.literal(String.valueOf(result.count(NexusBuildPlan.Kind.BLOCKED)))
                        .withStyle(ChatFormatting.RED),
                Component.literal(String.valueOf(result.count(NexusBuildPlan.Kind.CLEAR)))
                        .withStyle(ChatFormatting.YELLOW),
                Component.literal(String.valueOf(result.missing())).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(String.valueOf(result.hatches())).withStyle(ChatFormatting.AQUA));
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(GTNAItems.NEXUS_STRUCTURE_TERMINAL.get())) return;
        if (!(MetaMachine.getMachine(event.getLevel(), event.getPos()) instanceof IMultiController controller)) {
            return;
        }
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = event.getLevel().getGameTime();
        Long last = LAST_SCAN.put(player.getUUID(), now);
        if (last != null && now - last < 10) return;
        scanAndShow(player, controller, stack);
    }
}
