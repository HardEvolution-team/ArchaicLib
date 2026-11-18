package com.electrifiedded.archaiclib.original.client;

import com.electrifiedded.archaiclib.ExampleMod;
import com.electrifiedded.archaiclib.original.OriginalModAssets;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Serves selected asset domains out of an original (old-version) mod jar the player supplied.
 * Lookup is case-insensitive, so 1.7-era names such as {@code lang/en_US.lang} answer 1.12 requests
 * for {@code lang/en_us.lang}. Only the declared domains are exposed.
 */
public final class OriginalJarResourcePack implements IResourcePack {

    private final File file;
    private final Set<String> domains = new HashSet<>();
    /** lower-case path -> real entry name */
    private final Map<String, String> index = new HashMap<>();
    private ZipFile zip;

    public OriginalJarResourcePack(File file, Iterable<String> domains) {
        this.file = file;
        for (String d : domains) this.domains.add(d);
        try {
            Enumeration<? extends ZipEntry> en = zip().entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                String lower = e.getName().toLowerCase(Locale.ROOT);
                if (!lower.startsWith("assets/")) continue;
                int slash = lower.indexOf('/', 7);
                if (slash < 0 || !this.domains.contains(lower.substring(7, slash))) continue;
                index.putIfAbsent(lower, e.getName());
            }
        } catch (IOException e) {
            ExampleMod.LOGGER.error("Cannot index original jar {}", file, e);
        }
    }

    private synchronized ZipFile zip() throws IOException {
        if (zip == null) zip = new ZipFile(file);
        return zip;
    }

    private String entryFor(ResourceLocation loc) {
        String lower = ("assets/" + loc.getNamespace() + "/" + loc.getPath()).toLowerCase(Locale.ROOT);
        String alias = OriginalModAssets.aliasFor(lower);
        return alias != null ? index.get(alias) : index.get(lower);
    }

    @Override
    public InputStream getInputStream(ResourceLocation loc) throws IOException {
        String entry = entryFor(loc);
        if (entry == null) throw new FileNotFoundException(loc.toString());
        ZipFile z = zip();
        return new ByteArrayInputStream(OriginalModAssets.readAll(z.getInputStream(z.getEntry(entry))));
    }

    @Override
    public boolean resourceExists(ResourceLocation loc) {
        return domains.contains(loc.getNamespace()) && entryFor(loc) != null;
    }

    @Override
    public Set<String> getResourceDomains() {
        return domains;
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
        return "ArchaicOriginal:" + file.getName();
    }
}
