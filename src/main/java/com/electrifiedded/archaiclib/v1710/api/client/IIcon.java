package com.electrifiedded.archaiclib.v1710.api.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IIcon {
    int getIconWidth();

    int getIconHeight();

    float getMinU();

    float getMaxU();

    float getInterpolatedU(double u);

    float getMinV();

    float getMaxV();

    float getInterpolatedV(double v);

    String getIconName();
}
