package com.electrifiedded.archaiclib.v189.engine;

import net.minecraft.util.EnumFacing.Axis;
import org.lwjgl.util.vector.Vector3f;

public class BlockPartRotation {
   public final Vector3f origin;
   public final Axis axis;
   public final float angle;
   public final boolean rescale;

   public BlockPartRotation(Vector3f var1, Axis var2, float var3, boolean var4) {
      this.origin = var1;
      this.axis = var2;
      this.angle = var3;
      this.rescale = var4;
   }
}
