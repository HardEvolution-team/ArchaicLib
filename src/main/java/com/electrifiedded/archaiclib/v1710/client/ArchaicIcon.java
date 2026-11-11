package com.electrifiedded.archaiclib.v1710.client;

import com.electrifiedded.archaiclib.v1710.api.client.IIcon;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public record ArchaicIcon(TextureAtlasSprite sprite) implements IIcon {
    @Override
    public int getIconWidth() {
        return sprite.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return sprite.getIconHeight();
    }

    @Override
    public float getMinU() {
        return sprite.getMinU();
    }

    @Override
    public float getMaxU() {
        return sprite.getMaxU();
    }

    @Override
    public float getInterpolatedU(double u) {
        return sprite.getInterpolatedU(u);
    }

    @Override
    public float getMinV() {
        return sprite.getMinV();
    }

    @Override
    public float getMaxV() {
        return sprite.getMaxV();
    }

    @Override
    public float getInterpolatedV(double v) {
        return sprite.getInterpolatedV(v);
    }

    @Override
    public String getIconName() {
        return sprite.getIconName();
    }
}
