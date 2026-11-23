package com.electrifiedded.archaiclib.v1710.client.renderer;

import com.electrifiedded.archaiclib.Reference;
import com.electrifiedded.archaiclib.v1710.api.client.ArchaicMinecraftForgeClient;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

/**
 * Wraps a normal (flat) item model: GUI / ground / fixed use the base model, hand perspectives switch to the
 * built-in TEISR path so the item's 1.7 {@code IItemRenderer} draws EQUIPPED / EQUIPPED_FIRST_PERSON.
 * Registered through {@link ArchaicMinecraftForgeClient#registerHeldItemRenderer}.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Reference.MOD_ID)
public final class LegacyHeldItemModel implements IBakedModel {
    private final IBakedModel base;

    public LegacyHeldItemModel(IBakedModel base) {
        this.base = base;
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        for (ModelResourceLocation mrl : ArchaicMinecraftForgeClient.HELD_MODELS) {
            IBakedModel m = event.getModelRegistry().getObject(mrl);
            if (m != null && !(m instanceof LegacyHeldItemModel)) {
                event.getModelRegistry().putObject(mrl, new LegacyHeldItemModel(m));
            }
        }
    }

    private static boolean held(TransformType t) {
        return t == TransformType.FIRST_PERSON_LEFT_HAND || t == TransformType.FIRST_PERSON_RIGHT_HAND
                || t == TransformType.THIRD_PERSON_LEFT_HAND || t == TransformType.THIRD_PERSON_RIGHT_HAND;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
        if (held(type)) {
            return ArchaicDummyBakedModel.INSTANCE.handlePerspective(type);
        }
        return base.handlePerspective(type);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return base.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return base.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return base.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return base.getParticleTexture();
    }

    @Override
    @SuppressWarnings("deprecation")
    public net.minecraft.client.renderer.block.model.ItemCameraTransforms getItemCameraTransforms() {
        return base.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return base.getOverrides();
    }
}
