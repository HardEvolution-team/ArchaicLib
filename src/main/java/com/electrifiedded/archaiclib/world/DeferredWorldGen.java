package com.electrifiedded.archaiclib.world;

import java.util.ArrayDeque;
import java.util.Iterator;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Runs old "build the whole structure from IWorldGenerator.generate" code after population instead of inside it.
 * <p>
 * On 1.12 a 1.7 style structure placed during {@code populate} that reaches into neighbour chunks loads and populates
 * them on the spot (cascading worldgen: lag spikes, chains of structures, "loaded a new chunk" warnings). Jobs queued
 * here run at the end of the server tick (a configurable number per tick), when no chunk is being populated, so the
 * neighbour chunks are generated and populated normally first. For huge multi-chunk builds use {@link ChunkStagedGen}.
 * Jobs of a world that unloads are dropped.
 */
public final class DeferredWorldGen {
    /** Jobs run per server tick. */
    public static int JOBS_PER_TICK = 1;
    private static final ArrayDeque<Job> JOBS = new ArrayDeque<>();
    private static boolean registered;

    private static final class Job {
        final World world;
        final Runnable run;

        Job(World world, Runnable run) {
            this.world = world;
            this.run = run;
        }
    }

    private DeferredWorldGen() {}

    private static synchronized void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(new Handler());
    }

    /** Queue a job for the world (server side only; client worlds are ignored). */
    public static synchronized void add(World world, Runnable job) {
        if (world.isRemote) return;
        register();
        JOBS.add(new Job(world, job));
    }

    public static synchronized int pending() {
        return JOBS.size();
    }

    private static synchronized Job poll() {
        return JOBS.poll();
    }

    public static final class Handler {
        @SubscribeEvent
        public void onTick(TickEvent.ServerTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            for (int i = 0; i < JOBS_PER_TICK; i++) {
                Job j = poll();
                if (j == null) return;
                j.run.run();
            }
        }

        @SubscribeEvent
        public void onUnload(WorldEvent.Unload e) {
            synchronized (DeferredWorldGen.class) {
                for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) if (it.next().world == e.getWorld()) it.remove();
            }
        }
    }
}
