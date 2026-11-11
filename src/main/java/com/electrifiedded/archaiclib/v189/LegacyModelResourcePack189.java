package com.electrifiedded.archaiclib.v189;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@SideOnly(Side.CLIENT)
public final class LegacyModelResourcePack189 implements IResourcePack {
    private static final String PNG = ".png";

    private final IResourcePack source;

    public LegacyModelResourcePack189(IResourcePack source) {
        this.source = source;
    }

    public IResourcePack getSource() {
        return source;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        InputStream in = source.getInputStream(location);
        String path = pathOf(location);
        if (!LegacyModelConverter189.isModel(path)) return in;
        try (InputStream stream = in) {
            byte[] raw = readAll(stream);
            byte[] converted = LegacyModelConverter189.convert(path, raw, this::textureExists);
            return new ByteArrayInputStream(converted);
        }
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return source.resourceExists(location);
    }

    @Override
    public Set<String> getResourceDomains() {
        return source.getResourceDomains();
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String type) throws IOException {
        return source.getPackMetadata(serializer, type);
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        return source.getPackImage();
    }

    @Override
    public String getPackName() {
        return source.getPackName();
    }

    private boolean textureExists(String namespace, String path) {
        String texture = path;
        if (texture.startsWith("textures/")) texture = texture.substring("textures/".length());
        if (texture.endsWith(PNG)) texture = texture.substring(0, texture.length() - PNG.length());
        try {
            return source.resourceExists(new ResourceLocation(namespace, "textures/" + texture + PNG));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String dumpResource(String id) {
        try {
            net.minecraft.client.resources.IResource resource = net.minecraft.client.Minecraft.getMinecraft()
                    .getResourceManager().getResource(new ResourceLocation(id));
            try (InputStream in = resource.getInputStream()) {
                return new String(readAll(in), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
            return "ERR " + t;
        }
    }

    private static String pathOf(ResourceLocation location) {
        return "assets/" + location.getNamespace() + "/" + location.getPath();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
