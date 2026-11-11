package com.electrifiedded.archaiclib.v1710.api.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@FunctionalInterface
public interface IIconRegister {
    IIcon registerIcon(String name);
}
