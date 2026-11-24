package com.electrifiedded.archaiclib.legacy.registry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 1.7/1.8 entity string ids ("Zombie", "PigZombie", "LavaSlime", "Ozelot" ...) -> 1.12 registry names
 * ("minecraft:zombie", "minecraft:zombie_pigman" ...), e.g. for {@code MobSpawnerBaseLogic.setEntityName} in
 * replayed structures, DungeonHooks entries or spawn-egg NBT. Unknown names containing ':' are returned as is,
 * other unknown names are lower-cased into the minecraft domain.
 */
public final class LegacyEntityNames {
    private LegacyEntityNames() {}

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        String[] t = {
                "Item", "item", "XPOrb", "xp_orb", "LeashKnot", "leash_knot", "Painting", "painting", "Arrow", "arrow",
                "Snowball", "snowball", "Fireball", "fireball", "SmallFireball", "small_fireball", "ThrownEnderpearl", "ender_pearl",
                "EyeOfEnderSignal", "eye_of_ender_signal", "ThrownPotion", "potion", "ThrownExpBottle", "xp_bottle", "ItemFrame", "item_frame",
                "WitherSkull", "wither_skull", "PrimedTnt", "tnt", "FallingSand", "falling_block", "FireworksRocketEntity", "fireworks_rocket",
                "Boat", "boat", "MinecartRideable", "minecart", "MinecartChest", "chest_minecart", "MinecartFurnace", "furnace_minecart",
                "MinecartTNT", "tnt_minecart", "MinecartHopper", "hopper_minecart", "MinecartSpawner", "spawner_minecart",
                "MinecartCommandBlock", "commandblock_minecart", "Creeper", "creeper", "Skeleton", "skeleton", "Spider", "spider",
                "Giant", "giant", "Zombie", "zombie", "Slime", "slime", "Ghast", "ghast", "PigZombie", "zombie_pigman",
                "Enderman", "enderman", "CaveSpider", "cave_spider", "Silverfish", "silverfish", "Blaze", "blaze", "LavaSlime", "magma_cube",
                "EnderDragon", "ender_dragon", "WitherBoss", "wither", "Bat", "bat", "Witch", "witch", "Endermite", "endermite",
                "Guardian", "guardian", "Pig", "pig", "Sheep", "sheep", "Cow", "cow", "Chicken", "chicken", "Squid", "squid",
                "Wolf", "wolf", "MushroomCow", "mooshroom", "SnowMan", "snowman", "Ozelot", "ocelot", "VillagerGolem", "villager_golem",
                "EntityHorse", "horse", "Rabbit", "rabbit", "Villager", "villager", "EnderCrystal", "ender_crystal", "ArmorStand", "armor_stand"};
        for (int i = 0; i < t.length; i += 2) MAP.put(t[i], "minecraft:" + t[i + 1]);
    }

    /** "Zombie" -> "minecraft:zombie"; "mymod.Foo" -> "mymod:foo" (1.7 mod entity names are "modid.Name"). */
    public static String toModern(String legacy) {
        if (legacy == null) return null;
        String m = MAP.get(legacy);
        if (m != null) return m;
        if (legacy.indexOf(':') >= 0) return legacy;
        int dot = legacy.indexOf('.');
        if (dot > 0) return legacy.substring(0, dot).toLowerCase(Locale.ROOT) + ":" + legacy.substring(dot + 1).toLowerCase(Locale.ROOT);
        return "minecraft:" + legacy.toLowerCase(Locale.ROOT);
    }
}
