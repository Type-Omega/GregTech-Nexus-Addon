package com.raishxn.gtna.common.item.terminal.plan;

import com.gregtechceu.gtceu.api.pattern.MultiblockState;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.item.terminal.NexusBuildCheckGuard;

import java.util.*;

/**
 * Builds that are too big for one tick (GTIA-D-439 T4): above {@link #INSTANT_LIMIT} cells of work the build becomes a
 * server job that places at most {@link #BLOCKS_PER_TICK} blocks or {@link #NANOS_PER_TICK} per tick, shows its
 * progress on the action bar and can be cancelled; the reserved material goes back when it is cancelled.
 */
@Mod.EventBusSubscriber(modid = GTNACORE.MOD_ID)
public final class NexusBuildJobs {

    // EN: ~4096 cells fit in one tick on a normal server (the 1882 ms build was ~20k cells, two setBlock each);
    // EN: 2048 blocks or 2 ms per tick keeps a job under a tenth of the 50 ms tick.
    // PT: ~4096 células cabem num tick num servidor comum (o build de 1882 ms tinha ~20 mil células, dois setBlock
    // PT: cada); 2048 blocos ou 2 ms por tick mantêm o job abaixo de um décimo do tick de 50 ms.
    // ZH: 普通服务器一刻能放下约 4096 格（耗时 1882 ms 的那次约 2 万格、每格两次 setBlock）；
    // ZH: 每刻 2048 个方块或 2 ms，让任务占用不到 50 ms 一刻的十分之一。
    public static final int INSTANT_LIMIT = 4096;
    public static final int BLOCKS_PER_TICK = 2048;
    public static final long NANOS_PER_TICK = 2_000_000L;

    private static final Map<UUID, Job> JOBS = new LinkedHashMap<>();

    private NexusBuildJobs() {}

    public record Job(Player player, MultiblockState state, NexusBuildExecutor executor, NexusMaterials materials) {}

    /** Runs the build now when it is small, otherwise queues it. Returns true when it was queued. */
    public static boolean start(Player player, MultiblockState state, NexusBuildExecutor executor,
                                NexusMaterials materials, int work) {
        if (work <= INSTANT_LIMIT) {
            NexusBuildCheckGuard.run(state, () -> {
                executor.step(Integer.MAX_VALUE, Long.MAX_VALUE);
                executor.finish();
            });
            materials.returnLeftovers();
            return false;
        }
        cancel(player);
        JOBS.put(player.getUUID(), new Job(player, state, executor, materials));
        return true;
    }

    public static boolean isRunning(Player player) {
        return JOBS.containsKey(player.getUUID());
    }

    /** Stops the player's job and returns what it had not placed. Returns true if there was one. */
    public static boolean cancel(Player player) {
        Job job = JOBS.remove(player.getUUID());
        if (job == null) return false;
        NexusBuildCheckGuard.run(job.state, job.executor::finish);
        job.materials.returnLeftovers();
        player.displayClientMessage(Component.translatable("gtna.terminal.nexus.job.cancelled",
                job.executor.placed + job.executor.replaced), true);
        return true;
    }

    /** Runs the player's job until done (at most 10 000 rounds); for tests and dev tools. */
    public static void drain(Player player) {
        for (int round = 0; round < 10_000; round++) {
            Job job = JOBS.get(player.getUUID());
            if (job == null || !advance(job)) return;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !JOBS.isEmpty()) tick();
    }

    private static void tick() {
        for (Job job : List.copyOf(JOBS.values())) advance(job);
    }

    /** One tick of {@code job}; returns false once it is finished or dropped. */
    private static boolean advance(Job job) {
        if (job.player.isRemoved() && !(job.player instanceof net.minecraftforge.common.util.FakePlayer)) {
            JOBS.remove(job.player.getUUID());
            job.materials.returnLeftovers();
            return false;
        }
        long deadline = System.nanoTime() + NANOS_PER_TICK;
        boolean[] done = new boolean[1];
        NexusBuildCheckGuard.quiet(job.state, () -> done[0] = job.executor.step(BLOCKS_PER_TICK, deadline));
        if (done[0]) {
            JOBS.remove(job.player.getUUID());
            NexusBuildCheckGuard.run(job.state, job.executor::finish);
            job.materials.returnLeftovers();
            job.player.displayClientMessage(Component.translatable("gtna.terminal.nexus.job.done",
                    job.executor.placed + job.executor.replaced, job.executor.skipped), true);
            return false;
        }
        if (job.player instanceof ServerPlayer) {
            job.player.displayClientMessage(bar(job.executor.progress(), job.executor.total()), true);
        }
        return true;
    }

    /** A text progress bar for the action bar. */
    static Component bar(int done, int total) {
        int width = 20, filled = total == 0 ? width : Math.min(width, done * width / total);
        String bar = "§a" + "|".repeat(filled) + "§8" + "|".repeat(width - filled);
        return Component.translatable("gtna.terminal.nexus.job.progress", bar, total == 0 ? 100 : done * 100 / total);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (Job job : List.copyOf(JOBS.values())) job.materials.returnLeftovers();
        JOBS.clear();
    }
}
