package com.electrifiedded.archaiclib.util;

import java.util.Random;

/**
 * Drop-in {@link Random} replacement: SplitMix64, no CAS/AtomicLong per call (java.util.Random does a
 * compareAndSet on every next()). Not thread-safe per instance - same as how old mods use their Randoms
 * (one per entity/explosion/tile). For cross-thread static use take {@link #shared()}.
 * Seeded generation that must match vanilla sequences (worldgen with world seed) should keep java.util.Random.
 */
public class FastRandom extends Random {
    private static final long serialVersionUID = 1L;
    private static final long GOLDEN = 0x9E3779B97F4A7C15L;
    private static final ThreadLocal<FastRandom> SHARED = ThreadLocal.withInitial(FastRandom::new);

    private long state; // no initializer: super() calls setSeed before field init would run

    public FastRandom() {
        super(0L);
        this.state = mix(System.nanoTime() ^ Thread.currentThread().getId() * GOLDEN ^ SEED_UNIQ.getAndAdd(GOLDEN));
    }

    public FastRandom(long seed) {
        super(seed);
    }

    private static final java.util.concurrent.atomic.AtomicLong SEED_UNIQ = new java.util.concurrent.atomic.AtomicLong(0x2545F4914F6CDD1DL);

    /** Per-thread instance for static/shared call sites ({@code new Random().nextInt(..)} one-offs). */
    public static FastRandom shared() {
        return SHARED.get();
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private long nextRaw() {
        return mix(state += GOLDEN);
    }

    @Override
    public void setSeed(long seed) {
        state = seed;
        // Random.haveNextNextGaussian is private; Random.setSeed would clear it, emulate via super only when safe
    }

    @Override
    protected int next(int bits) {
        return (int) (nextRaw() >>> (64 - bits));
    }

    @Override
    public int nextInt() {
        return (int) nextRaw();
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        // Lemire multiply-shift, bias < 2^-32 - fine for game randomness
        return (int) (((nextRaw() >>> 32) * bound) >>> 32);
    }

    @Override
    public long nextLong() {
        return nextRaw();
    }

    @Override
    public boolean nextBoolean() {
        return nextRaw() < 0;
    }

    @Override
    public float nextFloat() {
        return (nextRaw() >>> 40) * 0x1.0p-24f;
    }

    @Override
    public double nextDouble() {
        return (nextRaw() >>> 11) * 0x1.0p-53;
    }
}
