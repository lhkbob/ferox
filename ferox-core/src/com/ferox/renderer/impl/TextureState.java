package com.ferox.renderer.impl;

import com.ferox.math.Color4f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FixedFunctionRenderer.CombineFunction;
import com.ferox.renderer.FixedFunctionRenderer.CombineOp;
import com.ferox.renderer.FixedFunctionRenderer.CombineSource;
import com.ferox.renderer.FixedFunctionRenderer.EnvMode;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.resource.TextureImage;

/**
 * Data structure holding onto the per-texture unit state needed for texturing
 * in {@link FixedFunctionRenderer}.
 * 
 * @author Michael Ludwig
 */
class TextureState {
	// TextureState does not track texture transforms, or eye planes
	// since these are difficult to track
	
	boolean enabled = false;
	TextureImage image = null;
	
	EnvMode envMode = EnvMode.MODULATE;
	Color4f color = new Color4f(0f, 0f, 0f, 0f);
	
	TexCoordSource tcS = TexCoordSource.ATTRIBUTE;
	TexCoordSource tcT = TexCoordSource.ATTRIBUTE;
	TexCoordSource tcR = TexCoordSource.ATTRIBUTE;
	TexCoordSource tcQ = TexCoordSource.ATTRIBUTE;
	
	final Vector4f objPlaneS = new Vector4f(1f, 0f, 0f, 0f);
	final Vector4f objPlaneT = new Vector4f(0f, 1f, 0f, 0f);
	final Vector4f objPlaneR = new Vector4f(0f, 0f, 0f, 0f);
	final Vector4f objPlaneQ = new Vector4f(0f, 0f, 0f, 0f);
	
	CombineFunction rgbFunc = CombineFunction.MODULATE;
	CombineFunction alphaFunc = CombineFunction.MODULATE;
	
	final CombineOp[] opRgb = {CombineOp.COLOR, CombineOp.COLOR, CombineOp.ALPHA};
	final CombineOp[] opAlpha = {CombineOp.ALPHA, CombineOp.ALPHA, CombineOp.ALPHA};
	
	final CombineSource[] srcRgb = {CombineSource.CURR_TEX, CombineSource.PREV_TEX, CombineSource.CONST_COLOR};
	final CombineSource[] srcAlpha = {CombineSource.CURR_TEX, CombineSource.PREV_TEX, CombineSource.CONST_COLOR};
}
