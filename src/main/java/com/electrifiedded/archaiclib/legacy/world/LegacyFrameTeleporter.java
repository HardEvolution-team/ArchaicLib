package com.electrifiedded.archaiclib.legacy.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;

/**
 * Teleporter for {@link LegacyPortalBlock}: looks for an existing portal block near the entity's position in the
 * destination world (radius {@link #searchRadius}), otherwise builds a fresh 4x5 frame with a lit portal on the surface.
 */
public class LegacyFrameTeleporter implements ITeleporter {
    protected final WorldServer world;
    protected final LegacyPortalBlock portal;
    protected final Block frame;
    public int searchRadius = 32;

    public LegacyFrameTeleporter(WorldServer world, LegacyPortalBlock portal, Block frame) {
        this.world = world;
        this.portal = portal;
        this.frame = frame;
    }

    @Override
    public void placeEntity(World w, Entity e, float yaw) {
        BlockPos at = find(e);
        if (at == null) at = build(e);
        e.motionX = e.motionY = e.motionZ = 0;
        if (e instanceof net.minecraft.entity.player.EntityPlayerMP mp)
            mp.connection.setPlayerLocation(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, e.rotationPitch);
        else e.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, e.rotationPitch);
        e.timeUntilPortal = 40;
    }

    protected BlockPos find(Entity e) {
        int ex = (int) Math.floor(e.posX), ey = (int) Math.floor(e.posY), ez = (int) Math.floor(e.posZ);
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = ex - searchRadius; x <= ex + searchRadius; x++)
            for (int z = ez - searchRadius; z <= ez + searchRadius; z++) {
                if (!world.isBlockLoaded(p.setPos(x, 64, z))) world.getChunk(x >> 4, z >> 4);
                for (int y = world.getActualHeight() - 1; y >= 1; y--) {
                    p.setPos(x, y, z);
                    if (world.getBlockState(p).getBlock() == portal) {
                        double d = p.distanceSq(ex, ey, ez);
                        if (d < bd) { bd = d; best = p.toImmutable(); }
                    }
                }
            }
        if (best == null) return null;
        while (world.getBlockState(best.down()).getBlock() == portal) best = best.down();
        return best;
    }

    protected BlockPos build(Entity e) {
        int x = (int) Math.floor(e.posX), z = (int) Math.floor(e.posZ);
        world.getChunk(x >> 4, z >> 4);
        int top = Math.min(world.getActualHeight(), 256) - 8;
        int y = Math.max(4, Math.min(top, world.getHeight(x, z)));
        if (world.getHeight(x, z) <= 0) y = 64;
        IBlockState f = frame.getDefaultState();
        for (int l = -1; l <= 2; l++)
            for (int dz = -1; dz <= 1; dz++)
                for (int h = 0; h <= 3; h++) world.setBlockState(new BlockPos(x + l, y + h, z + dz), Blocks.AIR.getDefaultState(), 2);
        for (int l = -1; l <= 2; l++)
            for (int dz = -1; dz <= 1; dz++) world.setBlockState(new BlockPos(x + l, y - 1, z + dz), f, 2);
        for (int i = 0; i <= 3; i++) {
            world.setBlockState(new BlockPos(x - 1, y + i, z), f, 2);
            world.setBlockState(new BlockPos(x + 2, y + i, z), f, 2);
        }
        for (int l = 0; l <= 1; l++) world.setBlockState(new BlockPos(x + l, y + 3, z), f, 2);
        portal.tryToCreatePortal(world, new BlockPos(x, y, z));
        return new BlockPos(x, y, z);
    }
}
