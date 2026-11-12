package com.electrifiedded.archaiclib.v189.engine;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.EnumFacing.Axis;
import org.lwjgl.util.vector.Vector3f;

public class BlockPart {
   public final Vector3f positionFrom;
   public final Vector3f positionTo;
   public final Map<EnumFacing, BlockPartFace> mapFaces;
   public final BlockPartRotation partRotation;
   public final boolean shade;

   public BlockPart(Vector3f var1, Vector3f var2, Map<EnumFacing, BlockPartFace> var3, BlockPartRotation var4, boolean var5) {
      this.positionFrom = var1;
      this.positionTo = var2;
      this.mapFaces = var3;
      this.partRotation = var4;
      this.shade = var5;
      this.setDefaultUvs();
   }

   private void setDefaultUvs() {
      for (Entry entry : this.mapFaces.entrySet()) {
         float[] afloat = this.getFaceUvs((EnumFacing)entry.getKey());
         ((BlockPartFace)entry.getValue()).blockFaceUV.setUvs(afloat);
      }
   }

   private float[] getFaceUvs(EnumFacing var1) {
      float[] afloat;
      switch (var1) {
         case DOWN:
         case UP:
            afloat = new float[]{this.positionFrom.x, this.positionFrom.z, this.positionTo.x, this.positionTo.z};
            break;
         case NORTH:
         case SOUTH:
            afloat = new float[]{this.positionFrom.x, 16.0F - this.positionTo.y, this.positionTo.x, 16.0F - this.positionFrom.y};
            break;
         case WEST:
         case EAST:
            afloat = new float[]{this.positionFrom.z, 16.0F - this.positionTo.y, this.positionTo.z, 16.0F - this.positionFrom.y};
            break;
         default:
            throw new NullPointerException();
      }

      return afloat;
   }

   static class Deserializer implements JsonDeserializer<BlockPart> {
      public BlockPart deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         JsonObject jsonobject = var1.getAsJsonObject();
         Vector3f vector3f = this.parsePositionFrom(jsonobject);
         Vector3f vector3f1 = this.parsePositionTo(jsonobject);
         BlockPartRotation blockpartrotation = this.parseRotation(jsonobject);
         Map map = this.parseFacesCheck(var3, jsonobject);
         if (jsonobject.has("shade") && !JsonUtils.isBoolean(jsonobject, "shade")) {
            throw new JsonParseException("Expected shade to be a Boolean");
         } else {
            boolean flag = JsonUtils.getBoolean(jsonobject, "shade", true);
            return new BlockPart(vector3f, vector3f1, map, blockpartrotation, flag);
         }
      }

      private BlockPartRotation parseRotation(JsonObject var1) {
         BlockPartRotation blockpartrotation = null;
         if (var1.has("rotation")) {
            JsonObject jsonobject = JsonUtils.getJsonObject(var1, "rotation");
            Vector3f vector3f = this.parsePosition(jsonobject, "origin");
            vector3f.scale(0.0625F);
            Axis enumfacing$axis = this.parseAxis(jsonobject);
            float f = this.parseAngle(jsonobject);
            boolean flag = JsonUtils.getBoolean(jsonobject, "rescale", false);
            blockpartrotation = new BlockPartRotation(vector3f, enumfacing$axis, f, flag);
         }

         return blockpartrotation;
      }

      private float parseAngle(JsonObject var1) {
         float f = JsonUtils.getFloat(var1, "angle");
         if (f != 0.0F && MathHelper.abs(f) != 22.5F && MathHelper.abs(f) != 45.0F) {
            throw new JsonParseException("Invalid rotation " + f + " found, only -45/-22.5/0/22.5/45 allowed");
         } else {
            return f;
         }
      }

      private Axis parseAxis(JsonObject var1) {
         String s = JsonUtils.getString(var1, "axis");
         Axis enumfacing$axis = Axis.byName(s.toLowerCase());
         if (enumfacing$axis == null) {
            throw new JsonParseException("Invalid rotation axis: " + s);
         } else {
            return enumfacing$axis;
         }
      }

      private Map<EnumFacing, BlockPartFace> parseFacesCheck(JsonDeserializationContext var1, JsonObject var2) {
         Map map = this.parseFaces(var1, var2);
         if (map.isEmpty()) {
            throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
         } else {
            return map;
         }
      }

      private Map<EnumFacing, BlockPartFace> parseFaces(JsonDeserializationContext var1, JsonObject var2) {
         EnumMap enummap = Maps.newEnumMap(EnumFacing.class);
         JsonObject jsonobject = JsonUtils.getJsonObject(var2, "faces");

         for (Entry entry : jsonobject.entrySet()) {
            EnumFacing enumfacing = this.parseEnumFacing((String)entry.getKey());
            enummap.put(enumfacing, (BlockPartFace)var1.deserialize((JsonElement)entry.getValue(), BlockPartFace.class));
         }

         return enummap;
      }

      private EnumFacing parseEnumFacing(String var1) {
         EnumFacing enumfacing = EnumFacing.byName(var1);
         if (enumfacing == null) {
            throw new JsonParseException("Unknown facing: " + var1);
         } else {
            return enumfacing;
         }
      }

      private Vector3f parsePositionTo(JsonObject var1) {
         Vector3f vector3f = this.parsePosition(var1, "to");
         if (!(vector3f.x < -16.0F)
            && !(vector3f.y < -16.0F)
            && !(vector3f.z < -16.0F)
            && !(vector3f.x > 32.0F)
            && !(vector3f.y > 32.0F)
            && !(vector3f.z > 32.0F)) {
            return vector3f;
         } else {
            throw new JsonParseException("'to' specifier exceeds the allowed boundaries: " + vector3f);
         }
      }

      private Vector3f parsePositionFrom(JsonObject var1) {
         Vector3f vector3f = this.parsePosition(var1, "from");
         if (!(vector3f.x < -16.0F)
            && !(vector3f.y < -16.0F)
            && !(vector3f.z < -16.0F)
            && !(vector3f.x > 32.0F)
            && !(vector3f.y > 32.0F)
            && !(vector3f.z > 32.0F)) {
            return vector3f;
         } else {
            throw new JsonParseException("'from' specifier exceeds the allowed boundaries: " + vector3f);
         }
      }

      private Vector3f parsePosition(JsonObject var1, String var2) {
         JsonArray jsonarray = JsonUtils.getJsonArray(var1, var2);
         if (jsonarray.size() != 3) {
            throw new JsonParseException("Expected 3 " + var2 + " values, found: " + jsonarray.size());
         } else {
            float[] afloat = new float[3];

            for (int i = 0; i < afloat.length; i++) {
               afloat[i] = JsonUtils.getFloat(jsonarray.get(i), var2 + "[" + i + "]");
            }

            return new Vector3f(afloat[0], afloat[1], afloat[2]);
         }
      }
   }
}
