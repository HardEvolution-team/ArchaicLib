package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**
 * 1.7.10 {@code MovingObjectPosition}: public int block coords and an int {@code sideHit}
 * (0 down, 1 up, 2 north, 3 south, 4 west, 5 east - same order as {@link EnumFacing#getIndex()}).
 * Lets ported code keep {@code mop.blockX} / {@code mop.sideHit == 4} style logic unchanged.
 */
public class LegacyMOP {
    public enum Type { MISS, BLOCK, ENTITY }

    public Type typeOfHit;
    public int blockX, blockY, blockZ;
    /** -1 when no side (entity hit / miss). */
    public int sideHit = -1;
    public Vec3d hitVec;
    public Entity entityHit;

    public LegacyMOP(int x, int y, int z, int side, Vec3d hit) {
        this.typeOfHit = Type.BLOCK;
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
        this.sideHit = side;
        this.hitVec = hit;
    }

    public LegacyMOP(Entity entity) {
        this(entity, new Vec3d(entity.posX, entity.posY, entity.posZ));
    }

    public LegacyMOP(Entity entity, Vec3d hit) {
        this.typeOfHit = Type.ENTITY;
        this.entityHit = entity;
        this.hitVec = hit;
    }

    /** @return null for null or MISS results, like 1.7 rayTraceBlocks. */
    public static LegacyMOP of(RayTraceResult r) {
        if (r == null || r.typeOfHit == RayTraceResult.Type.MISS) return null;
        if (r.typeOfHit == RayTraceResult.Type.ENTITY) return new LegacyMOP(r.entityHit, r.hitVec);
        BlockPos p = r.getBlockPos();
        return new LegacyMOP(p.getX(), p.getY(), p.getZ(), r.sideHit == null ? -1 : r.sideHit.getIndex(), r.hitVec);
    }

    public BlockPos getBlockPos() {
        return new BlockPos(blockX, blockY, blockZ);
    }

    public EnumFacing getFacing() {
        return sideHit < 0 ? null : EnumFacing.byIndex(sideHit);
    }

    /** Block next to the hit face (the 1.7 "sideHit == 4 ? x - 1 : ..." pattern). */
    public BlockPos getOffsetPos() {
        EnumFacing f = getFacing();
        return f == null ? getBlockPos() : getBlockPos().offset(f);
    }

    @Override
    public String toString() {
        return "LegacyMOP{" + typeOfHit + ", " + blockX + "," + blockY + "," + blockZ + ", side=" + sideHit + ", hit=" + hitVec + ", entity=" + entityHit + "}";
    }
}
