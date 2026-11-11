package com.electrifiedded.archaiclib.v1710.api.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public final class LegacyEntityMover {

    private LegacyEntityMover() {}

    public static void move(Entity entity, double x, double y, double z,
                            Function<AxisAlignedBB, List<AxisAlignedBB>> boxes) {
        if (entity.noClip) {
            entity.setEntityBoundingBox(entity.getEntityBoundingBox().offset(x, y, z));
            entity.resetPositionToBB();
            return;
        }

        entity.world.profiler.startSection("move");
        final double ox = x, oy = y, oz = z;
        final AxisAlignedBB start = entity.getEntityBoundingBox();
        AxisAlignedBB bb = start;

        List<AxisAlignedBB> list = boxes.apply(bb.expand(x, y, z));
        for (AxisAlignedBB box : list) y = box.calculateYOffset(bb, y);
        bb = bb.offset(0, y, 0);
        boolean grounded = entity.onGround || oy != y && oy < 0;
        for (AxisAlignedBB box : list) x = box.calculateXOffset(bb, x);
        bb = bb.offset(x, 0, 0);
        for (AxisAlignedBB box : list) z = box.calculateZOffset(bb, z);
        bb = bb.offset(0, 0, z);

        if (entity.stepHeight > 0 && grounded && (ox != x || oz != z)) {
            double noStepX = x, noStepY = y, noStepZ = z;
            AxisAlignedBB noStepBB = bb;

            x = ox;
            y = entity.stepHeight;
            z = oz;
            bb = start;
            list = boxes.apply(bb.expand(ox, y, oz));
            for (AxisAlignedBB box : list) y = box.calculateYOffset(bb, y);
            bb = bb.offset(0, y, 0);
            for (AxisAlignedBB box : list) x = box.calculateXOffset(bb, x);
            bb = bb.offset(x, 0, 0);
            for (AxisAlignedBB box : list) z = box.calculateZOffset(bb, z);
            bb = bb.offset(0, 0, z);

            y = -entity.stepHeight;
            for (AxisAlignedBB box : list) y = box.calculateYOffset(bb, y);
            bb = bb.offset(0, y, 0);

            if (noStepX * noStepX + noStepZ * noStepZ >= x * x + z * z) {
                x = noStepX;
                y = noStepY;
                z = noStepZ;
                bb = noStepBB;
            }
        }

        entity.world.profiler.endStartSection("rest");
        entity.setEntityBoundingBox(bb);
        entity.resetPositionToBB();
        entity.collidedHorizontally = ox != x || oz != z;
        entity.collidedVertically = oy != y;
        entity.onGround = oy != y && oy < 0;
        entity.collided = entity.collidedHorizontally || entity.collidedVertically;

        if (entity.onGround) {
            entity.fallDistance = 0;
        } else if (y < 0) {
            entity.fallDistance -= (float) y;
        }

        if (ox != x) entity.motionX = 0;
        if (oy != y) entity.motionY = 0;
        if (oz != z) entity.motionZ = 0;
        entity.world.profiler.endSection();
    }

    public static List<AxisAlignedBB> blockBoxes(World world, Entity entity, AxisAlignedBB area,
                                                 BiPredicate<IBlockState, BlockPos> filter) {
        List<AxisAlignedBB> out = new ArrayList<>();
        int x0 = MathHelper.floor(area.minX), x1 = MathHelper.floor(area.maxX + 1);
        int y0 = MathHelper.floor(area.minY), y1 = MathHelper.floor(area.maxY + 1);
        int z0 = MathHelper.floor(area.minZ), z1 = MathHelper.floor(area.maxZ + 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int bx = x0; bx < x1; bx++) {
            for (int bz = z0; bz < z1; bz++) {
                if (!world.isBlockLoaded(pos.setPos(bx, 64, bz))) continue;
                boolean inside = bx >= -30000000 && bx < 30000000 && bz >= -30000000 && bz < 30000000;
                for (int by = y0 - 1; by < y1; by++) {
                    pos.setPos(bx, by, bz);
                    IBlockState state = inside ? world.getBlockState(pos) : Blocks.STONE.getDefaultState();
                    if (filter == null || filter.test(state, pos)) {
                        state.addCollisionBoxToList(world, pos, area, out, entity, false);
                    }
                }
            }
        }
        return out;
    }

    public static void addEntityBoxes(Entity self, AxisAlignedBB area, Predicate<Entity> filter,
                                      List<AxisAlignedBB> out) {
        for (Entity other : self.world.getEntitiesWithinAABBExcludingEntity(self, area.grow(0.25))) {
            if (filter != null && !filter.test(other)) continue;
            AxisAlignedBB box = other.getCollisionBoundingBox();
            if (box != null && box.intersects(area)) out.add(box);
            box = self.getCollisionBox(other);
            if (box != null && box.intersects(area)) out.add(box);
        }
    }
}
