package com.electrifiedded.archaiclib.v1710.client.renderer;

import com.electrifiedded.archaiclib.v1710.api.client.ArchaicMinecraftForgeClient;
import com.electrifiedded.archaiclib.v1710.api.client.IItemRenderer;
import com.electrifiedded.archaiclib.v1710.api.client.IItemRenderer.ItemRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ArchaicTEISR extends TileEntityItemStackRenderer {

    public static final ArchaicTEISR INSTANCE = new ArchaicTEISR();

    static TransformType currentTransformType = TransformType.NONE;

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        TransformType transform = currentTransformType;
        currentTransformType = TransformType.NONE;

        IItemRenderer renderer = ArchaicMinecraftForgeClient.getRenderer(stack.getItem());
        if (renderer == null) return;

        ItemRenderType renderType = getRenderTypeFromTransform(transform);
        if (!renderer.handleRenderType(stack, renderType)) return;

        Minecraft mc = Minecraft.getMinecraft();
        Object entity = renderType == ItemRenderType.EQUIPPED_FIRST_PERSON ? mc.player : null;

        GlStateManager.pushMatrix();
        try {
            if (transform == TransformType.FIXED) {
                GlStateManager.translate(0.5F, 0.5F, 0.5F);
                GlStateManager.rotate(135, 0, 1, 0);
                GlStateManager.rotate(180, 1, 0, 0);
                GlStateManager.translate(-0.5F, -0.5F, -0.5F);
                GlStateManager.translate(0, -0.2F, 0);
            } else {
                setup1710Transforms(transform, renderType);
            }
            renderer.renderItem(renderType, stack, null, entity);
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        }
    }

    private static void setup1710Transforms(TransformType transform, ItemRenderType renderType) {
        boolean isLeft = transform == TransformType.FIRST_PERSON_LEFT_HAND;
        switch (renderType) {
            case INVENTORY -> {
                GlStateManager.translate(0.5F, 0.5F, 0.5F);
                GlStateManager.scale(0.625F, 0.625F, 0.625F);
                // 1.7.10 RenderItem: rotate(210,X) rotate(45,Y) rotate(-90,Y) on a y-down GUI = Rx(30) Ry(-45) here
                GlStateManager.rotate(30.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(-45.0F, 0.0F, 1.0F, 0.0F);
            }
            case EQUIPPED_FIRST_PERSON -> {
                float sideMultiplier = isLeft ? -1.0F : 1.0F;
                GlStateManager.translate(0.35F * sideMultiplier, -0.05F, 0.15F);
                GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                if (isLeft) {
                    GlStateManager.translate(0.5F, 0.5F, 0.5F);
                    GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.translate(-0.2F, -0.5F, -0.4F);
                }
                GlStateManager.scale(0.6F, 0.6F, 0.6F);
            }
            case EQUIPPED -> {
                GlStateManager.translate(0.5F, 0.5F, 0.5F);
                GlStateManager.translate(0.0F, 0.15625F, 0.0F);
                GlStateManager.scale(0.375F, 0.375F, 0.375F);
                GlStateManager.translate(0F, 0, 0.2F);
                GlStateManager.rotate(-45.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.translate(-0.5F, -0.5F, -0.5F);
            }
            case ENTITY -> {
                GlStateManager.translate(0.5F, 0.5F, 0.5F);
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
            }
            default -> {
            }
        }
    }

    private static ItemRenderType getRenderTypeFromTransform(TransformType transform) {
        return switch (transform) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> ItemRenderType.EQUIPPED_FIRST_PERSON;
            case GUI -> ItemRenderType.INVENTORY;
            case GROUND -> ItemRenderType.ENTITY;
            default -> ItemRenderType.EQUIPPED;
        };
    }
}
