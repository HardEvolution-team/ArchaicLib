package com.electrifiedded.archaiclib.legacy.sound;

import net.minecraft.init.SoundEvents;

/**
 * Extra 1.7 sound names for {@link LegacySounds#get} (iron golem, step.*, liquid.*, note.*, misc).
 * Call {@link #register()} once (preInit) before using the names; repeated calls are harmless.
 * @since 1.31.0
 */
public final class LegacySoundNames17 {
    private static boolean done;

    private LegacySoundNames17() {}

    public static synchronized void register() {
        if (done) return;
        done = true;
        LegacySounds.map("mob.irongolem.throw", SoundEvents.ENTITY_IRONGOLEM_ATTACK);
        LegacySounds.map("mob.irongolem.walk", SoundEvents.ENTITY_IRONGOLEM_STEP);
        LegacySounds.map("mob.irongolem.hit", SoundEvents.ENTITY_IRONGOLEM_HURT);
        LegacySounds.map("mob.irongolem.death", SoundEvents.ENTITY_IRONGOLEM_DEATH);
        LegacySounds.map("step.stone", SoundEvents.BLOCK_STONE_STEP);
        LegacySounds.map("step.wood", SoundEvents.BLOCK_WOOD_STEP);
        LegacySounds.map("step.gravel", SoundEvents.BLOCK_GRAVEL_STEP);
        LegacySounds.map("step.grass", SoundEvents.BLOCK_GRASS_STEP);
        LegacySounds.map("step.sand", SoundEvents.BLOCK_SAND_STEP);
        LegacySounds.map("step.snow", SoundEvents.BLOCK_SNOW_STEP);
        LegacySounds.map("step.cloth", SoundEvents.BLOCK_CLOTH_STEP);
        LegacySounds.map("step.ladder", SoundEvents.BLOCK_LADDER_STEP);
        LegacySounds.map("liquid.splash", SoundEvents.ENTITY_GENERIC_SPLASH);
        LegacySounds.map("liquid.swim", SoundEvents.ENTITY_GENERIC_SWIM);
        LegacySounds.map("liquid.water", SoundEvents.BLOCK_WATER_AMBIENT);
        LegacySounds.map("liquid.lava", SoundEvents.BLOCK_LAVA_AMBIENT);
        LegacySounds.map("liquid.lavapop", SoundEvents.BLOCK_LAVA_POP);
        LegacySounds.map("note.bass", SoundEvents.BLOCK_NOTE_BASS);
        LegacySounds.map("note.bassattack", SoundEvents.BLOCK_NOTE_BASS);
        LegacySounds.map("note.bd", SoundEvents.BLOCK_NOTE_BASEDRUM);
        LegacySounds.map("note.harp", SoundEvents.BLOCK_NOTE_HARP);
        LegacySounds.map("note.hat", SoundEvents.BLOCK_NOTE_HAT);
        LegacySounds.map("note.pling", SoundEvents.BLOCK_NOTE_PLING);
        LegacySounds.map("note.snare", SoundEvents.BLOCK_NOTE_SNARE);
        LegacySounds.map("random.pop", SoundEvents.ENTITY_ITEM_PICKUP);
        LegacySounds.map("random.orb", SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
        LegacySounds.map("random.anvil_use", SoundEvents.BLOCK_ANVIL_USE);
        LegacySounds.map("random.anvil_break", SoundEvents.BLOCK_ANVIL_BREAK);
        LegacySounds.map("random.anvil_land", SoundEvents.BLOCK_ANVIL_LAND);
        LegacySounds.map("random.door_open", SoundEvents.BLOCK_WOODEN_DOOR_OPEN);
        LegacySounds.map("random.door_close", SoundEvents.BLOCK_WOODEN_DOOR_CLOSE);
        LegacySounds.map("random.click", SoundEvents.UI_BUTTON_CLICK);
        LegacySounds.map("random.fizz", SoundEvents.BLOCK_FIRE_EXTINGUISH);
        LegacySounds.map("random.splash", SoundEvents.ENTITY_BOBBER_SPLASH);
        LegacySounds.map("random.levelup", SoundEvents.ENTITY_PLAYER_LEVELUP);
    }
}
