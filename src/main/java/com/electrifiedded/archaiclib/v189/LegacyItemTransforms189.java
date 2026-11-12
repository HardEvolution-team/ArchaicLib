package com.electrifiedded.archaiclib.v189;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.vecmath.Matrix4f;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Port of the Minecraft 1.8.9 item transform pipeline (ItemRenderer.transformFirstPersonItem/renderItem,
 * LayerHeldItem, RenderItem.preTransform/renderItemModelTransform/setupGuiTransform/renderItem,
 * ItemCameraTransforms.applyTransform) for models that came from 1.8 resources.
 * <p>
 * The raw 1.8 display numbers are applied in the 1.8 order and the difference between the 1.12 and
 * 1.8 render anchors is folded into one exact matrix {@code X = A112^-1 * A18 * D18 * S(0.5)},
 * returned through Forge's handlePerspective. No translation of numbers, no 1.12 clamps.
 * Left hand: Forge mirrors the matrix (flipX * X * flipX), an exact mirror of the 1.8 right hand.
 */
@SideOnly(Side.CLIENT)
public final class LegacyItemTransforms189 {

    /** JSON key written by {@link LegacyModelConverter189}: {"display": original 1.8 display}. */
    public static final String MARKER = "archaiclib_legacy189";

    private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger("ArchaicLib");
    private static Map<String, String> vanillaParents;
    private static Map<String, JsonObject> vanillaDisplay;

    private LegacyItemTransforms189() {}

    /** 1.8 ItemTransformVec3f, with the 1.8 deserializer clamps. */
    public static final class Vec {
        final float rx, ry, rz, tx, ty, tz, sx, sy, sz;

        Vec(float[] r, float[] t, float[] s) {
            rx = r[0]; ry = r[1]; rz = r[2];
            tx = clamp(t[0] * 0.0625F, 1.5F); ty = clamp(t[1] * 0.0625F, 1.5F); tz = clamp(t[2] * 0.0625F, 1.5F);
            sx = clamp(s[0], 4F); sy = clamp(s[1], 4F); sz = clamp(s[2], 4F);
        }

        private Vec(Vec o, float tx, float ty, float tz) {
            rx = o.rx; ry = o.ry; rz = o.rz; sx = o.sx; sy = o.sy; sz = o.sz;
            this.tx = tx; this.ty = ty; this.tz = tz;
        }

        /** Translation already in blocks (from an engine transform). */
        Vec withTranslation(float x, float y, float z) {
            return new Vec(this, x, y, z);
        }

        /** 1.8 applyTransform order: T * Ry * Rx * Rz * S. */
        Matrix4f matrix() {
            return mul(translate(tx, ty, tz), rot(ry, 1), rot(rx, 0), rot(rz, 2), scale(sx, sy, sz));
        }
    }

    /** Resolved 1.8 transforms of one model (null entry = 1.8 DEFAULT = identity). */
    public static final class Resolved {
        public final Vec thirdPerson, firstPerson, gui;

        Resolved(Vec tp, Vec fp, Vec gui) {
            this.thirdPerson = tp;
            this.firstPerson = fp;
            this.gui = gui;
        }
    }

    public static IBakedModel wrap(ModelBlock model, IBakedModel baked, ResourceLocation location, boolean identityState) {
        if (model == null || baked == null || baked instanceof LegacyBakedModel189) return baked;
        try {
            Resolved r = resolve(model);
            return r == null ? baked : new LegacyBakedModel189(baked, r, location, identityState);
        } catch (Throwable t) {
            LOG.warn("[ArchaicLib] legacy 1.8 transform resolve failed for {}: {}", model.name, t.toString());
            return baked;
        }
    }

    /** Null unless some model in the chain came from 1.8 resources. */
    public static Resolved resolve(ModelBlock leaf) {
        boolean legacy = false;
        for (ModelBlock m = leaf; m != null; m = m.parent) {
            if (((ILegacyModelBlock189) m).archaiclib$getLegacyDisplay() != null) { legacy = true; break; }
        }
        if (!legacy) return null;
        return new Resolved(find(leaf, "thirdperson"), find(leaf, "firstperson"), find(leaf, "gui"));
    }

    /** Like 1.8 ModelBlock.getTransform: per type, the first model in the chain with a custom value wins. */
    private static Vec find(ModelBlock leaf, String key) {
        String name = null;
        for (ModelBlock m = leaf; m != null; m = m.parent) {
            JsonObject own = ((ILegacyModelBlock189) m).archaiclib$getLegacyDisplay();
            if (own != null) {
                JsonElement display = own.get("display");
                Vec v = display != null && display.isJsonObject() ? parse(display.getAsJsonObject().get(key)) : null;
                if (v != null) return v;
            } else if (name != null) {
                // a vanilla parent: use what that model had in 1.8.9, not its 1.12 numbers
                Vec v = vanilla(name, key);
                if (v != null) return v;
            }
            ResourceLocation p = m.getParentLocation();
            name = p == null ? null : p.toString();
        }
        return null;
    }

    private static Vec vanilla(String name, String key) {
        String n = name;
        if (n.startsWith("minecraft:")) n = n.substring("minecraft:".length());
        else if (n.startsWith("archaiclib:legacy189/")) n = n.substring("archaiclib:legacy189/".length());
        else if (n.indexOf(':') >= 0) return null;
        loadVanilla();
        for (int guard = 0; n != null && guard < 32; guard++) {
            JsonObject d = vanillaDisplay.get(n);
            if (d != null) {
                Vec v = parse(d.get(key));
                if (v != null) return v;
            }
            n = vanillaParents.get(n);
        }
        return null;
    }

    private static synchronized void loadVanilla() {
        if (vanillaDisplay != null) return;
        Map<String, String> parents = new HashMap<>();
        Map<String, JsonObject> display = new HashMap<>();
        try (InputStream in = LegacyItemTransforms189.class.getResourceAsStream("/assets/archaiclib/legacy189/vanilla_display.json")) {
            if (in != null) {
                JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("parents").entrySet()) {
                    String p = e.getValue().getAsString();
                    parents.put(e.getKey(), p.startsWith("minecraft:") ? p.substring(10) : p);
                }
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("display").entrySet()) {
                    display.put(e.getKey(), e.getValue().getAsJsonObject());
                }
            }
        } catch (Throwable t) {
            LOG.warn("[ArchaicLib] cannot read 1.8.9 vanilla display table: {}", t.toString());
        }
        vanillaParents = parents;
        vanillaDisplay = display;
    }

    static Vec parse(JsonElement e) {
        if (e == null || !e.isJsonObject()) return null;
        JsonObject o = e.getAsJsonObject();
        return new Vec(vec(o, "rotation", 0F), vec(o, "translation", 0F), vec(o, "scale", 1F));
    }

    private static float[] vec(JsonObject o, String key, float def) {
        float[] out = {def, def, def};
        JsonElement e = o.get(key);
        if (e != null && e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            for (int i = 0; i < 3 && i < a.size(); i++) {
                try { out[i] = a.get(i).getAsFloat(); } catch (RuntimeException ignored) {}
            }
        }
        return out;
    }

    /** Display of a vanilla 1.8.9 model as written in its json (null if it had none). */
    public static JsonObject vanillaDisplay(String path) {
        loadVanilla();
        JsonObject d = vanillaDisplay.get(path);
        return d == null ? null : d.deepCopy();
    }

    /** Matrix for callers on the 1.12 camera-transform path (dropped items, other mods) with a 1.8 engine model. */
    public static Matrix4f engineMatrix(com.electrifiedded.archaiclib.v189.engine.LegacyModelEngine189.Model m, TransformType type) {
        com.electrifiedded.archaiclib.v189.engine.ItemCameraTransforms t = m.transforms;
        switch (type) {
            case GROUND: return mul(toVec(t.ground).matrix(), scale(0.5F, 0.5F, 0.5F));
            case FIXED: return mul(toVec(t.fixed).matrix(), scale(0.5F, 0.5F, 0.5F));
            case HEAD: return mul(toVec(t.head).matrix(), scale(0.5F, 0.5F, 0.5F));
            default:
                return matrix(new Resolved(toVec(t.thirdPerson), toVec(t.firstPerson), toVec(t.gui)), type, m.isGui3d());
        }
    }

    private static Vec toVec(com.electrifiedded.archaiclib.v189.engine.ItemTransformVec3f v) {
        Vec out = new Vec(new float[]{v.rotation.x, v.rotation.y, v.rotation.z}, new float[]{0, 0, 0},
                new float[]{v.scale.x, v.scale.y, v.scale.z});
        return out.withTranslation(v.translation.x, v.translation.y, v.translation.z);
    }

    // ---- matrices ----------------------------------------------------------------------------

    /** Matrix replacing the 1.12 camera transform for the given type, or null to keep the 1.12 path. */
    public static Matrix4f matrix(Resolved r, TransformType type, boolean gui3d) {
        switch (type) {
            case FIRST_PERSON_RIGHT_HAND:
            case FIRST_PERSON_LEFT_HAND:
                // 1.12 anchor = 1.8 anchor * Ry(-45); 1.8 then scales 0.4 (transformFirstPersonItem) * 2 (renderItem).
                return mul(rot(45F, 1), scale(0.8F, 0.8F, 0.8F), d(r.firstPerson), scale(0.5F, 0.5F, 0.5F));
            case THIRD_PERSON_RIGHT_HAND:
            case THIRD_PERSON_LEFT_HAND:
                // undo 1.12 LayerHeldItem (Rx(-90) Ry(180) T(1/16, 2/16, -10/16)), redo 1.8 LayerHeldItem + scale 2.
                return mul(translate(-0.0625F, -0.125F, 0.625F), rot(-180F, 1), rot(90F, 0),
                        translate(-0.0625F, 0.4375F, 0.0625F), scale(2F, 2F, 2F), d(r.thirdPerson), scale(0.5F, 0.5F, 0.5F));
            case GUI:
                // undo 1.12 setupGuiTransform (scale(1,-1,1) * 16), redo the 1.8 one.
                Matrix4f base = gui3d
                        ? mul(scale(40F, 40F, 40F), rot(210F, 0), rot(-135F, 1))
                        : mul(scale(64F, 64F, 64F), rot(180F, 0));
                return mul(scale(1F / 16F, -1F / 16F, 1F / 16F), scale(0.5F, 0.5F, -0.5F), base, d(r.gui), scale(0.5F, 0.5F, 0.5F));
            default:
                return null;
        }
    }

    private static Matrix4f d(Vec v) {
        return v == null ? identity() : v.matrix();
    }

    private static float clamp(float v, float lim) {
        return v < -lim ? -lim : (v > lim ? lim : v);
    }

    private static Matrix4f identity() {
        Matrix4f m = new Matrix4f();
        m.setIdentity();
        return m;
    }

    private static Matrix4f translate(float x, float y, float z) {
        Matrix4f m = identity();
        m.m03 = x; m.m13 = y; m.m23 = z;
        return m;
    }

    private static Matrix4f scale(float x, float y, float z) {
        Matrix4f m = identity();
        m.m00 = x; m.m11 = y; m.m22 = z;
        return m;
    }

    /** GL-style rotation in degrees, axis 0=x 1=y 2=z. */
    private static Matrix4f rot(float deg, int axis) {
        Matrix4f m = identity();
        if (deg == 0F) return m;
        float a = (float) Math.toRadians(deg);
        if (axis == 0) m.rotX(a);
        else if (axis == 1) m.rotY(a);
        else m.rotZ(a);
        return m;
    }

    private static Matrix4f mul(Matrix4f... ms) {
        Matrix4f r = new Matrix4f(ms[0]);
        for (int i = 1; i < ms.length; i++) r.mul(ms[i]);
        return r;
    }
}
