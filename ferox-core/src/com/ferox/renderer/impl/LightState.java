package com.ferox.renderer.impl;

import com.ferox.math.Color4f;
import com.ferox.renderer.FixedFunctionRenderer;

/**
 * Data structure holding onto the per-light state needed for
 * {@link FixedFunctionRenderer}.
 * 
 * @author Michael Ludwig
 */
class LightState {
	// LightState does not track position or direction since
	// they're stored by OpenGL after being modified by the current MV matrix
	
	final Color4f ambient = new Color4f(0f, 0f, 0f, 1f);
	final Color4f specular = new Color4f(0f, 0f, 0f, 1f);
	final Color4f diffuse = new Color4f(0f, 0f, 0f, 1f);
	
	float constAtt = 1f;
	float linAtt = 0f;
	float quadAtt = 0f;
	
	float spotAngle = 180f;
	
	boolean enabled = false;
}
