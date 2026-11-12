package com.electrifiedded.archaiclib.v189;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

/** Baked model from 1.8 resources: geometry from 1.12 baking, hand/GUI placement from the 1.8.9 pipeline. */
@SideOnly(Side.CLIENT)
public class LegacyBakedModel189 implements IBakedModel {
    private final IBakedModel parent;
    private final LegacyItemTransforms189.Resolved transforms;
    private final net.minecraft.util.ResourceLocation location;
    private final boolean useEngineQuads;
    private com.electrifiedded.archaiclib.v189.engine.LegacyModelEngine189.Model engine;
    private boolean engineTried;

    public LegacyBakedModel189(IBakedModel parent, LegacyItemTransforms189.Resolved transforms,
                               net.minecraft.util.ResourceLocation location, boolean useEngineQuads) {
        this.parent = parent;
        this.transforms = transforms;
        this.location = location;
        this.useEngineQuads = useEngineQuads;
    }

    /** The model baked by the ported 1.8.9 pipeline (lazy, needs the stitched atlas), or null. */
    public com.electrifiedded.archaiclib.v189.engine.LegacyModelEngine189.Model engine() {
        if (!engineTried) {
            engineTried = true;
            if (location != null) engine = com.electrifiedded.archaiclib.v189.engine.LegacyModelEngine189.bake(location);
        }
        return engine;
    }

    public IBakedModel getParent() {
        return parent;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (useEngineQuads && engine() != null) return engine.getQuads(state, side, rand);
        return parent.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return parent.isAmbientOcclusion();
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state) {
        return parent.isAmbientOcclusion(state);
    }

    @Override
    public boolean isGui3d() {
        return parent.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return parent.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return parent.getParticleTexture();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms() {
        return parent.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return parent.getOverrides();
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
        Pair<? extends IBakedModel, Matrix4f> base = parent.handlePerspective(type);
        Matrix4f legacy = engine() != null
                ? LegacyItemTransforms189.engineMatrix(engine, type)
                : LegacyItemTransforms189.matrix(transforms, type, parent.isGui3d());
        if (legacy == null) return base;
        IBakedModel model = engine != null ? engine
                : base == null || base.getLeft() == null ? parent : base.getLeft();
        return Pair.of(model, legacy);
    }
}
