package com.electrifiedded.archaiclib.legacy.sound;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

public final class LegacySounds {
    private static final Map<String, SoundEvent> LEGACY = new HashMap<>();

    static {
        LEGACY.put("random.explode", SoundEvents.ENTITY_GENERIC_EXPLODE);
        LEGACY.put("game.tnt.primed", SoundEvents.ENTITY_TNT_PRIMED);
        LEGACY.put("random.fuse", SoundEvents.ENTITY_TNT_PRIMED);
        LEGACY.put("creeper.primed", SoundEvents.ENTITY_CREEPER_PRIMED);
        LEGACY.put("random.bow", SoundEvents.ENTITY_ARROW_SHOOT);
        LEGACY.put("random.bowhit", SoundEvents.ENTITY_ARROW_HIT);
        LEGACY.put("random.break", SoundEvents.ENTITY_ITEM_BREAK);
        LEGACY.put("random.click", SoundEvents.UI_BUTTON_CLICK);
        LEGACY.put("random.pop", SoundEvents.ENTITY_ITEM_PICKUP);
        LEGACY.put("random.orb", SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
        LEGACY.put("random.levelup", SoundEvents.ENTITY_PLAYER_LEVELUP);
        LEGACY.put("random.fizz", SoundEvents.BLOCK_FIRE_EXTINGUISH);
        LEGACY.put("random.door_open", SoundEvents.BLOCK_WOODEN_DOOR_OPEN);
        LEGACY.put("random.door_close", SoundEvents.BLOCK_WOODEN_DOOR_CLOSE);
        LEGACY.put("random.chestopen", SoundEvents.BLOCK_CHEST_OPEN);
        LEGACY.put("random.chestclosed", SoundEvents.BLOCK_CHEST_CLOSE);
        LEGACY.put("random.anvil_land", SoundEvents.BLOCK_ANVIL_LAND);
        LEGACY.put("random.anvil_use", SoundEvents.BLOCK_ANVIL_USE);
        LEGACY.put("random.anvil_break", SoundEvents.BLOCK_ANVIL_BREAK);
        LEGACY.put("random.burp", SoundEvents.ENTITY_PLAYER_BURP);
        LEGACY.put("random.eat", SoundEvents.ENTITY_GENERIC_EAT);
        LEGACY.put("random.drink", SoundEvents.ENTITY_GENERIC_DRINK);
        LEGACY.put("random.splash", SoundEvents.ENTITY_GENERIC_SPLASH);
        LEGACY.put("random.swim", SoundEvents.ENTITY_GENERIC_SWIM);
        LEGACY.put("random.hurt", SoundEvents.ENTITY_PLAYER_HURT);
        LEGACY.put("random.successful_hit", SoundEvents.ENTITY_ARROW_HIT_PLAYER);
        LEGACY.put("fire.ignite", SoundEvents.ITEM_FLINTANDSTEEL_USE);
        LEGACY.put("fire.fire", SoundEvents.BLOCK_FIRE_AMBIENT);
        LEGACY.put("ambient.weather.thunder", SoundEvents.ENTITY_LIGHTNING_THUNDER);
        LEGACY.put("ambient.weather.rain", SoundEvents.WEATHER_RAIN);
        LEGACY.put("mob.ghast.scream", SoundEvents.ENTITY_GHAST_SCREAM);
        LEGACY.put("mob.ghast.fireball", SoundEvents.ENTITY_GHAST_SHOOT);
        LEGACY.put("mob.ghast.charge", SoundEvents.ENTITY_GHAST_WARN);
        LEGACY.put("mob.endermen.portal", SoundEvents.ENTITY_ENDERMEN_TELEPORT);
        LEGACY.put("mob.wither.spawn", SoundEvents.ENTITY_WITHER_SPAWN);
        LEGACY.put("mob.wither.shoot", SoundEvents.ENTITY_WITHER_SHOOT);
        LEGACY.put("mob.enderdragon.growl", SoundEvents.ENTITY_ENDERDRAGON_GROWL);
        LEGACY.put("liquid.lava", SoundEvents.BLOCK_LAVA_AMBIENT);
        LEGACY.put("liquid.lavapop", SoundEvents.BLOCK_LAVA_POP);
        LEGACY.put("liquid.water", SoundEvents.BLOCK_WATER_AMBIENT);
        LEGACY.put("dig.glass", SoundEvents.BLOCK_GLASS_BREAK);
        LEGACY.put("dig.stone", SoundEvents.BLOCK_STONE_BREAK);
        LEGACY.put("dig.wood", SoundEvents.BLOCK_WOOD_BREAK);
        LEGACY.put("dig.grass", SoundEvents.BLOCK_GRASS_BREAK);
        LEGACY.put("dig.gravel", SoundEvents.BLOCK_GRAVEL_BREAK);
        LEGACY.put("dig.sand", SoundEvents.BLOCK_SAND_BREAK);
        LEGACY.put("dig.snow", SoundEvents.BLOCK_SNOW_BREAK);
        LEGACY.put("dig.cloth", SoundEvents.BLOCK_CLOTH_BREAK);
        LEGACY.put("portal.portal", SoundEvents.BLOCK_PORTAL_AMBIENT);
        LEGACY.put("portal.trigger", SoundEvents.BLOCK_PORTAL_TRIGGER);
        LEGACY.put("portal.travel", SoundEvents.BLOCK_PORTAL_TRAVEL);

        // 1.7 short/old names (1.10.x 1.11.0)
        LEGACY.put("mob.cow", SoundEvents.ENTITY_COW_AMBIENT);
        LEGACY.put("mob.cowhurt", SoundEvents.ENTITY_COW_HURT);
        LEGACY.put("mob.cow.step", SoundEvents.ENTITY_COW_STEP);
        LEGACY.put("mob.pig", SoundEvents.ENTITY_PIG_AMBIENT);
        LEGACY.put("mob.pigdeath", SoundEvents.ENTITY_PIG_DEATH);
        LEGACY.put("mob.pig.step", SoundEvents.ENTITY_PIG_STEP);
        LEGACY.put("mob.chicken.step", SoundEvents.ENTITY_CHICKEN_STEP);
        LEGACY.put("mob.chicken.plop", SoundEvents.ENTITY_CHICKEN_EGG);
        LEGACY.put("mob.zombie.step", SoundEvents.ENTITY_ZOMBIE_STEP);
        LEGACY.put("ambient.cave.cave", SoundEvents.AMBIENT_CAVE);
        LEGACY.put("mob.wither.break_block", SoundEvents.ENTITY_WITHER_BREAK_BLOCK);
        LEGACY.put("mob.zombie.say", SoundEvents.ENTITY_ZOMBIE_AMBIENT);
        LEGACY.put("mob.zombie.hurt", SoundEvents.ENTITY_ZOMBIE_HURT);
        LEGACY.put("mob.zombie.death", SoundEvents.ENTITY_ZOMBIE_DEATH);
        LEGACY.put("mob.zombie", SoundEvents.ENTITY_ZOMBIE_AMBIENT);
        LEGACY.put("mob.zombiehurt", SoundEvents.ENTITY_ZOMBIE_HURT);
        LEGACY.put("mob.zombiedeath", SoundEvents.ENTITY_ZOMBIE_DEATH);
        LEGACY.put("mob.skeleton.say", SoundEvents.ENTITY_SKELETON_AMBIENT);
        LEGACY.put("mob.skeleton.hurt", SoundEvents.ENTITY_SKELETON_HURT);
        LEGACY.put("mob.skeleton.death", SoundEvents.ENTITY_SKELETON_DEATH);
        LEGACY.put("mob.spider.say", SoundEvents.ENTITY_SPIDER_AMBIENT);
        LEGACY.put("mob.spider.death", SoundEvents.ENTITY_SPIDER_DEATH);
        LEGACY.put("mob.creeper.say", SoundEvents.ENTITY_CREEPER_HURT);
        LEGACY.put("mob.creeper.death", SoundEvents.ENTITY_CREEPER_DEATH);
        LEGACY.put("mob.endermen.idle", SoundEvents.ENTITY_ENDERMEN_AMBIENT);
        LEGACY.put("mob.endermen.hit", SoundEvents.ENTITY_ENDERMEN_HURT);
        LEGACY.put("mob.endermen.death", SoundEvents.ENTITY_ENDERMEN_DEATH);
        LEGACY.put("mob.endermen.scream", SoundEvents.ENTITY_ENDERMEN_SCREAM);
        LEGACY.put("mob.endermen.stare", SoundEvents.ENTITY_ENDERMEN_STARE);
        LEGACY.put("mob.ghast.moan", SoundEvents.ENTITY_GHAST_AMBIENT);
        LEGACY.put("mob.ghast.death", SoundEvents.ENTITY_GHAST_DEATH);
        LEGACY.put("mob.cow.say", SoundEvents.ENTITY_COW_AMBIENT);
        LEGACY.put("mob.cow.hurt", SoundEvents.ENTITY_COW_HURT);
        LEGACY.put("mob.pig.say", SoundEvents.ENTITY_PIG_AMBIENT);
        LEGACY.put("mob.pig.death", SoundEvents.ENTITY_PIG_DEATH);
        LEGACY.put("mob.sheep.say", SoundEvents.ENTITY_SHEEP_AMBIENT);
        LEGACY.put("mob.chicken.say", SoundEvents.ENTITY_CHICKEN_AMBIENT);
        LEGACY.put("mob.chicken.hurt", SoundEvents.ENTITY_CHICKEN_HURT);
        LEGACY.put("mob.wolf.bark", SoundEvents.ENTITY_WOLF_AMBIENT);
        LEGACY.put("mob.wolf.growl", SoundEvents.ENTITY_WOLF_GROWL);
        LEGACY.put("mob.wolf.hurt", SoundEvents.ENTITY_WOLF_HURT);
        LEGACY.put("mob.wolf.death", SoundEvents.ENTITY_WOLF_DEATH);
        LEGACY.put("mob.villager.idle", SoundEvents.ENTITY_VILLAGER_AMBIENT);
        LEGACY.put("mob.villager.hit", SoundEvents.ENTITY_VILLAGER_HURT);
        LEGACY.put("mob.villager.death", SoundEvents.ENTITY_VILLAGER_DEATH);
        LEGACY.put("mob.sheep.shear", SoundEvents.ENTITY_SHEEP_SHEAR);
        LEGACY.put("mob.villager.haggle", SoundEvents.ENTITY_VILLAGER_TRADING);
        LEGACY.put("mob.villager.no", SoundEvents.ENTITY_VILLAGER_NO);
        LEGACY.put("mob.villager.yes", SoundEvents.ENTITY_VILLAGER_YES);
        LEGACY.put("mob.irongolem.hit", SoundEvents.ENTITY_IRONGOLEM_HURT);
        LEGACY.put("mob.irongolem.death", SoundEvents.ENTITY_IRONGOLEM_DEATH);
        LEGACY.put("mob.zombiepig.zpig", SoundEvents.ENTITY_ZOMBIE_PIG_AMBIENT);
        LEGACY.put("mob.zombiepig.zpighurt", SoundEvents.ENTITY_ZOMBIE_PIG_HURT);
        LEGACY.put("mob.zombiepig.zpigdeath", SoundEvents.ENTITY_ZOMBIE_PIG_DEATH);
        LEGACY.put("mob.silverfish.say", SoundEvents.ENTITY_SILVERFISH_AMBIENT);
        LEGACY.put("mob.silverfish.hit", SoundEvents.ENTITY_SILVERFISH_HURT);
        LEGACY.put("mob.silverfish.kill", SoundEvents.ENTITY_SILVERFISH_DEATH);
        LEGACY.put("mob.blaze.breathe", SoundEvents.ENTITY_BLAZE_AMBIENT);
        LEGACY.put("mob.blaze.hit", SoundEvents.ENTITY_BLAZE_HURT);
        LEGACY.put("mob.blaze.death", SoundEvents.ENTITY_BLAZE_DEATH);
        LEGACY.put("mob.bat.idle", SoundEvents.ENTITY_BAT_AMBIENT);
        LEGACY.put("mob.bat.hurt", SoundEvents.ENTITY_BAT_HURT);
        LEGACY.put("mob.bat.death", SoundEvents.ENTITY_BAT_DEATH);
        LEGACY.put("mob.slime.big", SoundEvents.ENTITY_SLIME_SQUISH);
        LEGACY.put("mob.slime.small", SoundEvents.ENTITY_SMALL_SLIME_SQUISH);
        LEGACY.put("mob.cat.meow", SoundEvents.ENTITY_CAT_AMBIENT);
        LEGACY.put("mob.cat.hitt", SoundEvents.ENTITY_CAT_HURT);
        LEGACY.put("mob.horse.idle", SoundEvents.ENTITY_HORSE_AMBIENT);
        LEGACY.put("mob.horse.hit", SoundEvents.ENTITY_HORSE_HURT);
        LEGACY.put("mob.horse.death", SoundEvents.ENTITY_HORSE_DEATH);
        LEGACY.put("mob.wither.idle", SoundEvents.ENTITY_WITHER_AMBIENT);
        LEGACY.put("mob.wither.hurt", SoundEvents.ENTITY_WITHER_HURT);
        LEGACY.put("mob.wither.death", SoundEvents.ENTITY_WITHER_DEATH);
        LEGACY.put("mob.spider.step", SoundEvents.ENTITY_SPIDER_STEP);
        LEGACY.put("mob.wolf.step", SoundEvents.ENTITY_WOLF_STEP);
    }

    private LegacySounds() {}

    public static void map(String legacyName, SoundEvent event) {
        LEGACY.put(legacyName, event);
    }

    public static SoundEvent get(String name) {
        SoundEvent e = LEGACY.get(name);
        if (e != null) return e;
        return SoundEvent.REGISTRY.getObject(new ResourceLocation(name));
    }

    public static void playSoundEffect(World world, double x, double y, double z, String name, float volume, float pitch) {
        SoundEvent e = get(name);
        if (e != null) world.playSound(null, x, y, z, e, SoundCategory.BLOCKS, volume, pitch);
    }

    public static void playSoundAtEntity(World world, Entity entity, String name, float volume, float pitch) {
        SoundEvent e = get(name);
        if (e != null) world.playSound(null, entity.posX, entity.posY, entity.posZ, e,
                entity instanceof EntityPlayer ? SoundCategory.PLAYERS : SoundCategory.NEUTRAL, volume, pitch);
    }

    public static void playSoundLocal(World world, double x, double y, double z, String name, float volume, float pitch, boolean distanceDelay) {
        SoundEvent e = get(name);
        if (e != null) world.playSound(x, y, z, e, SoundCategory.BLOCKS, volume, pitch, distanceDelay);
    }
}
