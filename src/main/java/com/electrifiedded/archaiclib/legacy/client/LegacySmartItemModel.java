package com.electrifiedded.archaiclib.legacy.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
public abstract class LegacySmartItemModel implements IBakedModel {
    public enum LegacyTransformType { NONE, THIRD_PERSON, FIRST_PERSON, HEAD, GUI, GROUND, FIXED }

    public static final IBakedModel EMPTY = new IBakedModel() {
        @Override public List<BakedQuad> getQuads(@Nullable IBlockState s, @Nullable EnumFacing f, long r) { return Collections.emptyList(); }
        @Override public boolean isAmbientOcclusion() { return false; }
        @Override public boolean isGui3d() { return false; }
        @Override public boolean isBuiltInRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleTexture() {
            return net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        }
        @SuppressWarnings("deprecation")
        @Override public ItemCameraTransforms getItemCameraTransforms() { return ItemCameraTransforms.DEFAULT; }
        @Override public ItemOverrideList getOverrides() { return ItemOverrideList.NONE; }
    };

    protected final IBakedModel baseModel;
    @Nullable protected EntityLivingBase holder;

    private final ItemOverrideList overrides = new ItemOverrideList(Collections.emptyList()) {
        @Override
        public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
            holder = entity;
            return LegacySmartItemModel.this.handleItemState(stack, world, entity);
        }
    };

    protected LegacySmartItemModel(IBakedModel baseModel) { this.baseModel = baseModel; }

    public static LegacyTransformType legacyType(TransformType t) {
        switch (t) {
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND: return LegacyTransformType.THIRD_PERSON;
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND: return LegacyTransformType.FIRST_PERSON;
            case HEAD: return LegacyTransformType.HEAD;
            case GUI: return LegacyTransformType.GUI;
            case GROUND: return LegacyTransformType.GROUND;
            case FIXED: return LegacyTransformType.FIXED;
            default: return LegacyTransformType.NONE;
        }
    }

    public static void applyLegacyHandTransform(TransformType t) { applyLegacyHandTransform(t, false); }

    public static void applyLegacyHandTransform(TransformType t, boolean gui3d) {
        switch (t) {
            case FIRST_PERSON_RIGHT_HAND:
            case FIRST_PERSON_LEFT_HAND:
                GlStateManager.rotate(t == TransformType.FIRST_PERSON_RIGHT_HAND ? 45.0F : -45.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.scale(0.4F, 0.4F, 0.4F);
                if (!gui3d) GlStateManager.scale(2.0F, 2.0F, 2.0F);
                break;
            case THIRD_PERSON_RIGHT_HAND:
            case THIRD_PERSON_LEFT_HAND: {
                float side = t == TransformType.THIRD_PERSON_RIGHT_HAND ? 1.0F : -1.0F;
                GlStateManager.translate(-side / 16.0F, -0.125F, 0.625F);
                GlStateManager.rotate(-180.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.translate(-0.0625F, 0.4375F, 0.0625F);
                if (!gui3d) GlStateManager.scale(2.0F, 2.0F, 2.0F);
                break;
            }
            default:
                break;
        }
    }

    public IBakedModel handleItemState(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) { return this; }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
        return baseModel.handlePerspective(type);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return baseModel == null ? Collections.emptyList() : baseModel.getQuads(state, side, rand);
    }

    @Override public boolean isAmbientOcclusion() { return baseModel.isAmbientOcclusion(); }
    @Override public boolean isGui3d() { return baseModel.isGui3d(); }
    @Override public boolean isBuiltInRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleTexture() { return baseModel.getParticleTexture(); }
    @SuppressWarnings("deprecation")
    @Override public ItemCameraTransforms getItemCameraTransforms() { return baseModel.getItemCameraTransforms(); }
    @Override public ItemOverrideList getOverrides() { return overrides; }
}
