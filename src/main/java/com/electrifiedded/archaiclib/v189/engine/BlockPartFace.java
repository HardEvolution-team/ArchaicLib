package com.electrifiedded.archaiclib.v189.engine;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.JsonUtils;

public class BlockPartFace {
   public static final EnumFacing FACING_DEFAULT = null;
   public final EnumFacing cullFace;
   public final int tintIndex;
   public final String texture;
   public final BlockFaceUV blockFaceUV;

   public BlockPartFace(EnumFacing var1, int var2, String var3, BlockFaceUV var4) {
      this.cullFace = var1;
      this.tintIndex = var2;
      this.texture = var3;
      this.blockFaceUV = var4;
   }

   static class Deserializer implements JsonDeserializer<BlockPartFace> {
      public BlockPartFace deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         JsonObject jsonobject = var1.getAsJsonObject();
         EnumFacing enumfacing = this.parseCullFace(jsonobject);
         int i = this.parseTintIndex(jsonobject);
         String s = this.parseTexture(jsonobject);
         BlockFaceUV blockfaceuv = (BlockFaceUV)var3.deserialize(jsonobject, BlockFaceUV.class);
         return new BlockPartFace(enumfacing, i, s, blockfaceuv);
      }

      protected int parseTintIndex(JsonObject var1) {
         return JsonUtils.getInt(var1, "tintindex", -1);
      }

      private String parseTexture(JsonObject var1) {
         return JsonUtils.getString(var1, "texture");
      }

      private EnumFacing parseCullFace(JsonObject var1) {
         String s = JsonUtils.getString(var1, "cullface", "");
         return EnumFacing.byName(s);
      }
   }
}
