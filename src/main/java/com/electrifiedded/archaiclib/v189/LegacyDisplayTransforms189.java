package com.electrifiedded.archaiclib.v189;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Translates 1.8.x item display transforms ("thirdperson", "firstperson", "gui", "head")
 * into the 1.9+ / 1.12.2 format ("thirdperson_righthand", "firstperson_righthand", ...).
 *
 * <p>The numbers can not be copied: 1.8 applies rotations in Y,X,Z order (1.12: X,Y,Z), clamps
 * translation to +-1.5 blocks, and renders hands / GUI with different base matrices. The base
 * matrices are not stored anywhere, so they are recovered from Mojang's own 1.8 -> 1.9 conversion
 * of the vanilla defaults: for every view and model kind {@code P112^-1 * P18 = D112 * D18^-1},
 * hence a custom 1.8 transform {@code O18} becomes {@code D112 * D18^-1 * O18}. A model using the
 * plain 1.8 defaults therefore gets exactly the 1.12 defaults.</p>
 *
 * <p>Pure Java + Gson, safe to call from a coremod / converter before Minecraft classes load.</p>
 */
public final class LegacyDisplayTransforms189 {

    /** Model kinds that have distinct vanilla defaults. */
    public enum Kind {
        GENERATED("item/generated"),
        HANDHELD("item/handheld"),
        ROD("item/handheld_rod"),
        BOW("item/generated"),
        BLOCK(null);

        /** 1.12 parent replacing {@code builtin/generated} for this kind, null for blocks. */
        public final String itemParent;

        Kind(String itemParent) {
            this.itemParent = itemParent;
        }

        public boolean isItem() {
            return this != BLOCK;
        }
    }

    /** rotation(3) translation(3, json units = 1/16 block) scale(3). */
    static final class Vec {
        final double[] r, t, s;

        Vec(double[] r, double[] t, double[] s) {
            this.r = r;
            this.t = t;
            this.s = s;
        }

        static Vec of(double rx, double ry, double rz, double tx, double ty, double tz, double sc) {
            return new Vec(new double[]{rx, ry, rz}, new double[]{tx, ty, tz}, new double[]{sc, sc, sc});
        }
    }

    private static final Vec IDENTITY = Vec.of(0, 0, 0, 0, 0, 0, 1);

    // 1.8.9 vanilla defaults (item/apple, item/diamond_sword, item/fishing_rod, item/bow, item/*_stairs).
    private static final Vec TP18_GENERATED = Vec.of(-90, 0, 0, 0, 1, -3, 0.55);
    private static final Vec TP18_HANDHELD = Vec.of(0, 90, -35, 0, 1.25, -3.5, 0.85);
    private static final Vec TP18_ROD = Vec.of(180, 90, -35, 0, 0, -3.5, 0.85);
    private static final Vec TP18_BOW = Vec.of(5, 80, -45, 0.75, 0, 0.25, 1);
    private static final Vec TP18_BLOCK = Vec.of(10, -45, 170, 0, 1.5, -2.75, 0.375);
    private static final Vec FP18_ITEM = Vec.of(0, -135, 25, 0, 4, 2, 1.7);
    private static final Vec FP18_ROD = Vec.of(0, 45, 25, 0, 4, 2, 1.7);

    // 1.12.2 vanilla defaults (item/generated, item/handheld, item/handheld_rod, item/bow, block/block).
    private static final Vec TP112_GENERATED = Vec.of(0, 0, 0, 0, 3, 1, 0.55);
    private static final Vec TP112_HANDHELD = Vec.of(0, -90, 55, 0, 4, 0.5, 0.85);
    private static final Vec TP112_ROD = Vec.of(0, 90, 55, 0, 4, 2.5, 0.85);
    private static final Vec TP112_BOW = Vec.of(-80, 260, -40, -1, -2, 2.5, 0.9);
    private static final Vec TP112_BLOCK = Vec.of(75, 45, 0, 0, 2.5, 0, 0.375);
    private static final Vec FP112_ITEM = Vec.of(0, -90, 25, 1.13, 3.2, 1.13, 0.68);
    private static final Vec FP112_ROD = Vec.of(0, 90, 25, 0, 1.6, 0.8, 0.68);
    private static final Vec FP112_BLOCK = Vec.of(0, 45, 0, 0, 0, 0, 0.4);
    private static final Vec GUI112_BLOCK = Vec.of(30, 225, 0, 0, 0, 0, 0.625);

    private LegacyDisplayTransforms189() {}

    /** True when the display object still uses 1.8 keys. */
    public static boolean isLegacy(JsonObject display) {
        return display != null && (display.has("thirdperson") || display.has("firstperson"));
    }

    /** Picks the item kind whose vanilla 1.8 defaults are closest to the given display. */
    public static Kind guessItemKind(JsonObject display) {
        if (display == null) return Kind.GENERATED;
        Vec tp = read18(display.get("thirdperson"));
        if (tp != null) {
            Kind best = Kind.GENERATED;
            double bestD = distance(tp, TP18_GENERATED);
            double d;
            if ((d = distance(tp, TP18_HANDHELD)) < bestD) { bestD = d; best = Kind.HANDHELD; }
            if ((d = distance(tp, TP18_ROD)) < bestD) { bestD = d; best = Kind.ROD; }
            if ((d = distance(tp, TP18_BOW)) < bestD) { best = Kind.BOW; }
            return best;
        }
        Vec fp = read18(display.get("firstperson"));
        if (fp != null && distance(fp, FP18_ROD) < distance(fp, FP18_ITEM)) return Kind.ROD;
        return Kind.GENERATED;
    }

    /**
     * Rewrites a 1.8 display object in place. Returns true if anything changed.
     * Slots the model does not define are left to the (1.12) parent, exactly like 1.8 inheritance.
     */
    public static boolean convert(JsonObject display, Kind kind) {
        if (!isLegacy(display) && !display.has("gui")) return false;
        boolean legacy = isLegacy(display);
        boolean changed = false;
        JsonElement tp = display.remove("thirdperson");
        if (tp != null) {
            Vec o = read18(tp);
            if (o != null && !display.has("thirdperson_righthand")) {
                Vec right = apply(tp18(kind), tp112(kind), o);
                display.add("thirdperson_righthand", write(right));
                // 1.8 had no off-hand: derive the left hand from the right one (mirrored like vanilla 1.12 models),
                // otherwise the 1.12 parent's left-hand transform (made for flat items) is applied to the 3D model.
                if (!display.has("thirdperson_lefthand")) display.add("thirdperson_lefthand", write(mirror(right)));
            }
            changed = true;
        }
        JsonElement fp = display.remove("firstperson");
        if (fp != null) {
            Vec o = read18(fp);
            if (o != null && !display.has("firstperson_righthand")) {
                Vec right = apply(fp18(kind), fp112(kind), o);
                display.add("firstperson_righthand", write(right));
                // 1.8 had no off-hand: derive the left hand from the right one (mirrored like vanilla 1.12 models),
                // otherwise the 1.12 parent's left-hand transform (made for flat items) is applied to the 3D model.
                if (!display.has("firstperson_lefthand")) display.add("firstperson_lefthand", write(mirror(right)));
            }
            changed = true;
        }
        // 1.8 rendered 3D models in the GUI through a hard-coded rotation; 1.9 moved it into block/block "gui".
        if (legacy && kind == Kind.BLOCK && display.has("gui")) {
            Vec o = read18(display.get("gui"));
            if (o != null) {
                display.add("gui", write(apply(IDENTITY, GUI112_BLOCK, o)));
                changed = true;
            }
        }
        return changed;
    }

    /** The 1.12 block/block display, for 1.8 root block models that have no parent at all. */
    public static void addBlockDefaults(JsonObject display) {
        putIfAbsent(display, "gui", GUI112_BLOCK);
        putIfAbsent(display, "ground", Vec.of(0, 0, 0, 0, 3, 0, 0.25));
        putIfAbsent(display, "fixed", Vec.of(0, 0, 0, 0, 0, 0, 0.5));
        putIfAbsent(display, "thirdperson_righthand", TP112_BLOCK);
        putIfAbsent(display, "firstperson_righthand", FP112_BLOCK);
        putIfAbsent(display, "firstperson_lefthand", Vec.of(0, 225, 0, 0, 0, 0, 0.4));
    }

    /** Left-hand variant of a right-hand transform (vanilla convention: rotation Y and Z negated). */
    static Vec mirror(Vec v) {
        return new Vec(new double[]{v.r[0], -v.r[1], -v.r[2]}, v.t.clone(), v.s.clone());
    }

    private static void putIfAbsent(JsonObject display, String key, Vec v) {
        if (!display.has(key)) display.add(key, write(v));
    }

    private static Vec tp18(Kind k) {
        switch (k) {
            case HANDHELD: return TP18_HANDHELD;
            case ROD: return TP18_ROD;
            case BOW: return TP18_BOW;
            case BLOCK: return TP18_BLOCK;
            default: return TP18_GENERATED;
        }
    }

    private static Vec tp112(Kind k) {
        switch (k) {
            case HANDHELD: return TP112_HANDHELD;
            case ROD: return TP112_ROD;
            case BOW: return TP112_BOW;
            case BLOCK: return TP112_BLOCK;
            default: return TP112_GENERATED;
        }
    }

    private static Vec fp18(Kind k) {
        if (k == Kind.BLOCK) return IDENTITY;
        return k == Kind.ROD ? FP18_ROD : FP18_ITEM;
    }

    private static Vec fp112(Kind k) {
        if (k == Kind.BLOCK) return FP112_BLOCK;
        return k == Kind.ROD ? FP112_ROD : FP112_ITEM;
    }

    // ---------------------------------------------------------------- math

    /** X = D112 * D18^-1 * O18, decomposed back into a 1.12 transform. */
    static Vec apply(Vec d18, Vec d112, Vec o18) {
        if (distance(o18, d18) < 1e-4) return d112;
        double[] m = mul(mul(matrix112(d112), invert(matrix18(d18))), matrix18(o18));
        return decompose(m);
    }

    /** 1.8.9 ItemCameraTransforms.applyTransform: translate, rotate Y, X, Z, scale. */
    static double[] matrix18(Vec v) {
        double[] m = translation(v.t);
        m = mul(m, rot(1, v.r[1]));
        m = mul(m, rot(0, v.r[0]));
        m = mul(m, rot(2, v.r[2]));
        return mul(m, scale(v.s));
    }

    /** 1.12.2 ItemCameraTransforms.applyTransformSide: translate, quaternion(X*Y*Z), scale. */
    static double[] matrix112(Vec v) {
        double[] m = translation(v.t);
        m = mul(m, rot(0, v.r[0]));
        m = mul(m, rot(1, v.r[1]));
        m = mul(m, rot(2, v.r[2]));
        return mul(m, scale(v.s));
    }

    static Vec decompose(double[] m) {
        double[] t = {m[3] * 16, m[7] * 16, m[11] * 16};
        double[][] c = new double[3][3]; // columns
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) c[i][j] = m[j * 4 + i];
        double[] s = new double[3];
        // Gram-Schmidt keeps R orthonormal even if the product has a little shear.
        s[0] = norm(c[0]);
        scaleVec(c[0], 1 / nz(s[0]));
        subProj(c[1], c[0]);
        s[1] = norm(c[1]);
        scaleVec(c[1], 1 / nz(s[1]));
        subProj(c[2], c[0]);
        subProj(c[2], c[1]);
        s[2] = norm(c[2]);
        scaleVec(c[2], 1 / nz(s[2]));
        double det = c[0][0] * (c[1][1] * c[2][2] - c[2][1] * c[1][2])
                - c[1][0] * (c[0][1] * c[2][2] - c[2][1] * c[0][2])
                + c[2][0] * (c[0][1] * c[1][2] - c[1][1] * c[0][2]);
        if (det < 0) {
            s[0] = -s[0];
            scaleVec(c[0], -1);
        }
        // R = Rx(a) Ry(b) Rz(c); r[row][col] = c[col][row]
        double r02 = c[2][0], r12 = c[2][1], r22 = c[2][2], r01 = c[1][0], r00 = c[0][0];
        double a, b, cc;
        b = Math.asin(Math.max(-1, Math.min(1, r02)));
        if (Math.abs(r02) < 0.99999) {
            a = Math.atan2(-r12, r22);
            cc = Math.atan2(-r01, r00);
        } else {
            a = Math.atan2(c[1][2], c[1][1]);
            cc = 0;
        }
        double[] r = {Math.toDegrees(a), Math.toDegrees(b), Math.toDegrees(cc)};
        for (int i = 0; i < 3; i++) {
            r[i] = round(normAngle(r[i]));
            t[i] = round(clamp(t[i], 80));
            s[i] = round(clamp(s[i], 4));
        }
        return new Vec(r, t, s);
    }

    private static double distance(Vec a, Vec b) {
        double d = 0;
        for (int i = 0; i < 3; i++) {
            d += Math.abs(normAngle(a.r[i] - b.r[i]));
            d += 10 * Math.abs(a.t[i] - b.t[i]);
            d += 50 * Math.abs(a.s[i] - b.s[i]);
        }
        return d;
    }

    private static double[] translation(double[] t) {
        double[] m = identity();
        m[3] = t[0] / 16;
        m[7] = t[1] / 16;
        m[11] = t[2] / 16;
        return m;
    }

    private static double[] scale(double[] s) {
        double[] m = identity();
        m[0] = s[0];
        m[5] = s[1];
        m[10] = s[2];
        return m;
    }

    private static double[] rot(int axis, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        double[] m = identity();
        if (axis == 0) { m[5] = c; m[6] = -s; m[9] = s; m[10] = c; }
        else if (axis == 1) { m[0] = c; m[2] = s; m[8] = -s; m[10] = c; }
        else { m[0] = c; m[1] = -s; m[4] = s; m[5] = c; }
        return m;
    }

    private static double[] identity() {
        return new double[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    private static double[] mul(double[] a, double[] b) {
        double[] r = new double[16];
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
                double v = 0;
                for (int k = 0; k < 4; k++) v += a[i * 4 + k] * b[k * 4 + j];
                r[i * 4 + j] = v;
            }
        return r;
    }

    /** Inverse of an affine matrix (3x3 linear part + translation). */
    private static double[] invert(double[] m) {
        double a = m[0], b = m[1], c = m[2], d = m[4], e = m[5], f = m[6], g = m[8], h = m[9], i = m[10];
        double A = e * i - f * h, B = -(d * i - f * g), C = d * h - e * g;
        double det = a * A + b * B + c * C;
        double inv = 1 / nz(det);
        double[] r = identity();
        r[0] = A * inv;
        r[1] = -(b * i - c * h) * inv;
        r[2] = (b * f - c * e) * inv;
        r[4] = B * inv;
        r[5] = (a * i - c * g) * inv;
        r[6] = -(a * f - c * d) * inv;
        r[8] = C * inv;
        r[9] = -(a * h - b * g) * inv;
        r[10] = (a * e - b * d) * inv;
        double tx = m[3], ty = m[7], tz = m[11];
        r[3] = -(r[0] * tx + r[1] * ty + r[2] * tz);
        r[7] = -(r[4] * tx + r[5] * ty + r[6] * tz);
        r[11] = -(r[8] * tx + r[9] * ty + r[10] * tz);
        return r;
    }

    private static double norm(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static void scaleVec(double[] v, double k) {
        v[0] *= k;
        v[1] *= k;
        v[2] *= k;
    }

    private static void subProj(double[] v, double[] unit) {
        double d = v[0] * unit[0] + v[1] * unit[1] + v[2] * unit[2];
        v[0] -= d * unit[0];
        v[1] -= d * unit[1];
        v[2] -= d * unit[2];
    }

    private static double nz(double v) {
        return Math.abs(v) < 1e-12 ? 1e-12 : v;
    }

    private static double normAngle(double a) {
        a = a % 360;
        if (a > 180) a -= 360;
        if (a <= -180) a += 360;
        return a;
    }

    private static double clamp(double v, double lim) {
        return Math.max(-lim, Math.min(lim, v));
    }

    private static double round(double v) {
        double r = Math.round(v * 1000) / 1000.0;
        return r == -0.0 ? 0.0 : r;
    }

    // ---------------------------------------------------------------- json

    /** Reads a 1.8 transform with 1.8 clamping (translation +-24, scale +-4). */
    static Vec read18(JsonElement e) {
        if (e == null || !e.isJsonObject()) return null;
        JsonObject o = e.getAsJsonObject();
        double[] r = vec(o.get("rotation"), 0);
        double[] t = vec(o.get("translation"), 0);
        double[] s = vec(o.get("scale"), 1);
        for (int i = 0; i < 3; i++) {
            t[i] = clamp(t[i], 24);
            s[i] = clamp(s[i], 4);
        }
        return new Vec(r, t, s);
    }

    private static double[] vec(JsonElement e, double def) {
        double[] v = {def, def, def};
        if (e == null || !e.isJsonArray()) return v;
        JsonArray a = e.getAsJsonArray();
        for (int i = 0; i < 3 && i < a.size(); i++) {
            try {
                v[i] = a.get(i).getAsDouble();
            } catch (Throwable ignored) {
            }
        }
        return v;
    }

    static JsonObject write(Vec v) {
        JsonObject o = new JsonObject();
        o.add("rotation", arr(v.r));
        o.add("translation", arr(v.t));
        o.add("scale", arr(v.s));
        return o;
    }

    private static JsonArray arr(double[] v) {
        JsonArray a = new JsonArray();
        for (double d : v) a.add(d);
        return a;
    }
}
