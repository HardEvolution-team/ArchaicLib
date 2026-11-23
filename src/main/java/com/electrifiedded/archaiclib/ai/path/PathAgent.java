package com.electrifiedded.archaiclib.ai.path;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Walks a mob along {@link PathSearch} paths: one move at a time from cell centre to cell centre (so it lines
 * up with doors and gaps and never wedges itself in a corner), digging and placing what the move needs through
 * {@link Hooks}, re-checking the next moves against the world, and planning again when a move fails, the mob
 * is pushed off the path or a block changed. A move that keeps failing makes its cell expensive for a while,
 * so the next plan goes another way instead of into the same corner.
 * <p>
 * Use: create one per mob (or per job), call {@link #setGoal} when the target changes and {@link #tick()}
 * every tick while it should move; stop the mob's own navigator meanwhile.
 */
public class PathAgent {

    public enum State { IDLE, SEARCHING, MOVING, DONE, FAILED }

    /** What the mob does to the world for a move. */
    public interface Hooks {
        /** One tick of digging {@code p}; true once the block is gone. */
        boolean dig(BlockPos p);

        /** Puts a helper block at {@code p}; true if placed. */
        boolean place(BlockPos p);

        /** Opens a wooden door or gate at {@code p} (no-op otherwise). */
        default void open(BlockPos p) {}
    }

    private final EntityLiving mob;
    private final World world;
    private final PathRules rules;
    private final Hooks hooks;
    private final int height;

    private Goal goal;
    private double speed = 0.6;
    private State state = State.IDLE;
    private PathSearch search;
    private MovePath path;
    private int idx;
    private int stepTicks;
    private int idleTicks;
    private int replans;
    private int searchTicks;
    private long failedAt;
    private boolean pillarJumped;
    /** ticks spent digging for the current move, and blocks dug for it (gravel, sand, water refilling...) */
    private int digTicksMove, digsMove;
    /** ticks without the mob getting any closer to the next cell (pushing into something the plan did not see) */
    private int noProgressTicks;
    private double bestDist;
    /** ticks without a cell to plan from (in the air, wedged) */
    private int noStartTicks;
    /** cells the mob got stuck in, with extra cost (forgotten after a while) */
    private LongIntMap avoid = new LongIntMap(32);
    private long avoidSince;
    private String lastFail = "";

    public int maxNodes = 16000;

    public PathAgent(EntityLiving mob, PathRules rules, Hooks hooks) {
        this.mob = mob;
        this.world = mob.world;
        this.rules = rules;
        this.hooks = hooks;
        this.height = Math.max(1, MathHelper.ceil(mob.height));
    }

    public State state() {
        return state;
    }

    public Goal goal() {
        return goal;
    }

    /** Ticks without completing a move (or since the search started). */
    public int idleTicks() {
        return idleTicks;
    }

    public MovePath path() {
        return path;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setGoal(Goal g, double speed) {
        this.goal = g;
        this.speed = speed;
        replans = 0;
        idleTicks = 0;
        startSearch();
    }

    public void clear() {
        goal = null;
        search = null;
        path = null;
        state = State.IDLE;
        idleTicks = 0;
    }

    /** Ticks since the last failure (FAILED state retries after a while on its own). */
    public long sinceFailure() {
        return world.getTotalWorldTime() - failedAt;
    }

    private void startSearch() {
        path = null;
        BlockPos start = startCell();
        if (start == null || goal == null) {
            search = null;
            state = goal == null ? State.IDLE : State.SEARCHING; // in the air: wait for the landing
            if (goal != null && ++noStartTicks > 60) {
                noStartTicks = 0;
                fail("no start cell at " + mob.getPosition());
            }
            return;
        }
        noStartTicks = 0;
        if (world.getTotalWorldTime() - avoidSince > 2400) {
            avoid = new LongIntMap(32);
            avoidSince = world.getTotalWorldTime();
        }
        search = new PathSearch(world, rules, height, start, goal, maxNodes, avoid);
        searchTicks = 0;
        state = State.SEARCHING;
    }

    /** Feet cell to plan from: where it stands (on the floor, in water or on a ladder). */
    private BlockPos startCell() {
        BlockPos feet = new BlockPos(mob.posX, mob.posY + 0.2, mob.posZ);
        CellCache c = new CellCache(world, rules);
        // standing on a slab, the lower half of a stair, a bed: the feet are inside that block's cell, the
        // cell to plan from is the one above it
        BlockPos up = feet.up();
        if (!c.open(feet.getX(), feet.getY(), feet.getZ()) && c.open(up.getX(), up.getY(), up.getZ()) && c.standable(up.getX(), up.getY(), up.getZ())) return up;
        for (int i = 0; i < 3; i++) {
            BlockPos p = feet.down(i);
            if (c.standable(p.getX(), p.getY(), p.getZ()) && c.open(p.getX(), p.getY(), p.getZ())) return p;
        }
        return mob.onGround || mob.isInWater() ? feet : null;
    }

    public void tick() {
        if (world.isRemote || goal == null) return;
        idleTicks++;
        switch (state) {
            case SEARCHING:
                if (search == null) {
                    startSearch();
                    if (search == null) return;
                }
                if (++searchTicks > 1200 || search.step()) {
                    MovePath p = search.isDone() ? search.result() : null;
                    search = null;
                    if (p == null || p.length() < 2 && !p.complete) {
                        fail("no path");
                    } else if (p.length() < 2) {
                        state = State.DONE;
                    } else {
                        path = p;
                        idx = 0;
                        resetStep();
                        state = State.MOVING;
                    }
                }
                return;
            case MOVING:
                follow();
                return;
            case FAILED:
                if (sinceFailure() > 200) startSearch();
                return;
            default:
        }
    }

    private void fail(String why) {
        state = State.FAILED;
        failedAt = world.getTotalWorldTime();
        lastFail = why;
        path = null;
    }

    private void resetStep() {
        stepTicks = 0;
        pillarJumped = false;
        digTicksMove = 0;
        digsMove = 0;
        noProgressTicks = 0;
        bestDist = Double.MAX_VALUE;
    }

    private void replan(boolean blame) {
        if (blame && path != null && idx + 1 < path.length()) {
            // a cell that keeps failing gets dearer each time (40, 160, 640...): the next plans go round it
            long k = LongMap.pack(path.x[idx + 1], path.y[idx + 1], path.z[idx + 1]);
            int old = avoid.get(k);
            avoid.put(k, old == LongIntMap.MISSING ? 40 : Math.min(20000, old * 4));
        }
        if (++replans > 12) {
            fail("too many replans");
            return;
        }
        startSearch();
    }

    // --- following ----------------------------------------------------------------------------------

    private void follow() {
        if (idx >= path.length() - 1) {
            if (path.complete) {
                state = State.DONE;
                path = null;
            } else {
                startSearch(); // the end of a partial path: plan the next piece from here
            }
            return;
        }
        int ax = path.x[idx], ay = path.y[idx], az = path.z[idx];
        int bx = path.x[idx + 1], by = path.y[idx + 1], bz = path.z[idx + 1];
        Move m = path.move[idx + 1];
        stepTicks++;

        // pushed off the path (a mob, water, a fall)
        double cx = bx + 0.5, cz = bz + 0.5;
        double offA = horiz(ax + 0.5, az + 0.5), offB = horiz(cx, cz);
        if (Math.min(offA, offB) > 2.2 || mob.posY < Math.min(ay, by) - 2.5 || mob.posY > Math.max(ay, by) + 2.5) {
            replan(false);
            return;
        }
        // the world changed: the next moves must still work
        if (stepTicks % 20 == 1 && !stillValid()) {
            replan(false);
            return;
        }
        // a fast mob runs past the centre of b into the next cell: catch the index up instead of
        // turning back (that was the dithering), and never act on a move it is no longer at
        if (idx + 2 < path.length() && fastAndOpen(idx + 1) && fastAndOpen(idx + 2) && !inCell(path.x[idx + 1], path.y[idx + 1], path.z[idx + 1], 0.0)
                && inCell(path.x[idx + 2], path.y[idx + 2], path.z[idx + 2], 0.05) && path.move[idx + 2].ordinal() <= Move.DIAGONAL.ordinal()
                && obstacle(new CellCache(world, rules), path.move[idx + 2], bx, by, bz, path.x[idx + 2], path.y[idx + 2], path.z[idx + 2]) == null) {
            idx++;
            resetStep();
            return;
        }
        // open doors and gates on the next cells
        for (int i = 0; i < height; i++) hooks.open(new BlockPos(bx, by + i, bz));

        // clear the cells the move needs
        CellCache now = new CellCache(world, rules);
        BlockPos block = obstacle(now, m, ax, ay, az, bx, by, bz);
        if (block != null) {
            int bt = now.type(block.getX(), block.getY(), block.getZ());
            if ((bt & (CellCache.DEADLY | CellCache.UNLOADED)) != 0 || now.breakCost(block.getX(), block.getY(), block.getZ()) >= PathCosts.INF) {
                lastFail = "may not break " + block;
                replan(true);
                return;
            }
            mob.getNavigator().clearPath();
            // dig only from the cell the move starts at (or ends at): a mob that slid or ran ahead would
            // otherwise break a block that is not in its way at all - with a fast tool before any check
            if (horiz(ax + 0.5, az + 0.5) > 0.9 && horiz(cx, cz) > 0.9 || Math.abs(mob.posY - ay) > 1.2 && Math.abs(mob.posY - by) > 1.2) {
                mob.getMoveHelper().setMoveTo(ax + 0.5, ay, az + 0.5, speed * 0.5);
                if (++digTicksMove > 60) replan(false);
                return;
            }
            mob.getMoveHelper().setMoveTo(mob.posX, mob.posY, mob.posZ, 0.0); // stand still while digging
            // digging is progress, not being stuck - unless it never ends (a block the mob cannot really
            // break, sand or water that keeps coming back)
            if (++digTicksMove > 500 || digsMove > 10) {
                lastFail = "digging " + block + " leads nowhere";
                replan(true);
                return;
            }
            if (hooks.dig(block)) {
                idleTicks = 0;
                digsMove++;
            }
            stepTicks = Math.min(stepTicks, 20);
            noProgressTicks = 0;
            idleTicks = Math.max(0, idleTicks - 1);
            return;
        }
        // put down the bridge or the step
        BlockPos support = support(now, m, ax, ay, az, bx, by, bz);
        if (support != null && m != Move.PILLAR) {
            if (!hooks.place(support)) {
                lastFail = "cannot place " + support;
                replan(true);
                return;
            }
        }
        drive(m, ax, ay, az, bx, by, bz);
        if (state != State.MOVING || path == null) return;
        if (arrived(m, bx, by, bz)) {
            idx++;
            resetStep();
            idleTicks = 0;
            if (replans > 0 && idx > 4) replans--;
            return;
        }
        // walking moves: no step closer for 30 ticks means it pushes against something the plan did not
        // expect (a fence corner, a mob, a block that changed) - no point pushing for the full timeout
        if (m == Move.TRAVERSE || m == Move.DIAGONAL || m == Move.DESCEND) {
            double d = horiz(cx, cz);
            if (d < bestDist - 0.05) {
                bestDist = d;
                noProgressTicks = 0;
            } else if (++noProgressTicks > 30) {
                if (mob.onGround) mob.getJumpHelper().setJumping(); // one try over a lip first
                if (noProgressTicks > 45) {
                    lastFail = "no progress on " + m + " to " + bx + "," + by + "," + bz;
                    replan(true);
                    return;
                }
            }
        }
        if (stepTicks > timeout(m)) {
            lastFail = "stuck on " + m + " to " + bx + "," + by + "," + bz;
            replan(true);
        }
    }

    private int timeout(Move m) {
        switch (m) {
            case PILLAR: return 80;
            case CLIMB_UP:
            case CLIMB_DOWN:
            case SWIM_UP:
            case SWIM_DOWN: return 100;
            case FALL: return 120;
            default: return 60;
        }
    }

    private double horiz(double x, double z) {
        double dx = mob.posX - x, dz = mob.posZ - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean stillValid() {
        Moves check = new Moves(new CellCache(world, rules), height);
        for (int i = idx + 1; i < Math.min(path.length(), idx + 5); i++) {
            double c = check.cost(path.move[i], path.x[i - 1], path.y[i - 1], path.z[i - 1], path.x[i], path.y[i], path.z[i]);
            if (c >= PathCosts.INF) return false;
        }
        return true;
    }

    /** The first block that is in the way of the move right now, or null. */
    private BlockPos obstacle(CellCache c, Move m, int ax, int ay, int az, int bx, int by, int bz) {
        switch (m) {
            case TRAVERSE:
                return firstSolid(c, bx, by, bz, height);
            case ASCEND: {
                BlockPos p = firstSolid(c, ax, ay + height, az, 1);
                return p != null ? p : firstSolid(c, bx, by, bz, height);
            }
            case DESCEND: {
                BlockPos p = firstSolid(c, bx, by + height, bz, 1);
                return p != null ? p : firstSolid(c, bx, by, bz, height);
            }
            case DOWNWARD:
                return firstSolid(c, bx, by, bz, 1);
            case PILLAR:
                return firstSolid(c, ax, ay + height, az, 1);
            default:
                return null;
        }
    }

    private BlockPos firstSolid(CellCache c, int x, int y, int z, int n) {
        for (int i = n - 1; i >= 0; i--) { // top first: no gravel landing on the head
            int t = c.type(x, y + i, z);
            if ((t & CellCache.PASS) == 0) return new BlockPos(x, y + i, z);
        }
        return null;
    }

    /** The helper block the move needs and that is still missing, or null. */
    private BlockPos support(CellCache c, Move m, int ax, int ay, int az, int bx, int by, int bz) {
        if (m == Move.TRAVERSE && !c.standable(bx, by, bz)) return new BlockPos(bx, by - 1, bz);
        if (m == Move.ASCEND && !c.standable(bx, by, bz)) return new BlockPos(bx, ay, bz);
        if (m == Move.PILLAR) return new BlockPos(ax, ay, az);
        return null;
    }

    private void drive(Move m, int ax, int ay, int az, int bx, int by, int bz) {
        double cx = bx + 0.5, cz = bz + 0.5;
        switch (m) {
            case PILLAR: {
                centre(ax + 0.5, az + 0.5);
                if (mob.onGround && mob.posY < ay + 0.5 && !pillarJumped) {
                    mob.getJumpHelper().setJumping();
                    pillarJumped = true;
                } else if (pillarJumped && mob.posY >= ay + 1.0) {
                    BlockPos p = new BlockPos(ax, ay, az);
                    if (world.getBlockState(p).getMaterial().isReplaceable() && !hooks.place(p)) {
                        lastFail = "cannot pillar";
                        replan(true);
                    }
                } else if (pillarJumped && mob.onGround && mob.posY < ay + 0.5) {
                    pillarJumped = false; // landed without placing: jump again
                }
                return;
            }
            case CLIMB_UP:
                centre(cx, cz);
                mob.motionY = Math.max(mob.motionY, 0.2);
                return;
            case CLIMB_DOWN:
                centre(cx, cz);
                mob.motionY = Math.min(mob.motionY, -0.12);
                return;
            case SWIM_UP:
                centre(cx, cz);
                mob.motionY = Math.max(mob.motionY, 0.08);
                return;
            case SWIM_DOWN:
                centre(cx, cz);
                mob.motionY = Math.min(mob.motionY, -0.08);
                return;
            default:
        }
        mob.getMoveHelper().setMoveTo(cx, by, cz, speed);
        if (m == Move.ASCEND && mob.posY < by - 0.4 && horiz(cx, cz) < 1.4) {
            if (mob.onGround || mob.isInWater()) mob.getJumpHelper().setJumping();
        }
        if (mob.isInWater() && by >= ay && mob.posY < by + 0.1) mob.motionY = Math.max(mob.motionY, 0.04);
    }

    /** Pulls the mob to a cell centre without walking (ladders, pillars, water columns). */
    private void centre(double cx, double cz) {
        mob.motionX += (cx - mob.posX) * 0.2 - mob.motionX * 0.5;
        mob.motionZ += (cz - mob.posZ) * 0.2 - mob.motionZ * 0.5;
        mob.getMoveHelper().setMoveTo(cx, mob.posY, cz, speed * 0.3);
    }

    private boolean arrived(Move m, int bx, int by, int bz) {
        double dy = mob.posY - by;
        boolean supported = mob.onGround || mob.isInWater() || mob.isOnLadder();
        if (m == Move.CLIMB_UP || m == Move.SWIM_UP) return dy > -0.05 && horiz(bx + 0.5, bz + 0.5) < 0.6;
        if (m == Move.CLIMB_DOWN || m == Move.SWIM_DOWN) return dy < 0.5 && horiz(bx + 0.5, bz + 0.5) < 0.6;
        if (!supported || dy < -0.6 || dy > (mob.isInWater() ? 1.2 : 0.7)) return false;
        double tol = 0.3;
        // a straight run on the same level: no need to hit every centre exactly
        if (idx + 2 < path.length() && (m == Move.TRAVERSE || m == Move.DIAGONAL)) {
            Move next = path.move[idx + 2];
            int ndx = path.x[idx + 2] - bx, ndz = path.z[idx + 2] - bz;
            int dx = bx - path.x[idx], dz = bz - path.z[idx];
            boolean doorAhead = (new CellCache(world, rules).type(path.x[idx + 2], path.y[idx + 2], path.z[idx + 2]) & CellCache.DOOR) != 0;
            if (next == m && ndx == dx && ndz == dz && !doorAhead && path.y[idx + 2] == by) tol = 0.6;
        }
        if (Math.abs(mob.posX - (bx + 0.5)) < tol && Math.abs(mob.posZ - (bz + 0.5)) < tol) return true;
        // fast mobs: crossed the centre of b (past it along the a->b direction, still inside the cell)
        if ((m == Move.TRAVERSE || m == Move.DIAGONAL || m == Move.DESCEND || m == Move.ASCEND) && fastAndOpen(idx + 1)
                && (idx + 2 >= path.length() || fastAndOpen(idx + 2))) {
            double ux = bx - path.x[idx], uz = bz - path.z[idx];
            double along = (mob.posX - (bx + 0.5)) * ux + (mob.posZ - (bz + 0.5)) * uz;
            return along >= 0 && inCell(bx, by, bz, 0.0);
        }
        return false;
    }

    /**
     * Overshoot handling only for mobs that really run fast (normal walkers keep exact centre-to-centre
     * steps) and never at doors, gates or 1-wide gaps, where cutting the corner walks into the frame.
     */
    private boolean fastAndOpen(int i) {
        double v = Math.sqrt(mob.motionX * mob.motionX + mob.motionZ * mob.motionZ);
        if (v < 0.22) return false;
        CellCache c = new CellCache(world, rules);
        int x = path.x[i], y = path.y[i], z = path.z[i];
        for (int h = 0; h < height; h++) if ((c.type(x, y + h, z) & CellCache.DOOR) != 0) return false;
        // narrow: walls on both sides across the step direction
        int px = i > 0 ? path.x[i - 1] : x, pz = i > 0 ? path.z[i - 1] : z;
        int dx = x - px, dz = z - pz;
        if (dx != 0 && dz != 0) return true;
        int sx = dz, sz = dx; // perpendicular
        boolean a = (c.type(x + sx, y, z + sz) & CellCache.PASS) == 0, b = (c.type(x - sx, y, z - sz) & CellCache.PASS) == 0;
        return !(a || b);
    }

    /** The mob's feet are inside cell x,z (grown by {@code grow}) at about level y. */
    private boolean inCell(int x, int y, int z, double grow) {
        return mob.posX >= x - grow && mob.posX <= x + 1 + grow && mob.posZ >= z - grow && mob.posZ <= z + 1 + grow
                && Math.abs(mob.posY - y) < 0.7;
    }

    /** Dev: a full search from where the mob stands to {@code g} right now, described in one line. */
    public String probe(Goal g) {
        BlockPos start = startCell();
        if (start == null) return "no start cell (in the air)";
        PathSearch s = new PathSearch(world, rules, height, start, g, maxNodes, avoid);
        long t = System.nanoTime();
        MovePath p = s.runNow();
        StringBuilder b = new StringBuilder("from " + start + " nodes=" + s.expanded() + " " + (System.nanoTime() - t) / 1000 + "us ");
        if (p == null) return b.append("NO PATH").toString();
        b.append(p.complete ? "complete" : "partial").append(" cost=").append((int) p.cost).append(':');
        for (int i = 1; i < p.length(); i++) b.append(' ').append(p.move[i].name().charAt(0)).append(p.move[i].name().charAt(1)).append(p.x[i]).append(',').append(p.y[i]).append(',').append(p.z[i]);
        return b.toString();
    }

    /** Dev: cell type bits along x from x0-6 to x0+6 at y,z, and move costs east from each. */
    public String probeCells(int x0, int y, int z) {
        CellCache c = new CellCache(world, rules);
        Moves mv = new Moves(c, height);
        StringBuilder b = new StringBuilder();
        for (int x = x0 - 6; x <= x0 + 6; x++) {
            b.append(x).append(':').append(Integer.toHexString(c.type(x, y, z))).append('/').append(Integer.toHexString(c.type(x, y - 1, z)))
                    .append(" E=").append((int) Math.min(9999, mv.traverse(x, y, z, x + 1, z)))
                    .append(" S=").append((int) Math.min(9999, mv.traverse(x, y, z, x, z + 1))).append("; ");
        }
        return b.toString();
    }

    public String debug() {
        String s = state + (goal == null ? "" : " " + goal);
        if (path != null) s += " step " + idx + "/" + (path.length() - 1) + (path.complete ? "" : " partial") + " " + (idx + 1 < path.length() ? path.move[idx + 1] : "");
        if (search != null) s += " searching " + search.expanded();
        if (!lastFail.isEmpty()) s += " last: " + lastFail;
        return s;
    }
}
