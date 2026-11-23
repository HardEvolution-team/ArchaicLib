package com.electrifiedded.archaiclib.ai;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Ground navigator for mobs that are supposed to get somewhere (workers, helpers, followers).
 * On top of {@link PathNavigateGround}:
 * <ul>
 *     <li>uses {@link SmartWalkNodeProcessor} (no walking over tree tops, wooden fence gates are passable);</li>
 *     <li>opens wooden doors and fence gates on its path and closes them again behind itself;</li>
 *     <li>a stuck watchdog: when the mob has a path but does not move it opens doors around, slides to the
 *     middle of its cell and hops, then skips to the next path point, and finally gives the path up;</li>
 *     <li>pops the mob out when it ends up inside a block (leaves grown in, a block placed on it...).</li>
 * </ul>
 * Use: override {@code createNavigator(World)} of the mob and return {@code new SmartGroundNavigator(this, world)}.
 * {@link #getGiveUps()} counts paths dropped by the watchdog, so AI code can notice "this target is unreachable".
 */
public class SmartGroundNavigator extends PathNavigateGround {

    private static final int CHECK_INTERVAL = 10;

    private SmartWalkNodeProcessor smartProcessor;
    private boolean openDoors = true;
    private boolean closeBehind = true;
    private final List<BlockPos> opened = new ArrayList<>();

    private double lastX, lastY, lastZ;
    private int stuckChecks;
    private int giveUps;
    private int tick;

    public SmartGroundNavigator(EntityLiving entity, World world) {
        super(entity, world);
        setBreakDoors(true);
        setEnterDoors(true);
    }

    @Override
    protected PathFinder getPathFinder() {
        // runs from the PathNavigate constructor: fields of this class are not initialised yet
        SmartWalkNodeProcessor p = new SmartWalkNodeProcessor();
        p.setCanEnterDoors(true);
        this.nodeProcessor = p;
        this.smartProcessor = p;
        return new PathFinder(p);
    }

    public SmartWalkNodeProcessor smartProcessor() {
        return smartProcessor;
    }

    public SmartGroundNavigator setAvoidLeaves(boolean b) {
        smartProcessor.setAvoidLeaves(b);
        return this;
    }

    /** Whether closed wooden fence gates are planned through (and opened). */
    public SmartGroundNavigator setPassGates(boolean b) {
        smartProcessor.setPassGates(b);
        return this;
    }

    /** Whether the navigator itself opens wooden doors/gates on the path (default on). */
    public SmartGroundNavigator setOpenDoors(boolean b) {
        openDoors = b;
        return this;
    }

    /** Whether doors/gates it opened are closed again once the mob is through (default on). */
    public SmartGroundNavigator setCloseBehind(boolean b) {
        closeBehind = b;
        return this;
    }

    /** How many paths the stuck watchdog has given up so far (monotonic counter). */
    public int getGiveUps() {
        return giveUps;
    }

    @Override
    public void onUpdateNavigation() {
        super.onUpdateNavigation();
        if (world.isRemote) return;
        tick++;
        if (openDoors) openAhead();
        if (closeBehind && !opened.isEmpty() && tick % 5 == 0) closeBehind();
        if (tick % CHECK_INTERVAL == 0) watchdog();
    }

    // --- path following -------------------------------------------------------------------------------

    /**
     * Vanilla path following with two changes that stop mobs wedging themselves in doorways and on corners:
     * <ul>
     *     <li>a "narrow" path point (door, gate, a cell walled in on both sides across the way) and the point
     *     before it are only counted as reached when the mob is close to the cell centre, so it lines up with
     *     the opening before it walks in instead of hitting the frame at an angle;</li>
     *     <li>no shortcut past or diagonally into a narrow point, and a shortcut must be clear along both
     *     edges of the body, not only along its centre line (vanilla checks a strip one block wide, which
     *     clips corners for mobs walking off-centre).</li>
     * </ul>
     */
    @Override
    protected void pathFollow() {
        Path path = currentPath;
        Vec3d pos = getEntityPosition();
        int len = path.getCurrentPathLength();
        int sameLevelEnd = len;
        for (int j = path.getCurrentPathIndex(); j < len; j++) {
            if (path.getPathPointFromIndex(j).y != Math.floor(pos.y)) {
                sameLevelEnd = j;
                break;
            }
        }
        float loose = entity.width > 0.75F ? entity.width / 2.0F : 0.75F - entity.width / 2.0F;
        int cur = path.getCurrentPathIndex();
        boolean tight = isNarrow(path, cur) || cur + 1 < len && isNarrow(path, cur + 1);
        maxDistanceToWaypoint = tight ? Math.min(loose, 0.22F) : loose;
        Vec3d at = path.getCurrentPos();
        if (Math.abs(entity.posX - (at.x + 0.5)) < maxDistanceToWaypoint
                && Math.abs(entity.posZ - (at.z + 0.5)) < maxDistanceToWaypoint
                && Math.abs(entity.posY - at.y) < 1.0) {
            path.setCurrentPathIndex(cur + 1);
            cur++;
        }
        // shortcuts never skip a narrow point: it has to be walked into straight from the point before it
        int limit = sameLevelEnd - 1;
        for (int j = cur; j <= limit; j++) {
            if (isNarrow(path, j)) {
                limit = j == cur ? cur : j - 1;
                break;
            }
        }
        int sx = MathHelper.ceil(entity.width), sy = MathHelper.ceil(entity.height);
        for (int j = limit; j > cur; j--) {
            Vec3d to = path.getVectorFromIndex(entity, j);
            if (isDirectPathBetweenPoints(pos, to, sx, sy, sx) && edgesClear(pos, to, sx, sy)) {
                path.setCurrentPathIndex(j);
                break;
            }
        }
        checkForStuck(pos);
    }

    /** The straight walk is also clear along the left and right edge of the body. */
    private boolean edgesClear(Vec3d from, Vec3d to, int sx, int sy) {
        double dx = to.x - from.x, dz = to.z - from.z;
        double l = Math.sqrt(dx * dx + dz * dz);
        if (l < 1e-4) return true;
        double half = entity.width / 2.0 + 0.05;
        double px = -dz / l * half, pz = dx / l * half;
        for (int s = -1; s <= 1; s += 2) {
            Vec3d a = from.add(px * s, 0, pz * s), b = to.add(px * s, 0, pz * s);
            if (!isDirectPathBetweenPoints(a, b, sx, sy, sx)) return false;
        }
        return true;
    }

    /** Doors, gates and one-wide gaps (walled on both sides across the direction of travel) along the path. */
    protected boolean isNarrow(Path path, int i) {
        if (i < 0 || i >= path.getCurrentPathLength()) return false;
        if (path != narrowPath || narrowCache.length != path.getCurrentPathLength()) {
            narrowPath = path;
            narrowCache = new byte[path.getCurrentPathLength()];
        }
        if (narrowCache[i] == 0) narrowCache[i] = computeNarrow(path, i) ? (byte) 1 : (byte) 2;
        return narrowCache[i] == 1;
    }

    private Path narrowPath;
    private byte[] narrowCache = new byte[0];

    private boolean computeNarrow(Path path, int i) {
        PathPoint pt = path.getPathPointFromIndex(i);
        BlockPos p = new BlockPos(pt.x, pt.y, pt.z);
        if (isPassageBlock(world.getBlockState(p)) || isPassageBlock(world.getBlockState(p.up()))) return true;
        PathPoint ref = i > 0 ? path.getPathPointFromIndex(i - 1) : i + 1 < path.getCurrentPathLength() ? path.getPathPointFromIndex(i + 1) : null;
        if (ref == null) return false;
        int dx = pt.x - ref.x, dz = pt.z - ref.z;
        int head = Math.max(1, MathHelper.ceil(entity.height)) - 1;
        if (dx != 0 && wallsBoth(p, EnumFacing.NORTH, head)) return true;
        return dz != 0 && wallsBoth(p, EnumFacing.WEST, head);
    }

    private boolean wallsBoth(BlockPos p, EnumFacing side, int head) {
        boolean a = false, b = false;
        for (int y = 0; y <= head; y++) {
            a |= TerrainAssist.blocks(world, p.up(y).offset(side));
            b |= TerrainAssist.blocks(world, p.up(y).offset(side.getOpposite()));
        }
        return a && b;
    }

    /** Doors, fence gates and trapdoors: blocks a mob has to pass through square-on. */
    public static boolean isPassageBlock(IBlockState s) {
        net.minecraft.block.Block b = s.getBlock();
        return b instanceof BlockDoor || b instanceof BlockFenceGate || b instanceof net.minecraft.block.BlockTrapDoor;
    }

    // --- doors and gates ------------------------------------------------------------------------------

    private static boolean isWoodDoor(IBlockState s) {
        return s.getBlock() instanceof BlockDoor && s.getMaterial() == Material.WOOD;
    }

    private void openAhead() {
        Path path = getPath();
        if (path == null || path.isFinished()) return;
        int from = path.getCurrentPathIndex();
        int to = Math.min(path.getCurrentPathLength(), from + 3);
        for (int i = Math.max(0, from - 1); i < to; i++) {
            PathPoint pt = path.getPathPointFromIndex(i);
            BlockPos p = new BlockPos(pt.x, pt.y, pt.z);
            if (entity.getDistanceSq(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) > 4.0) continue;
            open(p);
            open(p.up());
        }
    }

    /** Opens a wooden door or fence gate at {@code p}; true if something was opened. */
    public boolean open(BlockPos p) {
        IBlockState s = world.getBlockState(p);
        if (isWoodDoor(s)) {
            BlockPos lower = s.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER ? p.down() : p;
            if (BlockDoor.isOpen(world, lower)) return false;
            ((BlockDoor) s.getBlock()).toggleDoor(world, lower, true);
            remember(lower);
            return true;
        }
        if (smartProcessor.passesGates() && SmartWalkNodeProcessor.isOpenableGate(s) && !s.getValue(BlockFenceGate.OPEN)) {
            EnumFacing face = s.getValue(BlockFenceGate.FACING);
            // swing away from the mob like a player-opened gate
            EnumFacing toMob = EnumFacing.getFacingFromVector((float) (entity.posX - (p.getX() + 0.5)), 0, (float) (entity.posZ - (p.getZ() + 0.5)));
            if (face == toMob) face = face.getOpposite();
            world.setBlockState(p, s.withProperty(BlockFenceGate.OPEN, true).withProperty(BlockFenceGate.FACING, face), 10);
            world.playEvent(null, 1008, p, 0);
            remember(p);
            return true;
        }
        return false;
    }

    private void remember(BlockPos p) {
        BlockPos im = p.toImmutable();
        if (!opened.contains(im)) opened.add(im);
    }

    private void closeBehind() {
        for (Iterator<BlockPos> it = opened.iterator(); it.hasNext(); ) {
            BlockPos p = it.next();
            if (!world.isBlockLoaded(p)) { it.remove(); continue; }
            double d = entity.getDistanceSq(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            if (d < 2.5 * 2.5) continue;
            if (d < 16 * 16 && !world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(p).grow(0.1, 0, 0.1).expand(0, 1, 0)).isEmpty()) {
                continue; // someone (a herded animal, another villager) is still in the doorway
            }
            IBlockState s = world.getBlockState(p);
            if (isWoodDoor(s) && BlockDoor.isOpen(world, p)) {
                ((BlockDoor) s.getBlock()).toggleDoor(world, p, false);
            } else if (SmartWalkNodeProcessor.isOpenableGate(s) && s.getValue(BlockFenceGate.OPEN)) {
                world.setBlockState(p, s.withProperty(BlockFenceGate.OPEN, false), 10);
                world.playEvent(null, 1014, p, 0);
            }
            it.remove();
        }
    }

    // --- watchdog -------------------------------------------------------------------------------------

    private void watchdog() {
        if (entity.isRiding()) return;
        if (popOutOfBlock()) return;
        Path path = getPath();
        double dx = entity.posX - lastX, dy = entity.posY - lastY, dz = entity.posZ - lastZ;
        lastX = entity.posX;
        lastY = entity.posY;
        lastZ = entity.posZ;
        if (path == null || path.isFinished() || dx * dx + dy * dy + dz * dz > 0.04) {
            stuckChecks = 0;
            return;
        }
        stuckChecks++;
        BlockPos feet = new BlockPos(entity);
        if (stuckChecks == 2 || stuckChecks == 4) {
            open(feet);
            open(feet.up());
            for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
                open(feet.offset(f));
                open(feet.offset(f).up());
            }
            entity.motionX += (feet.getX() + 0.5 - entity.posX) * 0.3;
            entity.motionZ += (feet.getZ() + 0.5 - entity.posZ) * 0.3;
            if (entity.onGround) entity.getJumpHelper().setJumping();
        } else if (stuckChecks == 6) {
            // a fresh path from the exact current position often avoids the corner it was wedged in
            PathPoint end = path.getFinalPathPoint();
            if (end != null) {
                Path fresh = getPathToPos(new BlockPos(end.x, end.y, end.z));
                if (fresh != null) setPath(fresh, speed);
            }
        } else if (stuckChecks >= 8) {
            stuckChecks = 0;
            if (!hopToNextPoint(path)) {
                clearPath();
                giveUps++;
            }
        }
    }

    private boolean hopToNextPoint(Path path) {
        if (path.isFinished()) return false;
        int idx = path.getCurrentPathIndex();
        Vec3d next = path.getVectorFromIndex(entity, idx);
        if (entity.getDistanceSq(next.x, next.y, next.z) < 0.25 && idx + 1 < path.getCurrentPathLength()) {
            next = path.getVectorFromIndex(entity, idx + 1);
        }
        if (entity.getDistanceSq(next.x, next.y, next.z) > 9) return false;
        AxisAlignedBB to = entity.getEntityBoundingBox().offset(next.x - entity.posX, next.y - entity.posY, next.z - entity.posZ);
        if (!world.getCollisionBoxes(entity, to).isEmpty() || world.containsAnyLiquid(to)) return false;
        entity.setPositionAndUpdate(next.x, next.y, next.z);
        return true;
    }

    /** Moves the mob to the nearest free spot when its body is inside a solid block. */
    private boolean popOutOfBlock() {
        AxisAlignedBB box = entity.getEntityBoundingBox().shrink(0.05);
        if (world.getCollisionBoxes(entity, box).isEmpty()) return false;
        // a door/gate/trapdoor closing on the mob, or a pane/fence edge it leans on, is not "inside a block"
        boolean solidInside = false;
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            net.minecraft.block.state.IBlockState st = world.getBlockState(p);
            if (st.isNormalCube() && st.getCollisionBoundingBox(world, p) != null) { solidInside = true; break; }
        }
        if (!solidInside) return false;
        BlockPos feet = new BlockPos(entity);
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.getAllInBoxMutable(feet.add(-2, -1, -2), feet.add(2, 1, 2))) {
            AxisAlignedBB at = entity.getEntityBoundingBox().offset(p.getX() + 0.5 - entity.posX, p.getY() - entity.posY, p.getZ() + 0.5 - entity.posZ);
            if (!world.getCollisionBoxes(entity, at).isEmpty() || world.containsAnyLiquid(at)) continue;
            if (!world.getBlockState(p.down()).getMaterial().blocksMovement()) continue;
            // stay on the same level: never onto a wall top when a spot beside it is free
            double d = p.distanceSq(feet) + (p.getY() > feet.getY() ? 6 : 0);
            if (d < bd) { bd = d; best = p.toImmutable(); }
        }
        if (best == null) return false;
        entity.setPositionAndUpdate(best.getX() + 0.5, best.getY(), best.getZ() + 0.5);
        clearPath();
        return true;
    }
}
