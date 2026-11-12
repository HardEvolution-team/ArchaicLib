package com.electrifiedded.archaiclib.v189.engine;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.EnumFacing.Axis;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public enum ModelRotation {
   X0_Y0(0, 0),
   X0_Y90(0, 90),
   X0_Y180(0, 180),
   X0_Y270(0, 270),
   X90_Y0(90, 0),
   X90_Y90(90, 90),
   X90_Y180(90, 180),
   X90_Y270(90, 270),
   X180_Y0(180, 0),
   X180_Y90(180, 90),
   X180_Y180(180, 180),
   X180_Y270(180, 270),
   X270_Y0(270, 0),
   X270_Y90(270, 90),
   X270_Y180(270, 180),
   X270_Y270(270, 270);

   private static final Map<Integer, ModelRotation> mapRotations = Maps.newHashMap();
   private final int combinedXY;
   private final Matrix4f matrix4d;
   private final int quartersX;
   private final int quartersY;

   private static int combineXY(int var0, int var1) {
      return var0 * 360 + var1;
   }

   private ModelRotation(int var3, int var4) {
      this.combinedXY = combineXY(var3, var4);
      this.matrix4d = new Matrix4f();
      Matrix4f matrix4f = new Matrix4f();
      matrix4f.setIdentity();
      Matrix4f.rotate(-var3 * (float) (Math.PI / 180.0), new Vector3f(1.0F, 0.0F, 0.0F), matrix4f, matrix4f);
      this.quartersX = Math.abs(var3 / 90);
      Matrix4f matrix4f1 = new Matrix4f();
      matrix4f1.setIdentity();
      Matrix4f.rotate(-var4 * (float) (Math.PI / 180.0), new Vector3f(0.0F, 1.0F, 0.0F), matrix4f1, matrix4f1);
      this.quartersY = Math.abs(var4 / 90);
      Matrix4f.mul(matrix4f1, matrix4f, this.matrix4d);
   }

   public Matrix4f getMatrix4d() {
      return this.matrix4d;
   }

   public EnumFacing rotateFace(EnumFacing var1) {
      EnumFacing enumfacing = var1;

      for (int i = 0; i < this.quartersX; i++) {
         enumfacing = enumfacing.rotateAround(Axis.X);
      }

      if (enumfacing.getAxis() != Axis.Y) {
         for (int j = 0; j < this.quartersY; j++) {
            enumfacing = enumfacing.rotateAround(Axis.Y);
         }
      }

      return enumfacing;
   }

   public int rotateVertex(EnumFacing var1, int var2) {
      int i = var2;
      if (var1.getAxis() == Axis.X) {
         i = (var2 + this.quartersX) % 4;
      }

      EnumFacing enumfacing = var1;

      for (int j = 0; j < this.quartersX; j++) {
         enumfacing = enumfacing.rotateAround(Axis.X);
      }

      if (enumfacing.getAxis() == Axis.Y) {
         i = (i + this.quartersY) % 4;
      }

      return i;
   }

   public static ModelRotation getModelRotation(int var0, int var1) {
      return mapRotations.get(combineXY(MathHelper.normalizeAngle(var0, 360), MathHelper.normalizeAngle(var1, 360)));
   }

   static {
      for (ModelRotation modelrotation : values()) {
         mapRotations.put(modelrotation.combinedXY, modelrotation);
      }
   }
}
