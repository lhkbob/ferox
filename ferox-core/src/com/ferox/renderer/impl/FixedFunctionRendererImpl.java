package com.ferox.renderer.impl;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;
import com.ferox.resource.TextureImage;

/**
 * <p>
 * FixedFunctionRendererImpl is a complete implementation of
 * FixedFunctionRenderer that delegates each method call to an appropriate
 * delegate. It combines the functionality of {@link RendererDelegate} and
 * {@link FixedFunctionRendererDelegate} to complete the FixedFunctionRenderer
 * interface.
 * </p>
 * <p>
 * Implementations can reduce the amount of repeated work by just implementing
 * one of each type of delegate. This way the functionality exposed in the
 * top-level Renderer is not duplicated in both FixedFunctionRenderer and
 * GlslRenderer implementations.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class FixedFunctionRendererImpl implements FixedFunctionRenderer {
	private final RendererDelegate coreD;
	private final FixedFunctionRendererDelegate ffpD;

	/**
	 * Create a FixedFunctionRendererImpl that combines the two delegates'
	 * functionality together. The given delegates should not be used by any of
	 * Renderer implementation to guarantee the correctness of its state
	 * tracking.
	 * 
	 * @param renderDelegate The RendererDelegate that handles the majority of
	 *            methods declared in {@link Renderer}
	 * @param ffpDelegate The FixedFunctionRendererDelegate that handles the
	 *            methods declared in {@link FixedFunctionRenderer} and actually
	 *            rendering the Geometries
	 * @throws NullPointerException if either delegate is null
	 */
	public FixedFunctionRendererImpl(RendererDelegate renderDelegate, 
									 FixedFunctionRendererDelegate ffpDelegate) {
		if (renderDelegate == null || ffpDelegate == null)
			throw new NullPointerException("Must specify non-null RendererDelegates");
		coreD = renderDelegate;
		ffpD = ffpDelegate;
	}
	
	@Override
	public void setAlphaTest(Comparison test, float refValue) {
		ffpD.setAlphaTest(test, refValue);
	}

	@Override
	public void setBindings(String vertices, String normals, String[] texCoords) {
		ffpD.setBindings(vertices, normals, texCoords);
	}

	@Override
	public void setFogColor(Color4f color) {
		ffpD.setFogColor(color);
	}

	@Override
	public void setFogEnabled(boolean enable) {
		ffpD.setFogEnabled(enable);
	}

	@Override
	public void setFogExponential(float density, boolean squared) {
		ffpD.setFogExponential(density, squared);
	}

	@Override
	public void setFogLinear(float start, float end) {
		ffpD.setFogLinear(start, end);
	}

	@Override
	public void setGlobalAmbientLight(Color4f ambient) {
		ffpD.setGlobalAmbientLight(ambient);
	}

	@Override
	public void setLightAttenuation(int light, float constant, float linear, float quadratic) {
		ffpD.setLightAttenuation(light, constant, linear, quadratic);
	}

	@Override
	public void setLightColor(int light, Color4f amb, Color4f diff, Color4f spec) {
		ffpD.setLightColor(light, amb, diff, spec);
	}

	@Override
	public void setLightEnabled(int light, boolean enable) {
		ffpD.setLightEnabled(light, enable);
	}

	@Override
	public void setLightPosition(int light, Vector4f pos) {
		ffpD.setLightPosition(light, pos);
	}

	@Override
	public void setLightingEnabled(boolean enable) {
		ffpD.setLightingEnabled(enable);
	}

	@Override
	public void setLightingModel(boolean smoothed, boolean twoSided) {
		ffpD.setLightingModel(smoothed, twoSided);
	}

	@Override
	public void setLineAntiAliasingEnabled(boolean enable) {
		ffpD.setLineAntiAliasingEnabled(enable);
	}

	@Override
	public void setLineSize(float width) {
		ffpD.setLineSize(width);
	}

	@Override
	public void setMaterial(Color4f amb, Color4f diff, Color4f spec, Color4f emm) {
		ffpD.setMaterial(amb, diff, spec, emm);
	}

	@Override
	public void setMaterialShininess(float shininess) {
		ffpD.setMaterialShininess(shininess);
	}

	@Override
	public void setModelViewMatrix(Matrix4f modelView) {
		ffpD.setModelViewMatrix(modelView);
	}

	@Override
	public void setNormalBinding(String name) {
		ffpD.setNormalBinding(name);
	}

	@Override
	public void setPointAntiAliasingEnabled(boolean enable) {
		ffpD.setPointAntiAliasingEnabled(enable);
	}

	@Override
	public void setPointSize(float width) {
		ffpD.setPointSize(width);
	}

	@Override
	public void setPolygonAntiAliasingEnabled(boolean enable) {
		ffpD.setPolygonAntiAliasingEnabled(enable);
	}

	@Override
	public void setProjectionMatrix(Matrix4f projection) {
		ffpD.setProjectionMatrix(projection);
	}

	@Override
	public void setSpotlight(int light, Vector3f dir, float angle) {
		ffpD.setSpotlight(light, dir, angle);
	}

	@Override
	public void setTexture(int tex, TextureImage image) {
		ffpD.setTexture(tex, image);
	}

	@Override
	public void setTextureColor(int tex, Color4f color) {
		ffpD.setTextureColor(tex, color);
	}

	@Override
	public void setTextureCombineFunction(int tex, CombineFunction rgbFunc, CombineFunction alphaFunc) {
		ffpD.setTextureCombineFunction(tex, rgbFunc, alphaFunc);
	}

	@Override
	public void setTextureCombineOpAlpha(int tex, int operand, CombineSource src, CombineOp op) {
		ffpD.setTextureCombineOpAlpha(tex, operand, src, op);
	}

	@Override
	public void setTextureCombineOpRgb(int tex, int operand, CombineSource src, CombineOp op) {
		ffpD.setTextureCombineOpRgb(tex, operand, src, op);
	}

	@Override
	public void setTextureCoordGeneration(int tex, TexCoordSource gen) {
		ffpD.setTextureCoordGeneration(tex, gen);
	}

	@Override
	public void setTextureCoordGeneration(int tex, TexCoord coord, TexCoordSource gen) {
		ffpD.setTextureCoordGeneration(tex, coord, gen);
	}

	@Override
	public void setTextureCoordinateBinding(int tex, String name) {
		ffpD.setTextureCoordinateBinding(tex, name);
	}

	@Override
	public void setTextureEnabled(int tex, boolean enable) {
		ffpD.setTextureEnabled(tex, enable);
	}

	@Override
	public void setTextureEyePlane(int tex, TexCoord coord, Vector4f plane) {
		ffpD.setTextureEyePlane(tex, coord, plane);
	}

	@Override
	public void setTextureMode(int tex, EnvMode mode) {
		ffpD.setTextureMode(tex, mode);
	}

	@Override
	public void setTextureObjectPlane(int tex, TexCoord coord, Vector4f plane) {
		ffpD.setTextureObjectPlane(tex, coord, plane);
	}

	@Override
	public void setTextureTransform(int tex, Matrix4f matrix) {
		ffpD.setTextureTransform(tex, matrix);
	}

	@Override
	public void setVertexBinding(String name) {
		ffpD.setVertexBinding(name);
	}

	@Override
	public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, Color4f color, float depth, int stencil) {
		coreD.clear(clearColor, clearDepth, clearStencil, color, depth, stencil);
	}

	@Override
	public int render(Geometry g) {
		return ffpD.render(g);
	}

	@Override
	public void reset() {
		coreD.reset();
		ffpD.reset();
	}

	@Override
	public void setBlendColor(Color4f color) {
		coreD.setBlendColor(color);
	}

	@Override
	public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst) {
		coreD.setBlendMode(function, src, dst);
	}

	@Override
	public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst) {
		coreD.setBlendModeAlpha(function, src, dst);
	}

	@Override
	public void setBlendModeRgb(BlendFunction function, BlendFactor src, BlendFactor dst) {
		coreD.setBlendModeRgb(function, src, dst);
	}

	@Override
	public void setBlendingEnabled(boolean enable) {
		coreD.setBlendingEnabled(enable);
	}

	@Override
	public void setColorWriteMask(boolean red, boolean green, boolean blue, boolean alpha) {
		coreD.setColorWriteMask(red, green, blue, alpha);
	}

	@Override
	public void setDepthOffsets(float factor, float units) {
		coreD.setDepthOffsets(factor, units);
	}

	@Override
	public void setDepthOffsetsEnabled(boolean enable) {
		coreD.setDepthOffsetsEnabled(enable);
	}

	@Override
	public void setDepthTest(Comparison test) {
		coreD.setDepthTest(test);
	}

	@Override
	public void setDepthWriteMask(boolean mask) {
		coreD.setDepthWriteMask(mask);
	}

	@Override
	public void setDrawStyle(DrawStyle style) {
		coreD.setDrawStyle(style);
	}

	@Override
	public void setDrawStyle(DrawStyle front, DrawStyle back) {
		coreD.setDrawStyle(front, back);
	}

	@Override
	public void setStencilTest(Comparison test, int refValue, int testMask) {
		coreD.setStencilTest(test, refValue, testMask);
	}

	@Override
	public void setStencilTestBack(Comparison test, int refValue, int testMask) {
		coreD.setStencilTestBack(test, refValue, testMask);
	}

	@Override
	public void setStencilTestEnabled(boolean enable) {
		coreD.setStencilTestEnabled(enable);
	}

	@Override
	public void setStencilTestFront(Comparison test, int refValue, int testMask) {
		coreD.setStencilTestFront(test, refValue, testMask);
	}

	@Override
	public void setStencilUpdateOps(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass) {
		coreD.setStencilUpdateOps(stencilFail, depthFail, depthPass);
	}

	@Override
	public void setStencilUpdateOpsBack(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass) {
		coreD.setStencilUpdateOpsBack(stencilFail, depthFail, depthPass);
	}

	@Override
	public void setStencilUpdateOpsFront(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass) {
		coreD.setStencilUpdateOpsFront(stencilFail, depthFail, depthPass);
	}

	@Override
	public void setStencilWriteMask(int mask) {
		coreD.setStencilWriteMask(mask);
	}

	@Override
	public void setStencilWriteMask(int front, int back) {
		coreD.setStencilWriteMask(front, back);
	}
}
