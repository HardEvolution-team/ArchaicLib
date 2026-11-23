package com.electrifiedded.archaiclib.ai.path;

import java.util.Arrays;

/** Small open-addressing long -> int map (no boxing); {@link #MISSING} for absent keys. */
final class LongIntMap {

    static final int MISSING = Integer.MIN_VALUE;

    private static final long EMPTY = Long.MIN_VALUE;

    private long[] keys;
    private int[] vals;
    private int size;
    private int mask;

    LongIntMap(int capacity) {
        int cap = Integer.highestOneBit(Math.max(16, capacity) - 1) << 1;
        keys = new long[cap];
        Arrays.fill(keys, EMPTY);
        vals = new int[cap];
        mask = cap - 1;
    }

    private static int hash(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        return (int) k;
    }

    int get(long k) {
        int i = hash(k) & mask;
        while (true) {
            long c = keys[i];
            if (c == EMPTY) return MISSING;
            if (c == k) return vals[i];
            i = (i + 1) & mask;
        }
    }

    void put(long k, int v) {
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

    private void grow() {
        long[] ok = keys;
        int[] ov = vals;
        keys = new long[ok.length * 2];
        Arrays.fill(keys, EMPTY);
        vals = new int[ok.length * 2];
        mask = keys.length - 1;
        size = 0;
        for (int i = 0; i < ok.length; i++) if (ok[i] != EMPTY) put(ok[i], ov[i]);
    }

}
