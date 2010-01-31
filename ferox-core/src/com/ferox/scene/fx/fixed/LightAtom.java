package com.ferox.scene.fx.fixed;

import com.ferox.math.Color4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.BoundVolume;

public class LightAtom {
	public boolean castsShadows;
	
	public Color4f diffuse;
	public Color4f specular;
	
	public Vector3f position;
	public BoundVolume worldBounds;

	public Vector3f direction;
	public float cutoffAngle;
	
	public float constAtt;
	public float linAtt;
	public float quadAtt;
	
	public float specularExponent;
}
