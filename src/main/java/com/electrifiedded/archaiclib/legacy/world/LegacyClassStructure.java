package com.electrifiedded.archaiclib.legacy.world;


import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Replays a 1.7 "schematic to java" structure generator straight from the ORIGINAL class file (read at runtime from the
 * original mod jar, so no original code ships in the port). The class's {@code generate(World, Random, int, int, int)}
 * is symbolically interpreted with ASM: every {@code world.setBlock(x+dx, y+dy, z+dz, Block[, meta, flags])},
 * {@code setBlockToAir} and {@code setBlockMetadataWithNotify} whose coordinates are "parameter + constant" becomes an
 * op; helper methods of the same class called with (world, rand, coords...) are followed. Branches are flattened
 * (both sides are recorded), random/unknown coordinates are skipped. Blocks are {@code GETSTATIC} fields, resolved by a
 * callback (mod block fields) or the 1.7 {@code Blocks} SRG table; {@code Block.getBlockById(const)} also works.
 *
 * <p>Typical use:
 * <pre>
 *   LegacyClassStructure s = LegacyClassStructure.load(OriginalModAssets.readOriginal(MODID, "a/b/WorldGenTower.class"), resolver);
 *   s.place(world, x, y, z, 2, null);
 * </pre>
 */
public final class LegacyClassStructure {
    private static final Logger LOG = LogManager.getLogger("ArchaicLib/LegacyClassStructure");
    /** 1.7 World SRG names. */
    private static final String SET_BLOCK = "func_147449_b", SET_BLOCK_META = "func_147465_d", SET_AIR = "func_147468_f",
            SET_META = "func_72921_c", BLOCK_BY_ID = "func_149729_e";
    private static final String GET_TE = "func_147438_o", FILL_CHEST = "func_76293_a", SPAWNER_LOGIC = "func_145881_a", SET_MOB = "func_98272_a";
    private static final String GENERATE = "func_76484_a", GEN_DESC = "(Lnet/minecraft/world/World;Ljava/util/Random;III)Z";

    /** Called for each placed block (e.g. to fill chests or set spawner mobs). */
    public interface PlaceListener {
        void placed(World world, BlockPos pos, IBlockState state);
    }

    /** Chest / spawner set-up found in the original generate(): applied after all blocks by {@link #place(World, int, int, int, int, PlaceListener, TileListener)}. */
    public interface TileListener {
        /** {@code WeightedRandomChestContent.generateChestContents(rand, Owner.FIELD, te, n)}; lootField = "owner/Class.FIELD". */
        default void chest(World world, BlockPos pos, String lootField) {}
        /** {@code spawner.getSpawnerBaseLogic().setEntityName("Name")} (1.7 entity name). */
        default void spawner(World world, BlockPos pos, String mob) {}
    }

    public static final class TileOp {
        public final int bx, ox, by, oy, bz, oz;
        public final String lootField, spawnerMob;
        TileOp(Sym te, String loot, String mob) {
            bx = te.teX.base; ox = te.teX.off; by = te.teY.base; oy = te.teY.off; bz = te.teZ.base; oz = te.teZ.off;
            lootField = loot; spawnerMob = mob;
        }
    }

    private final List<TileOp> tileOps = new ArrayList<>();

    public List<TileOp> tileOps() { return Collections.unmodifiableList(tileOps); }

    /** One recorded op; base* = index of the generate() int parameter (0,1,2 = x,y,z) or -1 for an absolute value. */
    public static final class Op {
        public final int bx, ox, by, oy, bz, oz;
        public final Block block; // null = metadata-only change
        public final int meta;
        Op(int bx, int ox, int by, int oy, int bz, int oz, Block block, int meta) {
            this.bx = bx; this.ox = ox; this.by = by; this.oy = oy; this.bz = bz; this.oz = oz; this.block = block; this.meta = meta;
        }
    }

    private final List<Op> ops;
    public final int unresolved;
    private int minX, minY, minZ, maxX, maxY, maxZ;

    private LegacyClassStructure(List<Op> ops, int unresolved) {
        this.ops = ops;
        this.unresolved = unresolved;
        if (!ops.isEmpty()) {
            minX = minY = minZ = Integer.MAX_VALUE;
            maxX = maxY = maxZ = Integer.MIN_VALUE;
            for (Op o : ops) {
                minX = Math.min(minX, o.ox); maxX = Math.max(maxX, o.ox);
                minY = Math.min(minY, o.oy); maxY = Math.max(maxY, o.oy);
                minZ = Math.min(minZ, o.oz); maxZ = Math.max(maxZ, o.oz);
            }
        }
    }

    public List<Op> ops() { return Collections.unmodifiableList(ops); }
    public int size() { return ops.size(); }
    /** Bounding box of the offsets (relative to the generate() coordinates): {minX,minY,minZ,maxX,maxY,maxZ}. */
    public int[] bounds() { return new int[]{minX, minY, minZ, maxX, maxY, maxZ}; }

    /**
     * Resolves a {@code GETSTATIC owner.name} block reference. Return null to fall back to the vanilla 1.7 Blocks table.
     */
    public interface BlockResolver extends BiFunction<String, String, Block> {}

    /** Parse with the 1.7 generate(World,Random,III)Z entry point. Never throws: a broken class gives an empty structure. */
    public static LegacyClassStructure load(byte[] classBytes, BlockResolver resolver) {
        return load(classBytes, GENERATE, GEN_DESC, resolver);
    }

    public static LegacyClassStructure load(byte[] classBytes, String method, String desc, BlockResolver resolver) {
        if (classBytes == null) return new LegacyClassStructure(Collections.emptyList(), 0);
        try {
            ClassNode cn = new ClassNode();
            new ClassReader(classBytes).accept(cn, ClassReader.SKIP_FRAMES);
            Ctx ctx = new Ctx(cn, resolver);
            MethodNode m = ctx.find(method, desc);
            if (m == null) {
                LOG.warn("{} has no {}{}", cn.name, method, desc);
                return new LegacyClassStructure(Collections.emptyList(), 0);
            }
            // generate(World, Random, x, y, z): int params 3,4,5 -> bases 0,1,2
            Map<Integer, Sym> params = new HashMap<>();
            Type[] args = Type.getArgumentTypes(m.desc);
            int local = (m.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0, intIdx = 0;
            for (Type t : args) {
                if (t.getSort() == Type.INT) params.put(local, Sym.coord(intIdx++, 0));
                local += t.getSize();
            }
            ctx.run(m, params, 0);
            List<Op> list = new ArrayList<>(ctx.ops.size());
            for (Map.Entry<Long, Op> e : ctx.ops.entrySet()) list.add(e.getValue());
            LegacyClassStructure r = new LegacyClassStructure(list, ctx.unresolved);
            r.tileOps.addAll(ctx.tileOps.values());
            return r;
        } catch (Throwable t) {
            LOG.error("Could not read legacy structure class", t);
            return new LegacyClassStructure(Collections.emptyList(), 0);
        }
    }

    /** Places all ops, then runs the recorded chest/spawner set-ups through {@code tiles}. */
    public int place(World world, int x, int y, int z, int flags, PlaceListener listener, TileListener tiles) {
        int n = place(world, x, y, z, flags, listener);
        if (tiles != null) {
            int[] base = {x, y, z};
            for (TileOp t : tileOps) {
                BlockPos p = new BlockPos((t.bx < 0 ? 0 : base[t.bx]) + t.ox, (t.by < 0 ? 0 : base[t.by]) + t.oy, (t.bz < 0 ? 0 : base[t.bz]) + t.oz);
                if (t.lootField != null) tiles.chest(world, p, t.lootField);
                if (t.spawnerMob != null) tiles.spawner(world, p, t.spawnerMob);
            }
        }
        return n;
    }

    /** Places all ops relative to (x,y,z) (the original generate() arguments) with the given setBlockState flags. */
    public int place(World world, int x, int y, int z, int flags, PlaceListener listener) {
        int[] base = {x, y, z};
        int n = 0;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (Op o : ops) {
            int px = (o.bx < 0 ? 0 : base[o.bx]) + o.ox;
            int py = (o.by < 0 ? 0 : base[o.by]) + o.oy;
            int pz = (o.bz < 0 ? 0 : base[o.bz]) + o.oz;
            if (py < 0 || py > 255) continue;
            mp.setPos(px, py, pz);
            IBlockState st;
            if (o.block == null) {
                IBlockState cur = world.getBlockState(mp);
                st = LegacyBlockIds.state(cur.getBlock(), o.meta);
            } else st = LegacyBlockIds.state(o.block, o.meta);
            if (st == null) continue;
            BlockPos p = mp.toImmutable();
            world.setBlockState(p, st, flags);
            n++;
            if (listener != null && st.getBlock() != Blocks.AIR) listener.placed(world, p, st);
        }
        return n;
    }

    // ------------------------------------------------------------------ symbolic interpretation

    /** Symbolic value: an int "param[base] + off" / constant (base -1), a static field ref, a block, or unknown. */
    static final class Sym implements Value {
        static final Sym UNKNOWN = new Sym(1, -2, 0, null, null), UNKNOWN2 = new Sym(2, -2, 0, null, null);
        final int size, base, off;
        final String field; // "owner.name" of a GETSTATIC
        final Block block;
        Sym teX, teY, teZ; // world.getTileEntity(x, y, z) result (or its spawner logic)
        boolean spawnerLogic;
        String str; // LDC string
        Sym(int size, int base, int off, String field, Block block) { this.size = size; this.base = base; this.off = off; this.field = field; this.block = block; }
        static Sym te(Sym x, Sym y, Sym z, boolean logic) { Sym s = new Sym(1, -3, 0, null, null); s.teX = x; s.teY = y; s.teZ = z; s.spawnerLogic = logic; return s; }
        static Sym str(String v) { Sym s = new Sym(1, -4, 0, null, null); s.str = v; return s; }
        static Sym coord(int base, int off) { return new Sym(1, base, off, null, null); }
        static Sym cst(int v) { return new Sym(1, -1, v, null, null); }
        boolean isInt() { return base >= -1 && field == null && block == null; }
        @Override public int getSize() { return size; }
        @Override public boolean equals(Object o) {
            return o instanceof Sym s && s.size == size && s.base == base && s.off == off && Objects.equals(s.field, field) && s.block == block
                    && Objects.equals(s.teX, teX) && Objects.equals(s.teY, teY) && Objects.equals(s.teZ, teZ) && s.spawnerLogic == spawnerLogic && Objects.equals(s.str, str);
        }
        @Override public int hashCode() { return Objects.hash(size, base, off, field); }
    }

    private static final class Ctx {
        final ClassNode cn;
        final BlockResolver resolver;
        final LinkedHashMap<Long, Op> ops = new LinkedHashMap<>();
        final LinkedHashMap<Long, TileOp> tileOps = new LinkedHashMap<>();
        int unresolved;
        int callSerial;

        Ctx(ClassNode cn, BlockResolver resolver) { this.cn = cn; this.resolver = resolver; }

        MethodNode find(String name, String desc) {
            for (MethodNode m : cn.methods) if (m.name.equals(name) && (desc == null || m.desc.equals(desc))) return m;
            return null;
        }

        Block resolve(String field) {
            int dot = field.lastIndexOf('.');
            String owner = field.substring(0, dot), name = field.substring(dot + 1);
            Block b = resolver == null ? null : resolver.apply(owner, name);
            if (b == null && owner.equals("net/minecraft/init/Blocks")) b = LegacyBlocks17.bySrg(name);
            return b;
        }

        void run(MethodNode m, Map<Integer, Sym> params, int depth) throws AnalyzerException {
            if (depth > 16) return;
            int serial = callSerial++;
            Interp in = new Interp(this, params, serial, depth);
            new Analyzer<>(in).analyze(cn.name, m);
        }
    }

    private static final class Interp extends Interpreter<Sym> {
        final Ctx ctx;
        final Map<Integer, Sym> params;
        final int serial, depth;
        MethodNode current;

        Interp(Ctx ctx, Map<Integer, Sym> params, int serial, int depth) {
            super(Opcodes.ASM9);
            this.ctx = ctx; this.params = params; this.serial = serial; this.depth = depth;
        }

        @Override public Sym newValue(Type t) {
            if (t == null) return Sym.UNKNOWN;
            if (t == Type.VOID_TYPE) return null;
            return t.getSize() == 2 ? Sym.UNKNOWN2 : Sym.UNKNOWN;
        }

        @Override public Sym newParameterValue(boolean isInstanceMethod, int local, Type type) {
            Sym s = params.get(local);
            return s != null ? s : newValue(type);
        }

        @Override public Sym newOperation(AbstractInsnNode insn) {
            switch (insn.getOpcode()) {
                case Opcodes.ICONST_M1: case Opcodes.ICONST_0: case Opcodes.ICONST_1: case Opcodes.ICONST_2:
                case Opcodes.ICONST_3: case Opcodes.ICONST_4: case Opcodes.ICONST_5:
                    return Sym.cst(insn.getOpcode() - Opcodes.ICONST_0);
                case Opcodes.BIPUSH: case Opcodes.SIPUSH: return Sym.cst(((IntInsnNode) insn).operand);
                case Opcodes.LDC: {
                    Object c = ((LdcInsnNode) insn).cst;
                    if (c instanceof Integer i) return Sym.cst(i);
                    if (c instanceof String str) return Sym.str(str);
                    return (c instanceof Long || c instanceof Double) ? Sym.UNKNOWN2 : Sym.UNKNOWN;
                }
                case Opcodes.LCONST_0: case Opcodes.LCONST_1: case Opcodes.DCONST_0: case Opcodes.DCONST_1: return Sym.UNKNOWN2;
                case Opcodes.GETSTATIC: {
                    FieldInsnNode f = (FieldInsnNode) insn;
                    if (Type.getType(f.desc).getSize() == 2) return Sym.UNKNOWN2;
                    return new Sym(1, -2, 0, f.owner + "." + f.name, null);
                }
                default: return Sym.UNKNOWN;
            }
        }

        @Override public Sym copyOperation(AbstractInsnNode insn, Sym v) { return v; }

        @Override public Sym unaryOperation(AbstractInsnNode insn, Sym v) {
            switch (insn.getOpcode()) {
                case Opcodes.IINC: {
                    IincInsnNode ii = (IincInsnNode) insn;
                    return v.isInt() ? Sym.coord(v.base, v.off + ii.incr) : Sym.UNKNOWN;
                }
                case Opcodes.INEG: return v.isInt() && v.base == -1 ? Sym.cst(-v.off) : Sym.UNKNOWN;
                case Opcodes.CHECKCAST: return v;
                case Opcodes.I2L: case Opcodes.I2D: case Opcodes.F2L: case Opcodes.F2D: case Opcodes.LNEG: case Opcodes.DNEG:
                    return Sym.UNKNOWN2;
                case Opcodes.GETFIELD: return Type.getType(((FieldInsnNode) insn).desc).getSize() == 2 ? Sym.UNKNOWN2 : Sym.UNKNOWN;
                default: return Sym.UNKNOWN;
            }
        }

        @Override public Sym binaryOperation(AbstractInsnNode insn, Sym a, Sym b) {
            int op = insn.getOpcode();
            if (op == Opcodes.IADD && a.isInt() && b.isInt()) {
                if (a.base == -1) return Sym.coord(b.base, a.off + b.off);
                if (b.base == -1) return Sym.coord(a.base, a.off + b.off);
                return Sym.UNKNOWN;
            }
            if (op == Opcodes.ISUB && a.isInt() && b.isInt() && b.base == -1) return Sym.coord(a.base, a.off - b.off);
            if (op == Opcodes.IMUL && a.isInt() && b.isInt() && a.base == -1 && b.base == -1) return Sym.cst(a.off * b.off);
            switch (op) {
                case Opcodes.LADD: case Opcodes.LSUB: case Opcodes.LMUL: case Opcodes.LDIV: case Opcodes.LREM: case Opcodes.LAND:
                case Opcodes.LOR: case Opcodes.LXOR: case Opcodes.LSHL: case Opcodes.LSHR: case Opcodes.LUSHR: case Opcodes.LALOAD:
                case Opcodes.DADD: case Opcodes.DSUB: case Opcodes.DMUL: case Opcodes.DDIV: case Opcodes.DREM: case Opcodes.DALOAD:
                    return Sym.UNKNOWN2;
                default: return op >= Opcodes.IF_ICMPEQ && op <= Opcodes.IF_ACMPNE || op == Opcodes.PUTFIELD ? null : Sym.UNKNOWN;
            }
        }

        @Override public Sym ternaryOperation(AbstractInsnNode insn, Sym a, Sym b, Sym c) { return null; }

        @Override public Sym naryOperation(AbstractInsnNode insn, List<? extends Sym> vals) {
            if (insn instanceof MethodInsnNode mi) {
                Type ret = Type.getReturnType(mi.desc);
                int argOff = insn.getOpcode() == Opcodes.INVOKESTATIC ? 0 : 1;
                switch (mi.name) {
                    case SET_BLOCK, SET_BLOCK_META, SET_AIR, SET_META -> record(insn, mi, vals, argOff);
                    case GET_TE -> {
                        if (mi.desc.startsWith("(III") && vals.size() >= argOff + 3) {
                            Sym x = vals.get(argOff), y = vals.get(argOff + 1), z = vals.get(argOff + 2);
                            if (x.isInt() && y.isInt() && z.isInt()) return Sym.te(x, y, z, false);
                        }
                    }
                    case SPAWNER_LOGIC -> {
                        Sym t = vals.get(0);
                        if (t != null && t.teX != null) return Sym.te(t.teX, t.teY, t.teZ, true);
                    }
                    case SET_MOB -> {
                        Sym t = vals.get(0), name = vals.size() > 1 ? vals.get(1) : null;
                        if (t != null && t.teX != null && t.spawnerLogic && name != null && name.str != null)
                            ctx.tileOps.put(((long) serial << 32) | indexOf(insn), new TileOp(t, null, name.str));
                    }
                    case FILL_CHEST -> {
                        // static generateChestContents(Random, WeightedRandomChestContent[], IInventory, int)
                        if (vals.size() >= 3) {
                            Sym loot = vals.get(1), t = vals.get(2);
                            if (t != null && t.teX != null && loot != null && loot.field != null)
                                ctx.tileOps.put(((long) serial << 32) | indexOf(insn), new TileOp(t, loot.field, null));
                        }
                    }
                    case BLOCK_BY_ID -> {
                        Sym id = vals.get(0);
                        if (id.isInt() && id.base == -1) return new Sym(1, -2, 0, null, Block.getBlockById(id.off));
                    }
                    default -> {
                        if (mi.owner.equals(ctx.cn.name) && mi.desc.contains("Lnet/minecraft/world/World;")) follow(mi, vals);
                    }
                }
                return newValue(ret);
            }
            if (insn instanceof InvokeDynamicInsnNode indy) return newValue(Type.getReturnType(indy.desc));
            return Sym.UNKNOWN; // MULTIANEWARRAY
        }

        private void record(AbstractInsnNode insn, MethodInsnNode mi, List<? extends Sym> v, int o) {
            if (!mi.desc.startsWith("(III")) return;
            Sym x = v.get(o), y = v.get(o + 1), z = v.get(o + 2);
            if (!x.isInt() || !y.isInt() || !z.isInt()) { ctx.unresolved++; return; }
            Block block;
            int meta = 0;
            switch (mi.name) {
                case SET_AIR -> block = Blocks.AIR;
                case SET_META -> {
                    Sym m = v.get(o + 3);
                    if (!m.isInt() || m.base != -1) return;
                    block = null;
                    meta = m.off;
                }
                default -> {
                    Sym b = v.get(o + 3);
                    block = b.block != null ? b.block : b.field != null ? ctx.resolve(b.field) : null;
                    if (block == null) { ctx.unresolved++; return; }
                    if (mi.name.equals(SET_BLOCK_META)) {
                        Sym m = v.get(o + 4);
                        if (m.isInt() && m.base == -1) meta = m.off;
                    }
                }
            }
            long key = ((long) serial << 32) | indexOf(insn);
            ctx.ops.put(key, new Op(x.base, x.off, y.base, y.off, z.base, z.off, block, meta));
        }

        private int indexOf(AbstractInsnNode insn) {
            int i = 0;
            for (AbstractInsnNode n = insn; n.getPrevious() != null; n = n.getPrevious()) i++;
            return i;
        }

        private final Set<String> followed = new HashSet<>();

        private void follow(MethodInsnNode mi, List<? extends Sym> vals) {
            MethodNode target = ctx.find(mi.name, mi.desc);
            if (target == null) return;
            // the Analyzer may visit this call more than once (loops/fixpoint): follow each call site once
            String site = mi.name + mi.desc + "@" + System.identityHashCode(mi);
            if (!followed.add(site)) return;
            Map<Integer, Sym> p = new HashMap<>();
            Type[] args = Type.getArgumentTypes(mi.desc);
            boolean isStatic = mi.getOpcode() == Opcodes.INVOKESTATIC;
            int local = isStatic ? 0 : 1, vi = isStatic ? 0 : 1;
            for (Type t : args) {
                Sym s = vals.get(vi++);
                if (s != null && (s.isInt() && s.base != -2)) p.put(local, s);
                local += t.getSize();
            }
            try {
                ctx.run(target, p, depth + 1);
            } catch (AnalyzerException e) {
                LOG.warn("structure helper {} failed: {}", mi.name, e.getMessage());
            }
        }

        @Override public void returnOperation(AbstractInsnNode insn, Sym v, Sym expected) {}

        @Override public Sym merge(Sym a, Sym b) {
            if (a.equals(b)) return a;
            return a.size == 2 || b.size == 2 ? Sym.UNKNOWN2 : Sym.UNKNOWN;
        }
    }
}
