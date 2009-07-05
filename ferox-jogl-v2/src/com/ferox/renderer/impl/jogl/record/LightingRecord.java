package com.ferox.renderer.impl.jogl.record;

import javax.media.opengl.GL2;

/**
 * Class storing all the state relevant to lighting and material properties.
 * 
 * @author Michael Ludwig
 */
public class LightingRecord {
	/**
	 * Effect record for an individual light. Certain lighting properties here,
	 * such as position and direction are actually modified by the current
	 * modelview matrix when set. Because of this, the values should not be
	 * taken as actual state, but are an easy way of storing the data before
	 * passing into opengl.
	 */
	public static class LightRecord {
		public final float[] ambient = { 0f, 0f, 0f, 1f };
		public final float[] diffuse = { 0f, 0f, 0f, 1f };
		public final float[] specular = { 0f, 0f, 0f, 1f };
		public final float[] position = { 0f, 0f, 1f, 0f };

		public float constantAttenuation = 1f;
		public float linearAttenuation = 0f;
		public float quadraticAttenuation = 0f;

		public final float[] spotDirection = { 0f, 0f, -1f };
		public float spotExponent = 0f;
		public float spotCutoff = 180f;

		public boolean enabled = false;
	}

	/* Color_Material state. */
	public boolean enableColorMaterial = false;
	public int colorMaterialParameter = GL2.GL_AMBIENT_AND_DIFFUSE;
	public int colorMaterialFace = GL2.GL_FRONT_AND_BACK;

	/* Material state. */
	public final float[] matFrontAmbient = { .2f, .2f, .2f, 1f };
	public final float[] matFrontDiffuse = { .8f, .8f, .8f, 1f };
	public final float[] matFrontSpecular = { 0f, 0f, 0f, 1f };
	public final float[] matFrontEmission = { 0f, 0f, 0f, 1f };
	public float matFrontShininess = 0f;

	public final float[] matBackAmbient = { .2f, .2f, .2f, 1f };
	public final float[] matBackDiffuse = { .8f, .8f, .8f, 1f };
	public final float[] matBackSpecular = { 0f, 0f, 0f, 1f };
	public final float[] matBackEmission = { 0f, 0f, 0f, 1f };
	public float matBackShininess = 0f;

	/* Lighting model. */
	public final float[] lightModelAmbient = { .2f, .2f, .2f, 1f };
	public boolean lightModelLocalViewer = false;
	public boolean lightModelTwoSided = false;
	public int lightModelColorControl = GL2.GL_SINGLE_COLOR;

	public boolean enableLighting = false;

	/* Light units. */
	public final LightRecord[] lightUnits;

	public LightingRecord(int maxLights) {
		lightUnits = new LightRecord[maxLights];
		for (int i = 0; i < lightUnits.length; i++) {
			lightUnits[i] = new LightRecord();
			if (i == 0) {
				// must adjust diffuse and specular color
				lightUnits[i].diffuse[0] = 1f;
				lightUnits[i].diffuse[1] = 1f;
				lightUnits[i].diffuse[2] = 1f;
				lightUnits[i].diffuse[3] = 1f;

				lightUnits[i].specular[0] = 1f;
				lightUnits[i].specular[1] = 1f;
				lightUnits[i].specular[2] = 1f;
				lightUnits[i].specular[3] = 1f;
			}
		}
	}
}
