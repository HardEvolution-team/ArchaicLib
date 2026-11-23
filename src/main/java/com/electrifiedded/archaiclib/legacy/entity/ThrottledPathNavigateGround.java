package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.world.World;

/**
 * Ground navigator for legacy mobs whose AI calls {@code tryMoveToEntityLiving} every tick (common in 1.7 mods:
 * a full A* search per tick per mob, very expensive for big bosses). Reuses the previous answer for up to
 * {@code repathTicks} ticks while the target stays within ~2 blocks of where it was - the way vanilla melee AI repaths.
 * Use from {@code createNavigator(World)}.
 */
public class ThrottledPathNavigateGround extends PathNavigateGround {
    private final int repathTicks;
    private Entity lastTarget;
    private long lastTime;
    private boolean lastFound;
    private double lastX, lastY, lastZ;

    public ThrottledPathNavigateGround(EntityLiving entity, World world) {
        this(entity, world, 10);
    }

    public ThrottledPathNavigateGround(EntityLiving entity, World world, int repathTicks) {
        super(entity, world);
        this.repathTicks = repathTicks;
    }

    @Override
    public boolean tryMoveToEntityLiving(Entity target, double speed) {
        long now = this.world.getTotalWorldTime();
        if (target == this.lastTarget && now - this.lastTime < this.repathTicks && now >= this.lastTime
                && target.getDistanceSq(this.lastX, this.lastY, this.lastZ) < 4.0) {
            this.setSpeed(speed);
            return this.lastFound && !this.noPath();
        }
        boolean found = super.tryMoveToEntityLiving(target, speed);
        this.lastFound = found;
        this.lastTarget = target;
        this.lastTime = now;
        this.lastX = target.posX;
        this.lastY = target.posY;
        this.lastZ = target.posZ;
        return found;
    }
}
