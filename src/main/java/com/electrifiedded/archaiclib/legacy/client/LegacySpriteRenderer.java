package com.electrifiedded.archaiclib.legacy.client;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * The common 1.7 projectile renderer: a camera-facing quad with a whole texture file
 * (glRotate(180 - playerViewY), glRotate(-playerViewX), tessellator quad of size 1 scaled by {@code scale}).
 */
public class LegacySpriteRenderer<T extends Entity> extends Render<T> {
    private final ResourceLocation texture;
    private final float scale;

    public LegacySpriteRenderer(RenderManager manager, ResourceLocation texture, float scale) {
        super(manager);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float yaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y, (float)z);
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(this.scale, this.scale, this.scale);
        this.bindEntityTexture(entity);
        GlStateManager.rotate(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((this.renderManager.options.thirdPersonView == 2 ? -1 : 1) * -this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
        b.pos(-0.5, -0.5, 0.0).tex(0, 1).normal(0.0F, 1.0F, 0.0F).endVertex();
        b.pos(0.5, -0.5, 0.0).tex(1, 1).normal(0.0F, 1.0F, 0.0F).endVertex();
        b.pos(0.5, 0.5, 0.0).tex(1, 0).normal(0.0F, 1.0F, 0.0F).endVertex();
        b.pos(-0.5, 0.5, 0.0).tex(0, 0).normal(0.0F, 1.0F, 0.0F).endVertex();
        t.draw();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return this.texture;
    }
}
