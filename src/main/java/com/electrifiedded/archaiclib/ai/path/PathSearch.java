package com.electrifiedded.archaiclib.ai.path;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;

/**
 * Incremental A* over {@link Move}s (the approach of Baritone, written anew for 1.12 mobs): costs are ticks,
 * digging and placing are moves with their own cost, so "walk around" and "dig through" compete fairly.
 * <ul>
 *     <li>runs on the server thread in slices: all searches together use at most {@link #BUDGET_NANOS} per
 *     server tick, a search simply continues next tick;</li>
 *     <li>goal too far for the node limit: the best partial path toward it, picked with Baritone's
 *     coefficient trick (the node with the best {@code h + g/k} for growing k that is at least five blocks
 *     from the start), then the next piece from its end. Goal walled in (every reachable cell searched):
 *     no path at all, never a walk into the corner nearest to it;</li>
 *     <li>never loads chunks: unloaded cells are walls.</li>
 * </ul>
 */
public final class PathSearch {

    /** server-wide time budget per tick for all searches */
    public static long BUDGET_NANOS = 3_000_000L;
    private static final long SLICE_NANOS = 1_500_000L;
    private static final double[] COEF = {1.5, 2, 2.5, 3, 4, 5, 10};
    private static final double MIN_IMPROVEMENT = 0.01;

    private static int budgetTick = Integer.MIN_VALUE;
    private static long budgetLeft;

    private static final class Node {
        final int x, y, z;
        double g = Double.MAX_VALUE, h, f;
        Node parent;
        Move move = Move.START;
        int placed;
        int heap = -1;
        boolean closed;

        Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private final World world;
    private final Goal goal;
    private final Moves moves;
    private final CellCache cells;
    private final PathRules rules;
    private final LongMap<Node> nodes = new LongMap<>(1024);
    private final LongIntMap avoid;
    private final int maxNodes;
    private final int sx, sy, sz;
    private Node[] heap = new Node[256];
    private int heapSize;
    private final Node[] best = new Node[COEF.length];
    private final double[] bestScore = new double[COEF.length];
    private int expanded;
    private boolean done;
    private MovePath result;

    /**
     * @param height body height in cells (2 for villagers and zombies)
     * @param avoid  extra cost per packed cell (cells the mob got stuck in), may be null
     */
    public PathSearch(World world, PathRules rules, int height, BlockPos start, Goal goal, int maxNodes, LongIntMap avoid) {
        this.world = world;
        this.goal = goal;
        this.rules = rules;
        this.cells = new CellCache(world, rules);
        this.moves = new Moves(cells, height);
        // node limit grows with the distance: a near target gets a quick "no", a far one a long search
        double dist = goal.heuristic(start.getX(), start.getY(), start.getZ()) / PathCosts.WALK;
        // a goal above needs room to try scaffolding (pillars, steps): no quick "no" for those
        int floor = goal.heuristic(start.getX(), start.getY() + 3, start.getZ()) < goal.heuristic(start.getX(), start.getY(), start.getZ()) ? 10000 : 8000;
        this.maxNodes = (int) Math.max(floor, Math.min(maxNodes, floor + dist * dist * 6));
        this.avoid = avoid;
        sx = start.getX();
        sy = start.getY();
        sz = start.getZ();
        Node s = node(sx, sy, sz);
        s.g = 0;
        s.h = goal.heuristic(sx, sy, sz);
        s.f = s.h;
        Arrays.fill(bestScore, Double.MAX_VALUE);
        push(s);
    }

    public boolean isDone() {
        return done;
    }

    /** The path once {@link #isDone()}; null when there is no way at all. */
    public MovePath result() {
        return result;
    }

    public int expanded() {
        return expanded;
    }

    private static int serverTick(World w) {
        net.minecraft.server.MinecraftServer s = w.getMinecraftServer();
        return s == null ? (int) w.getTotalWorldTime() : s.getTickCounter();
    }

    /** Runs the whole search right now, ignoring the tick budget (commands, tests). */
    public MovePath runNow() {
        budgetTick = Integer.MIN_VALUE;
        while (!done) {
            budgetLeft = Long.MAX_VALUE / 4;
            budgetTick = serverTick(world);
            step();
        }
        budgetLeft = 0;
        return result;
    }

    /** Debug: type bits of a cell as this search sees them. */
    public int cellType(int x, int y, int z) {
        return cells.type(x, y, z);
    }

    /** Works on the search for a while. True when finished (see {@link #result()}). */
    public boolean step() {
        if (done) return true;
        int tick = serverTick(world);
        if (tick != budgetTick) {
            budgetTick = tick;
            budgetLeft = BUDGET_NANOS;
        }
        if (budgetLeft <= 0) return false;
        long t0 = System.nanoTime();
        long deadline = t0 + Math.min(budgetLeft, SLICE_NANOS);
        int k = 0;
        try {
            while (heapSize > 0) {
                if ((++k & 15) == 0 && System.nanoTime() > deadline) return false;
                Node n = pop();
                n.closed = true;
                if (goal.isEnd(n.x, n.y, n.z)) {
                    finish(n, true);
                    return true;
                }
                if (++expanded > maxNodes) {
                    finishPartial(); // too far to plan in one go: the best piece toward the goal
                    return true;
                }
                expand(n);
            }
            // every reachable cell seen and the goal is not among them: no way at all. A partial path here
            // would only lead into the corner nearest the goal.
            done = true;
            result = null;
            return true;
        } finally {
            budgetLeft -= System.nanoTime() - t0;
        }
    }

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAG = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private void expand(Node n) {
        int x = n.x, y = n.y, z = n.z;
        for (int[] d : DIRS) {
            int bx = x + d[0], bz = z + d[1];
            add(n, bx, y, bz, Move.TRAVERSE, moves.traverse(x, y, z, bx, bz));
            add(n, bx, y + 1, bz, Move.ASCEND, moves.ascend(x, y, z, bx, bz));
            add(n, bx, y - 1, bz, Move.DESCEND, moves.descend(x, y, z, bx, bz));
            int land = moves.fallLanding(y, bx, bz);
            if (land != Integer.MIN_VALUE) add(n, bx, land, bz, Move.FALL, moves.fall(x, y, z, bx, land, bz));
        }
        for (int[] d : DIAG) {
            int bx = x + d[0], bz = z + d[1];
            add(n, bx, y, bz, Move.DIAGONAL, moves.diagonal(x, y, z, bx, bz));
        }
        add(n, x, y + 1, z, Move.CLIMB_UP, moves.climbUp(x, y, z));
        add(n, x, y - 1, z, Move.CLIMB_DOWN, moves.climbDown(x, y, z));
        add(n, x, y - 1, z, Move.DOWNWARD, moves.downward(x, y, z));
        add(n, x, y + 1, z, Move.PILLAR, moves.pillar(x, y, z));
    }

    private void add(Node from, int x, int y, int z, Move m, double cost) {
        if (cost >= PathCosts.INF) return;
        int placed = from.placed + moves.places;
        if (moves.places > 0 && placed > rules.placeBudget()) return;
        if (avoid != null) {
            int a = avoid.get(LongMap.pack(x, y, z));
            if (a != LongIntMap.MISSING) cost += a;
        }
        double g = from.g + cost;
        Node nb = node(x, y, z);
        if (nb.closed || g >= nb.g - MIN_IMPROVEMENT) return;
        if (nb.g == Double.MAX_VALUE) nb.h = goal.heuristic(x, y, z);
        nb.g = g;
        nb.f = g + nb.h;
        nb.parent = from;
        nb.move = m;
        nb.placed = placed;
        if (nb.heap < 0) push(nb);
        else up(nb.heap);
        for (int i = 0; i < COEF.length; i++) {
            double score = nb.h + nb.g / COEF[i];
            if (score < bestScore[i] - MIN_IMPROVEMENT) {
                bestScore[i] = score;
                best[i] = nb;
            }
        }
    }

    private Node node(int x, int y, int z) {
        long k = LongMap.pack(x, y, z);
        Node n = nodes.get(k);
        if (n == null) {
            n = new Node(x, y, z);
            nodes.put(k, n);
        }
        return n;
    }

    private void finishPartial() {
        // a goal this close that still was not found is walled in: no partial walk toward it
        if (goal.heuristic(sx, sy, sz) < 24 * PathCosts.WALK) {
            done = true;
            result = null;
            return;
        }
        for (Node b : best) {
            if (b == null) continue;
            int dx = b.x - sx, dy = b.y - sy, dz = b.z - sz;
            if (dx * dx + dy * dy + dz * dz > 25) {
                finish(b, false);
                return;
            }
        }
        done = true;
        result = null;
    }

    private void finish(Node end, boolean complete) {
        int n = 0;
        for (Node p = end; p != null; p = p.parent) n++;
        MovePath path = new MovePath(n, complete, end.g);
        int i = n;
        for (Node p = end; p != null; p = p.parent) {
            i--;
            path.x[i] = p.x;
            path.y[i] = p.y;
            path.z[i] = p.z;
            path.move[i] = p.move;
        }
        done = true;
        result = path;
    }

    // --- binary heap on f (ties: lower h first) -----------------------------------------------------

    private static boolean less(Node a, Node b) {
        return a.f < b.f || a.f == b.f && a.h < b.h;
    }

    private void push(Node n) {
        if (heapSize == heap.length) heap = Arrays.copyOf(heap, heapSize * 2);
        heap[heapSize] = n;
        n.heap = heapSize;
        up(heapSize++);
    }

    private Node pop() {
        Node top = heap[0];
        heapSize--;
        heap[0] = heap[heapSize];
        heap[0].heap = 0;
        heap[heapSize] = null;
        if (heapSize > 0) down(0);
        top.heap = -1;
        return top;
    }

    private void up(int i) {
        Node n = heap[i];
        while (i > 0) {
            int p = (i - 1) >> 1;
            if (!less(n, heap[p])) break;
            heap[i] = heap[p];
            heap[i].heap = i;
            i = p;
        }
        heap[i] = n;
        n.heap = i;
    }

    private void down(int i) {
        Node n = heap[i];
        while (true) {
            int l = 2 * i + 1;
            if (l >= heapSize) break;
            int r = l + 1;
            int c = r < heapSize && less(heap[r], heap[l]) ? r : l;
            if (!less(heap[c], n)) break;
            heap[i] = heap[c];
            heap[i].heap = i;
            i = c;
        }
        heap[i] = n;
        n.heap = i;
    }
}
