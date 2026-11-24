package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A (possibly huge) set of {@link LegacyClassStructure}s placed at one fixed origin, pre-split per chunk so every chunk
 * writes only its own blocks straight into its {@link ChunkPrimer} during generation. Use this instead of
 * {@code structure.place(world, ...)} from a populate() call for structures spanning hundreds of chunks (1.7 mods
 * like Shadow World generated whole sky villages from chunk 0,0): no cascading chunk loads, no lag spike.
 * <p>
 * Ops are applied in the original order (later setBlock calls win); metadata-only ops modify the block already in the
 * primer. Tile entities of placed blocks are created lazily by the chunk, as for any generated block.
 */
public final class LegacyChunkedStructure {
    private final Map<Long, int[]> chunks = new HashMap<>();
    private final List<IBlockState> palette = new ArrayList<>();
    private final Map<IBlockState, Integer> paletteIdx = new HashMap<>();
    private final int[] bounds = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
    private int size;

    /** Entry encoding: x(4) | z(4) | y(8) | metaOnly(1) | meta(4) in the low 21 bits, palette index in the next int. */
    public LegacyChunkedStructure(int x, int y, int z, LegacyClassStructure... parts) {
        Map<Long, IntList> tmp = new HashMap<>();
        int[] base = {x, y, z};
        for (LegacyClassStructure s : parts) {
            if (s == null) continue;
            for (LegacyClassStructure.Op o : s.ops()) {
                int px = (o.bx < 0 ? 0 : base[o.bx]) + o.ox;
                int py = (o.by < 0 ? 0 : base[o.by]) + o.oy;
                int pz = (o.bz < 0 ? 0 : base[o.bz]) + o.oz;
                if (py < 0 || py > 255) continue;
                int head, pal;
                if (o.block == null) {
                    head = (px & 15) | (pz & 15) << 4 | py << 8 | 1 << 16 | (o.meta & 15) << 17;
                    pal = -1;
                } else {
                    IBlockState st = LegacyBlockIds.state(o.block, o.meta);
                    if (st == null) continue;
                    head = (px & 15) | (pz & 15) << 4 | py << 8;
                    pal = paletteIdx.computeIfAbsent(st, k -> { palette.add(k); return palette.size() - 1; });
                }
                tmp.computeIfAbsent(ChunkPos.asLong(px >> 4, pz >> 4), k -> new IntList()).add(head, pal);
                bounds[0] = Math.min(bounds[0], px); bounds[1] = Math.min(bounds[1], py); bounds[2] = Math.min(bounds[2], pz);
                bounds[3] = Math.max(bounds[3], px); bounds[4] = Math.max(bounds[4], py); bounds[5] = Math.max(bounds[5], pz);
                size++;
            }
        }
        for (Map.Entry<Long, IntList> e : tmp.entrySet()) chunks.put(e.getKey(), e.getValue().toArray());
    }

    public int size() { return size; }
    /** World-space bounding box {minX,minY,minZ,maxX,maxY,maxZ}. */
    public int[] bounds() { return bounds.clone(); }
    public boolean touches(int cx, int cz) { return chunks.containsKey(ChunkPos.asLong(cx, cz)); }

    /** Writes this chunk's part into the primer; returns the number of blocks written. */
    public int fill(int cx, int cz, ChunkPrimer primer) {
        int[] a = chunks.get(ChunkPos.asLong(cx, cz));
        if (a == null) return 0;
        for (int i = 0; i < a.length; i += 2) {
            int h = a[i], lx = h & 15, lz = h >> 4 & 15, y = h >> 8 & 255;
            IBlockState st;
            if ((h & 1 << 16) != 0) {
                IBlockState cur = primer.getBlockState(lx, y, lz);
                if (cur.getBlock() == Blocks.AIR) continue;
                st = LegacyBlockIds.state(cur.getBlock(), h >> 17 & 15);
                if (st == null) continue;
            } else st = palette.get(a[i + 1]);
            primer.setBlockState(lx, y, lz, st);
        }
        return a.length / 2;
    }

    private static final class IntList {
        int[] d = new int[64];
        int n;
        void add(int a, int b) {
            if (n + 2 > d.length) d = java.util.Arrays.copyOf(d, d.length * 2);
            d[n++] = a; d[n++] = b;
        }
        int[] toArray() { return java.util.Arrays.copyOf(d, n); }
    }
}
