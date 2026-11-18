package com.electrifiedded.archaiclib.original;

import com.electrifiedded.archaiclib.ExampleMod;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Support for ports of All-Rights-Reserved mods.
 * <p>
 * A port of an ARR mod does not redistribute the original assets. The port declares which original
 * jar it needs; the player puts their own copy of that jar into {@code <game dir>/archaic_originals/}
 * (NOT into {@code mods/}), and ArchaicLib serves the requested asset domains straight out of it as a
 * built-in resource pack. Nothing from the original jar is ever class-loaded.
 * <p>
 * Call {@link #require} from the port's preInit on both sides. A dedicated server only logs, since
 * servers do not need assets.
 */
public final class OriginalModAssets {

    public static final String FOLDER = "archaic_originals";

    private static final Map<String, Requirement> REQUIREMENTS = new LinkedHashMap<>();
    private static File gameDir = new File(".");
    /** lower-case "assets/domain/path" requested by 1.12 -> lower-case path inside the original jar */
    private static final Map<String, String> ALIASES = new java.util.concurrent.ConcurrentHashMap<>();

    private OriginalModAssets() {}

    /** One original jar that a port needs for its assets. */
    public static final class Requirement {
        public final String portModId;
        public final String originalModId;
        public final String displayName;
        public final String downloadUrl;
        /** Asset domains served from the original jar, e.g. "helpfulvillagers". */
        public final List<String> domains;
        File jar;

        Requirement(String portModId, String originalModId, String displayName, String downloadUrl, List<String> domains) {
            this.portModId = portModId;
            this.originalModId = originalModId;
            this.displayName = displayName;
            this.downloadUrl = downloadUrl;
            this.domains = Collections.unmodifiableList(domains);
        }

        public File getJar() { return jar; }
        public boolean isPresent() { return jar != null; }
    }

    public static void setGameDir(File dir) {
        if (dir != null) gameDir = dir;
    }

    public static File folder() {
        File f = new File(gameDir, FOLDER);
        if (!f.isDirectory()) f.mkdirs();
        return f;
    }

    /**
     * Declare that {@code portModId} needs the assets of the original mod {@code originalModId}.
     *
     * @param displayName human readable name of the original file, e.g. "Helpful Villagers 1.3.1 (1.7.10)"
     * @param downloadUrl where the player can get the original jar (shown on the warning screen)
     * @param domains     asset domains to serve from that jar
     * @return true if a matching original jar was found
     */
    public static synchronized boolean require(String portModId, String originalModId, String displayName,
                                               String downloadUrl, String... domains) {
        List<String> doms = new ArrayList<>();
        for (String d : domains) doms.add(d.toLowerCase(Locale.ROOT));
        Requirement req = new Requirement(portModId, originalModId, displayName, downloadUrl, doms);
        req.jar = findJar(originalModId, doms);
        REQUIREMENTS.put(portModId, req);
        if (req.jar != null) {
            ExampleMod.LOGGER.info("[{}] using original assets from {}", portModId, req.jar.getName());
            OriginalAssetsHooks.onRequirementResolved(req);
        } else {
            ExampleMod.LOGGER.warn("[{}] original jar of '{}' not found in {} - its textures will be missing. Get it: {}",
                    portModId, originalModId, folder().getAbsolutePath(), downloadUrl);
        }
        return req.jar != null;
    }

    /**
     * Serve {@code domain:path} from a differently named file of the original jar, e.g. a 1.7 language file
     * {@code lang/zh_CN1.lang} for the 1.12 request {@code lang/zh_cn.lang}, or a texture that the original
     * keeps in another folder. Packs consult aliases on every lookup, so this may be called at any time
     * before the resources are (re)loaded - for the first load that means the mod constructor.
     */
    public static void alias(String domain, String path, String originalPath) {
        ALIASES.put(key(domain, path), key(domain, originalPath));
    }

    /**
     * Never serve {@code domain:path} from the original jar, e.g. a 1.7 {@code sounds.json} whose unqualified
     * sound names 1.12 would resolve against {@code minecraft:} (the port ships its own fixed copy).
     */
    public static void hide(String domain, String path) {
        ALIASES.put(key(domain, path), "");
    }

    /** @return the aliased lower-case entry path ("" = hidden) for a lower-case request path, or null */
    public static String aliasFor(String lowerAssetPath) {
        return ALIASES.get(lowerAssetPath);
    }

    private static String key(String domain, String path) {
        return ("assets/" + domain + "/" + path).toLowerCase(Locale.ROOT);
    }

    public static synchronized List<Requirement> all() {
        return Collections.unmodifiableList(new ArrayList<>(REQUIREMENTS.values()));
    }

    public static synchronized List<Requirement> missing() {
        List<Requirement> out = new ArrayList<>();
        for (Requirement r : REQUIREMENTS.values()) if (r.jar == null) out.add(r);
        return out;
    }

    public static synchronized boolean isPresent(String portModId) {
        Requirement r = REQUIREMENTS.get(portModId);
        return r != null && r.jar != null;
    }

    private static File findJar(String originalModId, List<String> domains) {
        File[] files = folder().listFiles();
        if (files == null) return null;
        File fallback = null;
        for (File f : files) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (!f.isFile() || !(n.endsWith(".jar") || n.endsWith(".zip"))) continue;
            try (ZipFile zip = new ZipFile(f)) {
                boolean hasDomains = true;
                for (String d : domains) {
                    if (!hasPrefix(zip, "assets/" + d + "/")) { hasDomains = false; break; }
                }
                if (!hasDomains) continue;
                if (declaresModId(zip, originalModId)) return f;
                if (fallback == null) fallback = f;
            } catch (IOException e) {
                ExampleMod.LOGGER.warn("Cannot read {}: {}", f, e.toString());
            }
        }
        return fallback;
    }

    /**
     * Every jar/zip in the originals folder with the asset domains it contains. Used on the client at mod
     * construction: 1.12 reloads resources only once, before any mod's preInit, so packs must exist by then.
     */
    public static Map<File, List<String>> discoverAll() {
        Map<File, List<String>> out = new LinkedHashMap<>();
        File[] files = folder().listFiles();
        if (files == null) return out;
        for (File f : files) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (!f.isFile() || !(n.endsWith(".jar") || n.endsWith(".zip"))) continue;
            try (ZipFile zip = new ZipFile(f)) {
                List<String> domains = new ArrayList<>();
                Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()) {
                    String name = en.nextElement().getName().toLowerCase(Locale.ROOT);
                    if (!name.startsWith("assets/")) continue;
                    int slash = name.indexOf('/', 7);
                    if (slash > 7) {
                        String d = name.substring(7, slash);
                        if (!domains.contains(d) && !d.equals("minecraft")) domains.add(d);
                    }
                }
                if (!domains.isEmpty()) out.put(f, domains);
            } catch (IOException e) {
                ExampleMod.LOGGER.warn("Cannot read {}: {}", f, e.toString());
            }
        }
        return out;
    }

    private static boolean hasPrefix(ZipFile zip, String prefix) {
        Enumeration<? extends ZipEntry> en = zip.entries();
        while (en.hasMoreElements()) {
            if (en.nextElement().getName().toLowerCase(Locale.ROOT).startsWith(prefix)) return true;
        }
        return false;
    }

    private static boolean declaresModId(ZipFile zip, String modId) {
        ZipEntry info = zip.getEntry("mcmod.info");
        if (info == null) return false;
        try {
            String s = new String(readAll(zip.getInputStream(info)), StandardCharsets.UTF_8);
            return Pattern.compile("\"modid\"\\s*:\\s*\"" + Pattern.quote(modId) + "\"").matcher(s).find();
        } catch (IOException e) {
            return false;
        }
    }

    public static byte[] readAll(InputStream in) throws IOException {
        try (InputStream i = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = i.read(buf)) > 0) out.write(buf, 0, r);
            return out.toByteArray();
        }
    }

    /**
     * Read a raw file of the original jar of {@code portModId} (any path, e.g. {@code assets/x/data/1.dat}),
     * on either side - for data files (schematics, tables) a 1.7 mod read with getResourceAsStream.
     * Requires a prior {@link #require}. Returns null when the jar or the entry is missing.
     */
    public static synchronized byte[] readOriginal(String portModId, String path) {
        Requirement req = REQUIREMENTS.get(portModId);
        if (req == null || req.jar == null) return null;
        try (ZipFile zip = new ZipFile(req.jar)) {
            ZipEntry e = zip.getEntry(path);
            if (e == null) {
                Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()) {
                    ZipEntry c = en.nextElement();
                    if (c.getName().equalsIgnoreCase(path)) { e = c; break; }
                }
            }
            return e == null ? null : readAll(zip.getInputStream(e));
        } catch (IOException ex) {
            ExampleMod.LOGGER.warn("[{}] cannot read {} from {}: {}", portModId, path, req.jar, ex.toString());
            return null;
        }
    }
}
