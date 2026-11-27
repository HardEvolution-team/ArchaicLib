package com.electrifiedded.archaiclib.util;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Drop-in {@code Set<BlockPos>} for explosion ray casts. 1.12 {@code BlockPos.hashCode} ((y + z*31)*31 + x)
 * collides massively in big volumes, so a {@code HashSet<BlockPos>} of a large blast degrades to seconds of
 * bucket walking. This stores packed longs in a fastutil set instead. Adds from other threads (see
 * {@link com.electrifiedded.archaiclib.world.ParallelRays}) go to per-thread shards merged on the next read.
 */
public final class BlockPosSet extends AbstractSet<BlockPos> {
    private final LongOpenHashSet set;
    private final Thread owner = Thread.currentThread();
    private final ThreadLocal<Shard> shard = ThreadLocal.withInitial(this::newShard);
    private final ConcurrentLinkedQueue<Shard> shards = new ConcurrentLinkedQueue<>();
    private long last = Long.MIN_VALUE;

    private static final class Shard {
        final LongOpenHashSet s = new LongOpenHashSet();
        long last = Long.MIN_VALUE;
    }

    private Shard newShard() {
        Shard s = new Shard();
        shards.add(s);
        return s;
    }

    public BlockPosSet() { set = new LongOpenHashSet(); }
    public BlockPosSet(int expected) { set = new LongOpenHashSet(expected); }

    @Override public boolean add(BlockPos p) {
        long k = p.toLong();
        if (Thread.currentThread() == owner) {
            if (k == last) return false; // consecutive ray steps in the same block
            last = k;
            return set.add(k);
        }
        Shard s = shard.get();
        if (k == s.last) return false;
        s.last = k;
        return s.s.add(k);
    }

    private void merge() {
        if (shards.isEmpty()) return;
        for (Shard s; (s = shards.poll()) != null; ) set.addAll(s.s);
    }

    @Override public boolean contains(Object o) { merge(); return o instanceof BlockPos && set.contains(((BlockPos) o).toLong()); }
    @Override public boolean remove(Object o) { merge(); last = Long.MIN_VALUE; return o instanceof BlockPos && set.remove(((BlockPos) o).toLong()); }
    @Override public int size() { merge(); return set.size(); }
    @Override public void clear() { merge(); set.clear(); last = Long.MIN_VALUE; }

    @Override public Iterator<BlockPos> iterator() {
        merge();
        LongIterator it = set.iterator();
        return new Iterator<BlockPos>() {
            public boolean hasNext() { return it.hasNext(); }
            public BlockPos next() { return BlockPos.fromLong(it.nextLong()); }
            public void remove() { it.remove(); }
        };
    }
}
