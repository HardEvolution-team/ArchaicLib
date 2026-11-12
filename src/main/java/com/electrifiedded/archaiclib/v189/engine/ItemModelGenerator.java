package com.electrifiedded.archaiclib.v189.engine;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.vector.Vector3f;

public class ItemModelGenerator {
   public static final List<String> LAYERS = Lists.newArrayList(new String[]{"layer0", "layer1", "layer2", "layer3", "layer4"});

   public ModelBlock makeItemModel(TextureMap var1, ModelBlock var2) {
      HashMap hashmap = Maps.newHashMap();
      ArrayList arraylist = Lists.newArrayList();

      for (int i = 0; i < LAYERS.size(); i++) {
         String s = LAYERS.get(i);
         if (!var2.isTexturePresent(s)) {
            break;
         }

         String s1 = var2.resolveTextureName(s);
         hashmap.put(s, s1);
         TextureAtlasSprite textureatlassprite = var1.getAtlasSprite(new ResourceLocation(s1).toString());
         arraylist.addAll(this.func_178394_a(i, s, textureatlassprite));
      }

      if (arraylist.isEmpty()) {
         return null;
      } else {
         hashmap.put("particle", var2.isTexturePresent("particle") ? var2.resolveTextureName("particle") : (String)hashmap.get("layer0"));
         return new ModelBlock(arraylist, hashmap, false, false, var2.getAllTransforms());
      }
   }

   private List<BlockPart> func_178394_a(int var1, String var2, TextureAtlasSprite var3) {
      HashMap hashmap = Maps.newHashMap();
      hashmap.put(EnumFacing.SOUTH, new BlockPartFace(null, var1, var2, new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0)));
      hashmap.put(EnumFacing.NORTH, new BlockPartFace(null, var1, var2, new BlockFaceUV(new float[]{16.0F, 0.0F, 0.0F, 16.0F}, 0)));
      ArrayList arraylist = Lists.newArrayList();
      arraylist.add(new BlockPart(new Vector3f(0.0F, 0.0F, 7.5F), new Vector3f(16.0F, 16.0F, 8.5F), hashmap, null, true));
      arraylist.addAll(this.func_178397_a(var3, var2, var1));
      return arraylist;
   }

   private List<BlockPart> func_178397_a(TextureAtlasSprite var1, String var2, int var3) {
      float f = var1.getIconWidth();
      float f1 = var1.getIconHeight();
      ArrayList arraylist = Lists.newArrayList();

      for (ItemModelGenerator.Span itemmodelgenerator$span : this.func_178393_a(var1)) {
         float f2 = 0.0F;
         float f3 = 0.0F;
         float f4 = 0.0F;
         float f5 = 0.0F;
         float f6 = 0.0F;
         float f7 = 0.0F;
         float f8 = 0.0F;
         float f9 = 0.0F;
         float f10 = 0.0F;
         float f11 = 0.0F;
         float f12 = itemmodelgenerator$span.func_178385_b();
         float f13 = itemmodelgenerator$span.func_178384_c();
         float f14 = itemmodelgenerator$span.func_178381_d();
         ItemModelGenerator.SpanFacing itemmodelgenerator$spanfacing = itemmodelgenerator$span.func_178383_a();
         switch (itemmodelgenerator$spanfacing) {
            case UP:
               f6 = f12;
               f2 = f12;
               f4 = f7 = f13 + 1.0F;
               f8 = f14;
               f3 = f14;
               f9 = f14;
               f5 = f14;
               f10 = 16.0F / f;
               f11 = 16.0F / (f1 - 1.0F);
               break;
            case DOWN:
               f9 = f14;
               f8 = f14;
               f6 = f12;
               f2 = f12;
               f4 = f7 = f13 + 1.0F;
               f3 = f14 + 1.0F;
               f5 = f14 + 1.0F;
               f10 = 16.0F / f;
               f11 = 16.0F / (f1 - 1.0F);
               break;
            case LEFT:
               f6 = f14;
               f2 = f14;
               f7 = f14;
               f4 = f14;
               f9 = f12;
               f3 = f12;
               f5 = f8 = f13 + 1.0F;
               f10 = 16.0F / (f - 1.0F);
               f11 = 16.0F / f1;
               break;
            case RIGHT:
               f7 = f14;
               f6 = f14;
               f2 = f14 + 1.0F;
               f4 = f14 + 1.0F;
               f9 = f12;
               f3 = f12;
               f5 = f8 = f13 + 1.0F;
               f10 = 16.0F / (f - 1.0F);
               f11 = 16.0F / f1;
         }

         float f15 = 16.0F / f;
         float f16 = 16.0F / f1;
         f2 *= f15;
         f4 *= f15;
         f3 *= f16;
         f5 *= f16;
         f3 = 16.0F - f3;
         f5 = 16.0F - f5;
         f6 *= f10;
         f7 *= f10;
         f8 *= f11;
         f9 *= f11;
         HashMap hashmap = Maps.newHashMap();
         hashmap.put(itemmodelgenerator$spanfacing.getFacing(), new BlockPartFace(null, var3, var2, new BlockFaceUV(new float[]{f6, f8, f7, f9}, 0)));
         switch (itemmodelgenerator$spanfacing) {
            case UP:
               arraylist.add(new BlockPart(new Vector3f(f2, f3, 7.5F), new Vector3f(f4, f3, 8.5F), hashmap, null, true));
               break;
            case DOWN:
               arraylist.add(new BlockPart(new Vector3f(f2, f5, 7.5F), new Vector3f(f4, f5, 8.5F), hashmap, null, true));
               break;
            case LEFT:
               arraylist.add(new BlockPart(new Vector3f(f2, f3, 7.5F), new Vector3f(f2, f5, 8.5F), hashmap, null, true));
               break;
            case RIGHT:
               arraylist.add(new BlockPart(new Vector3f(f4, f3, 7.5F), new Vector3f(f4, f5, 8.5F), hashmap, null, true));
         }
      }

      return arraylist;
   }

   private List<ItemModelGenerator.Span> func_178393_a(TextureAtlasSprite var1) {
      int i = var1.getIconWidth();
      int j = var1.getIconHeight();
      ArrayList arraylist = Lists.newArrayList();

      for (int k = 0; k < var1.getFrameCount(); k++) {
         int[] aint = var1.getFrameTextureData(k)[0];

         for (int l = 0; l < j; l++) {
            for (int i1 = 0; i1 < i; i1++) {
               boolean flag = !this.func_178391_a(aint, i1, l, i, j);
               this.func_178396_a(ItemModelGenerator.SpanFacing.UP, arraylist, aint, i1, l, i, j, flag);
               this.func_178396_a(ItemModelGenerator.SpanFacing.DOWN, arraylist, aint, i1, l, i, j, flag);
               this.func_178396_a(ItemModelGenerator.SpanFacing.LEFT, arraylist, aint, i1, l, i, j, flag);
               this.func_178396_a(ItemModelGenerator.SpanFacing.RIGHT, arraylist, aint, i1, l, i, j, flag);
            }
         }
      }

      return arraylist;
   }

   private void func_178396_a(
      ItemModelGenerator.SpanFacing var1, List<ItemModelGenerator.Span> var2, int[] var3, int var4, int var5, int var6, int var7, boolean var8
   ) {
      boolean flag = this.func_178391_a(var3, var4 + var1.func_178372_b(), var5 + var1.func_178371_c(), var6, var7) && var8;
      if (flag) {
         this.func_178395_a(var2, var1, var4, var5);
      }
   }

   private void func_178395_a(List<ItemModelGenerator.Span> var1, ItemModelGenerator.SpanFacing var2, int var3, int var4) {
      ItemModelGenerator.Span itemmodelgenerator$span = null;

      for (ItemModelGenerator.Span itemmodelgenerator$span1 : var1) {
         if (itemmodelgenerator$span1.func_178383_a() == var2) {
            int i = var2.func_178369_d() ? var4 : var3;
            if (itemmodelgenerator$span1.func_178381_d() == i) {
               itemmodelgenerator$span = itemmodelgenerator$span1;
               break;
            }
         }
      }

      int j = var2.func_178369_d() ? var4 : var3;
      int k = var2.func_178369_d() ? var3 : var4;
      if (itemmodelgenerator$span == null) {
         var1.add(new ItemModelGenerator.Span(var2, k, j));
      } else {
         itemmodelgenerator$span.func_178382_a(k);
      }
   }

   private boolean func_178391_a(int[] var1, int var2, int var3, int var4, int var5) {
      return var2 >= 0 && var3 >= 0 && var2 < var4 && var3 < var5 ? (var1[var3 * var4 + var2] >> 24 & 0xFF) == 0 : true;
   }

   static class Span {
      private final ItemModelGenerator.SpanFacing spanFacing;
      private int field_178387_b;
      private int field_178388_c;
      private final int field_178386_d;

      public Span(ItemModelGenerator.SpanFacing var1, int var2, int var3) {
         this.spanFacing = var1;
         this.field_178387_b = var2;
         this.field_178388_c = var2;
         this.field_178386_d = var3;
      }

      public void func_178382_a(int var1) {
         if (var1 < this.field_178387_b) {
            this.field_178387_b = var1;
         } else if (var1 > this.field_178388_c) {
            this.field_178388_c = var1;
         }
      }

      public ItemModelGenerator.SpanFacing func_178383_a() {
         return this.spanFacing;
      }

      public int func_178385_b() {
         return this.field_178387_b;
      }

      public int func_178384_c() {
         return this.field_178388_c;
      }

      public int func_178381_d() {
         return this.field_178386_d;
      }
   }

   static enum SpanFacing {
      UP(EnumFacing.UP, 0, -1),
      DOWN(EnumFacing.DOWN, 0, 1),
      LEFT(EnumFacing.EAST, -1, 0),
      RIGHT(EnumFacing.WEST, 1, 0);

      private final EnumFacing facing;
      private final int field_178373_f;
      private final int field_178374_g;

      private SpanFacing(EnumFacing var3, int var4, int var5) {
         this.facing = var3;
         this.field_178373_f = var4;
         this.field_178374_g = var5;
      }

      public EnumFacing getFacing() {
         return this.facing;
      }

      public int func_178372_b() {
         return this.field_178373_f;
      }

      public int func_178371_c() {
         return this.field_178374_g;
      }

      private boolean func_178369_d() {
         return this == DOWN || this == UP;
      }
   }
}
