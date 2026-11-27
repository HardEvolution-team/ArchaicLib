package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkGeneratorOverworld;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Overworld-noise terrain for 1.7 mod dimensions whose ChunkProvider was a copy of ChunkProviderGenerate with other
 * blocks: the vanilla 1.12 generator runs (biome surface, caves, ravines), then every block listed in
 * {@link #substitute} is swapped chunk-wide (e.g. stone -> mod stone, water -> mod fluid). Vanilla structures, lakes and
 * dungeons are off by default (see {@link #DEFAULT_OPTIONS}); add mod features in {@link #populate} overrides or an
 * IWorldGenerator.
 */
public class LegacyChunkGenerator extends ChunkGeneratorOverworld {
    public static final String DEFAULT_OPTIONS = "{\"useCaves\":true,\"useRavines\":true,\"useDungeons\":false,\"useWaterLakes\":false,"
            + "\"useLavaLakes\":false,\"useStrongholds\":false,\"useVillages\":false,\"useMineShafts\":false,\"useTemples\":false,"
            + "\"useMonuments\":false,\"useMansions\":false}";

    protected final World world;
    private final Map<Block, IBlockState> subst = new IdentityHashMap<>();

    public LegacyChunkGenerator(World world, long seed, String options) {
        super(world, seed, false, options == null ? DEFAULT_OPTIONS : options);
        this.world = world;
    }

    private final List<net.minecraft.world.gen.structure.MapGenStructure> structures = new ArrayList<>();

    /**
     * Adds a mod {@code MapGenStructure} (1.7 ChunkProvider*'s villageGenerator etc.): starts are recorded with the chunk,
     * pieces are placed in {@link #populate}, and {@code /locate} / {@code World.findNearestStructure} find it by its
     * {@code getStructureName()} (the 1.7 findClosestStructure name can be given as an alias).
     */
    public LegacyChunkGenerator structure(net.minecraft.world.gen.structure.MapGenStructure gen, String... aliases) {
        structures.add(gen);
        for (String a : aliases) structureAliases.put(a, gen);
        return this;
    }

    private final Map<String, net.minecraft.world.gen.structure.MapGenStructure> structureAliases = new java.util.HashMap<>();

    public List<net.minecraft.world.gen.structure.MapGenStructure> getStructures() { return structures; }

    private net.minecraft.world.gen.structure.MapGenStructure byName(String name) {
        net.minecraft.world.gen.structure.MapGenStructure g = structureAliases.get(name);
        if (g != null) return g;
        for (net.minecraft.world.gen.structure.MapGenStructure s : structures) if (s.getStructureName().equals(name)) return s;
        return null;
    }

    @Override
    public void populate(int x, int z) {
        super.populate(x, z);
        if (structures.isEmpty()) return;
        Random r = new Random(world.getSeed());
        long a = r.nextLong() / 2L * 2L + 1L, b = r.nextLong() / 2L * 2L + 1L;
        r.setSeed((long) x * a + (long) z * b ^ world.getSeed());
        net.minecraft.util.math.ChunkPos cp = new net.minecraft.util.math.ChunkPos(x, z);
        if (structDepth > 0) { // re-entrant populate (cascading chunk load): defer, the outer call is iterating StructureStart components
            pendingStructs.add(new Object[]{cp, r});
            return;
        }
        structDepth++;
        try {
            for (net.minecraft.world.gen.structure.MapGenStructure s : structures) s.generateStructure(world, r, cp);
            while (!pendingStructs.isEmpty()) {
                Object[] p = pendingStructs.remove(0);
                for (net.minecraft.world.gen.structure.MapGenStructure s : structures)
                    s.generateStructure(world, (Random) p[1], (net.minecraft.util.math.ChunkPos) p[0]);
            }
        } finally { structDepth--; }
    }
    private int structDepth = 0;
    private final java.util.List<Object[]> pendingStructs = new java.util.ArrayList<>();

    @Override
    public void recreateStructures(Chunk chunk, int x, int z) {
        super.recreateStructures(chunk, x, z);
        for (net.minecraft.world.gen.structure.MapGenStructure s : structures) s.generate(world, x, z, null);
    }

    @Override
    public net.minecraft.util.math.BlockPos getNearestStructurePos(World w, String name, net.minecraft.util.math.BlockPos pos, boolean unexplored) {
        net.minecraft.world.gen.structure.MapGenStructure g = byName(name);
        return g != null ? g.getNearestStructurePos(w, pos, unexplored) : super.getNearestStructurePos(w, name, pos, unexplored);
    }

    @Override
    public boolean isInsideStructure(World w, String name, net.minecraft.util.math.BlockPos pos) {
        net.minecraft.world.gen.structure.MapGenStructure g = byName(name);
        return g != null ? g.isInsideStructure(pos) : super.isInsideStructure(w, name, pos);
    }

    public LegacyChunkGenerator substitute(Block from, IBlockState to) {
        subst.put(from, to);
        return this;
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        Chunk c = super.generateChunk(x, z);
        for (net.minecraft.world.gen.structure.MapGenStructure s : structures) s.generate(world, x, z, null);
        if (subst.isEmpty()) return c;
        for (ExtendedBlockStorage s : c.getBlockStorageArray()) {
            if (s == null || s.isEmpty()) continue;
            for (int i = 0; i < 16; i++)
                for (int j = 0; j < 16; j++)
                    for (int k = 0; k < 16; k++) {
                        IBlockState to = subst.get(s.get(i, j, k).getBlock());
                        if (to != null) s.set(i, j, k, to);
                    }
        }
        return c;
    }
}
