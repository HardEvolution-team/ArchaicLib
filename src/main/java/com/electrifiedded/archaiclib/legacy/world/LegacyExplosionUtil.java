package com.electrifiedded.archaiclib.legacy.world;

import com.electrifiedded.archaiclib.legacy.sound.LegacySounds;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Building blocks of the 1.7 {@code Explosion} that old mods copy-pasted to make custom/shaped blasts:
 * the 16^3 ray grid (optionally restricted to a sub-cube of directions), the entity damage/knockback formula,
 * per-block destruction with drop chance, debris particles and the explosion sound.
 * All methods are server-side safe; particles go out via {@link LegacyParticles#spawnBroadcast}.
 */
public final class LegacyExplosionUtil {
    private LegacyExplosionUtil() {}

    /** Zero-power explosion object for {@code Block.onBlockExploded}/resistance calls (1.7 "FAKE_EXPLOSION"). */
    public static Explosion dummy(World world, double x, double y, double z) {
        return new Explosion(world, null, x, y, z, 0.0F, false, false);
    }

    /** 1.7 {@code playSoundEffect(x,y,z,"random.explode",4,(1+(r-r)*0.2)*0.7)}. */
    public static void explodeSound(World world, double x, double y, double z) {
        LegacySounds.playSoundEffect(world, x, y, z, "random.explode", 4.0F,
                (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
    }

    /**
     * 1.7 {@code doExplosionA} ray cast. Grid bounds are in 0..16 (doubled internally to the 0..31 ray grid like the
     * original mods did); pass 0,16,0,16,0,16 for a full sphere. Server only.
     *
     * @param forceInclude blocks always added even when the ray is spent (may be null)
     */
    public static Set<BlockPos> rayCast(World world, @Nullable Entity exploder, double x, double y, double z, float power,
                                        int sx, int ex, int sy, int ey, int sz, int ez,
                                        @Nullable Predicate<IBlockState> forceInclude) {
        Set<BlockPos> out = new LinkedHashSet<>();
        Explosion fake = dummy(world, x, y, z);
        for (int i = sx * 2; i < ex * 2; i++) {
            for (int j = sy * 2; j < ey * 2; j++) {
                for (int k = sz * 2; k < ez * 2; k++) {
                    if (i != 0 && i != 31 && j != 0 && j != 31 && k != 0 && k != 31) continue;
                    double dx = i / 31.0F * 2.0F - 1.0F, dy = j / 31.0F * 2.0F - 1.0F, dz = k / 31.0F * 2.0F - 1.0F;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= len;
                    dy /= len;
                    dz /= len;
                    float f = power * (0.7F + world.rand.nextFloat() * 0.6F);
                    double px = x, py = y, pz = z;
                    for (float step = 0.3F; f > 0.0F; f -= step * 0.75F) {
                        BlockPos p = new BlockPos(MathHelper.floor(px), MathHelper.floor(py), MathHelper.floor(pz));
                        IBlockState s = world.getBlockState(p);
                        if (s.getMaterial() != Material.AIR) {
                            f -= (s.getBlock().getExplosionResistance(world, p, exploder, fake) + 0.3F) * step;
                        }
                        if ((f > 0.0F || forceInclude != null && forceInclude.test(s)) && !world.isOutsideBuildHeight(p)) {
                            out.add(p);
                        }
                        px += dx * step;
                        py += dy * step;
                        pz += dz * step;
                    }
                }
            }
        }
        return out;
    }

    /** 1.7 explosion damage + knockback for entities within {@code radius} of the centre (AABB pre-filter like 1.7). */
    public static void damageEntitiesAround(World world, @Nullable Entity exclude, DamageSource src, double x, double y, double z, float radius) {
        if (world.isRemote) return;
        AxisAlignedBB box = new AxisAlignedBB(MathHelper.floor(x - radius - 1), MathHelper.floor(y - radius - 1), MathHelper.floor(z - radius - 1),
                MathHelper.floor(x + radius + 1), MathHelper.floor(y + radius + 1), MathHelper.floor(z + radius + 1));
        damageEntities(world, world.getEntitiesWithinAABBExcludingEntity(exclude, box), src, x, y, z, radius);
    }

    /** 1.7 explosion damage formula {@code ((i*i+i)/2*8*radius+1)} with block density, applied to the given list. */
    public static void damageEntities(World world, List<? extends Entity> list, DamageSource src, double x, double y, double z, float radius) {
        if (world.isRemote) return;
        Vec3d centre = new Vec3d(x, y, z);
        for (Entity e : list) {
            double dist = e.getDistance(x, y, z) / radius;
            if (dist > 1.0) continue;
            double dx = e.posX - x, dy = e.posY - y, dz = e.posZ - z;
            double len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            if (len == 0.0) continue;
            dx /= len;
            dy /= len;
            dz /= len;
            double impact = (1.0 - dist) * world.getBlockDensity(centre, e.getEntityBoundingBox());
            e.attackEntityFrom(src, (float) (int) ((impact * impact + impact) / 2.0 * 8.0 * radius + 1.0));
            e.motionX += dx * impact;
            e.motionY += dy * impact;
            e.motionZ += dz * impact;
            e.velocityChanged = true;
        }
    }

    /** {@code dropBlockAsItemWithChance} + {@code onBlockExploded}, skipping air. Server only. */
    public static void blastBlock(World world, BlockPos pos, float dropChance, Explosion explosion) {
        if (world.isRemote) return;
        IBlockState s = world.getBlockState(pos);
        if (s.getMaterial() == Material.AIR) return;
        if (s.getBlock().canDropFromExplosion(explosion)) s.getBlock().dropBlockAsItemWithChance(world, pos, s, dropChance, 0);
        s.getBlock().onBlockExploded(world, pos, explosion);
    }

    /** 1.7 {@code doExplosionB} debris: one "explode" + one "smoke" particle flying out from the centre. */
    public static void debrisParticles(World world, BlockPos pos, double cx, double cy, double cz, float power) {
        double px = pos.getX() + world.rand.nextFloat(), py = pos.getY() + world.rand.nextFloat(), pz = pos.getZ() + world.rand.nextFloat();
        double dx = px - cx, dy = py - cy, dz = pz - cz;
        double len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0.0) len = 1.0;
        dx /= len;
        dy /= len;
        dz /= len;
        double v = 0.5 / (len / power + 0.1);
        v *= world.rand.nextFloat() * world.rand.nextFloat() + 0.3F;
        dx *= v;
        dy *= v;
        dz *= v;
        LegacyParticles.spawnBroadcast(world, "explode", (px + cx) / 2.0, (py + cy) / 2.0, (pz + cz) / 2.0, dx, dy, dz);
        LegacyParticles.spawnBroadcast(world, "smoke", px, py, pz, dx, dy, dz);
    }
}
