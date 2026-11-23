package com.electrifiedded.archaiclib.legacy.tileentity;

import com.electrifiedded.archaiclib.legacy.world.LegacyWorld;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.7.10 style tile entity: public {@code xCoord/yCoord/zCoord}, a ticking {@link #updateEntity()},
 * and the 1.7 "description packet = full writeToNBT" sync behaviour
 * ({@link #getUpdatePacket()} / {@link #getUpdateTag()} / {@link #onDataPacket}).
 * <p>
 * Override {@link #writeLegacyNBT(NBTTagCompound)} / {@link #readLegacyNBT(NBTTagCompound)} (the old
 * writeToNBT/readFromNBT bodies, without returning a tag), {@link #onDescriptionPacket(NBTTagCompound)}
 * for a custom client reaction, and {@link #isTicking()} to disable ticking.
 */
public abstract class LegacyTileEntity extends TileEntity implements ITickable {
    public int xCoord, yCoord, zCoord;

    @Override
    public void setPos(BlockPos posIn) {
        super.setPos(posIn);
        syncCoords();
    }

    @Override
    public void setWorld(World worldIn) {
        super.setWorld(worldIn);
        syncCoords();
    }

    @Override
    public void validate() {
        super.validate();
        syncCoords();
    }

    protected void syncCoords() {
        BlockPos p = getPos();
        xCoord = p.getX();
        yCoord = p.getY();
        zCoord = p.getZ();
    }

    /** Set coords directly, e.g. for a virtual tile entity that never gets placed in the world. */
    public void setLegacyCoords(int x, int y, int z) {
        setPos(new BlockPos(x, y, z));
    }

    /** 1.7 updateEntity. */
    public void updateEntity() {}

    /** @return false to skip {@link #updateEntity()} calls. */
    public boolean isTicking() {
        return true;
    }

    @Override
    public final void update() {
        if (isTicking()) updateEntity();
    }

    /** Old writeToNBT body. super is already called. */
    public void writeLegacyNBT(NBTTagCompound tag) {}

    /** Old readFromNBT body. super is already called. */
    public void readLegacyNBT(NBTTagCompound tag) {}

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        writeLegacyNBT(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        syncCoords();
        readLegacyNBT(compound);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
        onDescriptionPacket(tag);
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
        onDescriptionPacket(pkt.getNbtCompound());
    }

    /** Called on the client after a description packet was read. */
    public void onDescriptionPacket(NBTTagCompound tag) {}

    /** 1.7 {@code worldObj.markBlockForUpdate(xCoord, yCoord, zCoord)}. */
    public void markBlockForUpdate() {
        LegacyWorld.markBlockForUpdate(world, xCoord, yCoord, zCoord);
    }

    /** 1.7 tile entities survived block state changes of their own block - keep that. */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /** 1.7 {@code getDistanceFrom(x, y, z)} (squared distance to block centre). */
    public double getDistanceFrom(double x, double y, double z) {
        double dx = xCoord + 0.5 - x, dy = yCoord + 0.5 - y, dz = zCoord + 0.5 - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public int getBlockMetadata() {
        if (world == null) return 0;
        IBlockState s = world.getBlockState(getPos());
        return s.getBlock().getMetaFromState(s);
    }
}
