package com.raishxn.gtna.common.item.terminal.plan;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;

import java.util.*;
import java.util.concurrent.Future;

/**
 * Crafting requests of the Nexus Terminal (GTIA-D-439 T9): for every missing item that has a pattern in the linked
 * network, AE2 calculates the job on its own thread; when the plan is ready and complete it is submitted. The
 * status of each request is kept for the Materials tab and the report. Requests time out after 20 s.
 * <p>
 * The flow (calculate, then submit, with a timeout and a status per item) follows gt_shanhai's crafting manager
 * (LGPL-3.0), rewritten for GTNA.
 */
@Mod.EventBusSubscriber(modid = GTNACORE.MOD_ID)
public final class NexusCrafting {

    public enum Status {
        CALCULATING,
        SUBMITTED,
        NO_PATTERN,
        MISSING_INPUTS,
        FAILED
    }

    private static final int TIMEOUT_TICKS = 400;

    private record Pending(NexusAE2Link.Session me, AEItemKey key, Future<ICraftingPlan> plan, long started) {}

    private static final Map<UUID, List<Pending>> PENDING = new HashMap<>();
    private static final Map<UUID, Map<AEItemKey, Status>> STATUS = new HashMap<>();

    private NexusCrafting() {}

    /** Requests every key of {@code missing} (item to amount) that has a pattern; returns how many were started. */
    public static int request(Player player, NexusAE2Link.Session me, Map<AEItemKey, Long> missing) {
        var crafting = me.grid().getCraftingService();
        var statuses = STATUS.computeIfAbsent(player.getUUID(), u -> new LinkedHashMap<>());
        var pending = PENDING.computeIfAbsent(player.getUUID(), u -> new ArrayList<>());
        ICraftingSimulationRequester requester = me::source;
        int started = 0;
        long now = player.level().getGameTime();
        for (var entry : missing.entrySet()) {
            AEItemKey key = entry.getKey();
            if (entry.getValue() <= 0) continue;
            if (!crafting.isCraftable(key)) {
                statuses.put(key, Status.NO_PATTERN);
                continue;
            }
            if (statuses.get(key) == Status.CALCULATING) continue;
            var future = crafting.beginCraftingCalculation(player.level(), requester, key, entry.getValue(),
                    CalculationStrategy.REPORT_MISSING_ITEMS);
            pending.add(new Pending(me, key, future, now));
            statuses.put(key, Status.CALCULATING);
            started++;
        }
        return started;
    }

    public static Map<AEItemKey, Status> statuses(Player player) {
        return STATUS.getOrDefault(player.getUUID(), Map.of());
    }

    public static void clear(Player player) {
        STATUS.remove(player.getUUID());
        PENDING.remove(player.getUUID());
    }

    /** Polls every calculation once; for the tick handler and tests. */
    public static void poll(net.minecraft.server.MinecraftServer server) {
        var it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var player = server.getPlayerList().getPlayer(entry.getKey());
            var statuses = STATUS.computeIfAbsent(entry.getKey(), u -> new LinkedHashMap<>());
            entry.getValue().removeIf(p -> {
                if (!p.plan.isDone()) {
                    if (server.overworld().getGameTime() - p.started > TIMEOUT_TICKS) {
                        p.plan.cancel(true);
                        statuses.put(p.key, Status.FAILED);
                        return true;
                    }
                    return false;
                }
                statuses.put(p.key, submit(p));
                return true;
            });
            if (entry.getValue().isEmpty()) {
                it.remove();
                if (player != null) player.displayClientMessage(summary(statuses), false);
            }
        }
    }

    private static Status submit(Pending p) {
        try {
            ICraftingPlan plan = p.plan.get();
            if (plan.simulation()) return Status.MISSING_INPUTS;
            IActionSource source = p.me.source();
            var result = p.me.grid().getCraftingService().submitJob(plan, null, null, false, source);
            return result.successful() ? Status.SUBMITTED : Status.FAILED;
        } catch (Exception e) {
            return Status.FAILED;
        }
    }

    static Component summary(Map<AEItemKey, Status> statuses) {
        int[] n = new int[Status.values().length];
        for (Status s : statuses.values()) n[s.ordinal()]++;
        return Component.translatable("gtna.terminal.nexus.craft.summary", n[Status.SUBMITTED.ordinal()],
                n[Status.NO_PATTERN.ordinal()], n[Status.MISSING_INPUTS.ordinal()], n[Status.FAILED.ordinal()]);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !PENDING.isEmpty()) poll(event.getServer());
    }
}
