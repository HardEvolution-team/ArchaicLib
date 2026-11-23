package com.electrifiedded.archaiclib.client.texture;

import com.electrifiedded.archaiclib.ExampleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A virtual resource pack whose PNG files are painted in code when something asks for them. Old mods that built
 * textures at runtime (1.7 custom TextureAtlasSprite: ore = base ore over stone, nugget = tinted ingot...) register
 * the generated path here and keep using ordinary texture / model machinery - the atlas, ArchaicItem and ArchaicBlock
 * icons all simply find a {@code textures/blocks/x.png}. Generators run on every resource reload and may read other
 * textures through {@code Minecraft.getResourceManager()} (but not their own paths). Nothing is shipped in a jar.
 *
 * <pre>
 * GeneratedTexturePack pack = GeneratedTexturePack.install("mymod");
 * pack.add("textures/blocks/poor_iron_ore.png", () -&gt; paint());
 * </pre>
 */
@SideOnly(Side.CLIENT)
public final class GeneratedTexturePack implements IResourcePack {
    private final String domain;
    private final Map<String, Supplier<BufferedImage>> files = new LinkedHashMap<>();

    private GeneratedTexturePack(String domain) {
        this.domain = domain;
    }

    /** Creates the pack and puts it into the default resource packs. Call before the first resource reload (preInit). */
    @SuppressWarnings("unchecked")
    public static GeneratedTexturePack install(String domain) {
        GeneratedTexturePack pack = new GeneratedTexturePack(domain);
        try {
            // preInit runs AFTER the startup reload: also inject into the live manager (else nothing finds the files)
            com.electrifiedded.archaiclib.client.resources.ArchaicPackInstaller.addDefaultPack(pack);
        } catch (Throwable t) {
            ExampleMod.LOGGER.error("Could not install generated texture pack for {}", domain, t);
        }
        return pack;
    }

    /** @param path e.g. {@code textures/blocks/foo.png} (lower-case) */
    public GeneratedTexturePack add(String path, Supplier<BufferedImage> painter) {
        files.put(path.toLowerCase(Locale.ROOT), painter);
        return this;
    }

    /** Reads a PNG through the resource manager, or null when it cannot be read. */
    public static BufferedImage read(ResourceLocation loc) {
        try (InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream()) {
            return ImageIO.read(in);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Lower-case paths ("assets/domain/path") for resource listers such as VintageFix. */
    public List<String> listPaths(Predicate<String> filter) {
        List<String> out = new ArrayList<>();
        for (String p : files.keySet()) {
            String full = "assets/" + domain + "/" + p;
            if (filter.test(full)) out.add(full);
        }
        return out;
    }

    @Override
    public InputStream getInputStream(ResourceLocation loc) throws IOException {
        Supplier<BufferedImage> s = domain.equals(loc.getNamespace()) ? files.get(loc.getPath().toLowerCase(Locale.ROOT)) : null;
        if (s == null) throw new FileNotFoundException(loc.toString());
        BufferedImage img;
        try {
            img = s.get();
        } catch (RuntimeException e) {
            throw new IOException("texture painter failed for " + loc, e);
        }
        if (img == null) throw new FileNotFoundException("no image generated for " + loc);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    public boolean resourceExists(ResourceLocation loc) {
        return domain.equals(loc.getNamespace()) && files.containsKey(loc.getPath().toLowerCase(Locale.ROOT));
    }

    @Override
    public Set<String> getResourceDomains() {
        Set<String> d = new HashSet<>();
        d.add(domain);
        return d;
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new FileNotFoundException("pack.png");
    }

    @Override
    public String getPackName() {
        return "ArchaicGenerated:" + domain;
    }
}
