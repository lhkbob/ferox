package com.ferox.scene.impl.fixed;

import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;

public class LightAtom {
	public static enum Type {
		AMBIENT, POINT, DIRECTION, SPOTLIGHT
	}
	
	// all light properties
	public Color4f diffuse;
	public Color4f specular;
	
	// dir + spot light properties
	public boolean castsShadows;
	public Vector3f direction;

	// spot properties
	public float cutoffAngle;

	// spot and point properties
	public Vector3f position;
	public float constCutoff;
	public float linCutoff;
	public float quadCutoff;
}
