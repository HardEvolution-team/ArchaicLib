package com.electrifiedded.archaiclib.v189.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.util.vector.Vector3f;

public class ItemTransformVec3f {
   public static final ItemTransformVec3f DEFAULT = new ItemTransformVec3f(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));
   public final Vector3f rotation;
   public final Vector3f translation;
   public final Vector3f scale;

   public ItemTransformVec3f(Vector3f var1, Vector3f var2, Vector3f var3) {
      this.rotation = new Vector3f(var1);
      this.translation = new Vector3f(var2);
      this.scale = new Vector3f(var3);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (this.getClass() != var1.getClass()) {
         return false;
      } else {
         ItemTransformVec3f itemtransformvec3f = (ItemTransformVec3f)var1;
         if (!this.rotation.equals(itemtransformvec3f.rotation)) {
            return false;
         } else {
            return !this.scale.equals(itemtransformvec3f.scale) ? false : this.translation.equals(itemtransformvec3f.translation);
         }
      }
   }

   @Override
   public int hashCode() {
      int i = this.rotation.hashCode();
      i = 31 * i + this.translation.hashCode();
      return 31 * i + this.scale.hashCode();
   }

   static class Deserializer implements JsonDeserializer<ItemTransformVec3f> {
      private static final Vector3f ROTATION_DEFAULT = new Vector3f(0.0F, 0.0F, 0.0F);
      private static final Vector3f TRANSLATION_DEFAULT = new Vector3f(0.0F, 0.0F, 0.0F);
      private static final Vector3f SCALE_DEFAULT = new Vector3f(1.0F, 1.0F, 1.0F);

      public ItemTransformVec3f deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         JsonObject jsonobject = var1.getAsJsonObject();
         Vector3f vector3f = this.parseVector3f(jsonobject, "rotation", ROTATION_DEFAULT);
         Vector3f vector3f1 = this.parseVector3f(jsonobject, "translation", TRANSLATION_DEFAULT);
         vector3f1.scale(0.0625F);
         vector3f1.x = MathHelper.clamp(vector3f1.x, -1.5F, 1.5F);
         vector3f1.y = MathHelper.clamp(vector3f1.y, -1.5F, 1.5F);
         vector3f1.z = MathHelper.clamp(vector3f1.z, -1.5F, 1.5F);
         Vector3f vector3f2 = this.parseVector3f(jsonobject, "scale", SCALE_DEFAULT);
         vector3f2.x = MathHelper.clamp(vector3f2.x, -4.0F, 4.0F);
         vector3f2.y = MathHelper.clamp(vector3f2.y, -4.0F, 4.0F);
         vector3f2.z = MathHelper.clamp(vector3f2.z, -4.0F, 4.0F);
         return new ItemTransformVec3f(vector3f, vector3f1, vector3f2);
      }

      private Vector3f parseVector3f(JsonObject var1, String var2, Vector3f var3) {
         if (!var1.has(var2)) {
            return var3;
         } else {
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
}
