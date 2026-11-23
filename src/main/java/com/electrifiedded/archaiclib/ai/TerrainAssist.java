package com.electrifiedded.archaiclib.ai;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

/**
 * "What is in my way?" for a ground mob that can dig and build (workers, miners, undead servants).
 * {@link #plan} looks at the one-cell step the mob wants to take toward a target and answers with a single
 * action: break a block (the ceiling over its head that stops the jump, the block in front, the floor when
 * the target is right below) or place one (a step to climb, a bridge over a gap, a pillar under itself when
 * the target is overhead). The caller decides whether it may do it (tools, protected blocks), does it over
 * time, then asks again. {@link #isNaturalTerrain} is a conservative "not built by a player" filter for
 * breaking outside of a work area.
 */
public final class TerrainAssist {

    public enum Kind { NONE, BREAK, PLACE, PILLAR }

    public static final class Step {
        public static final Step NONE = new Step(Kind.NONE, null);
        public final Kind kind;
        public final BlockPos pos;

        Step(Kind kind, BlockPos pos) {
            this.kind = kind;
            this.pos = pos;
        }

        public boolean isNone() {
            return kind == Kind.NONE;
        }

        @Override
        public String toString() {
            return kind + (pos == null ? "" : " " + pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }
    }

    private TerrainAssist() {}

    /** A block the body collides with. */
    public static boolean blocks(World world, BlockPos p) {
        IBlockState s = world.getBlockState(p);
        return s.getMaterial().blocksMovement() && s.getCollisionBoundingBox(world, p) != null;
    }

    private static boolean liquidAt(World world, BlockPos p) {
        return world.getBlockState(p).getMaterial().isLiquid();
    }

    /** True when breaking {@code p} would let a liquid in (it or a side/top neighbour is liquid). */
    public static boolean letsLiquidIn(World world, BlockPos p) {
        if (liquidAt(world, p)) return true;
        for (EnumFacing f : EnumFacing.values()) {
            if (f != EnumFacing.DOWN && liquidAt(world, p.offset(f))) return true;
        }
        return false;
    }

    /** Feet cell of the mob. */
    public static BlockPos feet(Entity mob) {
        return new BlockPos(mob.posX, mob.getEntityBoundingBox().minY + 0.01, mob.posZ);
    }

    /**
     * One action that gets {@code mob} a cell closer to the feet target {@code tx,ty,tz} (world coordinates:
     * block centre x/z, floor y), or NONE when the way is free or nothing sensible can be done.
     */
    public static Step plan(Entity mob, double tx, double ty, double tz) {
        World world = mob.world;
        BlockPos feet = feet(mob);
        int head = Math.max(1, MathHelper.ceil(mob.height)); // cells the body occupies (2 for villagers)
        double dx = tx - mob.posX, dz = tz - mob.posZ, dy = ty - feet.getY();
        double h = Math.sqrt(dx * dx + dz * dz);

        // target overhead: pillar up (jump, put a block under oneself), clearing the ceiling first
        if (h < 0.8 && dy >= 1) {
            BlockPos over = feet.up(head);
            if (blocks(world, over)) return breakable(world, over);
            return new Step(Kind.PILLAR, feet);
        }
        // target below: dig the floor (never into a liquid or a drop deeper than one block)
        if (h < 0.8 && dy <= -1) {
            BlockPos floor = feet.down();
            if (blocks(world, floor) && !liquidAt(world, floor.down()) && blocks(world, floor.down())) return breakable(world, floor);
            return Step.NONE;
        }
        if (h < 0.3) return Step.NONE;
        EnumFacing dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? EnumFacing.EAST : EnumFacing.WEST) : (dz > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH);
        BlockPos next = feet.offset(dir);

        if (dy >= 0.5) {
            // up one: the next cell is the step, above it room for the body, over our own head one more
            // free block for the jump (the usual reason a 2-high stair tunnel cannot be climbed)
            if (!blocks(world, next)) {
                if (!blocks(world, next.down())) return new Step(Kind.PLACE, next.down()); // gap: bridge first
                return new Step(Kind.PLACE, next);                                         // no step: build one
            }
            for (int i = 1; i <= head; i++) if (blocks(world, next.up(i))) return breakable(world, next.up(i));
            if (blocks(world, feet.up(head))) return breakable(world, feet.up(head));
            return Step.NONE;
        }
        if (dy <= -0.5) {
            // down one: the next cell free from the lower floor up to our own head height
            for (int i = head - 1; i >= -1; i--) {
                BlockPos p = next.up(i);
                if (blocks(world, p)) return breakable(world, p);
            }
            if (!blocks(world, next.down(2)) && !blocks(world, next.down(3)) && !blocks(world, next.down(4))) {
                return new Step(Kind.PLACE, next.down(2)); // deep drop under the step: put a floor in
            }
            return Step.NONE;
        }
        // level
        for (int i = head - 1; i >= 0; i--) {
            BlockPos p = next.up(i);
            if (blocks(world, p)) return breakable(world, p);
        }
        if (!blocks(world, next.down()) && !blocks(world, next.down(2)) && !blocks(world, next.down(3))) {
            return new Step(Kind.PLACE, next.down()); // bridge the gap
        }
        return Step.NONE;
    }

    /**
     * Line of sight from the mob's eyes to block {@code target}: null when it can see the block (any of its
     * centre or the face centres turned toward the eyes), otherwise the first solid block in the way of the
     * centre ray - the block to dig first when the target lies behind a wall ("tunnel to the ore").
     */
    @javax.annotation.Nullable
    public static BlockPos sightBlocker(net.minecraft.entity.EntityLivingBase mob, BlockPos target) {
        return sightBlocker(mob.world, mob.getPositionEyes(1.0F), target);
    }

    /** {@link #sightBlocker(net.minecraft.entity.EntityLivingBase, BlockPos)} from any eye position (planning a stand spot). */
    @javax.annotation.Nullable
    public static BlockPos sightBlocker(World world, net.minecraft.util.math.Vec3d eye, BlockPos target) {
        net.minecraft.util.math.Vec3d c = new net.minecraft.util.math.Vec3d(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        BlockPos first = null;
        java.util.List<net.minecraft.util.math.Vec3d> aims = new java.util.ArrayList<>(4);
        aims.add(c);
        for (EnumFacing f : EnumFacing.values()) {
            net.minecraft.util.math.Vec3d face = c.add(f.getXOffset() * 0.49, f.getYOffset() * 0.49, f.getZOffset() * 0.49);
            // only faces turned toward the eyes can be seen
            if ((eye.x - c.x) * f.getXOffset() + (eye.y - c.y) * f.getYOffset() + (eye.z - c.z) * f.getZOffset() > 0.5) aims.add(face);
        }
        for (net.minecraft.util.math.Vec3d aim : aims) {
            net.minecraft.util.math.RayTraceResult r = world.rayTraceBlocks(eye, aim, false, true, false);
            if (r == null || r.typeOfHit != net.minecraft.util.math.RayTraceResult.Type.BLOCK || r.getBlockPos().equals(target)) return null;
            if (first == null) first = r.getBlockPos().toImmutable();
        }
        return first;
    }

    private static Step breakable(World world, BlockPos p) {
        IBlockState s = world.getBlockState(p);
        if (s.getBlockHardness(world, p) < 0 || world.getTileEntity(p) != null || letsLiquidIn(world, p)) return Step.NONE;
        return new Step(Kind.BREAK, p.toImmutable());
    }

    /** Whether a block may be put at {@code p} without trapping the mob (outside its body, replaceable). */
    public static boolean canPlaceAt(Entity mob, BlockPos p) {
        IBlockState s = mob.world.getBlockState(p);
        if (!s.getBlock().isReplaceable(mob.world, p) && !s.getBlock().isAir(s, mob.world, p)) return false;
        return !mob.getEntityBoundingBox().intersects(new AxisAlignedBB(p));
    }

    /**
     * Whether a helper block (step, bridge, pillar) at {@code p} cannot spoil anything built: no door, gate,
     * trapdoor, ladder or bed within two blocks (a cobble block in front of a door locks everyone out), and
     * the ground under it is natural terrain, not a house floor, a road or a field.
     */
    /**
     * Something hangs on or stands on {@code p} that would pop off if it were broken: a torch, lever, button,
     * sign, ladder, door, rail, redstone, carpet or any block with a tile entity on top (markers, chests...).
     * Plants and grass do not count (they grow back / are worthless).
     */
    public static boolean supportsSomething(World world, BlockPos p) {
        if (attached(world, p.up(), true)) return true;
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) if (attached(world, p.offset(f), false)) return true;
        return false;
    }

    private static boolean attached(World world, BlockPos q, boolean onTop) {
        if (!world.isBlockLoaded(q)) return false;
        IBlockState s = world.getBlockState(q);
        Block b = s.getBlock();
        Material m = s.getMaterial();
        if (m == Material.AIR || m.isLiquid() || b instanceof net.minecraft.block.BlockBush || b instanceof net.minecraft.block.BlockVine) return false;
        if (b instanceof net.minecraft.block.BlockTorch || b instanceof net.minecraft.block.BlockLever || b instanceof net.minecraft.block.BlockButton
                || b instanceof net.minecraft.block.BlockLadder || b instanceof net.minecraft.block.BlockSign || b instanceof net.minecraft.block.BlockTripWireHook
                || b instanceof net.minecraft.block.BlockTrapDoor) return true;
        if (!onTop) return false;
        if (b instanceof net.minecraft.block.BlockDoor || b instanceof net.minecraft.block.BlockRailBase || b instanceof net.minecraft.block.BlockRedstoneWire
                || b instanceof net.minecraft.block.BlockCarpet || b instanceof net.minecraft.block.BlockPressurePlate || b instanceof net.minecraft.block.BlockBasePressurePlate
                || b instanceof net.minecraft.block.BlockRedstoneDiode || b instanceof net.minecraft.block.BlockFlowerPot) return true;
        // anything else small on top with a tile entity (mod markers, heads, banners...)
        return b.hasTileEntity(s) && !s.isFullCube();
    }

    public static boolean isHarmlessPlacement(World world, BlockPos p) {
        return isHarmlessPlacement(world, p, true);
    }

    /** {@link #isHarmlessPlacement(World, BlockPos)}; {@code checkFloor} false for a pillar on its own blocks. */
    public static boolean isHarmlessPlacement(World world, BlockPos p, boolean checkFloor) {
        for (BlockPos q : BlockPos.getAllInBoxMutable(p.add(-2, -2, -2), p.add(2, 2, 2))) {
            IBlockState s = world.getBlockState(q);
            Block b = s.getBlock();
            if (SmartGroundNavigator.isPassageBlock(s) || b instanceof net.minecraft.block.BlockLadder
                    || b instanceof net.minecraft.block.BlockBed || b instanceof net.minecraft.block.BlockStairs
                    || b == Blocks.GRASS_PATH || b == Blocks.FARMLAND) {
                return false;
            }
        }
        if (!checkFloor) return true;
        for (int dy = 1; dy <= 4; dy++) {
            BlockPos q = p.down(dy);
            IBlockState s = world.getBlockState(q);
            if (s.getMaterial().isLiquid()) return true;
            if (blocks(world, q)) return isNaturalTerrain(s);
        }
        return true;
    }

    /**
     * Blocks that are almost certainly world generation, not something a player built: stone, dirt, grass,
     * sand, gravel, clay, netherrack, end stone, snow, terracotta, sandstone, ores (ore dictionary "ore*").
     * Cobblestone, planks, bricks, glass... are never natural.
     */
    public static boolean isNaturalTerrain(IBlockState s) {
        Block b = s.getBlock();
        if (b == Blocks.STONE || b == Blocks.DIRT || b == Blocks.GRASS || b == Blocks.GRAVEL || b == Blocks.SAND
                || b == Blocks.SANDSTONE || b == Blocks.RED_SANDSTONE || b == Blocks.CLAY || b == Blocks.NETHERRACK
                || b == Blocks.END_STONE || b == Blocks.SOUL_SAND || b == Blocks.MYCELIUM || b == Blocks.SNOW
                || b == Blocks.SNOW_LAYER || b == Blocks.HARDENED_CLAY || b == Blocks.STAINED_HARDENED_CLAY
                || b == Blocks.MONSTER_EGG) {
            return true;
        }
        if (b == Blocks.FARMLAND || b == Blocks.GRASS_PATH) return false;
        Material m = s.getMaterial();
        if (m == Material.GROUND || m == Material.GRASS || m == Material.SAND || m == Material.CLAY) return true;
        ItemStack st = new ItemStack(b, 1, b.damageDropped(s));
        if (st.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(st)) {
            String n = OreDictionary.getOreName(id);
            if (n.startsWith("ore") || n.equals("stone") || n.equals("dirt") || n.equals("gravel") || n.equals("sand")) return true;
        }
        return false;
    }
}
