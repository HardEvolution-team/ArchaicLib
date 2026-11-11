package com.electrifiedded.archaiclib.v1710.client;

import com.electrifiedded.archaiclib.Reference;
import com.electrifiedded.archaiclib.v1710.api.block.IArchaicRenderBlock;
import net.minecraftforge.client.event.ColorHandlerEvent;
import com.electrifiedded.archaiclib.v1710.api.client.IIcon;
import com.electrifiedded.archaiclib.v1710.client.renderer.ArchaicBakedModel;
import com.electrifiedded.archaiclib.v1710.api.client.IIconRegister;
import com.electrifiedded.archaiclib.v1710.api.item.ArchaicItem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Reference.MOD_ID)
public final class ClientEventHandler {

    public static final Map<String, IIcon> ICON_REGISTRY = new Object2ObjectOpenHashMap<>();

    private static boolean loaderRegistered;

    private ClientEventHandler() {}

    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
        if (!loaderRegistered) {
            ModelLoaderRegistry.registerLoader(new ArchaicModelLoader());
            loaderRegistered = true;
        }

        for (Item item : Item.REGISTRY) {
            if (item instanceof ArchaicItem && item.getRegistryName() != null) {
                ModelLoader.setCustomModelResourceLocation(
                        item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
            }
        }

        for (Block block : Block.REGISTRY) {
            if (!(block instanceof IArchaicRenderBlock) || block.getRegistryName() == null) continue;
            Item item = Item.getItemFromBlock(block);
            if (item == Items.AIR) continue;
            ModelResourceLocation mrl = new ModelResourceLocation(block.getRegistryName(), "inventory");
            for (int meta = 0; meta < 16; meta++) {
                ModelLoader.setCustomModelResourceLocation(item, meta, mrl);
            }
        }
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        TextureMap map = event.getMap();
        if (!"textures".equals(map.getBasePath())) return;
        ICON_REGISTRY.clear();
        ArchaicModelLoader.BAKED.clear();

        IIconRegister reg = name -> {
            ResourceLocation loc = toSpriteLocation(name);
            return ICON_REGISTRY.computeIfAbsent(loc.toString(), _ -> new ArchaicIcon(map.registerSprite(loc)));
        };

        for (Block block : Block.REGISTRY) {
            if (block instanceof IArchaicRenderBlock archaic) {
                archaic.registerBlockIcons(reg);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockColors(ColorHandlerEvent.Block event) {
        for (Block block : Block.REGISTRY) {
            if (block instanceof IArchaicRenderBlock r) {
                event.getBlockColors().registerBlockColorHandler((state, world, pos, tint) -> {
                    if (tint >= ArchaicBakedModel.PART_COLOR_TINT) return tint & 0xFFFFFF;
                    if (!r.hasArchaicTint()) return 0xFFFFFF;
                    return world != null && pos != null ? r.colorMultiplier(world, pos) : r.getRenderColor(block.getMetaFromState(state));
                }, block);
            }
        }
    }

    @SubscribeEvent
    public static void onItemColors(ColorHandlerEvent.Item event) {
        for (Block block : Block.REGISTRY) {
            if (block instanceof IArchaicRenderBlock r) {
                Item item = Item.getItemFromBlock(block);
                if (item != Items.AIR) {
                    event.getItemColors().registerItemColorHandler((stack, tint) -> {
                        if (tint >= ArchaicBakedModel.PART_COLOR_TINT) return tint & 0xFFFFFF;
                        return r.hasArchaicTint() ? r.getRenderColor(stack.getMetadata()) : 0xFFFFFF;
                    }, item);
                }
            }
        }
    }

    static ResourceLocation toSpriteLocation(String name) {
        ResourceLocation raw = new ResourceLocation(name);
        String path = raw.getPath();
        return path.indexOf('/') >= 0 ? raw : new ResourceLocation(raw.getNamespace(), "blocks/" + path);
    }
}
