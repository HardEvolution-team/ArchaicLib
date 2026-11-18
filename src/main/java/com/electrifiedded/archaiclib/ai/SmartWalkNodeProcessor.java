package com.electrifiedded.archaiclib.ai;

import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Walk node processor for "worker" mobs ported from old mods (villagers, helpers, pets).
 * <ul>
 *     <li>tree tops are not walkable: a floor made of leaves is blocked, so mobs stop climbing into
 *     canopies and getting lost there (while standing on leaves they may still use them to get down);</li>
 *     <li>closed wooden fence gates are treated like closed wooden doors, so with
 *     {@link SmartGroundNavigator} the mob plans through them and opens them on the way.</li>
 * </ul>
 */
public class SmartWalkNodeProcessor extends WalkNodeProcessor {

    private boolean avoidLeaves = true;
    private boolean passGates = true;
    /** set per path search: the mob currently stands on leaves and has to be allowed to walk off them */
    private boolean escapingLeaves;

    public void setAvoidLeaves(boolean avoidLeaves) {
        this.avoidLeaves = avoidLeaves;
    }

    public void setPassGates(boolean passGates) {
        this.passGates = passGates;
    }

    public boolean passesGates() {
        return passGates;
    }

    @Override
    public void init(IBlockAccess source, EntityLiving mob) {
        super.init(source, mob);
        escapingLeaves = false;
        if (avoidLeaves) {
            BlockPos feet = new BlockPos(mob.posX, mob.getEntityBoundingBox().minY, mob.posZ);
            for (int dy = 1; dy <= 2 && !escapingLeaves; dy++) {
                escapingLeaves = source.getBlockState(feet.down(dy)).getMaterial() == Material.LEAVES;
            }
        }
    }

    @Override
    public PathNodeType getPathNodeType(IBlockAccess world, int x, int y, int z) {
        PathNodeType type = super.getPathNodeType(world, x, y, z);
        if (avoidLeaves && type == PathNodeType.WALKABLE && y > 0
                && world.getBlockState(new BlockPos(x, y - 1, z)).getMaterial() == Material.LEAVES) {
            return escapingLeaves ? PathNodeType.DANGER_OTHER : PathNodeType.BLOCKED;
        }
        return type;
    }

    @Override
    protected PathNodeType getPathNodeTypeRaw(IBlockAccess world, int x, int y, int z) {
        if (passGates) {
            IBlockState s = world.getBlockState(new BlockPos(x, y, z));
            if (s.getBlock() instanceof BlockFenceGate && s.getMaterial() == Material.WOOD && !s.getValue(BlockFenceGate.OPEN)) {
                return PathNodeType.DOOR_WOOD_CLOSED;
            }
        }
        return super.getPathNodeTypeRaw(world, x, y, z);
    }

    /** True for blocks this processor lets a mob path through after opening them. */
    public static boolean isOpenableGate(IBlockState s) {
        return s.getBlock() instanceof BlockFenceGate && s.getMaterial() == Material.WOOD;
    }
}
