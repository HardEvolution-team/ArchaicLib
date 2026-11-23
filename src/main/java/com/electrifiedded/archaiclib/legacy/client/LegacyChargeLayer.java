package com.electrifiedded.archaiclib.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Predicate;

/**
 * 1.7 "charged creeper" render pass (shouldRenderPass pass 1 with a scrolling texture matrix + additive blend)
 * as a 1.12 layer. Works for any model / texture / condition.
 */
@SideOnly(Side.CLIENT)
public class LegacyChargeLayer<T extends EntityLivingBase> implements LayerRenderer<T> {
    public static final ResourceLocation CREEPER_ARMOR = new ResourceLocation("textures/entity/creeper/creeper_armor.png");

    private final RenderLivingBase<T> renderer;
    private final ModelBase model;
    private final ResourceLocation texture;
    private final Predicate<T> active;
    private final float speed;

    public LegacyChargeLayer(RenderLivingBase<T> renderer, ModelBase model, ResourceLocation texture, Predicate<T> active) {
        this(renderer, model, texture, active, 0.01F);
    }

    public LegacyChargeLayer(RenderLivingBase<T> renderer, ModelBase model, ResourceLocation texture, Predicate<T> active, float speed) {
        this.renderer = renderer;
        this.model = model;
        this.texture = texture;
        this.active = active;
        this.speed = speed;
    }

    @Override
    public void doRenderLayer(T e, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                              float netHeadYaw, float headPitch, float scale) {
        if (!active.test(e)) return;
        boolean invisible = e.isInvisible();
        GlStateManager.depthMask(!invisible);
        renderer.bindTexture(texture);
        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        float f = (e.ticksExisted + partialTicks) * speed;
        GlStateManager.translate(f, f, 0.0F);
        GlStateManager.matrixMode(5888);
        GlStateManager.enableBlend();
        GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        model.setModelAttributes(renderer.getMainModel());
        model.setLivingAnimations(e, limbSwing, limbSwingAmount, partialTicks);
        Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
        model.render(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(5888);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(invisible);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
