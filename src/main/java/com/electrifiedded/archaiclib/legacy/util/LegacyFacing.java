package com.electrifiedded.archaiclib.legacy.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.7 int side helpers ({@code net.minecraft.util.Facing}, {@code ForgeDirection} offsets,
 * {@code BlockPistonBase.determineOrientation}). Side numbering 0..5 = down, up, north, south, west, east
 * (identical to {@link EnumFacing#getIndex()}).
 */
public final class LegacyFacing {
    private LegacyFacing() {}

    /** 1.7 {@code Facing.oppositeSide} */
    public static final int[] OPPOSITE = {1, 0, 3, 2, 5, 4};
    /** 1.7 {@code Facing.offsetsXForSide} etc. */
    public static final int[] OFFSET_X = {0, 0, 0, 0, -1, 1};
    public static final int[] OFFSET_Y = {-1, 1, 0, 0, 0, 0};
    public static final int[] OFFSET_Z = {0, 0, -1, 1, 0, 0};

    public static int opposite(int side) {
        return OPPOSITE[side % 6];
    }

    public static EnumFacing facing(int side) {
        return EnumFacing.byIndex(side % 6);
    }

    public static BlockPos offset(BlockPos pos, int side, int distance) {
        int s = side % 6;
        return pos.add(OFFSET_X[s] * distance, OFFSET_Y[s] * distance, OFFSET_Z[s] * distance);
    }

    /** 1.7 {@code BlockPistonBase.determineOrientation}: side facing the placer (up/down when looking steeply). */
    public static int determineOrientation(World world, BlockPos pos, EntityLivingBase placer) {
        return EnumFacing.getDirectionFromEntityLiving(pos, placer).getIndex();
    }
}
