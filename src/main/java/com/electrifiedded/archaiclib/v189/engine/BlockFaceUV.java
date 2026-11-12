package com.electrifiedded.archaiclib.v189.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.minecraft.util.JsonUtils;

public class BlockFaceUV {
   public float[] uvs;
   public final int rotation;

   public BlockFaceUV(float[] var1, int var2) {
      this.uvs = var1;
      this.rotation = var2;
   }

   public float func_178348_a(int var1) {
      if (this.uvs == null) {
         throw new NullPointerException("uvs");
      } else {
         int i = this.func_178347_d(var1);
         return i != 0 && i != 1 ? this.uvs[2] : this.uvs[0];
      }
   }

   public float func_178346_b(int var1) {
      if (this.uvs == null) {
         throw new NullPointerException("uvs");
      } else {
         int i = this.func_178347_d(var1);
         return i != 0 && i != 3 ? this.uvs[3] : this.uvs[1];
      }
   }

   private int func_178347_d(int var1) {
      return (var1 + this.rotation / 90) % 4;
   }

   public int func_178345_c(int var1) {
      return (var1 + (4 - this.rotation / 90)) % 4;
   }

   public void setUvs(float[] var1) {
      if (this.uvs == null) {
         this.uvs = var1;
      }
   }

   static class Deserializer implements JsonDeserializer<BlockFaceUV> {
      public BlockFaceUV deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         JsonObject jsonobject = var1.getAsJsonObject();
         float[] afloat = this.parseUV(jsonobject);
         int i = this.parseRotation(jsonobject);
         return new BlockFaceUV(afloat, i);
      }

      protected int parseRotation(JsonObject var1) {
         int i = JsonUtils.getInt(var1, "rotation", 0);
         if (i >= 0 && i % 90 == 0 && i / 90 <= 3) {
            return i;
         } else {
            throw new JsonParseException("Invalid rotation " + i + " found, only 0/90/180/270 allowed");
         }
      }

      private float[] parseUV(JsonObject var1) {
         if (!var1.has("uv")) {
            return null;
         } else {
            JsonArray jsonarray = JsonUtils.getJsonArray(var1, "uv");
            if (jsonarray.size() != 4) {
               throw new JsonParseException("Expected 4 uv values, found: " + jsonarray.size());
            } else {
               float[] afloat = new float[4];

               for (int i = 0; i < afloat.length; i++) {
                  afloat[i] = JsonUtils.getFloat(jsonarray.get(i), "uv[" + i + "]");
               }

               return afloat;
            }
         }
      }
   }
}
