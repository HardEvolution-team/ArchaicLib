package com.electrifiedded.archaiclib.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** ModelRenderer with its own uniform scale applied around its rotation point (children inherit it). */
@SideOnly(Side.CLIENT)
public class ScalableModelRenderer extends ModelRenderer {
    public float scaleX = 1.0F, scaleY = 1.0F, scaleZ = 1.0F;

    public ScalableModelRenderer(ModelBase model) {
        super(model);
    }

    public ScalableModelRenderer(ModelBase model, int u, int v) {
        super(model, u, v);
    }

    public ScalableModelRenderer setScale(float s) {
        scaleX = scaleY = scaleZ = s;
        return this;
    }

    @Override
    public void render(float scale) {
        if (isHidden || !showModel)
            return;
        if (scaleX == 1.0F && scaleY == 1.0F && scaleZ == 1.0F) {
            super.render(scale);
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
        GlStateManager.scale(scaleX, scaleY, scaleZ);
        GlStateManager.translate(-rotationPointX * scale, -rotationPointY * scale, -rotationPointZ * scale);
        super.render(scale);
        GlStateManager.popMatrix();
    }
}
