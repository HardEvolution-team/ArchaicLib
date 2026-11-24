package com.electrifiedded.archaiclib.v1710.api.block;

import com.electrifiedded.archaiclib.v1710.api.client.IIcon;
import com.electrifiedded.archaiclib.v1710.api.client.IIconRegister;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IArchaicRenderBlock {
    int RENDER_STANDARD = 0;
    int RENDER_CROSS = 1;
    int RENDER_CROPS = 6;
    /** 1.7 render type 8 (ladder): one double-sided plane 0.05 from the wall; the render key is the 1.7 meta 2..5 (wall at +Z/-Z/+X/-X), anything else = centred plane (items). */
    int RENDER_LADDER = 8;

    @SideOnly(Side.CLIENT)
    default void registerBlockIcons(IIconRegister reg) {
    }

    @SideOnly(Side.CLIENT)
    default IIcon getIcon(int side, int key) {
        return null;
    }

    default int getArchaicRenderKey(IBlockState state) {
        return ((Block)this).getMetaFromState(state);
    }

    default int getArchaicItemRenderKey(int meta) {
        return meta;
    }

    default int getArchaicRenderType(int key) {
        return RENDER_STANDARD;
    }

    default AxisAlignedBB getArchaicRenderBounds(int key) {
        return Block.FULL_BLOCK_AABB;
    }

    @SideOnly(Side.CLIENT)
    default java.util.List<ArchaicRenderPart> getArchaicRenderParts(int key) {
        return null;
    }

    default EnumFacing getArchaicRenderUp(int key) {
        return EnumFacing.UP;
    }

    default boolean hasArchaicTint() {
        return false;
    }

    default int colorMultiplier(IBlockAccess world, BlockPos pos) {
        return 0xFFFFFF;
    }

    default int getRenderColor(int meta) {
        return 0xFFFFFF;
    }
}
