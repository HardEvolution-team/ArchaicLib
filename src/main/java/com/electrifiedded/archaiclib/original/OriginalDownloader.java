package com.electrifiedded.archaiclib.original;

import com.electrifiedded.archaiclib.ExampleMod;
import com.electrifiedded.archaiclib.Reference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.ProgressManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fetches the original jars that ARR ports need, from the original authors' download places.
 * <p>
 * Every port ships {@code archaic_originals.json} in its jar root:
 * <pre>
 * { "port": "orespawn", "version": 1, "locked": false,
 *   "files": [ { "name": "OreSpawn 20.3 for 1.7.10", "file": "orespawn-1.7.10-20.3.jar",
 *                "sha256": "...", "size": 17541969,
 *                "page": "https://... (shown to the player for a manual download)",
 *                "mirrors": [ "https://direct-link-1", "https://direct-link-2" ] } ] }
 * </pre>
 * On first start the manifest is copied to {@code config/archaiclib/originals/<port>.json}; that copy is what
 * is used, so players and pack makers can read and fix links there. A newer shipped manifest replaces the
 * copy unless the copy says {@code "locked": true}. Files land in {@code archaic_originals/}, verified by
 * SHA-256; nothing is ever class-loaded. Runs at ArchaicLib construction, with a bar on the loading screen.
 */
public final class OriginalDownloader {

    public static final String MANIFEST = "archaic_originals.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** One manifest entry. */
    public static final class Entry {
        /** optional: the port modid passed to OriginalModAssets.require, when it differs from the manifest's port */
        public String modid;
        public String name;
        public String file;
        public String sha256;
        public long size;
        public String page;
        public List<String> mirrors = new ArrayList<>();
        /**
         * optional: the mirrors serve a zip and the wanted file is this entry inside it (the author's own
         * download was an archive). sha256/size then describe the extracted file.
         */
        public String inner;
        /**
         * optional: other acceptable files (another official build of the same version with its own hash and
         * mirrors), tried in order when every mirror of this entry fails; any of them counts as present.
         */
        public List<Entry> alternatives;
        /** set at runtime */
        transient String port;
        transient String error;

        public String port() { return port; }
        public String error() { return error; }
    }

    static final class Manifest {
        String port;
        int version;
        boolean locked;
        List<Entry> files = new ArrayList<>();
    }

    private static final Map<String, Manifest> MANIFESTS = new LinkedHashMap<>();
    private static final List<Entry> FAILED = Collections.synchronizedList(new ArrayList<>());
    private static boolean autoDownload = true;
    private static int timeoutSeconds = 30;
    private static volatile boolean ran;

    private OriginalDownloader() {}

    /** Called at ArchaicLib construction on both sides, before original packs are discovered. */
    public static synchronized void run(File gameDir) {
        if (ran) return;
        ran = true;
        try {
            File cfgDir = new File(gameDir, "config/archaiclib/originals");
            cfgDir.mkdirs();
            loadSettings(new File(gameDir, "config/archaiclib/originals.cfg"));
            loadManifests(cfgDir);
            List<Entry> todo = new ArrayList<>();
            Map<String, File> present = indexFolder();
            for (Manifest m : MANIFESTS.values()) {
                for (Entry e : m.files) {
                    if (e.file == null) continue;
                    if (!isPresent(e, present)) todo.add(e);
                }
            }
            if (todo.isEmpty()) return;
            if (!autoDownload) {
                for (Entry e : todo) fail(e, "auto download is off (config/archaiclib/originals.cfg)");
                return;
            }
            download(todo);
        } catch (Throwable t) {
            ExampleMod.LOGGER.error("Original downloader failed", t);
        }
    }

    private static void loadSettings(File f) {
        Configuration c = new Configuration(f);
        c.load();
        autoDownload = c.getBoolean("autoDownload", "general", true,
                "Download missing original mod files (needed by ARR ports for textures/sounds/data) from the links in config/archaiclib/originals/*.json");
        timeoutSeconds = c.getInt("stallTimeoutSeconds", "general", 5, 2, 300,
                "Give up on a link when connecting, or a pause in incoming data, takes longer than this. A slow but steady download is never cut off.");
        if (c.hasChanged()) c.save();
    }

    // ---------------------------------------------------------------- manifests

    private static void loadManifests(File cfgDir) throws IOException {
        Enumeration<URL> urls = OriginalDownloader.class.getClassLoader().getResources(MANIFEST);
        while (urls.hasMoreElements()) {
            URL u = urls.nextElement();
            Manifest shipped;
            try (Reader r = new InputStreamReader(u.openStream(), StandardCharsets.UTF_8)) {
                shipped = GSON.fromJson(r, Manifest.class);
            } catch (Exception ex) {
                ExampleMod.LOGGER.warn("Bad {} in {}: {}", MANIFEST, u, ex.toString());
                continue;
            }
            if (shipped == null || shipped.port == null) continue;
            File cfg = new File(cfgDir, shipped.port.toLowerCase(Locale.ROOT) + ".json");
            Manifest use = shipped;
            if (cfg.isFile()) {
                try (Reader r = new InputStreamReader(new FileInputStream(cfg), StandardCharsets.UTF_8)) {
                    Manifest local = GSON.fromJson(r, Manifest.class);
                    if (local != null && (local.locked || local.version >= shipped.version)) use = local;
                } catch (Exception ex) {
                    ExampleMod.LOGGER.warn("Cannot parse {} ({}), using the manifest shipped with {}", cfg, ex.toString(), shipped.port);
                }
            }
            if (use == shipped) {
                try (Writer w = Files.newBufferedWriter(cfg.toPath(), StandardCharsets.UTF_8)) {
                    GSON.toJson(shipped, w);
                }
            }
            if (use.port == null) use.port = shipped.port;
            for (Entry e : use.files) e.port = use.port;
            MANIFESTS.put(use.port, use);
        }
        ExampleMod.LOGGER.info("Original manifests: {}", MANIFESTS.keySet());
    }

    /** @return the manual download page declared for a port (first entry), or null */
    public static synchronized String pageFor(String port) {
        for (Manifest m : MANIFESTS.values()) {
            for (Entry e : m.files) {
                boolean mine = port.equalsIgnoreCase(e.modid != null ? e.modid : m.port);
                if (mine && e.page != null && !e.page.isEmpty()) return e.page;
            }
        }
        return null;
    }

    public static List<Entry> failed() {
        synchronized (FAILED) { return new ArrayList<>(FAILED); }
    }

    // ---------------------------------------------------------------- presence

    private static Map<String, File> indexFolder() {
        Map<String, File> out = new HashMap<>();
        File[] files = OriginalModAssets.folder().listFiles();
        if (files == null) return out;
        for (File f : files) {
            if (!f.isFile() || f.getName().endsWith(".part")) continue;
            out.put("name:" + f.getName().toLowerCase(Locale.ROOT), f);
        }
        return out;
    }

    private static final Map<File, String> HASHES = new HashMap<>();

    private static boolean isPresent(Entry e, Map<String, File> present) {
        if (e.alternatives != null) {
            for (Entry a : e.alternatives) if (a.file != null && isPresent(a, present)) return true;
        }
        if (present.containsKey("name:" + e.file.toLowerCase(Locale.ROOT))) return true;
        if (e.sha256 == null) return false;
        for (File f : present.values()) {
            if (e.size > 0 && f.length() != e.size) continue;
            String h = HASHES.computeIfAbsent(f, OriginalDownloader::sha256Quiet);
            if (e.sha256.equalsIgnoreCase(h)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- download

    private static void download(List<Entry> todo) {
        ProgressManager.ProgressBar outer = ProgressManager.push("ArchaicLib: downloading original mod files", todo.size());
        try {
            for (Entry e : todo) {
                outer.step(e.name != null ? e.name : e.file);
                downloadOne(e);
            }
        } finally {
            while (outer.getStep() < outer.getSteps()) outer.step("");
            ProgressManager.pop(outer);
        }
    }

    /** Download one entry, trying each mirror. Thread-safe, also used by the "retry" button. */
    public static boolean downloadOne(Entry e) {
        FAILED.remove(e);
        String err = tryMirrors(e);
        if (err == null) return true;
        if (e.alternatives != null) {
            for (Entry a : e.alternatives) {
                if (a.file == null) continue;
                a.port = e.port;
                ExampleMod.LOGGER.info("[{}] trying alternative file {}", e.port, a.file);
                if (tryMirrors(a) == null) return true;
            }
        }
        fail(e, err);
        return false;
    }

    /** @return null on success, else the last error */
    private static String tryMirrors(Entry e) {
        if (e.mirrors == null || e.mirrors.isEmpty()) return "no download links";
        File target = new File(OriginalModAssets.folder(), e.file);
        File part = new File(target.getPath() + ".part");
        String lastError = "?";
        for (String mirror : e.mirrors) {
            ExampleMod.LOGGER.info("[{}] downloading {} from {}", e.port, e.file, mirror);
            ProgressManager.ProgressBar bar = null;
            try {
                bar = pushBarIfSplash(e);
                long got = fetch(resolve(mirror), part, e, bar);
                if (e.inner != null && !e.inner.isEmpty()) got = extractInner(part, e.inner);
                if (e.size > 0 && got != e.size) throw new IOException("size " + got + " != " + e.size);
                if (e.sha256 != null && !e.sha256.isEmpty()) {
                    String h = sha256(part);
                    if (!e.sha256.equalsIgnoreCase(h)) throw new IOException("SHA-256 mismatch (" + h + ")");
                }
                Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                ExampleMod.LOGGER.info("[{}] got {} ({} bytes)", e.port, e.file, got);
                return null;
            } catch (Throwable t) {
                lastError = t.toString();
                ExampleMod.LOGGER.warn("[{}] {} failed from {}: {}", e.port, e.file, mirror, lastError);
                part.delete();
            } finally {
                if (bar != null) {
                    while (bar.getStep() < bar.getSteps()) bar.step("");
                    ProgressManager.pop(bar);
                }
            }
        }
        return lastError;
    }

    /**
     * A mirror may be {@code "scrape:<page url> <regex>"}: the page is fetched and the first match of the regex
     * is downloaded (sites whose direct links change, e.g. carry a time-based token). The SHA-256 check still
     * decides whether the result is accepted.
     */
    static String resolve(String mirror) throws IOException {
        if (!mirror.startsWith("scrape:")) return mirror;
        String rest = mirror.substring(7).trim();
        int sp = rest.indexOf(' ');
        if (sp < 0) throw new IOException("scrape mirror needs '<url> <regex>'");
        String page = rest.substring(0, sp);
        java.util.regex.Pattern pat = java.util.regex.Pattern.compile(rest.substring(sp + 1).trim());
        checkDns(page);
        HttpURLConnection c = (HttpURLConnection) new URL(page).openConnection();
        c.setConnectTimeout(timeoutSeconds * 1000);
        c.setReadTimeout(timeoutSeconds * 1000);
        c.setRequestProperty("User-Agent", BROWSER_UA);
        try (InputStream in = c.getInputStream()) {
            byte[] html = OriginalModAssets.readAll(in);
            if (html.length > 8 * 1024 * 1024) throw new IOException("page too big");
            java.util.regex.Matcher m = pat.matcher(new String(html, StandardCharsets.UTF_8));
            if (!m.find()) throw new IOException("no download link found on " + page);
            String link = (m.groupCount() > 0 && m.group(1) != null ? m.group(1) : m.group()).replace("&amp;", "&");
            return new URL(new URL(page), link).toString();
        } finally {
            c.disconnect();
        }
    }

    /** Java waits for DNS outside the connect timeout; bound it the same way. */
    private static void checkDns(String url) throws IOException {
        String host = new URL(url).getHost();
        java.util.concurrent.FutureTask<java.net.InetAddress> t = new java.util.concurrent.FutureTask<>(() -> java.net.InetAddress.getByName(host));
        Thread th = new Thread(t, "ArchaicLib DNS " + host);
        th.setDaemon(true);
        th.start();
        try {
            t.get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException ex) {
            throw new IOException("no DNS answer for " + host + " in " + timeoutSeconds + " s");
        } catch (java.util.concurrent.ExecutionException ex) {
            throw new IOException("cannot resolve " + host + ": " + ex.getCause());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        }
    }

    static final String BROWSER_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private static boolean splashPhase = true;

    /** Called once loading is over: the retry button runs off the splash, without progress bars. */
    public static void loadingFinished() { splashPhase = false; }

    private static ProgressManager.ProgressBar pushBarIfSplash(Entry e) {
        if (!splashPhase) return null;
        return ProgressManager.push("Downloading " + e.file, 100);
    }

    /** Replace the downloaded archive {@code part} by its entry {@code inner}; returns the new size. */
    private static long extractInner(File part, String inner) throws IOException {
        File tmp = new File(part.getPath() + ".x");
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(part)) {
            java.util.zip.ZipEntry ze = zip.getEntry(inner);
            if (ze == null) throw new IOException("archive has no " + inner);
            try (InputStream in = zip.getInputStream(ze); OutputStream o = new FileOutputStream(tmp)) {
                byte[] buf = new byte[65536];
                long n = 0;
                int r;
                while ((r = in.read(buf)) > 0) {
                    o.write(buf, 0, r);
                    n += r;
                    if (n > 512L * 1024 * 1024) throw new IOException("entry too big");
                }
            }
        }
        Files.move(tmp.toPath(), part.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return part.length();
    }


    private static long fetch(String url, File out, Entry e, ProgressManager.ProgressBar bar) throws IOException {
        HttpURLConnection c = null;
        for (int hop = 0; hop < 8; hop++) {
            checkDns(url);
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(timeoutSeconds * 1000);
            c.setReadTimeout(timeoutSeconds * 1000);
            c.setRequestProperty("User-Agent", BROWSER_UA);
            int code = c.getResponseCode();
            if (code >= 300 && code < 400 && c.getHeaderField("Location") != null) {
                url = new URL(new URL(url), c.getHeaderField("Location")).toString();
                c.disconnect();
                continue;
            }
            if (code != 200) throw new IOException("HTTP " + code);
            break;
        }
        long total = e.size > 0 && e.inner == null ? e.size : c.getContentLengthLong();
        long got = 0;
        int shown = 0;
        try (InputStream in = c.getInputStream(); OutputStream o = new FileOutputStream(out)) {
            byte[] buf = new byte[65536];
            int r;
            while ((r = in.read(buf)) > 0) {
                o.write(buf, 0, r);
                got += r;
                if (e.inner == null && e.size > 0 && got > e.size + 1024) throw new IOException("file bigger than expected");
                if (got > 512L * 1024 * 1024) throw new IOException("file too big");
                if (bar != null && total > 0) {
                    int pct = (int) Math.min(99, got * 100 / total);
                    while (shown < pct) {
                        shown++;
                        bar.step(String.format(Locale.ROOT, "%s  %.1f / %.1f MB", e.file, got / 1048576.0, total / 1048576.0));
                    }
                }
            }
        } finally {
            c.disconnect();
        }
        return got;
    }

    private static void fail(Entry e, String why) {
        e.error = why;
        if (!FAILED.contains(e)) FAILED.add(e);
        ExampleMod.LOGGER.error("[{}] could not get original file {}: {}. Download it manually from {} and put it into {}",
                e.port, e.file, why, e.page, OriginalModAssets.folder().getAbsolutePath());
    }

    // ---------------------------------------------------------------- hashing

    private static String sha256Quiet(File f) {
        try { return sha256(f); } catch (IOException ex) { return ""; }
    }

    public static String sha256(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[65536];
            int r;
            while ((r = in.read(buf)) > 0) md.update(buf, 0, r);
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }
}
