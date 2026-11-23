package com.electrifiedded.archaiclib.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Huge explosions without the freeze: a spherical blast that is carved chunk by chunk, nearest chunks first, under a
 * per-tick time budget, so it spreads out as a visible shock wave and the server keeps its TPS.
 * <p>
 * Whole 16x16x16 sections inside the sphere are dropped at once (array swap, no per-block cost); sections on the
 * edge are cleared block by block without neighbour updates. Everything goes - stone, ores, liquids, tile entities -
 * except bedrock-hard blocks when {@link #keepUnbreakable} is set (and the bottom layer y=0). Changed chunks are
 * resent to their watchers as whole chunks and re-lit lazily ({@link Chunk#resetRelightChecks()}).
 * <p>
 * Chunks that are not loaded are loaded from disk when they exist (never generated), wiped and queued for unload;
 * {@link #onlyLoaded} limits the blast to loaded chunks. Entities inside the wiped volume go to {@link #onEntity}.
 */
public final class ChunkBlast {
    private static final List<ChunkBlast> ACTIVE = new ArrayList<>();
    private static boolean registered;

    public final WorldServer world;
    public final double x, y, z, radius;
    /** Nanoseconds of carving per server tick (all blasts share it). */
    public static long budgetNanos = 6_000_000L;
    public boolean onlyLoaded;
    public boolean keepUnbreakable = true;
    public Consumer<Entity> onEntity;
    /** Called once when the wave has passed every chunk. */
    public Runnable onDone;
    /** Shock-wave particles at the front, for players within this distance of it (0 = none). */
    public double particleRange = 256.0D;

    private final int cx, cz, maxRing;
    private final double r2;
    private int ring, idx = -1;
    private int[] ringCells = new int[0];
    private long carved, skipped;

    private ChunkBlast(WorldServer world, double x, double y, double z, double radius) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.r2 = radius * radius;
        this.cx = MathHelper.floor(x) >> 4;
        this.cz = MathHelper.floor(z) >> 4;
        this.maxRing = (int)Math.ceil(radius / 16.0D) + 1;
    }

    /** Creates a blast; call {@link #start()} (after setting options) to run it. */
    public static ChunkBlast of(WorldServer world, double x, double y, double z, double radius) {
        return new ChunkBlast(world, x, y, z, radius);
    }

    public ChunkBlast start() {
        if (!registered) {
            registered = true;
            MinecraftForge.EVENT_BUS.register(ChunkBlast.class);
        }
        ACTIVE.add(this);
        return this;
    }

    /** Carves everything synchronously (small radii only). */
    public void runNow() {
        while (step(Long.MAX_VALUE)) {
        }
    }

    public boolean isDone() {
        return ring > maxRing;
    }

    /** Distance (blocks) the wave front has reached. */
    public double front() {
        return Math.min(radius, ring * 16.0D);
    }

    public long chunksCarved() {
        return carved;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ACTIVE.isEmpty())
            return;
        long deadline = System.nanoTime() + budgetNanos;
        Iterator<ChunkBlast> it = ACTIVE.iterator();
        int n = ACTIVE.size();
        while (it.hasNext()) {
            ChunkBlast b = it.next();
            long share = Math.max(500_000L, (deadline - System.nanoTime()) / Math.max(1, n--));
            boolean more = b.world.getMinecraftServer() != null && !b.world.isRemote && b.step(System.nanoTime() + share);
            b.waveParticles();
            if (!more) {
                it.remove();
                if (b.onDone != null)
                    b.onDone.run();
            }
        }
    }

    /** @return true while there is more to carve */
    private boolean step(long deadline) {
        ChunkProviderServer provider = world.getChunkProvider();
        while (ring <= maxRing) {
            if (idx < 0) {
                ringCells = ringCells(ring);
                idx = 0;
            }
            while (idx < ringCells.length) {
                int x0 = cx + ringCells[idx], z0 = cz + ringCells[idx + 1];
                idx += 2;
                if ((idx & 127) == 0 && System.nanoTime() >= deadline)
                    return true;
                if (!touches(x0, z0))
                    continue;
                Chunk chunk = provider.getLoadedChunk(x0, z0);
                boolean loadedHere = false;
                if (chunk == null) {
                    if (onlyLoaded || !regionExists(x0 >> 5, z0 >> 5) || !provider.isChunkGeneratedAt(x0, z0)) {
                        skipped++;
                        continue;
                    }
                    chunk = provider.loadChunk(x0, z0);
                    loadedHere = true;
                    if (chunk == null)
                        continue;
                }
                carve(chunk);
                carved++;

                if (loadedHere && world.getPlayerChunkMap().getEntry(x0, z0) == null)
                    provider.queueUnload(chunk);
                if (System.nanoTime() >= deadline)
                    return true;
            }
            ring++;
            idx = -1;
        }
        return false;
    }

    private final it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap regions = new it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap();

    /** Most of a huge blast is ungenerated wilderness: one file check per 32x32-chunk region instead of per chunk. */
    private boolean regionExists(int rx, int rz) {
        long key = (long)rx << 32 | (rz & 0xFFFFFFFFL);
        if (regions.containsKey(key))
            return regions.get(key);
        boolean exists = true;
        java.io.File dir = world.getChunkSaveLocation();
        if (dir != null)
            exists = new java.io.File(dir, "region/r." + rx + "." + rz + ".mca").exists();
        regions.put(key, exists);
        return exists;
    }

    private boolean touches(int chx, int chz) {
        double nx = MathHelper.clamp(x, chx << 4, (chx << 4) + 16), nz = MathHelper.clamp(z, chz << 4, (chz << 4) + 16);
        return (nx - x) * (nx - x) + (nz - z) * (nz - z) <= r2;
    }

    /** Chunk offsets on the square ring at Chebyshev distance r, as x,z pairs. */
    private static int[] ringCells(int r) {
        if (r == 0)
            return new int[] { 0, 0 };
        int[] out = new int[8 * r * 2];
        int i = 0;
        for (int k = -r; k < r; k++) {
            out[i++] = k; out[i++] = -r;
            out[i++] = r; out[i++] = k;
            out[i++] = -k; out[i++] = r;
            out[i++] = -r; out[i++] = -k;
        }
        return out;
    }

    private void carve(Chunk chunk) {
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        int bx = chunk.x << 4, bz = chunk.z << 4;
        int changedMask = 0;
        // per column: vertical half-extent of the sphere
        double[] half = new double[256];
        boolean any = false;
        for (int lx = 0; lx < 16; lx++)
            for (int lz = 0; lz < 16; lz++) {
                double dx = bx + lx + 0.5D - x, dz = bz + lz + 0.5D - z;
                double h2 = r2 - dx * dx - dz * dz;
                half[lz << 4 | lx] = h2 > 0 ? Math.sqrt(h2) : -1.0D;
                any |= h2 > 0;
            }
        if (!any)
            return;
        for (int sy = 0; sy < storage.length; sy++) {
            ExtendedBlockStorage sec = storage[sy];
            if (sec == Chunk.NULL_BLOCK_STORAGE || sec.isEmpty())
                continue;
            int y0 = sy << 4, y1 = y0 + 15;
            boolean full = sy > 0;
            if (full)
                for (int c = 0; c < 256 && full; c++)
                    full = half[c] >= 0 && y0 >= y - half[c] && y1 + 1 <= y + half[c];
            boolean changed = false, kept = false;
            IBlockState air = Blocks.AIR.getDefaultState();
            if (full && !keepUnbreakable) {
                storage[sy] = Chunk.NULL_BLOCK_STORAGE;
                changedMask |= 1 << sy;
                continue;
            }
            for (int ly = 0; ly < 16; ly++) {
                int wy = y0 + ly;
                if (wy == 0)
                    continue;
                for (int c = 0; c < 256; c++) {
                    if (!full) {
                        double h = half[c];
                        if (h < 0 || wy < y - h || wy > y + h)
                            continue;
                    }
                    int lx = c & 15, lz = c >> 4;
                    IBlockState st = sec.get(lx, ly, lz);
                    if (st.getBlock() == Blocks.AIR)
                        continue;
                    if (keepUnbreakable && unbreakable(st, bx + lx, wy, bz + lz)) {
                        kept = true;
                        continue;
                    }
                    if (!full)
                        sec.set(lx, ly, lz, air);
                    changed = true;
                }
            }
            if (full && !kept) {
                // nothing to keep: drop the whole section instead of clearing it cell by cell
                storage[sy] = Chunk.NULL_BLOCK_STORAGE;
                changedMask |= 1 << sy;
                continue;
            }
            if (full && changed)
                for (int ly = 0; ly < 16; ly++)
                    for (int c = 0; c < 256; c++) {
                        IBlockState st = sec.get(c & 15, ly, c >> 4);
                        if (st.getBlock() != Blocks.AIR && !unbreakable(st, bx + (c & 15), y0 + ly, bz + (c >> 4)))
                            sec.set(c & 15, ly, c >> 4, air);
                    }
            if (changed) {
                changedMask |= 1 << sy;
                if (sec.isEmpty() && sy > 0)
                    storage[sy] = Chunk.NULL_BLOCK_STORAGE;
            }
        }
        if (changedMask == 0 && onEntity == null)
            return;
        if (changedMask != 0) {
            // tile entities in carved cells
            for (BlockPos pos : new ArrayList<>(chunk.getTileEntityMap().keySet())) {
                if (inside(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) && pos.getY() > 0
                        && chunk.getBlockState(pos).getBlock() == Blocks.AIR) {
                    TileEntity te = chunk.getTileEntityMap().remove(pos);
                    if (te != null) {
                        te.invalidate();
                        world.loadedTileEntityList.remove(te);
                        world.tickableTileEntities.remove(te);
                    }
                }
            }
            chunk.generateSkylightMap();
            chunk.resetRelightChecks();
            chunk.markDirty();
            PlayerChunkMapEntry entry = world.getPlayerChunkMap().getEntry(chunk.x, chunk.z);
            if (entry != null && entry.isSentToPlayers())
                entry.sendPacket(new SPacketChunkData(chunk, 65535));
        }
        if (onEntity != null) {
            List<Entity> found = new ArrayList<>();
            for (net.minecraft.util.ClassInheritanceMultiMap<Entity> list : chunk.getEntityLists())
                for (Entity en : list)
                    if (inside(en.posX, en.posY + en.height * 0.5D, en.posZ))
                        found.add(en);
            for (Entity en : found)
                onEntity.accept(en);
        }
    }

    private final java.util.IdentityHashMap<IBlockState, Boolean> hardness = new java.util.IdentityHashMap<>();

    private boolean unbreakable(IBlockState st, int bx, int by, int bz) {
        Boolean b = hardness.get(st);
        if (b == null) {
            b = st.getBlockHardness(world, new BlockPos(bx, by, bz)) < 0.0F;
            hardness.put(st, b);
        }
        return b;
    }

    public boolean inside(double px, double py, double pz) {
        double dx = px - x, dy = py - y, dz = pz - z;
        return dx * dx + dy * dy + dz * dz <= r2;
    }

    private void waveParticles() {
        if (particleRange <= 0)
            return;
        double fr = front();
        for (EntityPlayerMP p : world.getPlayers(EntityPlayerMP.class, pl -> true)) {
            double dx = p.posX - x, dz = p.posZ - z, d = Math.sqrt(dx * dx + dz * dz);
            if (Math.abs(d - fr) > particleRange)
                continue;
            // a few bursts on the wave front around the player's bearing
            double a0 = Math.atan2(dz, dx);
            for (int i = -3; i <= 3; i++) {
                double a = a0 + i * Math.min(0.5D, 48.0D / Math.max(16.0D, fr));
                double px = x + Math.cos(a) * fr, pz = z + Math.sin(a) * fr;
                int top = world.getHeight(MathHelper.floor(px), MathHelper.floor(pz));
                world.spawnParticle(p, EnumParticleTypes.EXPLOSION_HUGE, true, px, Math.max(top, 1) + 2.0D, pz, 2, 6.0D, 4.0D, 6.0D, 0.0D);
            }
        }
    }
}
