package com.ferox.renderer.impl.jogl.record;

import javax.media.opengl.GL;

/** This class encapsulates all of the state necessary for
 * all of the available texture units and their environments.
 * 
 * This does not track per-object texture parameters.
 * 
 * @author Michael Ludwig
 *
 */
public class TextureRecord {
	/** Represents the state of a single texture unit. */
	public static class TextureUnit {
		public boolean enableCoordReplace = false;
		
		public int textureEnvMode = GL.GL_MODULATE;
		public final float[] textureEnvColor = {0f, 0f, 0f, 0f};
		public float textureLodBias = 0f;
		
		public final TextureGenRecord texGenR = new TextureGenRecord();
		public final TextureGenRecord texGenS = new TextureGenRecord();
		public final TextureGenRecord texGenT = new TextureGenRecord();
		public final TextureGenRecord texGenQ = new TextureGenRecord();

		public int combineRgb = GL.GL_MODULATE;
		public int combineAlpha = GL.GL_MODULATE;
		
		public int src0Rgb = GL.GL_TEXTURE;
		public int src1Rgb = GL.GL_PREVIOUS;
		public int src2Rgb = GL.GL_CONSTANT;
		public int src0Alpha = GL.GL_TEXTURE;
		public int src1Alpha = GL.GL_PREVIOUS;
		public int src2Alpha = GL.GL_CONSTANT;
		
		public int operand0Rgb = GL.GL_SRC_COLOR;
		public int operand1Rgb = GL.GL_SRC_COLOR;
		public int operand2Rgb = GL.GL_SRC_ALPHA;

		public int operand0Alpha = GL.GL_SRC_ALPHA;
		public int operand1Alpha = GL.GL_SRC_ALPHA;
		public int operand2Alpha = GL.GL_SRC_ALPHA;
		
		/** This property is a stand-in to the texture matrix stack.
		 * Convention should keep the texture matrix / unit at size 1.
		 * If that matrix is no-longer the identity, set this to false. */
		public boolean isTextureMatrixIdentity = true;
		
		/** These fields differ from the standard OpenGL state because
		 * we're imposing the constraint that only one target can be enabled
		 * and bound to at any usable point in time.
		 * 
		 * If enableTarget is true, enabledTarget must be one of GL_TEXTURE_2D/3D/CUBEMAP/RECTANGLE_ARB.
		 * If it's false, no texture should be bound, and enabledTarget should = -1. */
		public boolean enableTarget = false;
		public int enabledTarget = -1;
		public int texBinding = 0;
	}
	
	public static class TextureGenRecord {
		public boolean enableTexGen;
		public final float[] eyePlane = {};
		public final float[] objectPlane = {};
		public int textureGenMode = GL.GL_EYE_LINEAR;
	}
	
	/** For simplicity, activeTexture is valued 0 to maxUnits so it
	 * can be used to easily access textureUnits.  The actual
	 * active texture is GL_TEXTURE0 + activeTexture. */
	public int activeTexture = 0; 
	public final TextureUnit[] textureUnits;
	
	public TextureRecord(int maxUnits) {
		this.textureUnits = new TextureUnit[maxUnits];
		for (int i = 0; i < maxUnits; i++) 
			this.textureUnits[i] = new TextureUnit();
	}
}
