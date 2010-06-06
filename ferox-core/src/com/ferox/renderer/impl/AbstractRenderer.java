package com.ferox.renderer.impl;

import com.ferox.math.Color4f;
import com.ferox.renderer.Renderer;

public abstract class AbstractRenderer implements Renderer {
    private final RendererDelegate delegate;
    private boolean initialized;
    
    public AbstractRenderer(RendererDelegate delegate) {
        if (delegate == null)
            throw new NullPointerException("Delegate cannot be null");
        this.delegate = delegate;
        initialized = false;
    }
    
    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, Color4f color, float depth, int stencil) {
        delegate.clear(clearColor, clearDepth, clearStencil, color, depth, stencil);
    }

    @Override
    public void reset() {
        if (!initialized) {
            delegate.init();
            init();
            initialized = true;
        }
        delegate.reset();
    }

    @Override
    public void setBlendColor(Color4f color) {
        delegate.setBlendColor(color);
    }

    @Override
    public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst) {
        delegate.setBlendMode(function, src, dst);
    }

    @Override
    public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst) {
        delegate.setBlendModeAlpha(function, src, dst);
    }

    @Override
    public void setBlendModeRgb(BlendFunction function, BlendFactor src, BlendFactor dst) {
        delegate.setBlendModeRgb(function, src, dst);
    }

    @Override
    public void setBlendingEnabled(boolean enable) {
        delegate.setBlendingEnabled(enable);
    }

    @Override
    public void setColorWriteMask(boolean red, boolean green, boolean blue, boolean alpha) {
        delegate.setColorWriteMask(red, green, blue, alpha);
    }

    @Override
    public void setDepthOffsets(float factor, float units) {
        delegate.setDepthOffsets(factor, units);
    }

    @Override
    public void setDepthOffsetsEnabled(boolean enable) {
        delegate.setDepthOffsetsEnabled(enable);
    }

    @Override
    public void setDepthTest(Comparison test) {
        delegate.setDepthTest(test);
    }

    @Override
    public void setDepthWriteMask(boolean mask) {
        delegate.setDepthWriteMask(mask);
    }

    @Override
    public void setDrawStyle(DrawStyle style) {
        delegate.setDrawStyle(style);
    }

    @Override
    public void setDrawStyle(DrawStyle front, DrawStyle back) {
        delegate.setDrawStyle(front, back);
    }

    @Override
    public void setStencilTest(Comparison test, int refValue, int testMask) {
        delegate.setStencilTest(test, refValue, testMask);
    }

    @Override
    public void setStencilTestBack(Comparison test, int refValue, int testMask) {
        delegate.setStencilTestBack(test, refValue, testMask);
    }

    @Override
    public void setStencilTestEnabled(boolean enable) {
        delegate.setStencilTestEnabled(enable);
    }

    @Override
    public void setStencilTestFront(Comparison test, int refValue, int testMask) {
        delegate.setStencilTestFront(test, refValue, testMask);
    }

    @Override
    public void setStencilUpdateOps(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass) {
        delegate.setStencilUpdateOps(stencilFail, depthFail, depthPass);
    }

    @Override
    public void setStencilUpdateOpsBack(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass) {
        delegate.setStencilUpdateOpsBack(stencilFail, depthFail, depthPass);
    }

    @Override
    public void setStencilUpdateOpsFront(StencilOp stencilFail, StencilOp depthFail, StencilOp depthPass) {
        delegate.setStencilUpdateOpsFront(stencilFail, depthFail, depthPass);
    }

    @Override
    public void setStencilWriteMask(int mask) {
        delegate.setStencilWriteMask(mask);
    }

    @Override
    public void setStencilWriteMask(int front, int back) {
        delegate.setStencilWriteMask(front, back);
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        delegate.setViewport(x, y, width, height);
    }
    
    public void setSurfaceSize(int width, int height) {
        delegate.setSurfaceSize(width, height);
    }
    
    protected abstract void init();
}
