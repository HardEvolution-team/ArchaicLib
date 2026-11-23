package com.electrifiedded.archaiclib.legacy.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;

/**
 * Re-creates 1.7.10 {@code EntityCreature.updateEntityActionState()} (the pre-task "old AI") as a 1.12 AI task:
 * acquire a target through {@link ILegacyCreature#findPlayerToAttack()}, path towards it, call
 * {@link ILegacyCreature#attackEntity(Entity, float)} while it is visible, and wander randomly when idle.
 * Add it with a low priority number to the tasks of an entity implementing {@link ILegacyCreature}.
 */
public class EntityAILegacyCreature extends EntityAIBase {
    private final EntityCreature creature;
    private final ILegacyCreature legacy;
    private final double speed;
    private int pathTimer;

    public EntityAILegacyCreature(EntityCreature creature, double speed) {
        if (!(creature instanceof ILegacyCreature)) {
            throw new IllegalArgumentException(creature.getClass() + " must implement ILegacyCreature");
        }
        this.creature = creature;
        this.legacy = (ILegacyCreature) creature;
        this.speed = speed;
        this.setMutexBits(3);
    }

    public EntityAILegacyCreature(EntityCreature creature) {
        this(creature, 1.0);
    }

    @Override
    public boolean shouldExecute() {
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return true;
    }

    @Override
    public void updateTask() {
        Entity target = this.legacy.getEntityToAttack();
        if (target == null) {
            target = this.legacy.findPlayerToAttack();
            if (target != null) {
                this.legacy.setEntityToAttack(target);
                this.pathTimer = 0;
            }
        } else if (target.isEntityAlive() && !(target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.disableDamage)) {
            float distance = target.getDistance(this.creature);
            if (this.creature.canEntityBeSeen(target)) {
                this.legacy.attackEntity(target, distance);
            } else {
                this.legacy.attackBlockedEntity(target, distance);
            }
        } else {
            this.legacy.setEntityToAttack(null);
            target = null;
        }

        if (target != null) {
            if (target instanceof EntityLivingBase && this.creature.getAttackTarget() != target) {
                this.creature.setAttackTarget((EntityLivingBase) target);
            }
            if (--this.pathTimer <= 0 || this.creature.getNavigator().noPath()) {
                this.pathTimer = 10 + this.creature.getRNG().nextInt(10);
                this.creature.getNavigator().tryMoveToEntityLiving(target, this.speed);
            }
            this.creature.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        } else {
            int chance = this.legacy.legacyWanderChance();
            if (chance > 0 && this.creature.getNavigator().noPath() && this.creature.getRNG().nextInt(chance) == 0) {
                double x = this.creature.posX + (this.creature.getRNG().nextInt(13) - 6);
                double y = this.creature.posY + (this.creature.getRNG().nextInt(7) - 3);
                double z = this.creature.posZ + (this.creature.getRNG().nextInt(13) - 6);
                this.creature.getNavigator().tryMoveToXYZ(x, MathHelper.floor(y), z, this.speed);
            }
        }
    }
}
