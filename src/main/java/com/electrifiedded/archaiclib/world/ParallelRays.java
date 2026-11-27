package com.electrifiedded.archaiclib.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/**
 * Runs the outer slice loop of a copy-pasted 1.7 explosion ray cast (the {@code for i < smoothness} loop) on all
 * cores. The world is not written during that phase and the server thread just waits, so block reads through
 * {@link #state} are safe; a slice whose rays reach an unloaded chunk is aborted and replayed on the calling thread
 * with normal (chunk-loading) world access, so the result is identical to the serial loop.
 * Collect hits in a {@link com.electrifiedded.archaiclib.util.BlockPosSet} (thread-safe for adds from workers).
 */
public final class ParallelRays {
    private ParallelRays() {}

    /** Below this many slices the loop just runs serially. */
    public static int minSlices = 24;
    public static boolean enabled = true;

    private static final ThreadLocal<Boolean> WORKER = new ThreadLocal<>();
    private static final RuntimeException UNLOADED = new RuntimeException("unloaded", null, false, false) {};

    public static void run(World world, int slices, IntConsumer slice) {
        if (!enabled || slices < minSlices || !(world instanceof WorldServer)) {
            for (int i = 0; i < slices; i++) slice.accept(i);
            return;
        }
        ConcurrentLinkedQueue<Integer> redo = new ConcurrentLinkedQueue<>();
        IntStream.range(0, slices).parallel().forEach(i -> {
            Boolean prev = WORKER.get();
            WORKER.set(Boolean.TRUE);
            try {
                slice.accept(i);
            } catch (RuntimeException e) {
                redo.add(i); // unloaded chunk or a block that misbehaved off-thread: redo serially
            } finally {
                WORKER.set(prev);
            }
        });
        for (Integer i : redo) slice.accept(i);
    }

    /** Block read for ray casts: loaded-chunk only on workers (aborts the slice otherwise), plain world read on the caller. */
    public static IBlockState state(World world, BlockPos pos) {
        if (WORKER.get() == null) return world.getBlockState(pos);
        Chunk c = ((WorldServer) world).getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (c == null) throw UNLOADED;
        return c.getBlockState(pos);
    }
}
