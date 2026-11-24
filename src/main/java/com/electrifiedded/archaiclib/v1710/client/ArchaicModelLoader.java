package com.electrifiedded.archaiclib.v1710.client;

import com.electrifiedded.archaiclib.v1710.api.block.IArchaicRenderBlock;
import com.electrifiedded.archaiclib.v1710.api.client.ArchaicMinecraftForgeClient;
import com.electrifiedded.archaiclib.v1710.api.item.ArchaicItem;
import com.electrifiedded.archaiclib.v1710.client.renderer.ArchaicBakedModel;
import com.electrifiedded.archaiclib.v1710.client.renderer.ArchaicDummyBakedModel;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Function;

@SideOnly(Side.CLIENT)
public class ArchaicModelLoader implements ICustomModelLoader {

    private static final ResourceLocation ITEM_GENERATED = new ResourceLocation("item/generated");
    private static final String ITEM_PREFIX = "models/item/";

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        ResourceLocation itemName = itemName(modelLocation);
        if (itemName != null) {
            return ArchaicItem.TEXTURES.containsKey(itemName)
                    || ArchaicMinecraftForgeClient.hasCustomRenderer(itemName)
                    || Block.REGISTRY.getObject(itemName) instanceof IArchaicRenderBlock;
        }
        return blockFor(modelLocation) != null;
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        ResourceLocation itemName = itemName(modelLocation);
        if (itemName != null) {
            if (ArchaicMinecraftForgeClient.hasCustomRenderer(itemName)) {
                return (_, _, _) -> ArchaicDummyBakedModel.INSTANCE;
            }
            if (Block.REGISTRY.getObject(itemName) instanceof IArchaicRenderBlock block) {
                return new ArchaicBlockModel(block);
            }
            ResourceLocation texture = ArchaicItem.TEXTURES.get(itemName);
            if (texture != null) {
                java.util.List<ResourceLocation> extra = ArchaicItem.EXTRA_LAYERS.get(itemName);
                if (extra == null) {
                    return ModelLoaderRegistry.getModel(ITEM_GENERATED)
                            .retexture(ImmutableMap.of("layer0", texture.toString()));
                }
                ImmutableMap.Builder<String, String> layers = ImmutableMap.builder();
                layers.put("layer0", texture.toString());
                for (int i = 0; i < extra.size(); i++) layers.put("layer" + (i + 1), extra.get(i).toString());
                return ModelLoaderRegistry.getModel(ITEM_GENERATED).retexture(layers.build());
            }
        }
        IArchaicRenderBlock block = blockFor(modelLocation);
        if (block == null) {
            throw new IllegalStateException("ArchaicModelLoader can't load " + modelLocation);
        }
        return new ArchaicBlockModel(block);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
    }

    private static ResourceLocation itemName(ResourceLocation loc) {
        if (loc instanceof ModelResourceLocation mrl) {
            return "inventory".equals(mrl.getVariant())
                    ? new ResourceLocation(mrl.getNamespace(), mrl.getPath())
                    : null;
        }
        String path = loc.getPath();
        return path.startsWith(ITEM_PREFIX)
                ? new ResourceLocation(loc.getNamespace(), path.substring(ITEM_PREFIX.length()))
                : null;
    }

    private static IArchaicRenderBlock blockFor(ResourceLocation loc) {
        if (!(loc instanceof ModelResourceLocation)) return null;
        Block block = Block.REGISTRY.getObject(new ResourceLocation(loc.getNamespace(), loc.getPath()));
        return block instanceof IArchaicRenderBlock archaic ? archaic : null;
    }

    public static final java.util.Map<IArchaicRenderBlock, IBakedModel> BAKED = new java.util.concurrent.ConcurrentHashMap<>();

    private record ArchaicBlockModel(IArchaicRenderBlock block) implements IModel {
        @Override
        public IBakedModel bake(
                IModelState state,
                VertexFormat format,
                Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
            return BAKED.computeIfAbsent(block, b -> new ArchaicBakedModel(b, format, bakedTextureGetter));
        }
    }
}
