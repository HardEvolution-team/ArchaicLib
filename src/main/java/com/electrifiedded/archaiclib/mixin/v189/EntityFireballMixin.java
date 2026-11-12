package com.electrifiedded.archaiclib.mixin.v189;

import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.8 fireball NBT: only "direction" (motion), no "power". 1.8 kept such fireballs alive with zero
 * acceleration; 1.12 kills them on load. Legacy summon/drop NBT (e.g. {direction:[0,-1,0],ExplosionPower:3})
 * therefore never flew or exploded. Add a zero "power" so the 1.8 behaviour is preserved.
 */
@Mixin(EntityFireball.class)
public abstract class EntityFireballMixin {
    @Inject(method = "readEntityFromNBT", at = @At("HEAD"))
    private void archaiclib$legacyNoPower(NBTTagCompound tag, CallbackInfo ci) {
        if (!tag.hasKey("power", 9) && tag.hasKey("direction", 9) && tag.getTagList("direction", 6).tagCount() == 3) {
            NBTTagList l = new NBTTagList();
            for (int i = 0; i < 3; i++) l.appendTag(new NBTTagDouble(0.0));
            tag.setTag("power", l);
        }
    }
}
