package com.electrifiedded.archaiclib.legacy.world;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 1.7 {@code world.spawnParticle("largesmoke", ...)} string names mapped to 1.12 {@link EnumParticleTypes}.
 * {@link #spawn} behaves like the 1.7 call (client-only effect, no-op on a server world);
 * {@link #spawnBroadcast} sends the particles to nearby clients when called on the server, which is what
 * most 1.7 server-side entity code intended (on 1.7 single player it "worked" because both sides ran it).
 */
public final class LegacyParticles {
    private static final Map<String, EnumParticleTypes> LEGACY = new HashMap<>();

    static {
        for (EnumParticleTypes t : EnumParticleTypes.values()) LEGACY.put(t.getParticleName(), t);
        LEGACY.put("blockcrack", EnumParticleTypes.BLOCK_CRACK);
        LEGACY.put("iconcrack", EnumParticleTypes.ITEM_CRACK);
        LEGACY.put("blockdust", EnumParticleTypes.BLOCK_DUST);
    }

    private LegacyParticles() {}

    /** Name lookup; also accepts 1.7 "iconcrack_256_0" / "blockcrack_1_0" style prefixes. Null when unknown. */
    public static EnumParticleTypes get(String name) {
        EnumParticleTypes t = LEGACY.get(name);
        if (t == null && name != null && name.indexOf('_') > 0) t = LEGACY.get(name.substring(0, name.indexOf('_')));
        return t;
    }

    public static void spawn(World world, String name, double x, double y, double z, double vx, double vy, double vz) {
        EnumParticleTypes t = get(name);
        if (t != null) world.spawnParticle(t, x, y, z, vx, vy, vz);
    }

    /** Client: plain spawn. Server: one packet with speed 0 so velocity args act as in 1.7 (single particle). */
    public static void spawnBroadcast(World world, String name, double x, double y, double z, double vx, double vy, double vz) {
        EnumParticleTypes t = get(name);
        if (t == null) return;
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(t, x, y, z, 0, vx, vy, vz, 1.0);
        } else {
            world.spawnParticle(t, x, y, z, vx, vy, vz);
        }
    }
}
