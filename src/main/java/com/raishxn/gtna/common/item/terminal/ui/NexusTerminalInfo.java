package com.raishxn.gtna.common.item.terminal.ui;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.item.terminal.plan.NexusBuildJobs;
import com.raishxn.gtna.common.item.terminal.plan.NexusBuildPlan;
import com.raishxn.gtna.common.item.terminal.plan.NexusDismantle;
import com.raishxn.gtna.common.item.terminal.plan.NexusLastScan;
import com.raishxn.gtna.common.item.terminal.plan.NexusScan;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;
import org.jetbrains.annotations.Nullable;

/**
 * The texts the server owns on the Nexus Terminal screen: the target, the mode badge, the scan summary and the
 * Dismantle warning. The screen shows them in synced labels, so every method returns an empty string on the client.
 */
public final class NexusTerminalInfo {

    private static final int SUBTITLE_MAX = 46;

    private NexusTerminalInfo() {}

    public static boolean dismantleOn(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(NexusDismantle.MODE_KEY);
    }

    @Nullable
    static NexusScan.Result last(Player player) {
        NexusLastScan.Entry entry = player.level().isClientSide ? null : NexusLastScan.get(player);
        return entry == null ? null : entry.result();
    }

    /** The controller of the last scan, when it is still there. */
    @Nullable
    public static IMultiController target(Player player) {
        NexusLastScan.Entry last = player.level().isClientSide || player.getServer() == null ? null :
                NexusLastScan.get(player);
        if (last == null) return null;
        var level = player.getServer().getLevel(last.controller().dimension());
        return level != null && MetaMachine.getMachine(level, last.controller().pos()) instanceof IMultiController c ?
                c : null;
    }

    /** "Target: machine [x, y, z]" for the title bar. */
    public static String targetLine(Player player) {
        if (player.level().isClientSide) return "";
        IMultiController c = target(player);
        if (c == null) return Component.translatable("gtna.terminal.nexus.ui.no_target").getString();
        var pos = c.self().getPos();
        String name = c.self().getDefinition().getItem().getDescription().getString();
        String text = Component.translatable("gtna.terminal.nexus.ui.target", name, pos.getX(), pos.getY(),
                pos.getZ()).getString();
        return text.length() > SUBTITLE_MAX ? text.substring(0, SUBTITLE_MAX - 1) + "…" : text;
    }

    /** The mode badge: Build, Dismantle or the running job. */
    public static String badge(Player player, ItemStack stack) {
        if (player.level().isClientSide) return "";
        if (NexusBuildJobs.isRunning(player)) return "§b" + tr("ui.mode.running");
        return dismantleOn(stack) ? "§c" + tr("ui.mode.dismantle") : "§d" + tr("ui.mode.build");
    }

    public static boolean jobRunning(Player player) {
        return !player.level().isClientSide && NexusBuildJobs.isRunning(player);
    }

    /** ME state in one short word, for the narrow step column. */
    public static String meShort(Player player, ItemStack stack) {
        if (player.level().isClientSide) return "";
        if (!NexusAE2Link.isLinked(stack)) return tr("me.short.unlinked");
        return tr(NexusAE2Link.grid(stack, player.getServer()) != null ? "me.online" : "me.offline");
    }

    /**
     * A line of the Structure step's lower panel: the last scan while Dismantle is off, the Dismantle warning with
     * its preview while it is on.
     */
    public static String panelLine(Player player, ItemStack stack, int index) {
        if (player.level().isClientSide) return "";
        if (dismantleOn(stack)) return NexusDismantleView.line(player, stack, index);
        NexusDismantleView.forget(player);
        if (index == 0) return "§d" + tr("last_scan");
        return index <= 4 ? scanLine(player, index - 1) : "";
    }

    /** The scan summary, line 0 to 3, with the numbers in the colours of the boxes in the world. */
    public static String scanLine(Player player, int line) {
        NexusScan.Result r = last(player);
        if (r == null) return line == 0 ? tr("no_scan") : "";
        return switch (line) {
            case 0 -> tr("line.place", n("§a", r.count(NexusBuildPlan.Kind.PLACE)),
                    n("§6", r.count(NexusBuildPlan.Kind.REPLACE)));
            case 1 -> tr("line.blocked", n("§c", r.count(NexusBuildPlan.Kind.BLOCKED)),
                    n("§e", r.count(NexusBuildPlan.Kind.CLEAR)));
            case 2 -> tr("line.missing", n("§d", r.missing()));
            default -> tr("line.hatches", n("§b", r.hatches()));
        };
    }

    private static String n(String colour, int value) {
        return colour + value + "§f";
    }

    private static String tr(String key, Object... args) {
        return Component.translatable("gtna.terminal.nexus." + key, args).getString();
    }
}
