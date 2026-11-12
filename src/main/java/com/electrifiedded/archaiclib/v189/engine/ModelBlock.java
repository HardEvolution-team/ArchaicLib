package com.electrifiedded.archaiclib.v189.engine;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModelBlock {
   private static final Logger LOGGER = LogManager.getLogger();
   static final Gson SERIALIZER = new GsonBuilder()
      .registerTypeAdapter(ModelBlock.class, new ModelBlock.Deserializer())
      .registerTypeAdapter(BlockPart.class, new BlockPart.Deserializer())
      .registerTypeAdapter(BlockPartFace.class, new BlockPartFace.Deserializer())
      .registerTypeAdapter(BlockFaceUV.class, new BlockFaceUV.Deserializer())
      .registerTypeAdapter(ItemTransformVec3f.class, new ItemTransformVec3f.Deserializer())
      .registerTypeAdapter(ItemCameraTransforms.class, new ItemCameraTransforms.Deserializer())
      .create();
   private final List<BlockPart> elements;
   private final boolean gui3d;
   private final boolean ambientOcclusion;
   private ItemCameraTransforms cameraTransforms;
   public String name = "";
   protected final Map<String, String> textures;
   protected ModelBlock parent;
   protected ResourceLocation parentLocation;

   public static ModelBlock deserialize(Reader var0) {
      return (ModelBlock)SERIALIZER.fromJson(var0, ModelBlock.class);
   }

   public static ModelBlock deserialize(String var0) {
      return deserialize(new StringReader(var0));
   }

   protected ModelBlock(List<BlockPart> var1, Map<String, String> var2, boolean var3, boolean var4, ItemCameraTransforms var5) {
      this(null, var1, var2, var3, var4, var5);
   }

   protected ModelBlock(ResourceLocation var1, Map<String, String> var2, boolean var3, boolean var4, ItemCameraTransforms var5) {
      this(var1, Collections.emptyList(), var2, var3, var4, var5);
   }

   private ModelBlock(ResourceLocation var1, List<BlockPart> var2, Map<String, String> var3, boolean var4, boolean var5, ItemCameraTransforms var6) {
      this.elements = var2;
      this.ambientOcclusion = var4;
      this.gui3d = var5;
      this.textures = var3;
      this.parentLocation = var1;
      this.cameraTransforms = var6;
   }

   public List<BlockPart> getElements() {
      return this.elements.isEmpty() && this.hasParent() ? this.parent.getElements() : this.elements;
   }

   private boolean hasParent() {
      return this.parent != null;
   }

   public boolean isAmbientOcclusion() {
      return this.hasParent() ? this.parent.isAmbientOcclusion() : this.ambientOcclusion;
   }

   public boolean isGui3d() {
      return this.gui3d;
   }

   public boolean isResolved() {
      return this.parentLocation == null || this.parent != null && this.parent.isResolved();
   }

   public void getParentFromMap(Map<ResourceLocation, ModelBlock> var1) {
      if (this.parentLocation != null) {
         this.parent = (ModelBlock)var1.get(this.parentLocation);
      }
   }

   public boolean isTexturePresent(String var1) {
      return !"missingno".equals(this.resolveTextureName(var1));
   }

   public String resolveTextureName(String var1) {
      if (!this.startsWithHash(var1)) {
         var1 = '#' + var1;
      }

      return this.resolveTextureName(var1, new ModelBlock.Bookkeep(this));
   }

   private String resolveTextureName(String var1, ModelBlock.Bookkeep var2) {
      if (this.startsWithHash(var1)) {
         if (this == var2.modelExt) {
            LOGGER.warn("Unable to resolve texture due to upward reference: " + var1 + " in " + this.name);
            return "missingno";
         } else {
            String s = this.textures.get(var1.substring(1));
            if (s == null && this.hasParent()) {
               s = this.parent.resolveTextureName(var1, var2);
            }

            var2.modelExt = this;
            if (s != null && this.startsWithHash(s)) {
               s = var2.model.resolveTextureName(s, var2);
            }

            return s != null && !this.startsWithHash(s) ? s : "missingno";
         }
      } else {
         return var1;
      }
   }

   private boolean startsWithHash(String var1) {
      return var1.charAt(0) == '#';
   }

   public ResourceLocation getParentLocation() {
      return this.parentLocation;
   }

   public ModelBlock getRootModel() {
      return this.hasParent() ? this.parent.getRootModel() : this;
   }

   public ItemCameraTransforms getAllTransforms() {
      ItemTransformVec3f itemtransformvec3f = this.getTransform(ItemCameraTransforms.TransformType.THIRD_PERSON);
      ItemTransformVec3f itemtransformvec3f1 = this.getTransform(ItemCameraTransforms.TransformType.FIRST_PERSON);
      ItemTransformVec3f itemtransformvec3f2 = this.getTransform(ItemCameraTransforms.TransformType.HEAD);
      ItemTransformVec3f itemtransformvec3f3 = this.getTransform(ItemCameraTransforms.TransformType.GUI);
      ItemTransformVec3f itemtransformvec3f4 = this.getTransform(ItemCameraTransforms.TransformType.GROUND);
      ItemTransformVec3f itemtransformvec3f5 = this.getTransform(ItemCameraTransforms.TransformType.FIXED);
      return new ItemCameraTransforms(
         itemtransformvec3f, itemtransformvec3f1, itemtransformvec3f2, itemtransformvec3f3, itemtransformvec3f4, itemtransformvec3f5
      );
   }

   private ItemTransformVec3f getTransform(ItemCameraTransforms.TransformType var1) {
      return this.parent != null && !this.cameraTransforms.func_181687_c(var1) ? this.parent.getTransform(var1) : this.cameraTransforms.getTransform(var1);
   }

   public static void checkModelHierarchy(Map<ResourceLocation, ModelBlock> var0) {
      for (ModelBlock modelblock : var0.values()) {
         try {
            ModelBlock modelblock1 = modelblock.parent;

            for (ModelBlock modelblock2 = modelblock1.parent; modelblock1 != modelblock2; modelblock2 = modelblock2.parent.parent) {
               modelblock1 = modelblock1.parent;
            }

            throw new ModelBlock.LoopException();
         } catch (NullPointerException nullpointerexception) {
         }
      }
   }

   static final class Bookkeep {
      public final ModelBlock model;
      public ModelBlock modelExt;

      private Bookkeep(ModelBlock var1) {
         this.model = var1;
      }
   }

   public static class Deserializer implements JsonDeserializer<ModelBlock> {
      public ModelBlock deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         JsonObject jsonobject = var1.getAsJsonObject();
         List list = this.getModelElements(var3, jsonobject);
         String s = this.getParent(jsonobject);
         boolean flag = StringUtils.isEmpty(s);
         boolean flag1 = list.isEmpty();
         {
            // ArchaicLib: 1.12 vanilla roots such as block/block have neither (1.8 had no such models).
            // ArchaicLib: 1.8 rejected "elements" + "parent"; 1.9+ (and add-ons relying on it) accept both.
            Map map = this.getTextures(jsonobject);
            boolean flag2 = this.getAmbientOcclusionEnabled(jsonobject);
            ItemCameraTransforms itemcameratransforms = ItemCameraTransforms.DEFAULT;
            if (jsonobject.has("display")) {
               JsonObject jsonobject1 = JsonUtils.getJsonObject(jsonobject, "display");
               itemcameratransforms = (ItemCameraTransforms)var3.deserialize(jsonobject1, ItemCameraTransforms.class);
            }

            return new ModelBlock(flag ? null : new ResourceLocation(s), list, map, flag2, true, itemcameratransforms);
         }
      }

      private Map<String, String> getTextures(JsonObject var1) {
         HashMap hashmap = Maps.newHashMap();
         if (var1.has("textures")) {
            JsonObject jsonobject = var1.getAsJsonObject("textures");

            for (Entry entry : jsonobject.entrySet()) {
               hashmap.put(entry.getKey(), ((JsonElement)entry.getValue()).getAsString());
            }
         }

         return hashmap;
      }

      private String getParent(JsonObject var1) {
         return JsonUtils.getString(var1, "parent", "");
      }

      protected boolean getAmbientOcclusionEnabled(JsonObject var1) {
         return JsonUtils.getBoolean(var1, "ambientocclusion", true);
      }

      protected List<BlockPart> getModelElements(JsonDeserializationContext var1, JsonObject var2) {
         ArrayList arraylist = Lists.newArrayList();
         if (var2.has("elements")) {
            for (JsonElement jsonelement : JsonUtils.getJsonArray(var2, "elements")) {
               arraylist.add((BlockPart)var1.deserialize(jsonelement, BlockPart.class));
            }
         }

         return arraylist;
      }
   }

   public static class LoopException extends RuntimeException {
   }
}
