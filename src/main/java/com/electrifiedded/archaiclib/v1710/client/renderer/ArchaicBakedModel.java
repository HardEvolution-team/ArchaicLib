package com.electrifiedded.archaiclib.v1710.client.renderer;

import com.electrifiedded.archaiclib.ExampleMod;
import com.electrifiedded.archaiclib.v1710.api.block.ArchaicRenderPart;
import com.electrifiedded.archaiclib.v1710.api.block.IArchaicRenderBlock;
import com.electrifiedded.archaiclib.v1710.api.client.IIcon;
import com.electrifiedded.archaiclib.v1710.client.ArchaicIcon;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import javax.vecmath.Vector4f;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector3f;

import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@SideOnly(Side.CLIENT)
public class ArchaicBakedModel implements IBakedModel {
    private static final ResourceLocation BLOCK_PARENT = new ResourceLocation("minecraft", "block/block");
    private static final ResourceLocation ITEM_GENERATED = new ResourceLocation("minecraft", "item/generated");
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final EnumFacing[] SIDES = {EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};

    private final IArchaicRenderBlock block;
    private final Function<ResourceLocation, TextureAtlasSprite> textureGetter;
    private final Map<Long, Quads> worldQuads = new ConcurrentHashMap<>();
    private final IBakedModel[] itemModels = new IBakedModel[16];
    private @Nullable IBakedModel blockTransforms;
    private final ItemOverrideList overrides = new ItemOverrideList(Collections.emptyList()) {
        @Override
        public IBakedModel handleItemState(
                IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
            IBakedModel model = getItemModel(stack.getMetadata());
            return model != null ? model : originalModel;
        }
    };

    private static final class Quads {
        final List<BakedQuad> general = new ArrayList<>();
        final Map<EnumFacing, List<BakedQuad>> culled = new EnumMap<>(EnumFacing.class);
        TextureAtlasSprite particle;
        boolean ao = true;
    }

    public ArchaicBakedModel(
            IArchaicRenderBlock block, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        this.block = block;
        this.textureGetter = textureGetter;
        try {
            IModel parent = ModelLoaderRegistry.getModel(BLOCK_PARENT);
            blockTransforms = parent.bake(TRSRTransformation.identity(), format, textureGetter);
        } catch (Exception e) {
            ExampleMod.LOGGER.error("Failed to load {} for {}", BLOCK_PARENT, name(), e);
        }

        IModel generated = null;
        for (int meta = 0; meta < 16; meta++) {
            int key = block.getArchaicItemRenderKey(meta);
            int type = safeParts(key) != null ? IArchaicRenderBlock.RENDER_STANDARD : safeType(key);
            if (type == IArchaicRenderBlock.RENDER_STANDARD) {
                Quads q = build(key, EnumFacing.UP);
                if (q != null) itemModels[meta] = new ItemCube(q);
                continue;
            }
            IIcon icon = safeIcon(2, key);
            if (icon == null) continue;
            try {
                if (generated == null) generated = ModelLoaderRegistry.getModel(ITEM_GENERATED);
                itemModels[meta] = generated.retexture(ImmutableMap.of("layer0", icon.getIconName()))
                        .bake(TRSRTransformation.identity(), format, textureGetter);
            } catch (Exception e) {
                ExampleMod.LOGGER.error("Failed to bake item model for {} meta {}", name(), meta, e);
            }
        }
    }

    private Object name() {
        return ((Block)block).getRegistryName();
    }

    private @Nullable IIcon safeIcon(int side, int key) {
        try {
            return block.getIcon(side, key);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private int safeType(int key) {
        try {
            return block.getArchaicRenderType(key);
        } catch (RuntimeException e) {
            return IArchaicRenderBlock.RENDER_STANDARD;
        }
    }

    private @Nullable List<ArchaicRenderPart> safeParts(int key) {
        try {
            return block.getArchaicRenderParts(key);
        } catch (RuntimeException e) {
            ExampleMod.LOGGER.error("getArchaicRenderParts failed for {} key {}", name(), key, e);
            return null;
        }
    }

    public static final int PART_COLOR_TINT = 0x1000000;

    private void buildParts(Quads q, List<ArchaicRenderPart> parts, ModelRotation rot, int blockTint) {
        TRSRTransformation tr = TRSRTransformation.blockCenterToCorner(new TRSRTransformation(rot));
        for (ArchaicRenderPart part : parts) {
            int tint = part.color != 0xFFFFFF ? PART_COLOR_TINT | part.color : blockTint;
            if (part.isBox) {
                double[] c = part.coords;
                float x1 = (float)c[0] * 16, y1 = (float)c[1] * 16, z1 = (float)c[2] * 16;
                float x2 = (float)c[3] * 16, y2 = (float)c[4] * 16, z2 = (float)c[5] * 16;
                Vector3f from = new Vector3f(x1, y1, z1), to = new Vector3f(x2, y2, z2);
                for (int side = 0; side < 6; side++) {
                    TextureAtlasSprite s = sprite(part.icons[side]);
                    if (s == null) continue;
                    if (q.particle == null) q.particle = s;
                    EnumFacing f = SIDES[side];
                    float[] uv = switch (f) {
                        case DOWN, UP -> new float[]{x1, z1, x2, z2};
                        case NORTH, SOUTH -> new float[]{x1, 16 - y2, x2, 16 - y1};
                        default -> new float[]{z1, 16 - y2, z2, 16 - y1};
                    };
                    for (int i = 0; i < 4; i++) uv[i] = Math.max(0, Math.min(16, uv[i]));
                    boolean touches = part.cull && switch (f) {
                        case DOWN -> y1 <= 0;
                        case UP -> y2 >= 16;
                        case NORTH -> z1 <= 0;
                        case SOUTH -> z2 >= 16;
                        case WEST -> x1 <= 0;
                        case EAST -> x2 >= 16;
                    };
                    EnumFacing cull = touches ? rot.rotate(f) : null;
                    BakedQuad quad = BAKERY.makeBakedQuad(from, to, new BlockPartFace(cull, tint, "", new BlockFaceUV(uv, 0)), s, f, rot, null, false, part.shade);
                    if (cull == null) q.general.add(quad);
                    else q.culled.computeIfAbsent(cull, _ -> new ArrayList<>()).add(quad);
                }
            } else {
                TextureAtlasSprite s = sprite(part.icons[0]);
                if (s == null) continue;
                if (q.particle == null) q.particle = s;
                float[][] pos = new float[4][];
                for (int i = 0; i < 4; i++) {
                    Vector4f v = new Vector4f((float)part.coords[i * 3], (float)part.coords[i * 3 + 1], (float)part.coords[i * 3 + 2], 1.0F);
                    tr.transformPosition(v);
                    pos[i] = new float[]{v.x, v.y, v.z};
                }
                q.general.add(rawQuad(pos, part.uvs, s, tint, part.shade, false));
                if (part.doubleSided) q.general.add(rawQuad(pos, part.uvs, s, tint, part.shade, true));
            }
        }
    }

    private static BakedQuad rawQuad(float[][] pos, float[] uvs, TextureAtlasSprite s, int tint, boolean shade, boolean back) {
        int[] order = back ? new int[]{3, 2, 1, 0} : new int[]{0, 1, 2, 3};
        float[] a = pos[order[0]], b = pos[order[1]], c = pos[order[2]];
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1.0E-6F) {
            nx /= len;
            ny /= len;
            nz /= len;
        } else {
            ny = 1.0F;
        }
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(DefaultVertexFormats.ITEM);
        builder.setQuadOrientation(EnumFacing.getFacingFromVector(nx, ny, nz));
        builder.setTexture(s);
        builder.setQuadTint(tint);
        builder.setApplyDiffuseLighting(shade);
        VertexFormat fmt = DefaultVertexFormats.ITEM;
        for (int vi : order) {
            float[] p = pos[vi];
            for (int e = 0; e < fmt.getElementCount(); e++) {
                VertexFormatElement el = fmt.getElement(e);
                switch (el.getUsage()) {
                    case POSITION -> builder.put(e, p[0], p[1], p[2], 1.0F);
                    case COLOR -> builder.put(e, 1.0F, 1.0F, 1.0F, 1.0F);
                    case UV -> builder.put(e, s.getInterpolatedU(uvs[vi * 2]), s.getInterpolatedV(uvs[vi * 2 + 1]), 0.0F, 1.0F);
                    case NORMAL -> builder.put(e, nx, ny, nz, 0.0F);
                    default -> builder.put(e);
                }
            }
        }
        return builder.build();
    }

    private @Nullable TextureAtlasSprite sprite(@Nullable IIcon icon) {
        if (icon == null) return null;
        if (icon instanceof ArchaicIcon a) return a.sprite();
        return textureGetter.apply(new ResourceLocation(icon.getIconName()));
    }

    private static ModelRotation rotationFor(EnumFacing up) {
        return switch (up) {
            case DOWN -> ModelRotation.X180_Y0;
            case NORTH -> ModelRotation.X90_Y0;
            case EAST -> ModelRotation.X90_Y90;
            case SOUTH -> ModelRotation.X90_Y180;
            case WEST -> ModelRotation.X90_Y270;
            default -> ModelRotation.X0_Y0;
        };
    }

    private @Nullable Quads build(int key, EnumFacing up) {
        int tint = block.hasArchaicTint() ? 0 : -1;
        ModelRotation rot = rotationFor(up);
        Quads q = new Quads();
        List<ArchaicRenderPart> parts = safeParts(key);
        if (parts != null) {
            buildParts(q, parts, rot, tint);
            return q.particle != null ? q : null;
        }
        int type = safeType(key);
        if (type == IArchaicRenderBlock.RENDER_CROSS || type == IArchaicRenderBlock.RENDER_CROPS) {
            TextureAtlasSprite s = sprite(safeIcon(2, key));
            if (s == null) return null;
            q.particle = s;
            q.ao = false;
            if (type == IArchaicRenderBlock.RENDER_CROSS) {
                BlockPartRotation r = new BlockPartRotation(new Vector3f(0.5F, 0.5F, 0.5F), EnumFacing.Axis.Y, 45.0F, true);
                plane(q, new Vector3f(0.8F, 0, 8), new Vector3f(15.2F, 16, 8), EnumFacing.NORTH, EnumFacing.SOUTH, s, rot, r, tint);
                plane(q, new Vector3f(8, 0, 0.8F), new Vector3f(8, 16, 15.2F), EnumFacing.WEST, EnumFacing.EAST, s, rot, r, tint);
            } else {
                plane(q, new Vector3f(4, -1, 0), new Vector3f(4, 15, 16), EnumFacing.WEST, EnumFacing.EAST, s, rot, null, tint);
                plane(q, new Vector3f(12, -1, 0), new Vector3f(12, 15, 16), EnumFacing.WEST, EnumFacing.EAST, s, rot, null, tint);
                plane(q, new Vector3f(0, -1, 4), new Vector3f(16, 15, 4), EnumFacing.NORTH, EnumFacing.SOUTH, s, rot, null, tint);
                plane(q, new Vector3f(0, -1, 12), new Vector3f(16, 15, 12), EnumFacing.NORTH, EnumFacing.SOUTH, s, rot, null, tint);
            }
            return q;
        }

        if (type == IArchaicRenderBlock.RENDER_LADDER) {
            TextureAtlasSprite s = sprite(safeIcon(2, key));
            if (s == null) return null;
            q.particle = s;
            q.ao = false;
            switch (key) {
                case 2 -> plane(q, new Vector3f(0, 0, 15.2F), new Vector3f(16, 16, 15.2F), EnumFacing.NORTH, EnumFacing.SOUTH, s, rot, null, tint);
                case 3 -> plane(q, new Vector3f(0, 0, 0.8F), new Vector3f(16, 16, 0.8F), EnumFacing.SOUTH, EnumFacing.NORTH, s, rot, null, tint);
                case 4 -> plane(q, new Vector3f(15.2F, 0, 0), new Vector3f(15.2F, 16, 16), EnumFacing.WEST, EnumFacing.EAST, s, rot, null, tint);
                case 5 -> plane(q, new Vector3f(0.8F, 0, 0), new Vector3f(0.8F, 16, 16), EnumFacing.EAST, EnumFacing.WEST, s, rot, null, tint);
                default -> plane(q, new Vector3f(0, 0, 8), new Vector3f(16, 16, 8), EnumFacing.SOUTH, EnumFacing.NORTH, s, rot, null, tint);
            }
            return q;
        }

        AxisAlignedBB bb = block.getArchaicRenderBounds(key);
        float x1 = (float)bb.minX * 16, y1 = (float)bb.minY * 16, z1 = (float)bb.minZ * 16;
        float x2 = (float)bb.maxX * 16, y2 = (float)bb.maxY * 16, z2 = (float)bb.maxZ * 16;
        Vector3f from = new Vector3f(x1, y1, z1), to = new Vector3f(x2, y2, z2);
        boolean any = false;
        for (int side = 0; side < 6; side++) {
            TextureAtlasSprite s = sprite(safeIcon(side, key));
            if (s == null) continue;
            any = true;
            if (q.particle == null || side == 2) q.particle = s;
            EnumFacing f = SIDES[side];
            float[] uv = switch (f) {
                case DOWN, UP -> new float[]{x1, z1, x2, z2};
                case NORTH, SOUTH -> new float[]{x1, 16 - y2, x2, 16 - y1};
                default -> new float[]{z1, 16 - y2, z2, 16 - y1};
            };
            boolean touches = switch (f) {
                case DOWN -> y1 <= 0;
                case UP -> y2 >= 16;
                case NORTH -> z1 <= 0;
                case SOUTH -> z2 >= 16;
                case WEST -> x1 <= 0;
                case EAST -> x2 >= 16;
            };
            EnumFacing cull = touches ? rot.rotate(f) : null;
            BakedQuad quad = BAKERY.makeBakedQuad(from, to, new BlockPartFace(cull, tint, "", new BlockFaceUV(uv, 0)), s, f, rot, null, false, true);
            if (cull == null) q.general.add(quad);
            else q.culled.computeIfAbsent(cull, _ -> new ArrayList<>()).add(quad);
        }
        return any ? q : null;
    }

    private static void plane(Quads q, Vector3f from, Vector3f to, EnumFacing a, EnumFacing b, TextureAtlasSprite s,
                              ModelRotation rot, @Nullable BlockPartRotation r, int tint) {
        float[] uv = {0, 0, 16, 16};
        q.general.add(BAKERY.makeBakedQuad(from, to, new BlockPartFace(null, tint, "", new BlockFaceUV(uv, 0)), s, a, rot, r, false, true));
        q.general.add(BAKERY.makeBakedQuad(from, to, new BlockPartFace(null, tint, "", new BlockFaceUV(uv.clone(), 0)), s, b, rot, r, false, true));
    }

    /** per-state memo: quadsFor is hit ~8x per block per chunk rebuild (7 sides + AO). */
    private final java.util.concurrent.ConcurrentHashMap<IBlockState, Quads> stateQuads = new java.util.concurrent.ConcurrentHashMap<>();

    private Quads quadsFor(@Nullable IBlockState state) {
        if (state != null && !(state instanceof net.minecraftforge.common.property.IExtendedBlockState)) {
            Quads c = stateQuads.get(state);
            if (c != null) return c;
            c = quadsFor0(state);
            stateQuads.put(state, c);
            return c;
        }
        return quadsFor0(state);
    }

    private Quads quadsFor0(@Nullable IBlockState state) {
        int key = 0;
        EnumFacing up = EnumFacing.UP;
        if (state != null && state.getBlock() == block) {
            try {
                key = block.getArchaicRenderKey(state);
                up = block.getArchaicRenderUp(key);
            } catch (RuntimeException e) {
                key = 0;
            }
        }
        long k = ((long)key << 3) | up.ordinal();
        final int fk = key;
        final EnumFacing fu = up;
        return worldQuads.computeIfAbsent(k, _ -> {
            Quads b = build(fk, fu);
            return b != null ? b : new Quads();
        });
    }

    private @Nullable IBakedModel getItemModel(int meta) {
        if (meta >= 0 && meta < itemModels.length && itemModels[meta] != null) return itemModels[meta];
        return itemModels[0];
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (state == null) {
            IBakedModel m = getItemModel(0);
            return m != null ? m.getQuads(null, side, rand) : Collections.emptyList();
        }
        Quads q = quadsFor(state);
        if (side == null) return q.general;
        return q.culled.getOrDefault(side, Collections.emptyList());
    }

    @Override
    public boolean isAmbientOcclusion(IBlockState state) {
        return quadsFor(state).ao;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
        IBakedModel model = getItemModel(0);
        return model != null ? model.handlePerspective(type) : IBakedModel.super.handlePerspective(type);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        Quads q = quadsFor(null);
        if (q.particle != null) return q.particle;
        IBakedModel m = getItemModel(0);
        return m != null ? m.getParticleTexture() : textureGetter.apply(new ResourceLocation("missingno"));
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrides;
    }

    private final class ItemCube implements IBakedModel {
        private final List<BakedQuad> all;
        private final TextureAtlasSprite particle;

        ItemCube(Quads q) {
            ImmutableList.Builder<BakedQuad> b = ImmutableList.builder();
            b.addAll(q.general);
            q.culled.values().forEach(b::addAll);
            this.all = b.build();
            this.particle = q.particle;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return side == null ? all : Collections.emptyList();
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
            Matrix4f m = blockTransforms != null ? blockTransforms.handlePerspective(type).getRight() : null;
            return Pair.of(this, m);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return particle;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.NONE;
        }
    }
}
