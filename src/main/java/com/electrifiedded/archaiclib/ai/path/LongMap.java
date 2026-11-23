package com.electrifiedded.archaiclib.ai.path;

import java.util.Arrays;

/** Small open-addressing long -> object map (no boxing), enough for a path search. */
final class LongMap<V> {

    private static final long EMPTY = Long.MIN_VALUE;

    private long[] keys;
    private Object[] vals;
    private int size;
    private int mask;

    LongMap(int capacity) {
        int cap = Integer.highestOneBit(Math.max(16, capacity) - 1) << 1;
        keys = new long[cap];
        Arrays.fill(keys, EMPTY);
        vals = new Object[cap];
        mask = cap - 1;
    }

    private static int hash(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        return (int) k;
    }

    @SuppressWarnings("unchecked")
    V get(long k) {
        int i = hash(k) & mask;
        while (true) {
            long c = keys[i];
            if (c == EMPTY) return null;
            if (c == k) return (V) vals[i];
            i = (i + 1) & mask;
        }
    }

    void put(long k, V v) {
        if ((size + 1) * 2 > keys.length) grow();
        int i = hash(k) & mask;
        while (true) {
            long c = keys[i];
            if (c == EMPTY) {
                keys[i] = k;
                vals[i] = v;
                size++;
                return;
            }
            if (c == k) {
                vals[i] = v;
                return;
            }
            i = (i + 1) & mask;
        }
    }

    int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    private void grow() {
        long[] ok = keys;
        Object[] ov = vals;
        keys = new long[ok.length * 2];
        Arrays.fill(keys, EMPTY);
        vals = new Object[ok.length * 2];
        mask = keys.length - 1;
        size = 0;
        for (int i = 0; i < ok.length; i++) if (ok[i] != EMPTY) put(ok[i], (V) ov[i]);
    }

    static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
