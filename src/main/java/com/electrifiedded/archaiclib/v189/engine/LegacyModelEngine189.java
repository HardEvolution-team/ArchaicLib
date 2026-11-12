package com.electrifiedded.archaiclib.v189.engine;

import com.electrifiedded.archaiclib.v189.LegacyItemTransforms189;
import com.electrifiedded.archaiclib.v189.LegacyVanillaAssets189;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Engine in engine": loads a model the way Minecraft 1.8.9 ModelBakery did (original 1.8 json,
 * 1.8 parent chain, builtin/generated -> 1.8 ItemModelGenerator, 1.8 FaceBakery) and keeps the
 * 1.8 ItemCameraTransforms. Quads are handed to 1.12 as ordinary BakedQuads (same ITEM vertex layout).
 */
@SideOnly(Side.CLIENT)
public final class LegacyModelEngine189 {

    private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger("ArchaicLib");
    private static final String GENERATED_JSON =
            "{\"elements\":[{  \"from\": [0, 0, 0],   \"to\": [16, 16, 16],   \"faces\": {       \"down\": {\"uv\": [0, 0, 16, 16], \"texture\":\"\"}   }}]}";
    static final ModelBlock MODEL_GENERATED = ModelBlock.deserialize(GENERATED_JSON);
    static final ModelBlock MODEL_ENTITY = ModelBlock.deserialize(GENERATED_JSON);

    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final ItemModelGenerator ITEM_GENERATOR = new ItemModelGenerator();

    private LegacyModelEngine189() {}

    /** A model baked by the 1.8.9 code. */
    public static final class Model implements IBakedModel {
        public final ItemCameraTransforms transforms;
        private final boolean gui3d, ao;
        private final TextureAtlasSprite particle;
        private final List<net.minecraft.client.renderer.block.model.BakedQuad> general;
        private final Map<EnumFacing, List<net.minecraft.client.renderer.block.model.BakedQuad>> faces;

        Model(ItemCameraTransforms transforms, boolean gui3d, boolean ao, TextureAtlasSprite particle,
              List<net.minecraft.client.renderer.block.model.BakedQuad> general,
              Map<EnumFacing, List<net.minecraft.client.renderer.block.model.BakedQuad>> faces) {
            this.transforms = transforms;
            this.gui3d = gui3d;
            this.ao = ao;
            this.particle = particle;
            this.general = general;
            this.faces = faces;
        }

        public int quadCount() {
            int n = general.size();
            for (List<?> l : faces.values()) n += l.size();
            return n;
        }

        @Override
        public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
            if (side == null) return general;
            List<net.minecraft.client.renderer.block.model.BakedQuad> l = faces.get(side);
            return l == null ? Collections.emptyList() : l;
        }

        @Override public boolean isAmbientOcclusion() { return ao; }
        @Override public boolean isGui3d() { return gui3d; }
        @Override public boolean isBuiltInRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleTexture() { return particle; }
        @Override public ItemOverrideList getOverrides() { return ItemOverrideList.NONE; }
    }

    /** Bakes the model at {@code location} (e.g. "ns:item/name") with the 1.8.9 pipeline; null = not possible. */
    public static Model bake(ResourceLocation location) {
        if (location.getPath().startsWith("models/")) {
            location = new ResourceLocation(location.getNamespace(), location.getPath().substring("models/".length()));
        }
        try {
            ModelBlock model = load(location, new HashSet<>());
            if (model == null) { LOG.warn("[ArchaicLib] 1.8 engine: cannot load {}", location); return null; }
            ModelBlock root = model.getRootModel();
            TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
            if (root == MODEL_GENERATED) {
                ModelBlock generated = ITEM_GENERATOR.makeItemModel(atlas, model);
                if (generated == null) return null;
                generated.name = model.name;
                model = generated;
            } else if (root == MODEL_ENTITY) {
                return null; // builtin/entity: tile entity item renderer, 1.12 path
            }
            Model baked = bakeModel(model, atlas);
            LOG.debug("[ArchaicLib] 1.8 engine baked {}: {} quads, gui3d={}", location, baked.quadCount(), baked.isGui3d());
            return baked;
        } catch (Throwable t) {
            LOG.warn("[ArchaicLib] 1.8 engine could not bake {}: {}", location, t.toString());
            return null;
        }
    }

    /** Port of 1.8.9 ModelBakery.bakeModel(model, ModelRotation.X0_Y0, false). */
    private static Model bakeModel(ModelBlock model, TextureMap atlas) {
        TextureAtlasSprite particle = sprite(atlas, model.resolveTextureName("particle"));
        List<net.minecraft.client.renderer.block.model.BakedQuad> general = new ArrayList<>();
        Map<EnumFacing, List<net.minecraft.client.renderer.block.model.BakedQuad>> faces = new EnumMap<>(EnumFacing.class);
        for (BlockPart part : model.getElements()) {
            for (Map.Entry<EnumFacing, BlockPartFace> e : part.mapFaces.entrySet()) {
                BlockPartFace face = e.getValue();
                TextureAtlasSprite sprite = sprite(atlas, model.resolveTextureName(face.texture));
                BakedQuad q = FACE_BAKERY.makeBakedQuad(part.positionFrom, part.positionTo, face, sprite, e.getKey(),
                        ModelRotation.X0_Y0, part.partRotation, false, part.shade);
                net.minecraft.client.renderer.block.model.BakedQuad out = new net.minecraft.client.renderer.block.model.BakedQuad(
                        withNormals(q.getVertexData(), q.getFace()), q.getTintIndex(), q.getFace(), sprite, true, DefaultVertexFormats.ITEM);
                if (face.cullFace == null) general.add(out);
                else faces.computeIfAbsent(ModelRotation.X0_Y0.rotateFace(face.cullFace), k -> new ArrayList<>()).add(out);
            }
        }
        return new Model(model.getAllTransforms(), model.isGui3d(), model.isAmbientOcclusion(), particle, general, faces);
    }

    /**
     * 1.8 FaceBakery left the normal slot empty and RenderItem.putQuadNormal supplied the face normal at draw
     * time; 1.12 reads it from the vertex data, so it is written here (same face normal as 1.8).
     */
    private static int[] withNormals(int[] data, EnumFacing face) {
        int[] out = data.clone();
        net.minecraft.util.math.Vec3i n = face.getDirectionVec();
        int packed = ((byte) (n.getX() * 127) & 0xFF) | (((byte) (n.getY() * 127) & 0xFF) << 8) | (((byte) (n.getZ() * 127) & 0xFF) << 16);
        int stride = out.length / 4;
        for (int v = 0; v < 4; v++) out[v * stride + 6] = packed;
        return out;
    }

    private static TextureAtlasSprite sprite(TextureMap atlas, String name) {
        if (name == null || name.startsWith("#")) return atlas.getMissingSprite();
        return atlas.getAtlasSprite(new ResourceLocation(name).toString());
    }

    /** Port of 1.8.9 ModelBakery.loadModel + parent resolution, reading the original 1.8 json when available. */
    private static ModelBlock load(ResourceLocation location, Set<ResourceLocation> seen) throws Exception {
        String path = location.getPath();
        if ("builtin/generated".equals(path)) return MODEL_GENERATED;
        if ("builtin/entity".equals(path) || "builtin/compass".equals(path) || "builtin/clock".equals(path)) {
            return "builtin/entity".equals(path) ? MODEL_ENTITY : MODEL_GENERATED;
        }
        if (!seen.add(location)) return null;
        JsonObject json = read(location);
        if (json == null && "minecraft".equals(location.getNamespace()) && LegacyVanillaAssets189.isRemovedModel(path)) {
            json = read(new ResourceLocation(LegacyVanillaAssets189.replacementModel(path)));
        }
        if (json == null) return null;
        JsonObject source;
        JsonElement marker = json.get(LegacyItemTransforms189.MARKER);
        if (marker != null && marker.isJsonObject() && marker.getAsJsonObject().has("raw")) {
            source = marker.getAsJsonObject().getAsJsonObject("raw").deepCopy();
            // keep the converter's texture fixes (namespaces, removed 1.8 textures)
            if (json.has("textures")) source.add("textures", json.get("textures"));
        } else {
            source = json.deepCopy();
            source.remove("display");
            JsonObject vanilla = "minecraft".equals(location.getNamespace()) ? LegacyItemTransforms189.vanillaDisplay(path) : null;
            if (vanilla != null) source.add("display", vanilla);
            if (source.has("parent")) {
                String p = source.get("parent").getAsString();
                // a 1.12-only vanilla parent: 1.8 used builtin/generated for these
                if (p.equals("item/generated") || p.equals("minecraft:item/generated")) source.addProperty("parent", "builtin/generated");
            }
        }
        source.remove(LegacyItemTransforms189.MARKER);
        ModelBlock model = ModelBlock.deserialize(source.toString());
        model.name = location.toString();
        ResourceLocation parent = model.getParentLocation();
        if (parent != null) {
            model.parent = load(parent, seen);
            if (model.parent == null) {
                LOG.warn("[ArchaicLib] 1.8 engine: missing parent {} of {}", parent, location);
                return null;
            }
        }
        return model;
    }

    private static JsonObject read(ResourceLocation location) {
        ResourceLocation file = new ResourceLocation(location.getNamespace(), "models/" + location.getPath() + ".json");
        try (IResource res = Minecraft.getMinecraft().getResourceManager().getResource(file)) {
            JsonElement e = new JsonParser().parse(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8));
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
