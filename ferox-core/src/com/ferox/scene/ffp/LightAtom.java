package com.ferox.scene.ffp;

import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.BoundVolume;

public class LightAtom {
	public static enum Type {
		AMBIENT, DIRECTION, SPOTLIGHT
	}

	public int id;
	public Type type; // depending on type, certain properties are undefined
	public BoundVolume worldBounds;
	
	// all light properties
	public Color4f diffuse;
	public Color4f specular;
	
	// dir + spot light properties
	public boolean castsShadows;
	public Vector3f direction;

	// spot properties
	public Vector3f position;
	public float cutoffAngle;

	public float constCutoff;
	public float linCutoff;
	public float quadCutoff;
}
