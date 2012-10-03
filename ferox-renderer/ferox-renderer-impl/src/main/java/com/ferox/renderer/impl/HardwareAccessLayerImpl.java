package com.ferox.renderer.impl;

import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.Surface;
import com.ferox.renderer.TextureSurface;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Texture.Target;

/**
 * HardwareAccessLayerImpl is a simple implementation of
 * {@link HardwareAccessLayer} for use with the AbstractFramework. It uses the
 * ContextManager and ResourceManager of the framework to manage the active
 * surfaces and handle resource operations.
 * 
 * @author Michael Ludwig
 */
public class HardwareAccessLayerImpl implements HardwareAccessLayer {
    private final AbstractFramework framework;

    /**
     * Create a new HardwareAccessLayerImpl that will use the ContextManager and
     * ResourceManager of the given AbstractFramework
     * 
     * @param framework The framework this layer will be used with
     * @throws NullPointerException if framework is null
     */
    public HardwareAccessLayerImpl(AbstractFramework framework) {
        if (framework == null) {
            throw new NullPointerException("Framework cannot be null");
        }

        this.framework = framework;
    }

    @Override
    public Context setActiveSurface(Surface surface) {
        // Special handling for TextureSurface
        if (surface instanceof TextureSurface) {
            TextureSurface ts = (TextureSurface) surface;
            if (ts.getTarget() == Target.T_3D) {
                return setActiveSurface(ts, ts.getActiveDepthPlane());
            } else if (ts.getTarget() == Target.T_CUBEMAP) {
                return setActiveSurface(ts, ts.getActiveLayer());
            } else {
                return setActiveSurface(ts, 0);
            }
        }

        // Validate the Framework of the surface, we don't check destroyed
        // since that will be handled by the ContextManager
        if (surface != null && framework != surface.getFramework()) {
            throw new IllegalArgumentException("Surface is not owned by the current Framework");
        }

        // Since this isn't a TextureSurface, there is no need to validate
        // the layer and we just use 0.
        OpenGLContext context = framework.getContextManager()
                                         .setActiveSurface((AbstractSurface) surface, 0);
        if (context == null) {
            return null;
        }
        return new ContextImpl(context, (AbstractSurface) surface);
    }

    @Override
    public Context setActiveSurface(TextureSurface surface, int layer) {
        if (surface != null) {
            // Validate the Framework of the surface, we don't check destroyed
            // since that will be handled by the ContextManager
            if (framework != surface.getFramework()) {
                throw new IllegalArgumentException("Surface is not owned by the current Framework");
            }

            // Validate the layer argument
            int maxLayer;
            switch (surface.getTarget()) {
            case T_3D:
                maxLayer = surface.getDepth();
                break;
            case T_CUBEMAP:
                maxLayer = surface.getNumLayers();
                break;
            default:
                maxLayer = 1;
                break;
            }

            if (layer >= maxLayer || layer < 0) {
                throw new IllegalArgumentException("Layer is out of range, must be in [0, " + maxLayer + "), not: " + layer);
            }
        }

        OpenGLContext context = framework.getContextManager()
                                         .setActiveSurface((AbstractSurface) surface,
                                                           layer);
        if (context == null) {
            return null;
        }
        return new ContextImpl(context, (AbstractSurface) surface);
    }

    @Override
    public <R extends Resource> Status update(R resource) {
        OpenGLContext context = framework.getContextManager().ensureContext();
        return framework.getResourceManager().update(context, resource);
    }

    @Override
    public <R extends Resource> void dispose(R resource) {
        OpenGLContext context = framework.getContextManager().ensureContext();
        framework.getResourceManager().dispose(context, resource);
    }

    @Override
    public <R extends Resource> void reset(R resource) {
        framework.getResourceManager().reset(resource);
    }

    private class ContextImpl implements Context {
        private final OpenGLContext context;
        private final AbstractSurface surface;

        private Renderer selectedRenderer;

        public ContextImpl(OpenGLContext context, AbstractSurface surface) {
            this.context = context;
            this.surface = surface;
        }

        @Override
        public GlslRenderer getGlslRenderer() {
            if (selectedRenderer == null) {
                // need to select a renderer
                selectedRenderer = context.getRendererProvider()
                                          .getGlslRenderer(context.getRenderCapabilities());

                if (selectedRenderer != null) {
                    // have selected a GlslRenderer to use
                    selectedRenderer.setViewport(0, 0, surface.getWidth(),
                                                 surface.getHeight());
                }
            }

            if (selectedRenderer instanceof FixedFunctionRenderer) {
                throw new IllegalStateException("FixedFunctionRenderer already selected");
            } else {
                return (GlslRenderer) selectedRenderer; // may be null
            }
        }

        @Override
        public FixedFunctionRenderer getFixedFunctionRenderer() {
            if (selectedRenderer == null) {
                // need to select a renderer
                selectedRenderer = context.getRendererProvider()
                                          .getFixedFunctionRenderer(context.getRenderCapabilities());

                if (selectedRenderer != null) {
                    // have selected a FixedFunctionRenderer to use
                    selectedRenderer.setViewport(0, 0, surface.getWidth(),
                                                 surface.getHeight());
                }
            }

            if (selectedRenderer instanceof GlslRenderer) {
                throw new IllegalStateException("GlslRenderer already selected");
            } else {
                return (FixedFunctionRenderer) selectedRenderer; // may be null
            }
        }

        @Override
        public void flush() {
            surface.flush(context);
        }

        @Override
        public boolean hasGlslRenderer() {
            return context.getRendererProvider()
                          .getGlslRenderer(context.getRenderCapabilities()) != null;
        }

        @Override
        public boolean hasFixedFunctionRenderer() {
            return context.getRendererProvider()
                          .getFixedFunctionRenderer(context.getRenderCapabilities()) != null;
        }
    }
}
