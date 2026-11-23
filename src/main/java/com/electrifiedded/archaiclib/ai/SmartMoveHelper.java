package com.electrifiedded.archaiclib.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Move helper that copes with steps and corners better than vanilla's:
 * <ul>
 *     <li>vanilla only jumps when the target is higher than {@code stepHeight}; a mob with stepHeight 1 then
 *     never jumps, and when the step-up fails (low ceiling over its own cell, a fence, a slab edge) it presses
 *     against the block forever. This one jumps whenever it is blocked sideways while the target is higher,
 *     or while something knee-high with room above is in the way;</li>
 *     <li>no spinning on the spot: a target right under/over the mob (horizontally closer than 0.15) stops
 *     the turning instead of chasing an angle that flips every tick;</li>
 *     <li>{@link #blockedTicks()} counts ticks of being pushed against a wall, for unstick logic.</li>
 * </ul>
 * Install it in the mob's constructor: {@code moveHelper = new SmartMoveHelper(this);}.
 */
public class SmartMoveHelper extends EntityMoveHelper {

    private int blocked;
    /** Absolute AI move speed cap, or 0 = relative cap only (see {@link #setMaxAiSpeed}). */
    private float maxAiSpeed = 0F;
    /** Boosts above the BASE movement attribute (potions, modifiers, modded effects) allowed while walking. */
    private float boostAllowance = 1.25F;

    public SmartMoveHelper(EntityLiving entity) {
        super(entity);
    }

    /**
     * Caps the walking speed: with speed potions, attribute boosts or modded effects a mob otherwise runs
     * past every waypoint, turns back and dithers. By default the cap is {@code speed x BASE attribute x 1.25};
     * this sets an extra absolute cap ({@code <= 0}: none).
     */
    public SmartMoveHelper setMaxAiSpeed(float max) {
        this.maxAiSpeed = max;
        return this;
    }

    /** Ticks in a row the mob tried to walk but was blocked horizontally. */
    public int blockedTicks() {
        return blocked;
    }

    @Override
    public void onUpdateMoveHelper() {
        if (action == Action.MOVE_TO) {
            double dx = posX - entity.posX, dz = posZ - entity.posZ, dy = posY - entity.posY;
            if (dx * dx + dz * dz < 0.15 * 0.15 && Math.abs(dy) < 1.2) {
                // already there in plan view: stop instead of spinning round the target
                action = Action.WAIT;
                entity.setMoveForward(0.0F);
                blocked = 0;
                return;
            }
            super.onUpdateMoveHelper();
            limitSpeed(Math.sqrt(dx * dx + dz * dz));
            if (entity.isInWater() && !entity.isInLava()) {
                // in water a walking mob barely moves and a current pins it to a wall: paddle like a player
                double len = Math.sqrt(dx * dx + dz * dz);
                double push = 0.03 * Math.min(1.0, speed * 2);
                entity.motionX += dx / len * push;
                entity.motionZ += dz / len * push;
                if (entity.collidedHorizontally) {
                    blocked++;
                    entity.motionY = Math.max(entity.motionY, 0.3); // climb out over the edge
                }
                return;
            }
            if (entity.collidedHorizontally && !entity.isInLava()) {
                blocked++;
                if (entity.onGround && (dy > 0.4 || kneeHighAhead(dx, dz))) entity.getJumpHelper().setJumping();
            } else {
                blocked = 0;
            }
            return;
        }
        blocked = 0;
        super.onUpdateMoveHelper();
    }

    /** Speed cap plus braking near the target so a fast mob stops on it instead of overshooting. */
    private void limitSpeed(double dist) {
        net.minecraft.entity.ai.attributes.IAttributeInstance a = entity.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED);
        float limit = (float) (speed * a.getBaseValue() * boostAllowance);
        if (maxAiSpeed > 0) limit = Math.min(limit, maxAiSpeed);
        float ai = entity.getAIMoveSpeed();
        // walking velocity settles at about 2.2 x ai^2 blocks/tick: at most half the remaining distance per
        // tick (a normal walker is never slowed further than 0.4 blocks out)
        float brake = (float) Math.max(0.06, Math.sqrt(dist * 0.23));
        float cap = Math.min(limit, brake);
        if (ai > cap) {
            entity.setAIMoveSpeed(cap);
            entity.setMoveForward(cap);
        }
        if (entity.onGround) {
            double v = Math.sqrt(entity.motionX * entity.motionX + entity.motionZ * entity.motionZ);
            double vmax = Math.max(0.05, Math.min(2.4 * limit * limit, dist * 0.6));
            if (v > vmax) {
                entity.motionX *= vmax / v;
                entity.motionZ *= vmax / v;
            }
        }
    }

    /** Something up to 1.2 blocks high right in front of the mob, with room above it to jump onto. */
    private boolean kneeHighAhead(double dx, double dz) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-4) return false;
        AxisAlignedBB box = entity.getEntityBoundingBox().offset(dx / len * 0.4, 0, dz / len * 0.4);
        if (entity.world.getCollisionBoxes(entity, box).isEmpty()) return false;
        return entity.world.getCollisionBoxes(entity, box.offset(0, 1.25, 0)).isEmpty()
                && entity.world.getCollisionBoxes(entity, entity.getEntityBoundingBox().offset(0, 1.25, 0)).isEmpty();
    }
}
