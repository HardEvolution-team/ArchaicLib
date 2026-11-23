package com.electrifiedded.archaiclib.v1710.client.renderer;

import com.electrifiedded.archaiclib.v1710.api.client.ArchaicMinecraftForgeClient;
import com.electrifiedded.archaiclib.v1710.api.client.IItemRenderer;
import com.electrifiedded.archaiclib.v1710.api.client.IItemRenderer.ItemRenderType;
import com.electrifiedded.archaiclib.v1710.api.client.IItemRenderer.ItemRendererHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Runs 1.7.10 {@link IItemRenderer}s with the exact 1.7.10 (Forge 10.13) transforms. For GUI / ground / third
 * person the 1.12 transforms already applied by the caller are undone first, so the renderer sees the same GL
 * frame as in 1.7.10 (RenderItem / ForgeHooksClient / RenderPlayer.renderEquippedItems). First person is drawn by
 * {@link #renderFirstPerson} (called from ItemRendererMixin), a port of 1.7.10 ItemRenderer.renderItemInFirstPerson.
 */
@SideOnly(Side.CLIENT)
public class ArchaicTEISR extends TileEntityItemStackRenderer {

    public static final ArchaicTEISR INSTANCE = new ArchaicTEISR();

    static TransformType currentTransformType = TransformType.NONE;
    /** Entity of the current renderItemSide call (third person), passed to renderers as data[1] like 1.7. */
    public static EntityLivingBase currentEntity;

    /** The 1.7 renderer that handles {@code type} for this stack, or null (= 1.7 used the plain icon). */
    public static IItemRenderer rendererFor(ItemStack stack, ItemRenderType type) {
        if (stack == null || stack.isEmpty()) return null;
        IItemRenderer r = ArchaicMinecraftForgeClient.getRenderer(stack.getItem());
        return r != null && r.handleRenderType(stack, type) ? r : null;
    }

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        TransformType transform = currentTransformType;
        currentTransformType = TransformType.NONE;
        EntityLivingBase entity = currentEntity;
        currentEntity = null;

        ItemRenderType renderType = getRenderTypeFromTransform(transform);
        IItemRenderer renderer = rendererFor(stack, renderType);
        if (renderer == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        try {
            switch (transform) {
                case GUI -> renderInventory(renderer, stack);
                case GROUND -> renderEntity(renderer, stack);
                case FIXED -> {
                    GlStateManager.translate(0.5F, 0.5F, 0.5F);
                    GlStateManager.rotate(135, 0, 1, 0);
                    GlStateManager.rotate(180, 1, 0, 0);
                    GlStateManager.translate(-0.5F, -0.5F, -0.5F);
                    GlStateManager.translate(0, -0.2F, 0);
                    renderer.renderItem(ItemRenderType.ENTITY, stack, null, null);
                }
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                    // normally handled by renderFirstPerson; fallback for other callers
                    GlStateManager.translate(0.5F, 0.5F, 0.5F);
                    renderEquipped(ItemRenderType.EQUIPPED_FIRST_PERSON, renderer, stack, mc.player);
                }
                default -> renderThirdPerson(renderer, stack, entity, transform == TransformType.THIRD_PERSON_LEFT_HAND);
            }
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        }
    }

    // ------------------------------------------------------------------ GUI

    /** 1.12: T(x,y,100+z) T(8,8,0) S(1,-1,1) S(16) T(-.5) -> back to 1.7 T(x,y,0), then ForgeHooksClient.renderInventoryItem. */
    private static void renderInventory(IItemRenderer r, ItemStack stack) {
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        GlStateManager.scale(1F / 16F, 1F / 16F, 1F / 16F);
        GlStateManager.scale(1.0F, -1.0F, 1.0F);
        GlStateManager.translate(-8.0F, -8.0F, -100.0F);
        if (r.shouldUseRenderHelper(ItemRenderType.INVENTORY, stack, ItemRendererHelper.INVENTORY_BLOCK)) {
            GlStateManager.enableLighting();
            GlStateManager.translate(-2.0F, 3.0F, -3.0F);
            GlStateManager.scale(10F, 10F, 10F);
            GlStateManager.translate(1.0F, 0.5F, 1.0F);
            GlStateManager.scale(1.0F, 1.0F, -1F);
            GlStateManager.rotate(210F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(45F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-90F, 0.0F, 1.0F, 0.0F);
        } else {
            GlStateManager.disableLighting();
            GlStateManager.translate(0.0F, 0.0F, -3.0F);
        }
        r.renderItem(ItemRenderType.INVENTORY, stack, null);
        GlStateManager.enableLighting();
    }

    // ------------------------------------------------------------------ ground (EntityItem)

    /** 1.12: T(x, y+bob+0.25, z) R(spin) T(-.5) -> 1.7 T(x, y+bob, z) R(spin), then ForgeHooksClient.renderEntityItem. */
    private static void renderEntity(IItemRenderer r, ItemStack stack) {
        GlStateManager.translate(0.5F, 0.25F, 0.5F);
        boolean is3D = r.shouldUseRenderHelper(ItemRenderType.ENTITY, stack, ItemRendererHelper.BLOCK_3D);
        if (is3D || stack.getItem() instanceof ItemBlock) {
            float scale = stack.getItem() instanceof ItemBlock ? 0.25F : 0.5F;
            GlStateManager.scale(scale, scale, scale);
        } else {
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
        }
        r.renderItem(ItemRenderType.ENTITY, stack, null, null);
    }

    // ------------------------------------------------------------------ third person

    /**
     * 1.12 LayerHeldItem: arm.postRender, Rx(-90) Ry(180) T(+-1/16, .125, -.625), T(-.5) -> back to arm.postRender,
     * then 1.7.10 RenderPlayer/RenderBiped.renderEquippedItems (left hand = mirror of the right one).
     */
    private static void renderThirdPerson(IItemRenderer r, ItemStack stack, EntityLivingBase entity, boolean left) {
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        GlStateManager.translate((left ? 1 : -1) / 16.0F, -0.125F, 0.625F);
        GlStateManager.rotate(-180.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
        if (left) beginMirror();
        try {
            GlStateManager.translate(-0.0625F, 0.4375F, 0.0625F);
            boolean blocking = entity != null && entity.isHandActive() && entity.getItemInUseCount() > 0
                    && stack.getItemUseAction() == EnumAction.BLOCK;
            if (r.shouldUseRenderHelper(ItemRenderType.EQUIPPED, stack, ItemRendererHelper.BLOCK_3D)
                    || stack.getItem() instanceof ItemBlock) {
                float f = 0.5F * 0.75F;
                GlStateManager.translate(0.0F, 0.1875F, -0.3125F);
                GlStateManager.rotate(20.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.scale(-f, -f, f);
            } else if (stack.getItem().isFull3D()) {
                float f = 0.625F;
                if (stack.getItem().shouldRotateAroundWhenRendering()) {
                    GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager.translate(0.0F, -0.125F, 0.0F);
                }
                if (blocking) {
                    GlStateManager.translate(0.05F, 0.0F, -0.1F);
                    GlStateManager.rotate(-50.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(-10.0F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(-60.0F, 0.0F, 0.0F, 1.0F);
                }
                GlStateManager.translate(0.0F, 0.1875F, 0.0F);
                GlStateManager.scale(f, -f, f);
                GlStateManager.rotate(-100.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
            } else {
                float f = 0.375F;
                GlStateManager.translate(0.25F, 0.1875F, -0.1875F);
                GlStateManager.scale(f, f, f);
                GlStateManager.rotate(60.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(20.0F, 0.0F, 0.0F, 1.0F);
            }
            renderEquipped(ItemRenderType.EQUIPPED, r, stack, entity);
        } finally {
            if (left) endMirror();
        }
    }

    // ------------------------------------------------------------------ first person

    /** Port of the item part of 1.7.10 ItemRenderer.renderItemInFirstPerson (left hand = mirror). */
    public static void renderFirstPerson(AbstractClientPlayer player, EnumHand hand, boolean left, float swingProgress,
                                         ItemStack stack, float equipProgress, IItemRenderer r) {
        GlStateManager.pushMatrix();
        if (left) beginMirror();
        try {
            boolean inUse = player.isHandActive() && player.getActiveHand() == hand && player.getItemInUseCount() > 0;
            EnumAction action = inUse ? stack.getItemUseAction() : EnumAction.NONE;
            float swing = inUse ? 0F : swingProgress;
            if (!inUse) {
                float s1 = MathHelper.sin(swing * (float) Math.PI);
                float s2 = MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI);
                GlStateManager.translate(-s2 * 0.4F, MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI * 2.0F) * 0.2F, -s1 * 0.2F);
            } else if (action == EnumAction.EAT || action == EnumAction.DRINK) {
                float f6 = player.getItemInUseCount() - Minecraft.getMinecraft().getRenderPartialTicks() + 1.0F;
                float f7 = 1.0F - f6 / stack.getMaxItemUseDuration();
                float f8 = 1.0F - f7;
                f8 = f8 * f8 * f8;
                f8 = f8 * f8 * f8;
                f8 = f8 * f8 * f8;
                float f9 = 1.0F - f8;
                GlStateManager.translate(0.0F, MathHelper.abs(MathHelper.cos(f6 / 4.0F * (float) Math.PI) * 0.1F) * (f7 > 0.2F ? 1 : 0), 0.0F);
                GlStateManager.translate(f9 * 0.6F, -f9 * 0.5F, 0.0F);
                GlStateManager.rotate(f9 * 90.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(f9 * 10.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(f9 * 30.0F, 0.0F, 0.0F, 1.0F);
            }
            float f13 = 0.8F;
            GlStateManager.translate(0.7F * f13, -0.65F * f13 - equipProgress * 0.6F, -0.9F * f13);
            GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.enableRescaleNormal();
            float a = MathHelper.sin(swing * swing * (float) Math.PI);
            float b = MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI);
            GlStateManager.rotate(-a * 20.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-b * 20.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(-b * 80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(0.4F, 0.4F, 0.4F);
            if (action == EnumAction.BLOCK) {
                GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
            }
            if (stack.getItem().shouldRotateAroundWhenRendering()) GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            renderEquipped(ItemRenderType.EQUIPPED_FIRST_PERSON, r, stack, player);
        } finally {
            if (left) endMirror();
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        }
    }

    // ------------------------------------------------------------------ shared

    /** 1.7.10 ForgeHooksClient.renderEquippedItem. */
    private static void renderEquipped(ItemRenderType type, IItemRenderer r, ItemStack stack, EntityLivingBase entity) {
        GlStateManager.pushMatrix();
        if (r.shouldUseRenderHelper(type, stack, ItemRendererHelper.EQUIPPED_BLOCK)) {
            GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        } else {
            GlStateManager.enableRescaleNormal();
            GlStateManager.translate(0.0F, -0.3F, 0.0F);
            GlStateManager.scale(1.5F, 1.5F, 1.5F);
            GlStateManager.rotate(50.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(335.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(-0.9375F, -0.0625F, 0.0F);
        }
        r.renderItem(type, stack, null, entity);
        GlStateManager.popMatrix();
    }

    private static void beginMirror() {
        GlStateManager.scale(-1.0F, 1.0F, 1.0F);
        GlStateManager.cullFace(GlStateManager.CullFace.FRONT);
    }

    private static void endMirror() {
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
    }

    private static ItemRenderType getRenderTypeFromTransform(TransformType transform) {
        return switch (transform) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> ItemRenderType.EQUIPPED_FIRST_PERSON;
            case GUI -> ItemRenderType.INVENTORY;
            case GROUND, FIXED -> ItemRenderType.ENTITY;
            default -> ItemRenderType.EQUIPPED;
        };
    }
}
