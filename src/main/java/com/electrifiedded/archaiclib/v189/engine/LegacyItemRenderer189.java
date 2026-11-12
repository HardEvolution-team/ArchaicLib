package com.electrifiedded.archaiclib.v189.engine;

import com.electrifiedded.archaiclib.mixin.v189.RenderItemInvoker;
import com.electrifiedded.archaiclib.v189.LegacyBakedModel189;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Port of the Minecraft 1.8.9 item drawing code (ItemRenderer, RenderItem, LayerHeldItem) for models baked by
 * {@link LegacyModelEngine189}. 1.8 had no off hand: the left hand is drawn as an exact mirror of the right one.
 */
@SideOnly(Side.CLIENT)
public final class LegacyItemRenderer189 {

    /** true while drawing mirrored (left hand): cull faces are swapped. */
    private static boolean mirrored;

    private LegacyItemRenderer189() {}

    /** The 1.8 engine model for a stack, or null when the stack does not use a 1.8 model. */
    public static LegacyModelEngine189.Model modelFor(ItemStack stack, EntityLivingBase entity) {
        if (stack == null || stack.isEmpty()) return null;
        IBakedModel m = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack, entity == null ? null : entity.world, entity);
        return m instanceof LegacyBakedModel189 ? ((LegacyBakedModel189) m).engine() : null;
    }

    public static LegacyModelEngine189.Model modelFor(IBakedModel m) {
        return m instanceof LegacyBakedModel189 ? ((LegacyBakedModel189) m).engine() : null;
    }

    // ---------------------------------------------------------------- ItemRenderer (first person)

    /** 1.8.9 ItemRenderer.renderItemInFirstPerson, item part (the caller already did the camera/light setup). */
    public static void renderFirstPerson(AbstractClientPlayer player, float partialTicks, EnumHand hand, boolean leftSide,
                                         float swingProgress, ItemStack stack, float equipProgress, LegacyModelEngine189.Model model) {
        GlStateManager.pushMatrix();
        if (leftSide) beginMirror();
        float swing = hand == (player.swingingHand == null ? EnumHand.MAIN_HAND : player.swingingHand) ? swingProgress : 0F;
        if (player.isHandActive() && player.getActiveHand() == hand && player.getItemInUseCount() > 0) {
            EnumAction action = stack.getItemUseAction();
            switch (action) {
                case NONE:
                    transformFirstPersonItem(equipProgress, 0F);
                    break;
                case EAT:
                case DRINK:
                    performDrinking(player, partialTicks, stack);
                    transformFirstPersonItem(equipProgress, 0F);
                    break;
                case BLOCK:
                    transformFirstPersonItem(equipProgress, 0F);
                    doBlockTransformations();
                    break;
                case BOW:
                    transformFirstPersonItem(equipProgress, 0F);
                    doBowTransformations(player, partialTicks, stack);
                    break;
                default:
                    transformFirstPersonItem(equipProgress, 0F);
            }
        } else {
            doItemUsedTransformations(swing);
            transformFirstPersonItem(equipProgress, swing);
        }
        renderItem(player, stack, model, ItemCameraTransforms.TransformType.FIRST_PERSON);
        if (leftSide) endMirror();
        GlStateManager.popMatrix();
    }

    private static void doItemUsedTransformations(float swing) {
        float f = -0.4F * MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI);
        float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI * 2.0F);
        float f2 = -0.2F * MathHelper.sin(swing * (float) Math.PI);
        GlStateManager.translate(f, f1, f2);
    }

    private static void performDrinking(AbstractClientPlayer player, float partialTicks, ItemStack stack) {
        float f = player.getItemInUseCount() - partialTicks + 1.0F;
        float f1 = f / stack.getMaxItemUseDuration();
        float f2 = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
        if (f1 >= 0.8F) f2 = 0.0F;
        GlStateManager.translate(0.0F, f2, 0.0F);
        float f3 = 1.0F - (float) Math.pow(f1, 27.0);
        GlStateManager.translate(f3 * 0.6F, f3 * -0.5F, f3 * 0.0F);
        GlStateManager.rotate(f3 * 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f3 * 10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(f3 * 30.0F, 0.0F, 0.0F, 1.0F);
    }

    private static void transformFirstPersonItem(float equipProgress, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    private static void doBowTransformations(AbstractClientPlayer player, float partialTicks, ItemStack stack) {
        GlStateManager.rotate(-18.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-12.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-8.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-0.9F, 0.2F, 0.0F);
        float f = stack.getMaxItemUseDuration() - (player.getItemInUseCount() - partialTicks + 1.0F);
        float f1 = f / 20.0F;
        f1 = (f1 * f1 + f1 * 2.0F) / 3.0F;
        if (f1 > 1.0F) f1 = 1.0F;
        if (f1 > 0.1F) {
            float f2 = MathHelper.sin((f - 0.1F) * 1.3F);
            float f3 = f1 - 0.1F;
            float f4 = f2 * f3;
            GlStateManager.translate(f4 * 0.0F, f4 * 0.01F, f4 * 0.0F);
        }
        GlStateManager.translate(f1 * 0.0F, f1 * 0.0F, f1 * 0.1F);
        GlStateManager.scale(1.0F, 1.0F, 1.0F + f1 * 0.2F);
    }

    private static void doBlockTransformations() {
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
    }

    /** 1.8.9 ItemRenderer.renderItem(entity, stack, type). */
    public static void renderItem(EntityLivingBase entity, ItemStack stack, LegacyModelEngine189.Model model,
                                  ItemCameraTransforms.TransformType type) {
        Block block = Block.getBlockFromItem(stack.getItem());
        boolean translucent = block != null && block.getRenderLayer() == BlockRenderLayer.TRANSLUCENT;
        GlStateManager.pushMatrix();
        if (model.isGui3d()) {
            GlStateManager.scale(2.0F, 2.0F, 2.0F);
            if (translucent) GlStateManager.depthMask(false);
        }
        renderItemModelTransform(stack, model, type);
        if (translucent) GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }

    // ---------------------------------------------------------------- LayerHeldItem (third person)

    /** 1.8.9 LayerHeldItem.doRenderLayer after ModelBiped.postRenderArm (done by the caller for the right arm side). */
    public static void renderThirdPerson(EntityLivingBase entity, ItemStack stack, boolean leftSide, LegacyModelEngine189.Model model) {
        if (leftSide) beginMirror();
        GlStateManager.translate(-0.0625F, 0.4375F, 0.0625F);
        if (entity.isSneaking()) GlStateManager.translate(0.0F, 0.203125F, 0.0F);
        renderItem(entity, stack, model, ItemCameraTransforms.TransformType.THIRD_PERSON);
        if (leftSide) endMirror();
    }

    // ---------------------------------------------------------------- RenderItem

    /** 1.8.9 RenderItem.renderItemModelTransform. */
    public static void renderItemModelTransform(ItemStack stack, LegacyModelEngine189.Model model, ItemCameraTransforms.TransformType type) {
        TextureManager tm = Minecraft.getMinecraft().getTextureManager();
        tm.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        tm.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        if (!model.isGui3d()) GlStateManager.scale(2.0F, 2.0F, 2.0F); // preTransform
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        model.transforms.applyTransform(type);
        boolean negative = isThereOneNegativeScale(model.transforms.getTransform(type));
        cull(negative);
        renderItem(stack, model);
        cull(false);
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        tm.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        tm.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
    }

    private static boolean isThereOneNegativeScale(ItemTransformVec3f t) {
        return t.scale.x < 0.0F ^ t.scale.y < 0.0F ^ t.scale.z < 0.0F;
    }

    /** 1.8.9 RenderItem.renderItem(stack, model). */
    public static void renderItem(ItemStack stack, LegacyModelEngine189.Model model) {
        RenderItem ri = Minecraft.getMinecraft().getRenderItem();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        ((RenderItemInvoker) ri).archaiclib$renderModel(model, stack);
        if (stack.hasEffect()) ((RenderItemInvoker) ri).archaiclib$renderEffect(model);
        GlStateManager.popMatrix();
    }

    /** 1.8.9 RenderItem.renderItemIntoGUI (setupGuiTransform + GUI transform). */
    public static void renderItemIntoGUI(ItemStack stack, int x, int y, float zLevel, LegacyModelEngine189.Model model) {
        TextureManager tm = Minecraft.getMinecraft().getTextureManager();
        GlStateManager.pushMatrix();
        tm.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        tm.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.translate((float) x, (float) y, 100.0F + zLevel);
        GlStateManager.translate(8.0F, 8.0F, 0.0F);
        GlStateManager.scale(1.0F, 1.0F, -1.0F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        if (model.isGui3d()) {
            GlStateManager.scale(40.0F, 40.0F, 40.0F);
            GlStateManager.rotate(210.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.enableLighting();
        } else {
            GlStateManager.scale(64.0F, 64.0F, 64.0F);
            GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.disableLighting();
        }
        model.transforms.applyTransform(ItemCameraTransforms.TransformType.GUI);
        renderItem(stack, model);
        GlStateManager.disableAlpha();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        tm.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        tm.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
    }

    // ---------------------------------------------------------------- mirroring helpers

    private static void beginMirror() {
        GlStateManager.scale(-1.0F, 1.0F, 1.0F);
        mirrored = true;
        cull(false);
    }

    private static void endMirror() {
        mirrored = false;
        cull(false);
    }

    /** Cull back faces normally; front faces when exactly one of (mirror, negative scale) flips the winding. */
    private static void cull(boolean negativeScale) {
        GlStateManager.cullFace(negativeScale ^ mirrored ? GlStateManager.CullFace.FRONT : GlStateManager.CullFace.BACK);
    }
}
