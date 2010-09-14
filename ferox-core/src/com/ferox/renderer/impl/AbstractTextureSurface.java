package com.ferox.renderer.impl;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.resource.Mipmap;
import com.ferox.resource.Texture;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.Texture.Target;

/**
 * AbstractTextureSurface is a mostly complete implementation of TextureSurface
 * that is also an AbstractSurface. In most situations, the implementation
 * relies on a {@link TextureSurfaceDelegate} to provide the true functionality.
 * This allows the TextureSurface to rely on pbuffers or fbos easily.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractTextureSurface extends AbstractSurface implements TextureSurface {
    private volatile int activeLayer;
    private volatile int activeDepth;
    
    private int renderLayer;
    private int renderDepth;
    private boolean passRendered;
    
    private final TextureSurfaceDelegate delegate;
    private final TextureSurfaceOptions options;
    
    public AbstractTextureSurface(AbstractFramework framework, TextureSurfaceOptions options) {
        super(framework);
        if (options == null)
            options = new TextureSurfaceOptions();
        
        RenderCapabilities caps = framework.getCapabilities();
        options = validateFormat(options, caps);
        options = validateDimensions(options, caps);
        
        Texture[] colors = createColorTextures(options);
        Texture depth = createDepthTexture(options);
        
        ResourceManager manager = ((AbstractFramework) framework).getResourceManager();
        updateTextures(colors, depth, manager);
        
        this.options = options;
        activeLayer = options.getActiveLayer();
        activeDepth = options.getActiveDepthPlane();
        
        renderLayer = -1;
        renderDepth = -1;
        passRendered = false;
        try {
            // delegate to the subclass to provide true implementation of texture surface
            delegate = createDelegate(colors, depth, options);
        } catch(RuntimeException re) {
            for (int i = 0; i < colors.length; i++)
                manager.scheduleDispose(colors[i]);
            if (depth != null)
                manager.scheduleDispose(depth);
            
            throw new SurfaceCreationException("Exception occurred while creating TextureSurfaceDelegate", re);
        }
    }
    
    protected abstract TextureSurfaceDelegate createDelegate(Texture[] colors, Texture depth, TextureSurfaceOptions options);

    /**
     * Notify the AbstractTextureSurface of the layer and depth it is to render
     * the next pass into. Validation is performed on the layer and depth
     * arguments, but it is assumed that this is invoked within the paired
     * invocations of the pre/post actions of the AbstractSurface.
     * 
     * @param layer The layer to render into
     * @param depth The depth to render into
     * @throws IllegalArgumentException if layer or depth are invalid
     */
    public void setRenderLayer(int layer, int depth) {
        if (layer < 0 || layer >= getNumLayers())
            throw new IllegalArgumentException("Invalid layer");
        if (depth < 0 || depth >= getDepth())
            throw new IllegalArgumentException("Invalid depth");
        
        if (layer != renderLayer || depth != renderDepth) {
            if (passRendered)
                delegate.flushLayer();
            delegate.setLayer(layer, depth);
        }
        
        renderLayer = layer;
        renderDepth = depth;
        passRendered = true;
    }
    
    @Override
    public Context getContext() {
        return delegate.getContext();
    }
    
    @Override
    protected void init() {
        delegate.init();
    }

    @Override
    protected void postRender(Action next) {
        if (passRendered)
            delegate.flushLayer();
        delegate.postRender(next);
        passRendered = false;
        renderDepth = -1;
        renderLayer = -1;
    }

    @Override
    protected void preRender() {
        delegate.preRender();
    }
    
    @Override
    protected void destroyImpl() {
        delegate.destroy();
    }
    
    @Override
    public TextureSurfaceOptions getOptions() {
        return options;
    }

    @Override
    public int getActiveDepthPlane() {
        return activeDepth;
    }

    @Override
    public int getActiveLayer() {
        return activeLayer;
    }

    @Override
    public Texture getColorBuffer(int buffer) {
        return delegate.getColorBuffers()[buffer];
    }

    @Override
    public Texture getDepthBuffer() {
        return delegate.getDepthBuffer();
    }

    @Override
    public int getNumColorBuffers() {
        return delegate.getColorBuffers().length;
    }

    @Override
    public Target getTarget() {
        return options.getTarget();
    }

    @Override
    public void setActiveDepthPlane(int depth) {
        if (depth < 0 || depth >= getDepth())
            throw new IllegalArgumentException("Active depth is invalid: " + depth);
        activeDepth = depth;
    }

    @Override
    public void setActiveLayer(int layer) {
        if (layer < 0 || layer >= getNumLayers())
            throw new IllegalArgumentException("Active layer is invalid: " + layer);
        activeLayer = layer;
    }

    @Override
    public int getHeight() {
        return options.getHeight();
    }

    @Override
    public int getWidth() {
        return options.getWidth();
    }
    
    @Override
    public int getDepth() {
        return options.getDepth();
    }

    @Override
    public int getNumLayers() {
        if (getTarget() == Target.T_CUBEMAP)
            return 6;
        else
            return 1;
    }
    
    /*
     * Internal validation and option-modifications
     */
    
    private static TextureSurfaceOptions validateFormat(TextureSurfaceOptions options, RenderCapabilities caps) {
        int numBuffers = Math.min(caps.getMaxColorBuffers(), options.getNumColorBuffers());
        
        TextureFormat[] formats = new TextureFormat[numBuffers];
        for (int i = 0; i < formats.length; i++) {
            formats[i] = options.getColorBufferFormat(i);
            if (formats[i].isCompressed())
                throw new SurfaceCreationException("Cannot create a TextureSurface using a compressed format: " + formats[i]);
            
            if (!caps.getUnclampedFloatTextureSupport() && 
                (formats[i] == TextureFormat.RGB_FLOAT || formats[i] == TextureFormat.RGBA_FLOAT))
                formats[i] = (formats[i] == TextureFormat.RGB_FLOAT ? TextureFormat.RGB : TextureFormat.RGBA);
        }
        
        // FIXME: update capabilities to encode support for depth cubemaps
        options = options.setColorBufferFormats(formats);
        if (options.hasDepthTexture() && (options.getTarget() == Target.T_CUBEMAP || options.getTarget() == Target.T_3D))
            options = options.setUseDepthTexture(false);
        if (options.hasDepthTexture() && !caps.getDepthTextureSupport())
            options = options.setUseDepthTexture(false);
        return options;
    }
    
    private static TextureSurfaceOptions validateDimensions(TextureSurfaceOptions options, RenderCapabilities caps) {
        // set unneeded dimensions to expected values for target
        switch(options.getTarget()) {
        case T_1D:
            options = options.setHeight(1).setDepth(1);
            break;
        case T_2D:
            options = options.setDepth(1);
            break;
        case T_CUBEMAP:
            options = options.setHeight(options.getWidth()).setDepth(1);
            break;
        }
        
        if (!caps.getNpotTextureSupport()) {
            // make dimensions power of two's
            switch(options.getTarget()) {
            case T_3D:
                options = options.setDepth(ceilPot(options.getDepth()));
            case T_2D:
            case T_CUBEMAP:
                options = options.setHeight(ceilPot(options.getHeight()));
            case T_1D:
                options = options.setWidth(ceilPot(options.getWidth()));
            }
        }

        // clamp the dimensions to the max supported size
        int maxDimension = 0;
        switch(options.getTarget()) {
        case T_1D:
        case T_2D:
            maxDimension = caps.getMaxTextureSize();
            break;
        case T_CUBEMAP:
            maxDimension = caps.getMaxTextureCubeMapSize();
            break;
        case T_3D:
            maxDimension = caps.getMaxTexture3DSize();
            break;
        }
        maxDimension = Math.min(maxDimension, caps.getMaxTextureSurfaceSize());
        
        options = options.setWidth(Math.min(options.getWidth(), maxDimension))
                         .setHeight(Math.min(options.getHeight(), maxDimension))
                         .setDepth(Math.min(options.getDepth(), maxDimension));

        if (options.getActiveDepthPlane() >= options.getDepth())
            options = options.setActiveDepthPlane(options.getDepth() - 1);
        if (options.getTarget() == Target.T_CUBEMAP) {
            // cube map has 6 layers
            options = options.setActiveLayer(Math.min(options.getActiveLayer(), 6));
        } else
            options = options.setActiveLayer(0);
        
        return options;
    }
    
    private static Texture[] createColorTextures(TextureSurfaceOptions options) {
        Texture[] colorTextures = new Texture[options.getNumColorBuffers()];
        
        if (options.getTarget() == Target.T_CUBEMAP) {
            for (int i = 0; i < colorTextures.length; i++) {
                Mipmap[] mips = new Mipmap[] { createMipmap(options.getColorBufferFormat(i), options),
                                               createMipmap(options.getColorBufferFormat(i), options),
                                               createMipmap(options.getColorBufferFormat(i), options),
                                               createMipmap(options.getColorBufferFormat(i), options),
                                               createMipmap(options.getColorBufferFormat(i), options),
                                               createMipmap(options.getColorBufferFormat(i), options) };
                colorTextures[i] = new Texture(options.getTarget(), mips);
            }
        } else {
            for (int i = 0; i < colorTextures.length; i++)
                colorTextures[i] = new Texture(options.getTarget(), createMipmap(options.getColorBufferFormat(i), options));
        }
        
        return colorTextures;
    }
        
    private static Mipmap createMipmap(TextureFormat format, TextureSurfaceOptions options) {
        Class<? extends Buffer> type = format.getSupportedType();
        if (type == null)
            type = FloatBuffer.class;
        return new Mipmap(type, false, options.getWidth(), options.getHeight(), options.getDepth(), format);
    }
    
    private static Texture createDepthTexture(TextureSurfaceOptions options) {
        if (options.hasDepthTexture()) {
            return new Texture(options.getTarget(), createMipmap(TextureFormat.DEPTH, options));
        } else
            return null;
    }
    
    private static void updateTextures(Texture[] color, Texture depth, ResourceManager manager) {
        for (int i = 0; i < color.length; i++) {
            if (manager.getHandle(color[i]) == null) {
                // something went wrong, so we should fail
                for (int j = 0; j <= i; j++)
                    manager.scheduleDispose(color[i]);
                throw new SurfaceCreationException("Requested options created an invalid color texture, cannot construct the TextureSurface");
            }
        }
        if (depth != null) {
            if (manager.getHandle(depth) == null) {
                // fail like before
                for (int i = 0; i < color.length; i++)
                    manager.scheduleDispose(color[i]);
                manager.scheduleDispose(depth);
                throw new SurfaceCreationException("Requested options created an unsupported depth texture, cannot construct the TextureSurface");
            }
        }
    }
    
    private static int ceilPot(int num) {
        int pot = 1;
        while (pot < num)
            pot = pot << 1;
        return pot;
    }
}
