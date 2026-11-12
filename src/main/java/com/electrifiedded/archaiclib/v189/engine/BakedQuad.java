package com.electrifiedded.archaiclib.v189.engine;

import net.minecraft.util.EnumFacing;

public class BakedQuad {
   protected final int[] vertexData;
   protected final int tintIndex;
   protected final EnumFacing face;

   public BakedQuad(int[] var1, int var2, EnumFacing var3) {
      this.vertexData = var1;
      this.tintIndex = var2;
      this.face = var3;
   }

   public int[] getVertexData() {
      return this.vertexData;
   }

   public boolean hasTintIndex() {
      return this.tintIndex != -1;
   }

   public int getTintIndex() {
      return this.tintIndex;
   }

   public EnumFacing getFace() {
      return this.face;
   }
}
