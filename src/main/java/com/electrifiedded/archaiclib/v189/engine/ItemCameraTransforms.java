package com.electrifiedded.archaiclib.v189.engine;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.minecraft.client.renderer.GlStateManager;

public class ItemCameraTransforms {
   public static final ItemCameraTransforms DEFAULT = new ItemCameraTransforms();
   public static float field_181690_b = 0.0F;
   public static float field_181691_c = 0.0F;
   public static float field_181692_d = 0.0F;
   public static float field_181693_e = 0.0F;
   public static float field_181694_f = 0.0F;
   public static float field_181695_g = 0.0F;
   public static float field_181696_h = 0.0F;
   public static float field_181697_i = 0.0F;
   public static float field_181698_j = 0.0F;
   public final ItemTransformVec3f thirdPerson;
   public final ItemTransformVec3f firstPerson;
   public final ItemTransformVec3f head;
   public final ItemTransformVec3f gui;
   public final ItemTransformVec3f ground;
   public final ItemTransformVec3f fixed;

   private ItemCameraTransforms() {
      this(
         ItemTransformVec3f.DEFAULT,
         ItemTransformVec3f.DEFAULT,
         ItemTransformVec3f.DEFAULT,
         ItemTransformVec3f.DEFAULT,
         ItemTransformVec3f.DEFAULT,
         ItemTransformVec3f.DEFAULT
      );
   }

   public ItemCameraTransforms(ItemCameraTransforms var1) {
      this.thirdPerson = var1.thirdPerson;
      this.firstPerson = var1.firstPerson;
      this.head = var1.head;
      this.gui = var1.gui;
      this.ground = var1.ground;
      this.fixed = var1.fixed;
   }

   public ItemCameraTransforms(
      ItemTransformVec3f var1, ItemTransformVec3f var2, ItemTransformVec3f var3, ItemTransformVec3f var4, ItemTransformVec3f var5, ItemTransformVec3f var6
   ) {
      this.thirdPerson = var1;
      this.firstPerson = var2;
      this.head = var3;
      this.gui = var4;
      this.ground = var5;
      this.fixed = var6;
   }

   public void applyTransform(ItemCameraTransforms.TransformType var1) {
      ItemTransformVec3f itemtransformvec3f = this.getTransform(var1);
      if (itemtransformvec3f != ItemTransformVec3f.DEFAULT) {
         GlStateManager.translate(
            itemtransformvec3f.translation.x + field_181690_b,
            itemtransformvec3f.translation.y + field_181691_c,
            itemtransformvec3f.translation.z + field_181692_d
         );
         GlStateManager.rotate(itemtransformvec3f.rotation.y + field_181694_f, 0.0F, 1.0F, 0.0F);
         GlStateManager.rotate(itemtransformvec3f.rotation.x + field_181693_e, 1.0F, 0.0F, 0.0F);
         GlStateManager.rotate(itemtransformvec3f.rotation.z + field_181695_g, 0.0F, 0.0F, 1.0F);
         GlStateManager.scale(
            itemtransformvec3f.scale.x + field_181696_h, itemtransformvec3f.scale.y + field_181697_i, itemtransformvec3f.scale.z + field_181698_j
         );
      }
   }

   public ItemTransformVec3f getTransform(ItemCameraTransforms.TransformType var1) {
      switch (var1) {
         case THIRD_PERSON:
            return this.thirdPerson;
         case FIRST_PERSON:
            return this.firstPerson;
         case HEAD:
            return this.head;
         case GUI:
            return this.gui;
         case GROUND:
            return this.ground;
         case FIXED:
            return this.fixed;
         default:
            return ItemTransformVec3f.DEFAULT;
      }
   }

   public boolean func_181687_c(ItemCameraTransforms.TransformType var1) {
      return !this.getTransform(var1).equals(ItemTransformVec3f.DEFAULT);
   }

   static class Deserializer implements JsonDeserializer<ItemCameraTransforms> {
      public ItemCameraTransforms deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         JsonObject jsonobject = var1.getAsJsonObject();
         ItemTransformVec3f itemtransformvec3f = this.func_181683_a(var3, jsonobject, "thirdperson");
         ItemTransformVec3f itemtransformvec3f1 = this.func_181683_a(var3, jsonobject, "firstperson");
         ItemTransformVec3f itemtransformvec3f2 = this.func_181683_a(var3, jsonobject, "head");
         ItemTransformVec3f itemtransformvec3f3 = this.func_181683_a(var3, jsonobject, "gui");
         ItemTransformVec3f itemtransformvec3f4 = this.func_181683_a(var3, jsonobject, "ground");
         ItemTransformVec3f itemtransformvec3f5 = this.func_181683_a(var3, jsonobject, "fixed");
         return new ItemCameraTransforms(
            itemtransformvec3f, itemtransformvec3f1, itemtransformvec3f2, itemtransformvec3f3, itemtransformvec3f4, itemtransformvec3f5
         );
      }

      private ItemTransformVec3f func_181683_a(JsonDeserializationContext var1, JsonObject var2, String var3) {
         return var2.has(var3) ? (ItemTransformVec3f)var1.deserialize(var2.get(var3), ItemTransformVec3f.class) : ItemTransformVec3f.DEFAULT;
      }
   }

   public static enum TransformType {
      NONE,
      THIRD_PERSON,
      FIRST_PERSON,
      HEAD,
      GUI,
      GROUND,
      FIXED;
   }
}
