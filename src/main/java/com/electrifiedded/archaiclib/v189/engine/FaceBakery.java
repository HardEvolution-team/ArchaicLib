package com.electrifiedded.archaiclib.v189.engine;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class FaceBakery {
   private static final float SCALE_ROTATION_22_5 = 1.0F / (float)Math.cos((float) (Math.PI / 8)) - 1.0F;
   private static final float SCALE_ROTATION_GENERAL = 1.0F / (float)Math.cos((float) (Math.PI / 4)) - 1.0F;

   public BakedQuad makeBakedQuad(
      Vector3f var1,
      Vector3f var2,
      BlockPartFace var3,
      TextureAtlasSprite var4,
      EnumFacing var5,
      ModelRotation var6,
      BlockPartRotation var7,
      boolean var8,
      boolean var9
   ) {
      int[] aint = this.makeQuadVertexData(var3, var4, var5, this.getPositionsDiv16(var1, var2), var6, var7, var8, var9);
      EnumFacing enumfacing = getFacingFromVertexData(aint);
      if (var8) {
         this.lockUv(aint, enumfacing, var3.blockFaceUV, var4);
      }

      if (var7 == null) {
         this.applyFacing(aint, enumfacing);
      }

      return new BakedQuad(aint, var3.tintIndex, enumfacing);
   }

   private int[] makeQuadVertexData(
      BlockPartFace var1, TextureAtlasSprite var2, EnumFacing var3, float[] var4, ModelRotation var5, BlockPartRotation var6, boolean var7, boolean var8
   ) {
      int[] aint = new int[28];

      for (int i = 0; i < 4; i++) {
         this.fillVertexData(aint, i, var3, var1, var4, var2, var5, var6, var7, var8);
      }

      return aint;
   }

   private int getFaceShadeColor(EnumFacing var1) {
      float f = this.getFaceBrightness(var1);
      int i = MathHelper.clamp((int)(f * 255.0F), 0, 255);
      return 0xFF000000 | i << 16 | i << 8 | i;
   }

   private float getFaceBrightness(EnumFacing var1) {
      switch (var1) {
         case DOWN:
            return 0.5F;
         case UP:
            return 1.0F;
         case NORTH:
         case SOUTH:
            return 0.8F;
         case WEST:
         case EAST:
            return 0.6F;
         default:
            return 1.0F;
      }
   }

   private float[] getPositionsDiv16(Vector3f var1, Vector3f var2) {
      float[] afloat = new float[EnumFacing.values().length];
      afloat[EnumFaceDirection.Constants.WEST_INDEX] = var1.x / 16.0F;
      afloat[EnumFaceDirection.Constants.DOWN_INDEX] = var1.y / 16.0F;
      afloat[EnumFaceDirection.Constants.NORTH_INDEX] = var1.z / 16.0F;
      afloat[EnumFaceDirection.Constants.EAST_INDEX] = var2.x / 16.0F;
      afloat[EnumFaceDirection.Constants.UP_INDEX] = var2.y / 16.0F;
      afloat[EnumFaceDirection.Constants.SOUTH_INDEX] = var2.z / 16.0F;
      return afloat;
   }

   private void fillVertexData(
      int[] var1,
      int var2,
      EnumFacing var3,
      BlockPartFace var4,
      float[] var5,
      TextureAtlasSprite var6,
      ModelRotation var7,
      BlockPartRotation var8,
      boolean var9,
      boolean var10
   ) {
      EnumFacing enumfacing = var7.rotateFace(var3);
      int i = var10 ? this.getFaceShadeColor(enumfacing) : -1;
      EnumFaceDirection.VertexInformation enumfacedirection$vertexinformation = EnumFaceDirection.getFacing(var3).getVertexInformation(var2);
      Vector3f vector3f = new Vector3f(
         var5[enumfacedirection$vertexinformation.xIndex], var5[enumfacedirection$vertexinformation.yIndex], var5[enumfacedirection$vertexinformation.zIndex]
      );
      this.rotatePart(vector3f, var8);
      int j = this.rotateVertex(vector3f, var3, var2, var7, var9);
      this.storeVertexData(var1, j, var2, vector3f, i, var6, var4.blockFaceUV);
   }

   private void storeVertexData(int[] var1, int var2, int var3, Vector3f var4, int var5, TextureAtlasSprite var6, BlockFaceUV var7) {
      int i = var2 * 7;
      var1[i] = Float.floatToRawIntBits(var4.x);
      var1[i + 1] = Float.floatToRawIntBits(var4.y);
      var1[i + 2] = Float.floatToRawIntBits(var4.z);
      var1[i + 3] = var5;
      var1[i + 4] = Float.floatToRawIntBits(var6.getInterpolatedU(uv(var7.func_178348_a(var3))));
      var1[i + 4 + 1] = Float.floatToRawIntBits(var6.getInterpolatedV(uv(var7.func_178346_b(var3))));
   }

   private void rotatePart(Vector3f var1, BlockPartRotation var2) {
      if (var2 != null) {
         Matrix4f matrix4f = this.getMatrixIdentity();
         Vector3f vector3f = new Vector3f(0.0F, 0.0F, 0.0F);
         switch (var2.axis) {
            case X:
               Matrix4f.rotate(var2.angle * (float) (Math.PI / 180.0), new Vector3f(1.0F, 0.0F, 0.0F), matrix4f, matrix4f);
               vector3f.set(0.0F, 1.0F, 1.0F);
               break;
            case Y:
               Matrix4f.rotate(var2.angle * (float) (Math.PI / 180.0), new Vector3f(0.0F, 1.0F, 0.0F), matrix4f, matrix4f);
               vector3f.set(1.0F, 0.0F, 1.0F);
               break;
            case Z:
               Matrix4f.rotate(var2.angle * (float) (Math.PI / 180.0), new Vector3f(0.0F, 0.0F, 1.0F), matrix4f, matrix4f);
               vector3f.set(1.0F, 1.0F, 0.0F);
         }

         if (var2.rescale) {
            if (Math.abs(var2.angle) == 22.5F) {
               vector3f.scale(SCALE_ROTATION_22_5);
            } else {
               vector3f.scale(SCALE_ROTATION_GENERAL);
            }

            Vector3f.add(vector3f, new Vector3f(1.0F, 1.0F, 1.0F), vector3f);
         } else {
            vector3f.set(1.0F, 1.0F, 1.0F);
         }

         this.rotateScale(var1, new Vector3f(var2.origin), matrix4f, vector3f);
      }
   }

   public int rotateVertex(Vector3f var1, EnumFacing var2, int var3, ModelRotation var4, boolean var5) {
      if (var4 == ModelRotation.X0_Y0) {
         return var3;
      } else {
         this.rotateScale(var1, new Vector3f(0.5F, 0.5F, 0.5F), var4.getMatrix4d(), new Vector3f(1.0F, 1.0F, 1.0F));
         return var4.rotateVertex(var2, var3);
      }
   }

   private void rotateScale(Vector3f var1, Vector3f var2, Matrix4f var3, Vector3f var4) {
      Vector4f vector4f = new Vector4f(var1.x - var2.x, var1.y - var2.y, var1.z - var2.z, 1.0F);
      Matrix4f.transform(var3, vector4f, vector4f);
      vector4f.x = vector4f.x * var4.x;
      vector4f.y = vector4f.y * var4.y;
      vector4f.z = vector4f.z * var4.z;
      var1.set(vector4f.x + var2.x, vector4f.y + var2.y, vector4f.z + var2.z);
   }

   private Matrix4f getMatrixIdentity() {
      Matrix4f matrix4f = new Matrix4f();
      matrix4f.setIdentity();
      return matrix4f;
   }

   public static EnumFacing getFacingFromVertexData(int[] var0) {
      Vector3f vector3f = new Vector3f(Float.intBitsToFloat(var0[0]), Float.intBitsToFloat(var0[1]), Float.intBitsToFloat(var0[2]));
      Vector3f vector3f1 = new Vector3f(Float.intBitsToFloat(var0[7]), Float.intBitsToFloat(var0[8]), Float.intBitsToFloat(var0[9]));
      Vector3f vector3f2 = new Vector3f(Float.intBitsToFloat(var0[14]), Float.intBitsToFloat(var0[15]), Float.intBitsToFloat(var0[16]));
      Vector3f vector3f3 = new Vector3f();
      Vector3f vector3f4 = new Vector3f();
      Vector3f vector3f5 = new Vector3f();
      Vector3f.sub(vector3f, vector3f1, vector3f3);
      Vector3f.sub(vector3f2, vector3f1, vector3f4);
      Vector3f.cross(vector3f4, vector3f3, vector3f5);
      float f = (float)Math.sqrt(vector3f5.x * vector3f5.x + vector3f5.y * vector3f5.y + vector3f5.z * vector3f5.z);
      vector3f5.x /= f;
      vector3f5.y /= f;
      vector3f5.z /= f;
      EnumFacing enumfacing = null;
      float f1 = 0.0F;

      for (EnumFacing enumfacing1 : EnumFacing.values()) {
         Vec3i vec3i = enumfacing1.getDirectionVec();
         Vector3f vector3f6 = new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
         float f2 = Vector3f.dot(vector3f5, vector3f6);
         if (f2 >= 0.0F && f2 > f1) {
            f1 = f2;
            enumfacing = enumfacing1;
         }
      }

      return enumfacing == null ? EnumFacing.UP : enumfacing;
   }

   public void lockUv(int[] var1, EnumFacing var2, BlockFaceUV var3, TextureAtlasSprite var4) {
      for (int i = 0; i < 4; i++) {
         this.lockVertexUv(i, var1, var2, var3, var4);
      }
   }

   private void applyFacing(int[] var1, EnumFacing var2) {
      int[] aint = new int[var1.length];
      System.arraycopy(var1, 0, aint, 0, var1.length);
      float[] afloat = new float[EnumFacing.values().length];
      afloat[EnumFaceDirection.Constants.WEST_INDEX] = 999.0F;
      afloat[EnumFaceDirection.Constants.DOWN_INDEX] = 999.0F;
      afloat[EnumFaceDirection.Constants.NORTH_INDEX] = 999.0F;
      afloat[EnumFaceDirection.Constants.EAST_INDEX] = -999.0F;
      afloat[EnumFaceDirection.Constants.UP_INDEX] = -999.0F;
      afloat[EnumFaceDirection.Constants.SOUTH_INDEX] = -999.0F;

      for (int i = 0; i < 4; i++) {
         int j = 7 * i;
         float f = Float.intBitsToFloat(aint[j]);
         float f1 = Float.intBitsToFloat(aint[j + 1]);
         float f2 = Float.intBitsToFloat(aint[j + 2]);
         if (f < afloat[EnumFaceDirection.Constants.WEST_INDEX]) {
            afloat[EnumFaceDirection.Constants.WEST_INDEX] = f;
         }

         if (f1 < afloat[EnumFaceDirection.Constants.DOWN_INDEX]) {
            afloat[EnumFaceDirection.Constants.DOWN_INDEX] = f1;
         }

         if (f2 < afloat[EnumFaceDirection.Constants.NORTH_INDEX]) {
            afloat[EnumFaceDirection.Constants.NORTH_INDEX] = f2;
         }

         if (f > afloat[EnumFaceDirection.Constants.EAST_INDEX]) {
            afloat[EnumFaceDirection.Constants.EAST_INDEX] = f;
         }

         if (f1 > afloat[EnumFaceDirection.Constants.UP_INDEX]) {
            afloat[EnumFaceDirection.Constants.UP_INDEX] = f1;
         }

         if (f2 > afloat[EnumFaceDirection.Constants.SOUTH_INDEX]) {
            afloat[EnumFaceDirection.Constants.SOUTH_INDEX] = f2;
         }
      }

      EnumFaceDirection enumfacedirection = EnumFaceDirection.getFacing(var2);

      for (int i1 = 0; i1 < 4; i1++) {
         int j1 = 7 * i1;
         EnumFaceDirection.VertexInformation enumfacedirection$vertexinformation = enumfacedirection.getVertexInformation(i1);
         float f8 = afloat[enumfacedirection$vertexinformation.xIndex];
         float f3 = afloat[enumfacedirection$vertexinformation.yIndex];
         float f4 = afloat[enumfacedirection$vertexinformation.zIndex];
         var1[j1] = Float.floatToRawIntBits(f8);
         var1[j1 + 1] = Float.floatToRawIntBits(f3);
         var1[j1 + 2] = Float.floatToRawIntBits(f4);

         for (int k = 0; k < 4; k++) {
            int l = 7 * k;
            float f5 = Float.intBitsToFloat(aint[l]);
            float f6 = Float.intBitsToFloat(aint[l + 1]);
            float f7 = Float.intBitsToFloat(aint[l + 2]);
            if (MathHelper.epsilonEquals(f8, f5) && MathHelper.epsilonEquals(f3, f6) && MathHelper.epsilonEquals(f4, f7)) {
               var1[j1 + 4] = aint[l + 4];
               var1[j1 + 4 + 1] = aint[l + 4 + 1];
            }
         }
      }
   }

   private void lockVertexUv(int var1, int[] var2, EnumFacing var3, BlockFaceUV var4, TextureAtlasSprite var5) {
      int i = 7 * var1;
      float f = Float.intBitsToFloat(var2[i]);
      float f1 = Float.intBitsToFloat(var2[i + 1]);
      float f2 = Float.intBitsToFloat(var2[i + 2]);
      if (f < -0.1F || f >= 1.1F) {
         f -= MathHelper.floor(f);
      }

      if (f1 < -0.1F || f1 >= 1.1F) {
         f1 -= MathHelper.floor(f1);
      }

      if (f2 < -0.1F || f2 >= 1.1F) {
         f2 -= MathHelper.floor(f2);
      }

      float f3 = 0.0F;
      float f4 = 0.0F;
      switch (var3) {
         case DOWN:
            f3 = f * 16.0F;
            f4 = (1.0F - f2) * 16.0F;
            break;
         case UP:
            f3 = f * 16.0F;
            f4 = f2 * 16.0F;
            break;
         case NORTH:
            f3 = (1.0F - f) * 16.0F;
            f4 = (1.0F - f1) * 16.0F;
            break;
         case SOUTH:
            f3 = f * 16.0F;
            f4 = (1.0F - f1) * 16.0F;
            break;
         case WEST:
            f3 = f2 * 16.0F;
            f4 = (1.0F - f1) * 16.0F;
            break;
         case EAST:
            f3 = (1.0F - f2) * 16.0F;
            f4 = (1.0F - f1) * 16.0F;
      }

      int j = var4.func_178345_c(var1) * 7;
      var2[j + 4] = Float.floatToRawIntBits(var5.getInterpolatedU(f3));
      var2[j + 4 + 1] = Float.floatToRawIntBits(var5.getInterpolatedV(f4));
   }

   /**
    * ArchaicLib: 1.8 let a face UV run outside its sprite (0..16), which then sampled neighbouring sprites of the
    * atlas (garbage that depends on atlas layout). Keep it inside the face's own texture.
    */
   private static double uv(float v) {
      return v < 0.0F ? 0.0 : (v > 16.0F ? 16.0 : v);
   }
}
