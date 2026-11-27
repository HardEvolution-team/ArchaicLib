package com.electrifiedded.archaiclib.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

/**
 * Freeze-free mass block edits for copy-pasted old explosions: between {@link #open(World)} and {@link #close()},
 * every {@code World.setBlockState} on that server world (incl. {@code setBlockToAir}/{@code onBlockExploded})
 * writes straight into the chunk - no per-block light recalculation, neighbour cascade or block-change packet.
 * On close, blocks bordering the changed volume get one neighbour update, touched chunks are re-lit lazily and
 * resent to their watchers as whole chunks. Nested opens on the same world are counted; other worlds pass through.
 * <pre>if (BlastBatch.open(world)) try { ... } finally { BlastBatch.close(); }</pre>
 * (or call {@link #close()} only when {@code open} returned true).
 */
public final class BlastBatch {
    private BlastBatch() {}

    private static WorldServer world;
    private static int depth;
    private static final LongOpenHashSet CHANGED = new LongOpenHashSet();
    private static final LongOpenHashSet CHUNKS = new LongOpenHashSet();
    /** Above this many changed blocks the border neighbour pass is skipped (huge craters). */
    public static int maxNeighbourUpdates = 200_000;

    /** @return true when this call opened (or nested into) a batch and must be paired with {@link #close()}. */
    public static boolean open(World w) {
        if (!(w instanceof WorldServer)) return false;
        if (world != null && world != w) return false;
        world = (WorldServer) w;
        depth++;
        return true;
    }

    private static boolean registered;
    private static final java.util.Map<WorldServer, java.util.Set<Long>> PENDING = new java.util.WeakHashMap<>();

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void onTick(net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent ev) {
        if (ev.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END || PENDING.isEmpty()) return;
        for (java.util.Map.Entry<WorldServer, java.util.Set<Long>> en : PENDING.entrySet()) {
            WorldServer w = en.getKey();
            for (long key : en.getValue()) {
                int cx = (int) key, cz = (int) (key >> 32);
                Chunk c = w.getChunkProvider().getLoadedChunk(cx, cz);
                if (c == null) continue;
                c.resetRelightChecks();
                c.markDirty();
                PlayerChunkMapEntry e = w.getPlayerChunkMap().getEntry(cx, cz);
                if (e != null && e.isSentToPlayers()) e.sendPacket(new SPacketChunkData(c, 65535));
            }
        }
        PENDING.clear();
    }

    /**
     * Ray-grid size for a copy-pasted 1.7 explosion: enough rays that neighbours are at most ~0.8 block apart at the
     * farthest point a ray can reach ({@code 1.73 * power * stepScale}), never more than the configured grid.
     */
    public static int rayGrid(int configured, float power, double stepScale) {
        double reach = 1.734 * Math.abs(power) * Math.max(1.0, stepScale);
        int need = (int) Math.ceil(2.5 * reach) + 2;
        return Math.max(Math.min(configured, 16), Math.min(configured, need));
    }

    /** Max block positions put into an SPacketExplosion (the client breaks + particles each one on receipt). */
    public static int packetBlockLimit = 1024;

    /**
     * Evenly sampled subset of a blast's block list for {@code SPacketExplosion}: the chunks are resent anyway,
     * the list only drives the client's debris particles; the full list (100k+ for big TNT) froze the client.
     */
    public static java.util.List<BlockPos> packetSample(java.util.List<BlockPos> all) {
        int n = all.size();
        if (n <= packetBlockLimit) return all;
        java.util.List<BlockPos> out = new java.util.ArrayList<>(packetBlockLimit);
        double step = n / (double) packetBlockLimit;
        for (int i = 0; i < packetBlockLimit; i++) out.add(all.get((int) (i * step)));
        return out;
    }

    public static boolean isActive(World w) {
        return world == w && world != null;
    }

    /** Called from the World mixin while a batch is active. @return the setBlockState result */
    public static boolean set(World w, BlockPos pos, IBlockState state, int flags) {
        if (w.isOutsideBuildHeight(pos) || !w.isBlockLoaded(pos)) return false;
        Chunk chunk = w.getChunk(pos);
        IBlockState old = chunk.setBlockState(pos, state);
        if (old == null) return false;
        CHANGED.add(pos.toLong());
        CHUNKS.add(ChunkPos.asLong(chunk.x, chunk.z));
        return true;
    }

    public static void close() {
        if (world == null || --depth > 0) return;
        WorldServer w = world;
        world = null;
        depth = 0;
        try {
            if (CHANGED.size() <= maxNeighbourUpdates) {
                BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
                for (LongIterator it = CHANGED.iterator(); it.hasNext(); ) {
                    BlockPos p = BlockPos.fromLong(it.nextLong());
                    IBlockState s = w.getBlockState(p);
                    for (EnumFacing f : EnumFacing.VALUES) {
                        n.setPos(p.getX() + f.getXOffset(), p.getY() + f.getYOffset(), p.getZ() + f.getZOffset());
                        if (CHANGED.contains(n.toLong()) || !w.isBlockLoaded(n)) continue;
                        IBlockState ns = w.getBlockState(n);
                        if (ns.getMaterial() == net.minecraft.block.material.Material.AIR) continue;
                        BlockPos np = n.toImmutable();
                        ns.neighborChanged(w, np, s.getBlock(), p);
                    }
                }
            }
            // chunk resend is deferred to the end of the server tick: many blasts in one tick (TNT chains)
            // then cost one resend per chunk instead of one per blast
            java.util.Set<Long> pend = PENDING.computeIfAbsent(w, k -> new java.util.HashSet<>());
            for (LongIterator it = CHUNKS.iterator(); it.hasNext(); ) pend.add(it.nextLong());
            if (!registered) {
                registered = true;
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(BlastBatch.class);
            }
        } finally {
            CHANGED.clear();
            CHUNKS.clear();
        }
    }
}
