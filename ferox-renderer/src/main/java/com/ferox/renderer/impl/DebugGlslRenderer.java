package com.ferox.renderer.impl;

import com.ferox.math.*;
import com.ferox.renderer.*;

/**
 *
 */
public class DebugGlslRenderer implements GlslRenderer {
    private final GlslRenderer delegate;
    private final OpenGLContext context;

    public DebugGlslRenderer(OpenGLContext context, GlslRenderer delegate) {
        this.delegate = delegate;
        this.context = context;
    }

    private void checkGLErrors() {
        String error = context.checkGLErrors();
        if (error != null) {
            throw new FrameworkException("OpenGL error: " + error);
        }
    }

    @Override
    public ContextState<GlslRenderer> getCurrentState() {
        return delegate.getCurrentState();
    }

    @Override
    public void setCurrentState(ContextState<GlslRenderer> state) {
        delegate.setCurrentState(state);
        checkGLErrors();
    }

    @Override
    public void setShader(Shader shader) {
        delegate.setShader(shader);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, VertexAttribute attr) {
        delegate.bindAttribute(var, attr);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, int column, VertexAttribute attr) {
        delegate.bindAttribute(var, column, attr);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, double val) {
        delegate.bindAttribute(var, val);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, double v1, double v2) {
        delegate.bindAttribute(var, v1, v2);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, @Const Vector3 v) {
        delegate.bindAttribute(var, v);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, @Const Vector4 v) {
        delegate.bindAttribute(var, v);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, double m00, double m01, double m10, double m11) {
        delegate.bindAttribute(var, m00, m01, m10, m11);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, @Const Matrix3 v) {
        delegate.bindAttribute(var, v);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, @Const Matrix4 v) {
        delegate.bindAttribute(var, v);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, int val) {
        delegate.bindAttribute(var, val);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, int v1, int v2) {
        delegate.bindAttribute(var, v1, v2);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, int v1, int v2, int v3) {
        delegate.bindAttribute(var, v1, v2, v3);
        checkGLErrors();
    }

    @Override
    public void bindAttribute(Shader.Attribute var, int v1, int v2, int v3, int v4) {
        delegate.bindAttribute(var, v1, v2, v3, v4);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, double val) {
        delegate.setUniform(var, val);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, double v1, double v2) {
        delegate.setUniform(var, v1, v2);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const Vector3 v) {
        delegate.setUniform(var, v);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const Vector4 v) {
        delegate.setUniform(var, v);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, double m00, double m01, double m10, double m11) {
        delegate.setUniform(var, m00, m01, m10, m11);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const Matrix3 val) {
        delegate.setUniform(var, val);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const Matrix4 val) {
        delegate.setUniform(var, val);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, int val) {
        delegate.setUniform(var, val);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, int v1, int v2) {
        delegate.setUniform(var, v1, v2);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, int v1, int v2, int v3) {
        delegate.setUniform(var, v1, v2, v3);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, int v1, int v2, int v3, int v4) {
        delegate.setUniform(var, v1, v2, v3, v4);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, boolean val) {
        delegate.setUniform(var, val);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, boolean v1, boolean v2) {
        delegate.setUniform(var, v1, v2);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, boolean v1, boolean v2, boolean v3) {
        delegate.setUniform(var, v1, v2, v3);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, boolean v1, boolean v2, boolean v3, boolean v4) {
        delegate.setUniform(var, v1, v2, v3, v4);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, Sampler texture) {
        delegate.setUniform(var, texture);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const ColorRGB color) {
        delegate.setUniform(var, color);
        checkGLErrors();
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const ColorRGB color, boolean isHDR) {
        delegate.setUniform(var, color, isHDR);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, VertexAttribute attr) {
        delegate.bindAttributeArray(var, index, attr);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, int column, VertexAttribute attr) {
        delegate.bindAttributeArray(var, index, column, attr);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, double val) {
        delegate.bindAttributeArray(var, index, val);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, double v1, double v2) {
        delegate.bindAttributeArray(var, index, v1, v2);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, @Const Vector3 v) {
        delegate.bindAttributeArray(var, index, v);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, @Const Vector4 v) {
        delegate.bindAttributeArray(var, index, v);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, double m00, double m01, double m10,
                                   double m11) {
        delegate.bindAttributeArray(var, index, m00, m01, m10, m11);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, @Const Matrix3 v) {
        delegate.bindAttributeArray(var, index, v);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, @Const Matrix4 v) {
        delegate.bindAttributeArray(var, index, v);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, int val) {
        delegate.bindAttributeArray(var, index, val);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, int v1, int v2) {
        delegate.bindAttributeArray(var, index, v1, v2);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, int v1, int v2, int v3) {
        delegate.bindAttributeArray(var, index, v1, v2, v3);
        checkGLErrors();
    }

    @Override
    public void bindAttributeArray(Shader.Attribute var, int index, int v1, int v2, int v3, int v4) {
        delegate.bindAttributeArray(var, index, v1, v2, v3, v4);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, double val) {
        delegate.setUniformArray(var, index, val);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, double v1, double v2) {
        delegate.setUniformArray(var, index, v1, v2);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, @Const Vector3 v) {
        delegate.setUniformArray(var, index, v);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, @Const Vector4 v) {
        delegate.setUniformArray(var, index, v);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, double m00, double m01, double m10,
                                double m11) {
        delegate.setUniformArray(var, index, m00, m01, m10, m11);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, @Const Matrix3 val) {
        delegate.setUniformArray(var, index, val);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, @Const Matrix4 val) {
        delegate.setUniformArray(var, index, val);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, int val) {
        delegate.setUniformArray(var, index, val);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, int v1, int v2) {
        delegate.setUniformArray(var, index, v1, v2);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, int v1, int v2, int v3) {
        delegate.setUniformArray(var, index, v1, v2, v3);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, int v1, int v2, int v3, int v4) {
        delegate.setUniformArray(var, index, v1, v2, v3, v4);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, boolean val) {
        delegate.setUniformArray(var, index, val);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, boolean v1, boolean v2) {
        delegate.setUniformArray(var, index, v1, v2);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, boolean v1, boolean v2, boolean v3) {
        delegate.setUniformArray(var, index, v1, v2, v3);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, boolean v1, boolean v2, boolean v3,
                                boolean v4) {
        delegate.setUniformArray(var, index, v1, v2, v3, v4);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, Sampler texture) {
        delegate.setUniformArray(var, index, texture);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, @Const ColorRGB color) {
        delegate.setUniformArray(var, index, color);
        checkGLErrors();
    }

    @Override
    public void setUniformArray(Shader.Uniform var, int index, @Const ColorRGB color, boolean isHDR) {
        delegate.setUniformArray(var, index, color, isHDR);
        checkGLErrors();
    }

    @Override
    public void setPointAntiAliasingEnabled(boolean enable) {
        delegate.setPointAntiAliasingEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setLineAntiAliasingEnabled(boolean enable) {
        delegate.setLineAntiAliasingEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setPolygonAntiAliasingEnabled(boolean enable) {
        delegate.setPolygonAntiAliasingEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setPointSize(double width) {
        delegate.setPointSize(width);
        checkGLErrors();
    }

    @Override
    public void setLineSize(double width) {
        delegate.setLineSize(width);
        checkGLErrors();
    }

    @Override
    public void setStencilUpdate(StencilUpdate stencilFail, StencilUpdate depthFail,
                                 StencilUpdate depthPass) {
        delegate.setStencilUpdate(stencilFail, depthFail, depthPass);
        checkGLErrors();
    }

    @Override
    public void setStencilUpdateFront(StencilUpdate stencilFail, StencilUpdate depthFail,
                                      StencilUpdate depthPass) {
        delegate.setStencilUpdateFront(stencilFail, depthFail, depthPass);
        checkGLErrors();
    }

    @Override
    public void setStencilUpdateBack(StencilUpdate stencilFail, StencilUpdate depthFail,
                                     StencilUpdate depthPass) {
        delegate.setStencilUpdateBack(stencilFail, depthFail, depthPass);
        checkGLErrors();
    }

    @Override
    public void setStencilTest(Comparison test, int refValue, int testMask) {
        delegate.setStencilTest(test, refValue, testMask);
        checkGLErrors();
    }

    @Override
    public void setStencilTestFront(Comparison test, int refValue, int testMask) {
        delegate.setStencilTestFront(test, refValue, testMask);
        checkGLErrors();
    }

    @Override
    public void setStencilTestBack(Comparison test, int refValue, int testMask) {
        delegate.setStencilTestBack(test, refValue, testMask);
        checkGLErrors();
    }

    @Override
    public void setStencilTestEnabled(boolean enable) {
        delegate.setStencilTestEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setStencilWriteMask(int mask) {
        delegate.setStencilWriteMask(mask);
        checkGLErrors();
    }

    @Override
    public void setStencilWriteMask(int front, int back) {
        delegate.setStencilWriteMask(front, back);
        checkGLErrors();
    }

    @Override
    public void setDepthTest(Comparison test) {
        delegate.setDepthTest(test);
        checkGLErrors();
    }

    @Override
    public void setDepthWriteMask(boolean mask) {
        delegate.setDepthWriteMask(mask);
        checkGLErrors();
    }

    @Override
    public void setDepthOffsets(double factor, double units) {
        delegate.setDepthOffsets(factor, units);
        checkGLErrors();
    }

    @Override
    public void setDepthOffsetsEnabled(boolean enable) {
        delegate.setDepthOffsetsEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setDrawStyle(DrawStyle style) {
        delegate.setDrawStyle(style);
        checkGLErrors();
    }

    @Override
    public void setDrawStyle(DrawStyle front, DrawStyle back) {
        delegate.setDrawStyle(front, back);
        checkGLErrors();
    }

    @Override
    public void setColorWriteMask(boolean red, boolean green, boolean blue, boolean alpha) {
        delegate.setColorWriteMask(red, green, blue, alpha);
        checkGLErrors();
    }

    @Override
    public void setBlendingEnabled(boolean enable) {
        delegate.setBlendingEnabled(enable);
        checkGLErrors();
    }

    @Override
    public void setBlendColor(@Const Vector4 color) {
        delegate.setBlendColor(color);
        checkGLErrors();
    }

    @Override
    public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst) {
        delegate.setBlendMode(function, src, dst);
        checkGLErrors();
    }

    @Override
    public void setBlendModeRGB(BlendFunction function, BlendFactor src, BlendFactor dst) {
        delegate.setBlendModeRGB(function, src, dst);
        checkGLErrors();
    }

    @Override
    public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst) {
        delegate.setBlendModeAlpha(function, src, dst);
        checkGLErrors();
    }

    @Override
    public void setIndices(ElementBuffer indices) {
        delegate.setIndices(indices);
        checkGLErrors();
    }

    @Override
    public int render(PolygonType polyType, int offset, int count) {
        int rendered = delegate.render(polyType, offset, count);
        checkGLErrors();
        return rendered;
    }

    @Override
    public void reset() {
        delegate.reset();
        checkGLErrors();
    }

    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil) {
        delegate.clear(clearColor, clearDepth, clearStencil);
        checkGLErrors();
    }

    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, @Const Vector4 color,
                      double depth, int stencil) {
        delegate.clear(clearColor, clearDepth, clearStencil, color, depth, stencil);
        checkGLErrors();
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        delegate.setViewport(x, y, width, height);
        checkGLErrors();
    }
}
