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
    /** lower-case path -> real entry name of assets/minecraft/** (only reachable through aliases) */
    private final Map<String, String> hiddenIndex = new HashMap<>();
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
                if (slash < 0) continue;
                if (lower.startsWith("assets/minecraft/") && !this.domains.contains("minecraft")) { hiddenIndex.putIfAbsent(lower, e.getName()); continue; }
                if (!this.domains.contains(lower.substring(7, slash))) continue;
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
        String ns = loc.getNamespace();
        String orig = OriginalModAssets.originalDomainOf(ns);
        if (orig != null) {
            if (!domains.contains(orig)) return null;
        } else if (OriginalModAssets.isRemapped(ns)) {
            return null; // the original domain is only reachable under its port name
        }
        String req = ("assets/" + ns + "/" + loc.getPath()).toLowerCase(Locale.ROOT);
        String alias = OriginalModAssets.aliasFor(req);
        String target = alias != null ? alias : req;
        if (orig != null && target.startsWith("assets/" + ns + "/"))
            target = "assets/" + orig + target.substring(("assets/" + ns).length());
        String r = index.get(target);
        return r != null || alias == null ? r : hiddenIndex.get(target);
    }

    /**
     * Every asset path this pack serves, lower-case (lookups are case-insensitive), plus the port-side names of
     * aliased files. For resource listers such as VintageFix's texture pass.
     */
    public java.util.List<String> listPaths(java.util.function.Predicate<String> filter) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String lower : index.keySet()) {
            String l = lower;
            int slash = l.indexOf('/', 7);
            String d = l.substring(7, slash);
            if (OriginalModAssets.isRemapped(d)) {
                for (Map.Entry<String, String> m : OriginalModAssets.domainMap().entrySet()) {
                    if (m.getValue().equals(d)) { String r = "assets/" + m.getKey() + l.substring(slash); if (filter.test(r)) out.add(r); }
                }
                continue;
            }
            if (filter.test(l)) out.add(l);
        }
        for (String req : OriginalModAssets.aliasKeys()) {
            String target = OriginalModAssets.aliasFor(req);
            if (target != null && !target.isEmpty() && (index.containsKey(target) || hiddenIndex.containsKey(target)) && filter.test(req)) out.add(req);
        }
        return out;
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
        return entryFor(loc) != null;
    }

    @Override
    public Set<String> getResourceDomains() {
        Set<String> out = new HashSet<>();
        for (String d : domains) if (!OriginalModAssets.isRemapped(d)) out.add(d);
        for (Map.Entry<String, String> m : OriginalModAssets.domainMap().entrySet()) if (domains.contains(m.getValue())) out.add(m.getKey());
        return out;
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
