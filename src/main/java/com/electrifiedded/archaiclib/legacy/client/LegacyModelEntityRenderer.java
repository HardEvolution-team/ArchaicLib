package com.electrifiedded.archaiclib.legacy.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * The 1.7 boat-style renderer for non-living entities with a {@link ModelBase}: translate, rotate 180 - yaw,
 * scale(-1, -1, 1), model.render(..., 0.0625).
 */
public class LegacyModelEntityRenderer<T extends Entity> extends Render<T> {
    private final ModelBase model;
    private final ResourceLocation texture;

    public LegacyModelEntityRenderer(RenderManager manager, ModelBase model, ResourceLocation texture) {
        super(manager);
        this.model = model;
        this.texture = texture;
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float yaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y, (float)z);
        GlStateManager.rotate(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        this.bindEntityTexture(entity);
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        this.model.render(entity, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return this.texture;
    }
}
