/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.impl;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.*;

/**
 *
 */
public class DebugFixedFunctionRenderer implements FixedFunctionRenderer, Activateable {
    private final FixedFunctionRenderer renderer;
    private final OpenGLContext context;

    public DebugFixedFunctionRenderer(OpenGLContext context, FixedFunctionRenderer renderer) {
        this.context = context;
        this.renderer = renderer;
    }

    private void checkGLErrors() {
        String error = context.checkGLErrors();
        if (error != null) {
            throw new FrameworkException("OpenGL error: " + error);
        }
    }

    @Override
    public ContextState<FixedFunctionRenderer> getCurrentState() {
        return renderer.getCurrentState();
    }

    @Override
    public void setCurrentState(ContextState<FixedFunctionRenderer> state) {
        renderer.setCurrentState(state);
        checkGLErrors();
    }

    @Override
    public void setFogEnabled(boolean enable) {
        renderer.setFogEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setFogColor(@Const Vector4 color) {
        renderer.setFogColor(color);
        checkGLErrors();
    }

    @Override
    public void setFogLinear(double start, double end) {
        renderer.setFogLinear(start, end);
        checkGLErrors();
    }

    @Override
    public void setFogExponential(double density, boolean squared) {
        renderer.setFogExponential(density, squared);
        checkGLErrors();
    }

    @Override
    public void setAlphaTest(Comparison test, double refValue) {
        renderer.setAlphaTest(test, refValue);
        checkGLErrors();
    }

    @Override
    public void setLightingEnabled(boolean enable) {
        renderer.setLightingEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setGlobalAmbientLight(@Const Vector4 ambient) {
        renderer.setGlobalAmbientLight(ambient);
        checkGLErrors();
    }

    @Override
    public void setLightEnabled(int light, boolean enable) {
        renderer.setLightEnabled(light, enable);
        checkGLErrors();
    }

    @Override
    public void setLightPosition(int light, @Const Vector4 pos) {
        renderer.setLightPosition(light, pos);
        checkGLErrors();
    }

    @Override
    public void setLightColor(int light, @Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec) {
        renderer.setLightColor(light, amb, diff, spec);
        checkGLErrors();
    }

    @Override
    public void setSpotlight(int light, @Const Vector3 dir, double angle, double exponent) {
        renderer.setSpotlight(light, dir, angle, exponent);
        checkGLErrors();
    }

    @Override
    public void setLightAttenuation(int light, double constant, double linear, double quadratic) {
        renderer.setLightAttenuation(light, constant, linear, quadratic);
        checkGLErrors();
    }

    @Override
    public void setMaterial(@Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec,
                            @Const Vector4 emm) {
        renderer.setMaterial(amb, diff, spec, emm);
        checkGLErrors();
    }

    @Override
    public void setMaterialDiffuse(@Const Vector4 diff) {
        renderer.setMaterialDiffuse(diff);
        checkGLErrors();
    }

    @Override
    public void setMaterialAmbient(@Const Vector4 amb) {
        renderer.setMaterialAmbient(amb);
        checkGLErrors();
    }

    @Override
    public void setMaterialSpecular(@Const Vector4 spec) {
        renderer.setMaterialSpecular(spec);
        checkGLErrors();
    }

    @Override
    public void setMaterialEmissive(@Const Vector4 emm) {
        renderer.setMaterialEmissive(emm);
        checkGLErrors();
    }

    @Override
    public void setMaterialShininess(double shininess) {
        renderer.setMaterialShininess(shininess);
        checkGLErrors();
    }

    @Override
    public void setTexture(int tex, Sampler image) {
        renderer.setTexture(tex, image);
        checkGLErrors();
    }

    @Override
    public void setTextureColor(int tex, @Const Vector4 color) {
        renderer.setTextureColor(tex, color);
        checkGLErrors();
    }

    @Override
    public void setTextureCoordinateSource(int tex, TexCoordSource gen) {
        renderer.setTextureCoordinateSource(tex, gen);
        checkGLErrors();
    }

    @Override
    public void setTextureObjectPlanes(int tex, @Const Matrix4 planes) {
        renderer.setTextureObjectPlanes(tex, planes);
        checkGLErrors();
    }

    @Override
    public void setTextureEyePlanes(int tex, @Const Matrix4 planes) {
        renderer.setTextureEyePlanes(tex, planes);
        checkGLErrors();
    }

    @Override
    public void setTextureTransform(int tex, @Const Matrix4 matrix) {
        renderer.setTextureTransform(tex, matrix);
        checkGLErrors();
    }

    @Override
    public void setTextureCombineRGB(int tex, CombineFunction function, CombineSource src0,
                                     CombineOperand op0, CombineSource src1, CombineOperand op1,
                                     CombineSource src2, CombineOperand op2) {
        renderer.setTextureCombineRGB(tex, function, src0, op0, src1, op1, src2, op2);
        checkGLErrors();
    }

    @Override
    public void setTextureCombineAlpha(int tex, CombineFunction function, CombineSource src0,
                                       CombineOperand op0, CombineSource src1, CombineOperand op1,
                                       CombineSource src2, CombineOperand op2) {
        renderer.setTextureCombineAlpha(tex, function, src0, op0, src1, op1, src2, op2);
        checkGLErrors();
    }

    @Override
    public void setProjectionMatrix(@Const Matrix4 projection) {
        renderer.setProjectionMatrix(projection);
        checkGLErrors();
    }

    @Override
    public void setModelViewMatrix(@Const Matrix4 modelView) {
        renderer.setModelViewMatrix(modelView);
        checkGLErrors();
    }

    @Override
    public void setVertices(VertexAttribute vertices) {
        renderer.setVertices(vertices);
        checkGLErrors();
    }

    @Override
    public void setNormals(VertexAttribute normals) {
        renderer.setNormals(normals);
        checkGLErrors();
    }

    @Override
    public void setColors(VertexAttribute colors) {
        renderer.setColors(colors);
        checkGLErrors();
    }

    @Override
    public void setTextureCoordinates(int tex, VertexAttribute texCoords) {
        renderer.setTextureCoordinates(tex, texCoords);
        checkGLErrors();
    }

    @Override
    public void setPointAntiAliasingEnabled(boolean enable) {
        renderer.setPointAntiAliasingEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setLineAntiAliasingEnabled(boolean enable) {
        renderer.setLineAntiAliasingEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setPolygonAntiAliasingEnabled(boolean enable) {
        renderer.setPolygonAntiAliasingEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setPointSize(double width) {
        renderer.setPointSize(width);
        checkGLErrors();
    }

    @Override
    public void setLineSize(double width) {
        renderer.setLineSize(width);
        checkGLErrors();
    }

    @Override
    public void setStencilUpdate(StencilUpdate stencilFail, StencilUpdate depthFail,
                                 StencilUpdate depthPass) {
        renderer.setStencilUpdate(stencilFail, depthFail, depthPass);
        checkGLErrors();
    }

    @Override
    public void setStencilUpdateFront(StencilUpdate stencilFail, StencilUpdate depthFail,
                                      StencilUpdate depthPass) {
        renderer.setStencilUpdateFront(stencilFail, depthFail, depthPass);
        checkGLErrors();
    }

    @Override
    public void setStencilUpdateBack(StencilUpdate stencilFail, StencilUpdate depthFail,
                                     StencilUpdate depthPass) {
        renderer.setStencilUpdateBack(stencilFail, depthFail, depthPass);
        checkGLErrors();
    }

    @Override
    public void setStencilTest(Comparison test, int refValue, int testMask) {
        renderer.setStencilTest(test, refValue, testMask);
        checkGLErrors();
    }

    @Override
    public void setStencilTestFront(Comparison test, int refValue, int testMask) {
        renderer.setStencilTestFront(test, refValue, testMask);
        checkGLErrors();
    }

    @Override
    public void setStencilTestBack(Comparison test, int refValue, int testMask) {
        renderer.setStencilTestBack(test, refValue, testMask);
        checkGLErrors();
    }

    @Override
    public void setStencilTestEnabled(boolean enable) {
        renderer.setStencilTestEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setStencilWriteMask(int mask) {
        renderer.setStencilWriteMask(mask);
        checkGLErrors();
    }

    @Override
    public void setStencilWriteMask(int front, int back) {
        renderer.setStencilWriteMask(front, back);
        checkGLErrors();
    }

    @Override
    public void setDepthTest(Comparison test) {
        renderer.setDepthTest(test);
        checkGLErrors();
    }

    @Override
    public void setDepthWriteMask(boolean mask) {
        renderer.setDepthWriteMask(mask);
        checkGLErrors();
    }

    @Override
    public void setDepthOffsets(double factor, double units) {
        renderer.setDepthOffsets(factor, units);
        checkGLErrors();
    }

    @Override
    public void setDepthOffsetsEnabled(boolean enable) {
        renderer.setDepthOffsetsEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setDrawStyle(DrawStyle style) {
        renderer.setDrawStyle(style);
        checkGLErrors();
    }

    @Override
    public void setDrawStyle(DrawStyle front, DrawStyle back) {
        renderer.setDrawStyle(front, back);
        checkGLErrors();
    }

    @Override
    public void setColorWriteMask(boolean red, boolean green, boolean blue, boolean alpha) {
        renderer.setColorWriteMask(red, green, blue, alpha);
        checkGLErrors();
    }

    @Override
    public void setBlendingEnabled(boolean enable) {
        renderer.setBlendingEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setBlendColor(@Const Vector4 color) {
        renderer.setBlendColor(color);
        checkGLErrors();
    }

    @Override
    public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst) {
        renderer.setBlendMode(function, src, dst);
        checkGLErrors();
    }

    @Override
    public void setBlendModeRGB(BlendFunction function, BlendFactor src, BlendFactor dst) {
        renderer.setBlendModeRGB(function, src, dst);
        checkGLErrors();
    }

    @Override
    public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst) {
        renderer.setBlendModeAlpha(function, src, dst);
        checkGLErrors();
    }

    @Override
    public void setIndices(ElementBuffer indices) {
        renderer.setIndices(indices);
        checkGLErrors();
    }

    @Override
    public int render(PolygonType polyType, int offset, int count) {
        int rendered = renderer.render(polyType, offset, count);
        checkGLErrors();
        return rendered;
    }

    @Override
    public void reset() {
        renderer.reset();
        checkGLErrors();
    }

    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil) {
        renderer.clear(clearColor, clearDepth, clearStencil);
        checkGLErrors();
    }

    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, @Const Vector4 color,
                      double depth, int stencil) {
        renderer.clear(clearColor, clearDepth, clearStencil, color, depth, stencil);
        checkGLErrors();
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        renderer.setViewport(x, y, width, height);
        checkGLErrors();
    }

    @Override
    public void activate(AbstractSurface active) {
        if (renderer instanceof Activateable) {
            ((Activateable) renderer).activate(active);
        }
    }
}
