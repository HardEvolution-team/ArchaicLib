package com.electrifiedded.archaiclib.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Lag-free generation of big multi-chunk structures (old "build it all at once during populate" generators).
 * <p>
 * A structure job is <b>planned</b> on a background thread against a read-only view of the terrain: chunks that
 * already exist are read as they are, chunks that do not exist yet are read from a raw terrain copy
 * ({@code generateChunk} only: no population, no lighting, never added to the world). Nothing is ever loaded or
 * populated for the structure, so there is no cascading worldgen. Block writes of the plan are stored per chunk and
 * each chunk <b>writes its own part when it is populated</b> (or right away, synced to clients, if it already
 * exists). Raw terrain needed by the planner is produced on the server thread under a small per-tick time budget.
 * <p>
 * Ported code calls the {@code get/set} helpers of {@link #current()} while a job runs (see the mod's world access
 * facade); outside a job {@link #current()} is null and code may use the real world.
 */
public final class ChunkStagedGen {
    private ChunkStagedGen() {}

    /** Server thread time budget per tick, in ns, for raw terrain generation and applying committed blocks. */
    public static long TICK_BUDGET_NS = 3_000_000L;
    private static final int PROTO_CACHE = 96;
    private static final String DATA_NAME = "archaic_staged_gen";

    private static boolean registered;
    private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();
    private static final ConcurrentLinkedQueue<Request<?>> requests = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Runnable> mainTasks = new ConcurrentLinkedQueue<>();
    /** Blocks waiting to go into loaded+populated chunks, applied on the server thread under budget. */
    private static final ArrayDeque<LiveApply> liveApply = new ArrayDeque<>();
    private static final Map<Integer, LinkedHashMap<Long, Chunk>> protoCache = new HashMap<>();
    private static ExecutorService worker;
    private static volatile int generation; // bumped on server stop to cancel running jobs

    /** Call once (preInit/init) from any mod using this. */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        Handler h = new Handler();
        MinecraftForge.EVENT_BUS.register(h);
        GameRegistry.registerWorldGenerator(h, Integer.MIN_VALUE + 1); // applies stored blocks first in populate
    }

    public static Session current() {
        return CURRENT.get();
    }

    /** Queues a structure job for the world. Safe to call from populate (costs nothing there). */
    public static void submit(World world, Runnable job) {
        if (!(world instanceof WorldServer)) { job.run(); return; }
        register();
        WorldServer ws = (WorldServer) world;
        final int gen = generation;
        synchronized (ChunkStagedGen.class) {
            if (worker == null) {
                worker = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "ArchaicLib staged worldgen");
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY + 1);
                    return t;
                });
            }
            worker.execute(() -> {
                if (gen != generation) return;
                Session s = new Session(ws, gen);
                CURRENT.set(s);
                try {
                    job.run();
                    if (gen == generation) mainTasks.add(s::commit);
                } catch (Cancelled ignored) {
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    CURRENT.remove();
                }
            });
        }
    }

    /** Runs on the server thread after the current job's blocks are committed (or now, outside a job). */
    public static void onMain(Runnable r) {
        Session s = current();
        if (s != null) s.after.add(r);
        else r.run();
    }

    /**
     * Spawns an entity that belongs to a staged structure. Inside a job it is deferred to {@link #onMain}. On the
     * server thread it spawns now if its chunk is loaded and populated, otherwise the entity is saved (NBT) with the
     * chunk's stored blocks and spawned right after they are written. Never loads or generates a chunk.
     */
    public static void spawnEntity(World world, net.minecraft.entity.Entity e) {
        if (e == null) return;
        if (current() != null) { onMain(() -> spawnEntity(world, e)); return; }
        if (!(world instanceof WorldServer)) { world.spawnEntity(e); return; }
        WorldServer ws = (WorldServer) world;
        int cx = net.minecraft.util.math.MathHelper.floor(e.posX) >> 4, cz = net.minecraft.util.math.MathHelper.floor(e.posZ) >> 4;
        Chunk c = ws.getChunkProvider().getLoadedChunk(cx, cz);
        Data d = Data.get(ws);
        long key = ChunkPos.asLong(cx, cz);
        if (c != null && c.isTerrainPopulated() && !d.blocks.containsKey(key)) { ws.spawnEntity(e); return; }
        NBTTagCompound nbt = new NBTTagCompound();
        if (!e.writeToNBTOptional(nbt)) return;
        d.entities.computeIfAbsent(key, k -> new NBTTagList()).appendTag(nbt);
        d.markDirty();
        if (c != null && c.isTerrainPopulated()) mainTasks.add(() -> {
            Chunk now = ws.getChunkProvider().getLoadedChunk(cx, cz);
            if (now != null) applyStored(ws, now, true);
        });
    }

    /** A world operation that must run on the real world (vanilla tree/grass generators etc.). */
    public interface WorldOp { void run(WorldServer world, BlockPos pos, Random rand); }

    private static final Map<String, WorldOp> OPS = new ConcurrentHashMap<>();

    /** Registers a persistent deferred operation type (call during init, same id on every start). */
    public static void registerOp(String id, WorldOp op) { OPS.put(id, op); }

    /**
     * Runs {@code op} at {@code pos} on the real world as soon as every chunk within {@code radius} blocks is loaded
     * and has received its staged blocks (inside a job: queued with the job's blocks). Never loads a chunk; saved
     * with the world until it can run. Outside a job with the area ready it runs right away.
     */
    public static void deferOp(World world, String id, BlockPos pos, int radius, long seed) {
        Session s = current();
        if (s != null) { s.ops.add(new PendingOp(id, pos.toImmutable(), radius, seed)); return; }
        if (!(world instanceof WorldServer)) return;
        addOp((WorldServer) world, new PendingOp(id, pos.toImmutable(), radius, seed));
    }

    static final class PendingOp {
        final String id; final BlockPos pos; final int radius; final long seed;
        PendingOp(String id, BlockPos pos, int radius, long seed) { this.id = id; this.pos = pos; this.radius = radius; this.seed = seed; }
        NBTTagCompound write() {
            NBTTagCompound n = new NBTTagCompound();
            n.setString("i", id); n.setLong("p", pos.toLong()); n.setInteger("r", radius); n.setLong("s", seed);
            return n;
        }
        static PendingOp read(NBTTagCompound n) {
            return new PendingOp(n.getString("i"), BlockPos.fromLong(n.getLong("p")), n.getInteger("r"), n.getLong("s"));
        }
    }

    private static void addOp(WorldServer w, PendingOp op) {
        Data d = Data.get(w);
        long key = ChunkPos.asLong(op.pos.getX() >> 4, op.pos.getZ() >> 4);
        d.ops.computeIfAbsent(key, k -> new NBTTagList()).appendTag(op.write());
        d.markDirty();
        mainTasks.add(() -> tryOps(w, op.pos.getX() >> 4, op.pos.getZ() >> 4));
    }

    /** Chunk ready for an op: loaded (no load is triggered) and without pending staged blocks. */
    private static boolean ready(WorldServer w, Data d, int cx, int cz) {
        return w.getChunkProvider().getLoadedChunk(cx, cz) != null && !d.blocks.containsKey(ChunkPos.asLong(cx, cz))
            && !d.tiles.containsKey(ChunkPos.asLong(cx, cz));
    }

    /** Runs every stored op near chunk (cx, cz) whose whole area is ready. */
    private static void tryOps(WorldServer w, int cx, int cz) {
        Data d = Data.get(w);
        if (d.ops.isEmpty()) return;
        for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) {
            long key = ChunkPos.asLong(cx + ox, cz + oz);
            NBTTagList list = d.ops.get(key);
            if (list == null) continue;
            NBTTagList keep = new NBTTagList();
            List<PendingOp> run = new ArrayList<>();
            for (int i = 0; i < list.tagCount(); i++) {
                PendingOp op = PendingOp.read(list.getCompoundTagAt(i));
                boolean ok = true;
                int x0 = (op.pos.getX() - op.radius) >> 4, x1 = (op.pos.getX() + op.radius) >> 4;
                int z0 = (op.pos.getZ() - op.radius) >> 4, z1 = (op.pos.getZ() + op.radius) >> 4;
                for (int x = x0; x <= x1 && ok; x++) for (int z = z0; z <= z1 && ok; z++) ok = ready(w, d, x, z);
                if (ok) run.add(op); else keep.appendTag(list.getCompoundTagAt(i));
            }
            if (run.isEmpty()) continue;
            if (keep.tagCount() == 0) d.ops.remove(key); else d.ops.put(key, keep);
            d.markDirty();
            for (PendingOp op : run) {
                WorldOp f = OPS.get(op.id);
                if (f == null) continue;
                try { f.run(w, op.pos, new Random(op.seed)); } catch (Throwable t) { t.printStackTrace(); }
            }
        }
    }

    // ------------------------------------------------------------------ session (worker thread)

    private static final class Cancelled extends RuntimeException {
        Cancelled() { super(null, null, false, false); }
    }

    public static final class Session {
        public final WorldServer world;
        private final int gen;
        private final Map<Long, Chunk> views = new HashMap<>();
        /** chunk -> (packed local pos -> state) */
        private final Map<Long, Map<Integer, IBlockState>> writes = new LinkedHashMap<>();
        private final Map<BlockPos, TileEntity> tiles = new HashMap<>();
        private final List<Runnable> after = new ArrayList<>();
        private final List<PendingOp> ops = new ArrayList<>();

        Session(WorldServer w, int gen) { this.world = w; this.gen = gen; }

        private Chunk view(int cx, int cz) {
            long key = ChunkPos.asLong(cx, cz);
            Chunk c = views.get(key);
            if (c == null) {
                c = callMain(() -> mainView(world, cx, cz));
                views.put(key, c);
            }
            return c;
        }

        <T> T callMain(Supplier<T> s) {
            Request<T> r = new Request<>(s);
            requests.add(r);
            while (true) {
                if (gen != generation) throw new Cancelled();
                try {
                    return r.future.get(250, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignored) {
                } catch (InterruptedException e) {
                    throw new Cancelled();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        private static int local(BlockPos p) { return (p.getX() & 15) | ((p.getZ() & 15) << 4) | (p.getY() << 8); }

        public IBlockState getBlockState(BlockPos p) {
            if (p.getY() < 0 || p.getY() > 255) return Blocks.AIR.getDefaultState();
            Map<Integer, IBlockState> w = writes.get(ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4));
            if (w != null) {
                IBlockState s = w.get(local(p));
                if (s != null) return s;
            }
            Chunk c = view(p.getX() >> 4, p.getZ() >> 4);
            try {
                return c.getBlockState(p);
            } catch (RuntimeException e) { // racy read of a live chunk; ask the server thread
                return callMain(() -> mainView(world, p.getX() >> 4, p.getZ() >> 4).getBlockState(p));
            }
        }

        public boolean setBlockState(BlockPos p, IBlockState s) {
            if (p.getY() < 0 || p.getY() > 255) return false;
            writes.computeIfAbsent(ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4), k -> new HashMap<>()).put(local(p), s);
            tiles.remove(p);
            return true;
        }

        /** Detached tile entity for a block this job placed (filled by the job, copied in on apply). */
        public TileEntity getTileEntity(BlockPos p) {
            TileEntity te = tiles.get(p);
            if (te != null) return te;
            Map<Integer, IBlockState> w = writes.get(ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4));
            IBlockState s = w == null ? null : w.get(local(p));
            if (s == null || !s.getBlock().hasTileEntity(s)) return null;
            te = s.getBlock().createTileEntity(world, s);
            if (te == null) return null;
            te.setPos(p.toImmutable());
            tiles.put(p.toImmutable(), te);
            return te;
        }

        /** Top of the terrain column (first air above ground), like {@code getHeightValue}. */
        public int getHeight(int x, int z) {
            int h = view(x >> 4, z >> 4).getHeightValue(x & 15, z & 15);
            Map<Integer, IBlockState> w = writes.get(ChunkPos.asLong(x >> 4, z >> 4));
            if (w != null) {
                for (int y = 255; y >= h; y--) {
                    IBlockState s = w.get((x & 15) | ((z & 15) << 4) | (y << 8));
                    if (s != null && s.getLightOpacity() > 0) return y + 1;
                }
            }
            return h;
        }

        public boolean canSeeSky(BlockPos p) { return p.getY() >= getHeight(p.getX(), p.getZ()); }

        public int getLightFor(EnumSkyBlock type, BlockPos p) {
            if (type == EnumSkyBlock.SKY) return canSeeSky(p) ? 15 : 0;
            return 0;
        }

        public Biome getBiome(int x, int z) {
            Chunk c = view(x >> 4, z >> 4);
            int id = c.getBiomeArray()[(z & 15) << 4 | (x & 15)] & 255;
            Biome b = id == 255 ? null : Biome.getBiome(id);
            if (b != null) return b;
            return callMain(() -> world.getBiome(new BlockPos(x, 0, z)));
        }

        /** Server thread: route every chunk's blocks to "now" or "when that chunk generates". */
        void commit() {
            Data data = Data.get(world);
            ChunkProviderServer cps = world.getChunkProvider();
            for (Map.Entry<Long, Map<Integer, IBlockState>> e : writes.entrySet()) {
                long key = e.getKey();
                int cx = (int) key, cz = (int) (key >>> 32);
                int[] packed = new int[e.getValue().size() * 2];
                int i = 0;
                NBTTagList teList = new NBTTagList();
                for (Map.Entry<Integer, IBlockState> b : e.getValue().entrySet()) {
                    packed[i++] = b.getKey();
                    packed[i++] = Block.getStateId(b.getValue());
                }
                for (Map.Entry<BlockPos, TileEntity> t : tiles.entrySet()) {
                    BlockPos p = t.getKey();
                    if ((p.getX() >> 4) == cx && (p.getZ() >> 4) == cz) {
                        try { teList.appendTag(t.getValue().writeToNBT(new NBTTagCompound())); } catch (Throwable ignored) {}
                    }
                }
                Chunk c = cps.getLoadedChunk(cx, cz);
                if (c != null && c.isTerrainPopulated()) {
                    liveApply.add(new LiveApply(world, cx, cz, packed, teList));
                } else {
                    data.add(key, packed, teList);
                }
            }
            for (PendingOp op : ops) addOp(world, op);
            if (!after.isEmpty()) {
                List<Runnable> a = new ArrayList<>(after);
                liveApply.add(new LiveApply(a));
            }
        }
    }

    // ------------------------------------------------------------------ server thread side

    private static Chunk mainView(WorldServer w, int cx, int cz) {
        ChunkProviderServer cps = w.getChunkProvider();
        Chunk c = cps.getLoadedChunk(cx, cz);
        if (c != null) return c;
        LinkedHashMap<Long, Chunk> cache = protoCache.computeIfAbsent(w.provider.getDimension(), d -> new LinkedHashMap<Long, Chunk>(64, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Long, Chunk> e) { return size() > PROTO_CACHE; }
        });
        long key = ChunkPos.asLong(cx, cz);
        c = cache.get(key);
        if (c == null) {
            IChunkGenerator g = cps.chunkGenerator;
            c = g.generateChunk(cx, cz); // terrain only; never added to the world
            cache.put(key, c);
        }
        return c;
    }

    private static final class Request<T> {
        final Supplier<T> s;
        final CompletableFuture<T> future = new CompletableFuture<>();
        Request(Supplier<T> s) { this.s = s; }
        void run() {
            try { future.complete(s.get()); } catch (Throwable t) { future.completeExceptionally(t); }
        }
    }

    private static final class LiveApply {
        final WorldServer world; final int cx, cz; final int[] packed; final NBTTagList tiles; final List<Runnable> tasks;
        int index;
        LiveApply(WorldServer w, int cx, int cz, int[] packed, NBTTagList tiles) {
            this.world = w; this.cx = cx; this.cz = cz; this.packed = packed; this.tiles = tiles; this.tasks = null;
        }
        LiveApply(List<Runnable> tasks) { this.world = null; cx = cz = 0; packed = null; tiles = null; this.tasks = tasks; }
    }

    /** Writes stored blocks into a chunk. {@code sync}: chunk may already be on clients. Returns new index. */
    private static int applyBlocks(WorldServer w, Chunk c, int[] packed, int from, int maxBlocks, boolean sync) {
        int bx = c.x << 4, bz = c.z << 4;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int i = from;
        for (int n = 0; i < packed.length && n < maxBlocks; i += 2, n++) {
            int l = packed[i];
            p.setPos(bx + (l & 15), l >>> 8, bz + ((l >> 4) & 15));
            IBlockState s = Block.getStateById(packed[i + 1]);
            IBlockState old = c.getBlockState(p);
            if (old == s) continue;
            c.setBlockState(p, s);
            if (s.getLightValue() != old.getLightValue() || s.getLightOpacity() != old.getLightOpacity()) {
                w.checkLight(p);
            }
            if (sync) w.notifyBlockUpdate(p, old, s, 2);
        }
        c.markDirty();
        return i;
    }

    private static void applyTiles(WorldServer w, NBTTagList list) {
        if (list == null) return;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound nbt = list.getCompoundTagAt(i);
            BlockPos p = new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
            TileEntity te = w.getTileEntity(p);
            if (te != null) {
                try {
                    te.readFromNBT(nbt);
                    te.markDirty();
                    IBlockState s = w.getBlockState(p);
                    w.notifyBlockUpdate(p, s, s, 2);
                } catch (Throwable ignored) {}
            }
        }
    }

    private static void tick() {
        long start = System.nanoTime();
        Runnable t;
        while ((t = mainTasks.poll()) != null) t.run();
        // raw terrain / view requests (at least one per tick)
        boolean first = true;
        Request<?> r;
        while ((first || System.nanoTime() - start < TICK_BUDGET_NS) && (r = requests.poll()) != null) {
            r.run();
            first = false;
        }
        // blocks for chunks that already exist
        while (!liveApply.isEmpty() && System.nanoTime() - start < TICK_BUDGET_NS * 2) {
            LiveApply a = liveApply.peek();
            if (a.tasks != null) {
                liveApply.poll();
                for (Runnable x : a.tasks) {
                    try { x.run(); } catch (Throwable e) { e.printStackTrace(); }
                }
                continue;
            }
            Chunk c = a.world.getChunkProvider().getLoadedChunk(a.cx, a.cz);
            if (c == null) { // unloaded meanwhile: keep for its next load
                liveApply.poll();
                int[] rest = Arrays.copyOfRange(a.packed, a.index, a.packed.length);
                Data.get(a.world).add(ChunkPos.asLong(a.cx, a.cz), rest, a.tiles);
                continue;
            }
            a.index = applyBlocks(a.world, c, a.packed, a.index, 2048, true);
            if (a.index >= a.packed.length) {
                liveApply.poll();
                applyTiles(a.world, a.tiles);
                tryOps(a.world, a.cx, a.cz);
            }
        }
    }

    // ------------------------------------------------------------------ persistence

    public static final class Data extends WorldSavedData {
        final Map<Long, List<int[]>> blocks = new HashMap<>();
        final Map<Long, NBTTagList> tiles = new HashMap<>();
        final Map<Long, NBTTagList> entities = new HashMap<>();
        final Map<Long, NBTTagList> ops = new HashMap<>();

        public Data(String name) { super(name); }

        static Data get(World w) {
            Data d = (Data) w.getPerWorldStorage().getOrLoadData(Data.class, DATA_NAME);
            if (d == null) {
                d = new Data(DATA_NAME);
                w.getPerWorldStorage().setData(DATA_NAME, d);
            }
            return d;
        }

        void add(long key, int[] packed, NBTTagList teList) {
            if (packed.length > 0) blocks.computeIfAbsent(key, k -> new ArrayList<>()).add(packed);
            if (teList != null && teList.tagCount() > 0) {
                NBTTagList l = tiles.computeIfAbsent(key, k -> new NBTTagList());
                for (int i = 0; i < teList.tagCount(); i++) l.appendTag(teList.get(i).copy());
            }
            markDirty();
        }

        boolean has(long key) { return blocks.containsKey(key) || tiles.containsKey(key) || entities.containsKey(key); }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            NBTTagList list = nbt.getTagList("c", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound c = list.getCompoundTagAt(i);
                long key = c.getLong("k");
                NBTTagList parts = c.getTagList("b", 11);
                List<int[]> l = new ArrayList<>();
                for (int j = 0; j < parts.tagCount(); j++) l.add(((NBTTagIntArray) parts.get(j)).getIntArray());
                if (!l.isEmpty()) blocks.put(key, l);
                if (c.hasKey("t")) tiles.put(key, c.getTagList("t", 10));
                if (c.hasKey("n")) entities.put(key, c.getTagList("n", 10));
                if (c.hasKey("o")) ops.put(key, c.getTagList("o", 10));
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            NBTTagList list = new NBTTagList();
            Set<Long> keys = new HashSet<>(blocks.keySet());
            keys.addAll(tiles.keySet());
            keys.addAll(entities.keySet());
            keys.addAll(ops.keySet());
            for (long key : keys) {
                NBTTagCompound c = new NBTTagCompound();
                c.setLong("k", key);
                NBTTagList parts = new NBTTagList();
                for (int[] a : blocks.getOrDefault(key, Collections.emptyList())) parts.appendTag(new NBTTagIntArray(a));
                c.setTag("b", parts);
                if (tiles.containsKey(key)) c.setTag("t", tiles.get(key));
                if (entities.containsKey(key)) c.setTag("n", entities.get(key));
                if (ops.containsKey(key)) c.setTag("o", ops.get(key));
                list.appendTag(c);
            }
            nbt.setTag("c", list);
            return nbt;
        }
    }

    /** Applies the stored part of a chunk (populate: not yet on clients; load: may be). */
    private static void applyStored(WorldServer w, Chunk c, boolean sync) {
        Data d = Data.get(w);
        long key = ChunkPos.asLong(c.x, c.z);
        if (!d.has(key)) return;
        List<int[]> parts = d.blocks.remove(key);
        NBTTagList tl = d.tiles.remove(key);
        NBTTagList el = d.entities.remove(key);
        d.markDirty();
        if (parts != null) for (int[] p : parts) applyBlocks(w, c, p, 0, Integer.MAX_VALUE, sync);
        applyTiles(w, tl);
        if (el != null) for (int i = 0; i < el.tagCount(); i++) {
            try { net.minecraft.world.chunk.storage.AnvilChunkLoader.readWorldEntity(el.getCompoundTagAt(i), w, true); }
            catch (Throwable t) { t.printStackTrace(); }
        }
        tryOps(w, c.x, c.z);
    }

    public static final class Handler implements IWorldGenerator {
        @Override
        public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator gen, IChunkProvider provider) {
            if (world instanceof WorldServer) {
                Chunk c = ((WorldServer) world).getChunkProvider().getLoadedChunk(chunkX, chunkZ);
                if (c != null) applyStored((WorldServer) world, c, false);
                tryOps((WorldServer) world, chunkX, chunkZ);
            }
        }

        @SubscribeEvent
        public void onTick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.END) tick();
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onChunkLoad(ChunkEvent.Load e) {
            World w = e.getWorld();
            if (!(w instanceof WorldServer) || !e.getChunk().isTerrainPopulated()) return;
            Chunk c = e.getChunk();
            WorldServer ws = (WorldServer) w;
            Data d = Data.get(ws);
            if (!d.has(ChunkPos.asLong(c.x, c.z))) {
                if (!d.ops.isEmpty()) mainTasks.add(() -> tryOps(ws, c.x, c.z));
                return;
            }
            mainTasks.add(() -> {
                Chunk now = ws.getChunkProvider().getLoadedChunk(c.x, c.z);
                if (now != null) applyStored(ws, now, true);
            });
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload e) {
            if (e.getWorld().isRemote) return;
            protoCache.remove(e.getWorld().provider.getDimension());
            if (FMLCommonHandler.instance().getMinecraftServerInstance() == null
                || !FMLCommonHandler.instance().getMinecraftServerInstance().isServerRunning()) {
                stopAll();
            }
        }
    }

    /** Cancels running jobs and drops cached terrain (server stopping). Committed blocks stay saved. */
    public static synchronized void stopAll() {
        generation++;
        Request<?> r;
        while ((r = requests.poll()) != null) r.future.completeExceptionally(new Cancelled());
        mainTasks.clear();
        liveApply.clear();
        protoCache.clear();
        if (worker != null) {
            worker.shutdownNow();
            worker = null;
        }
    }

    public static boolean isAir(IBlockState s) { return s.getBlock() == Blocks.AIR; }

}
