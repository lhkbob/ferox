package com.ferox.renderer.impl.jogl.record;

import javax.media.opengl.GL;

/** Class storing all the state relevant to lighting and
 * material properties.
 * 
 * @author Michael Ludwig
 *
 */
public class LightingRecord {
	/** State record for an individual light.
	 * Certain lighting properties here, such as position
	 * and direction are actually modified by the current
	 * modelview matrix when set.  Because of this, the
	 * values should not be taken as actual state, but are
	 * an easy way of storing the data before passing into opengl. */
	public static class LightRecord {
		/* Initial values for these are bogus, just to force a color change. */
		public final float[] ambient = {-1f, -1f, -1f, -1f};
		public final float[] diffuse = {-1f, -1f, -1f, 1f};
		public final float[] specular = {0f, 0f, 0f, -1f};
		public final float[] position = {0f, 0f, 1f, 0f};
		
		public float constantAttenuation = 1f;
		public float linearAttenuation = 0f;
		public float quadraticAttenuation = 0f;
		
		public final float[] spotDirection = {0f, 0f, -1f};
		public float spotExponent = 0f;
		public float spotCutoff = 180f;
		
		public boolean enabled = false;
	}
	
	/* Color_Material state. */
	public boolean enableColorMaterial = false;
	public int colorMaterialParameter = GL.GL_AMBIENT_AND_DIFFUSE;
	public int colorMaterialFace = GL.GL_FRONT_AND_BACK;
	
	/* Material state. */
	public final float[] matFrontAmbient = {.2f, .2f, .2f, 1f};
	public final float[] matFrontDiffuse = {.8f, .8f, .8f, 1f};
	public final float[] matFrontSpecular = {0f, 0f, 0f, 1f};
	public final float[] matFrontEmission = {0f, 0f, 0f, 1f};
	public float matFrontShininess = 0f;
	
	public final float[] matBackAmbient = {.2f, .2f, .2f, 1f};
	public final float[] matBackDiffuse = {.8f, .8f, .8f, 1f};
	public final float[] matBackSpecular = {0f, 0f, 0f, 1f};
	public final float[] matBackEmission = {0f, 0f, 0f, 1f};
	public float matBackShininess = 0f;
	
	/* Lighting model. */
	public final float[] lightModelAmbient = {.2f, .2f, .2f, 1f};
	public boolean lightModelLocalViewer = false;
	public boolean lightModelTwoSided = false;
	public int lightModelColorControl = GL.GL_SINGLE_COLOR;
	
	public boolean enableLighting = false;
	
	/* Light units. */
	public final LightRecord[] lightUnits;
	
	public LightingRecord(int maxLights) {
		this.lightUnits = new LightRecord[maxLights];
		for (int i = 0; i < this.lightUnits.length; i++) 
			this.lightUnits[i] = new LightRecord();
	}
}
