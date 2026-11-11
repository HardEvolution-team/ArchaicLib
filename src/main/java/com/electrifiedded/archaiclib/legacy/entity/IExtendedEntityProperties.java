package com.electrifiedded.archaiclib.legacy.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public interface IExtendedEntityProperties {
    void saveNBTData(NBTTagCompound compound);

    void loadNBTData(NBTTagCompound compound);

    void init(Entity entity, World world);
}
