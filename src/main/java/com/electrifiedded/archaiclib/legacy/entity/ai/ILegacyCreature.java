package com.electrifiedded.archaiclib.legacy.entity.ai;

import net.minecraft.entity.Entity;

/**
 * The 1.7.10 "old AI" contract of {@code EntityCreature} (mobs whose {@code isAIEnabled()} was false):
 * {@code entityToAttack}, {@code findPlayerToAttack()} and {@code attackEntity(Entity, float)}.
 * Implement it on the ported entity and add {@link EntityAILegacyCreature} to its tasks.
 */
public interface ILegacyCreature {
    /** 1.7 {@code entityToAttack} getter (may be any entity, not only living ones). */
    Entity getEntityToAttack();

    void setEntityToAttack(Entity target);

    /** 1.7 {@code findPlayerToAttack()}; null = stay passive. Called while there is no target. */
    default Entity findPlayerToAttack() {
        return null;
    }

    /** 1.7 {@code attackEntity(Entity, float distance)}: called every tick the target is visible. */
    default void attackEntity(Entity target, float distance) {
    }

    /** 1.7 {@code attackBlockedEntity(Entity, float)}: target exists but cannot be seen. */
    default void attackBlockedEntity(Entity target, float distance) {
    }

    /** 1.7 {@code getBlockPathWeight}-free wander chance: 1 in N ticks when idle (1.7 default 80; 0 = never). */
    default int legacyWanderChance() {
        return 80;
    }
}
