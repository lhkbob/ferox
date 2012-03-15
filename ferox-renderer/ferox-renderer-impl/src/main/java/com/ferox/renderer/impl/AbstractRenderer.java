package com.ferox.renderer.impl;

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.Renderer;
import com.ferox.resource.VertexBufferObject;

/**
 * <p>
 * AbstractRenderer is the main super class for renderers used by
 * AbstractFrameworks. It takes a single RendererDelegate that handles the
 * actual implementation of the Renderer interface. It is extended by both
 * {@link AbstractFixedFunctionRenderer} and {@link AbstractGlslRenderer}, which
 * complete the implementations for the respective renderer types. It is
 * recommended that if an OpenGLContext provides both a
 * FixedFunctionRenderer and a GlslRenderer that both use the same
 * RendererDelegate instance since that state is shared by the context.
 * </p>
 * <p>
 * AbstractRenderer adds the
 * {@link #activate(AbstractSurface, OpenGLContext, ResourceManager)}
 * method which is invoked by AbstractSurface when it is activated. This
 * provides a hook for renderers to perform custom initialization of the OpenGL
 * state.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractRenderer implements Renderer {
    private final RendererDelegate delegate;
    
    protected OpenGLContext context;
    protected ResourceManager resourceManager;
    
    public AbstractRenderer(RendererDelegate delegate) {
        if (delegate == null)
            throw new NullPointerException("Delegate cannot be null");
        this.delegate = delegate;
    }
    
    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, @Const Vector4 color, float depth, int stencil) {
        delegate.clear(clearColor, clearDepth, clearStencil, color, depth, stencil);
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public void setBlendColor(@Const Vector4 color) {
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
    
    @Override
    public int render(PolygonType polyType, VertexBufferObject indices, int offset, int count) {
        return delegate.render(polyType, indices, offset, count);
    }

    @Override
    public int render(PolygonType polyType, int first, int count) {
        return delegate.render(polyType, first, count);
    }

    /**
     * Notify the renderer that the provided surface has been activated and will
     * be using this Renderer. The given context is the context for the current
     * thread and the ResourceManager is the resource manager of the surface's
     * owning framework.
     * 
     * @param active The now active surface
     * @param context The current context
     * @param resourceManager The ResourceManager to use
     */
    public void activate(AbstractSurface active, OpenGLContext context, ResourceManager resourceManager) {
        delegate.activate(active, context, resourceManager);
        
        this.context = context;
        this.resourceManager = resourceManager;
    }
}
