package com.ferox.renderer.impl;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.ResourceManager.LockToken;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Mipmap;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.TextureFormat;

/**
 * AbstractTextureSurface is a mostly complete implementation of TextureSurface
 * that is also an AbstractSurface. It handles the creation and updating of the
 * necessary Texture resources for the TextureSurface, based on the input
 * TextureSurfaceOptions.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractTextureSurface extends AbstractSurface implements TextureSurface {
    private volatile int activeLayer;
    private volatile int activeDepth;
    
    private final Texture[] colorTextures;
    private final Texture depthTexture;
    
    private final TextureSurfaceOptions options;
    
    private final LockToken<? extends Texture>[] colorLocks;
    private LockToken<? extends Texture> depthLock;
    
    @SuppressWarnings("unchecked")
    public AbstractTextureSurface(AbstractFramework framework, TextureSurfaceOptions options) {
        super(framework);
        if (options == null)
            options = new TextureSurfaceOptions();
        
        RenderCapabilities caps = framework.getCapabilities();
        options = validateFormat(options, caps);
        options = validateDimensions(options, caps);
        
        colorTextures = createColorTextures(this, options);
        depthTexture = createDepthTexture(this, options);
        
        updateTextures(colorTextures, depthTexture, framework);

        this.options = options;
        activeLayer = options.getActiveLayer();
        activeDepth = options.getActiveDepthPlane();
        
        colorLocks = new LockToken[colorTextures.length];
    }
    
    protected ResourceHandle getDepthHandle() {
        return depthLock.getResourceHandle();
    }
    
    protected ResourceHandle getColorHandle(int buffer) {
        return colorLocks[buffer].getResourceHandle();
    }
    
    @Override
    public void onSurfaceActivate(OpenGLContext context, int activeLayer) {
        super.onSurfaceActivate(context, activeLayer);
        
        // lock all used textures completely so that even if they aren't
        // mutated/read by another surface when being rendered into
        ResourceManager manager = getFramework().getResourceManager();
        for (int i = 0; i < colorLocks.length; i++) {
            colorLocks[i] = manager.acquireFullLock(context, colorTextures[i]);
        }
        
        if (depthTexture != null)
            depthLock = manager.acquireFullLock(context, depthTexture);
        else
            depthLock = null;
    }
    
    @Override
    public void onSurfaceDeactivate(OpenGLContext context) {
        super.onSurfaceDeactivate(context);
        
        // unlock all held locks in reverse order they were acquired
        ResourceManager manager = getFramework().getResourceManager();
        if (depthLock != null) {
            manager.unlock(depthLock);
            depthLock = null;
        }
        
        for (int i = colorLocks.length - 1; i >= 0; i--) {
            manager.unlock(colorLocks[i]);
            colorLocks[i] = null;
        }
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
        return colorTextures[buffer];
    }

    @Override
    public Texture getDepthBuffer() {
        return depthTexture;
    }

    @Override
    public int getNumColorBuffers() {
        return colorTextures.length;
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
    
    private static Texture[] createColorTextures(AbstractTextureSurface owner, TextureSurfaceOptions options) {
        Texture[] colorTextures = new Texture[options.getNumColorBuffers()];
        
        if (options.getTarget() == Target.T_CUBEMAP) {
            for (int i = 0; i < colorTextures.length; i++) {
                Mipmap[] mips = new Mipmap[] { createMipmap(options.getColorBufferFormat(i), options),
                                               createMipmap(options.getColorBufferFormat(i), options),
                                               createMipmap(options.getColorBufferFormat(i), options),
                                               createMipmap(options.getColorBufferFormat(i), options),
                                               createMipmap(options.getColorBufferFormat(i), options),
                                               createMipmap(options.getColorBufferFormat(i), options) };
                colorTextures[i] = new OwnedTexture(owner, options.getTarget(), mips);
            }
        } else {
            for (int i = 0; i < colorTextures.length; i++)
                colorTextures[i] = new OwnedTexture(owner, options.getTarget(), 
                                                    new Mipmap[] { createMipmap(options.getColorBufferFormat(i), options) });
        }
        
        return colorTextures;
    }
        
    private static Mipmap createMipmap(TextureFormat format, TextureSurfaceOptions options) {
        DataType type = format.getSupportedType();
        if (type == null)
            type = DataType.FLOAT;
        return new Mipmap(type, false, options.getWidth(), options.getHeight(), options.getDepth(), format);
    }
    
    private static Texture createDepthTexture(AbstractTextureSurface owner, TextureSurfaceOptions options) {
        if (options.hasDepthTexture()) {
            return new OwnedTexture(owner, options.getTarget(), 
                                    new Mipmap[] { createMipmap(TextureFormat.DEPTH, options) });
        } else
            return null;
    }
    
    private static void updateTextures(Texture[] color, Texture depth, AbstractFramework framework) {
        ContextManager contextManager = framework.getContextManager();
        if (contextManager.isContextThread()) {
            // Don't use the Framework methods since then we'll deadblock
            OpenGLContext context = contextManager.ensureContext();
            ResourceManager resourceManager = framework.getResourceManager();
            
            for (int i = 0; i < color.length; i++) {
                if (resourceManager.update(context, color[i]) != Status.READY) {
                    // Something went wrong, so we should fail
                    throw new SurfaceCreationException("Requested options created an invalid color texture, cannot construct the TextureSurface");
                }
            }
            if (depth != null) {
                if (resourceManager.update(context, depth) != Status.READY){ 
                    // Fail like before
                    throw new SurfaceCreationException("Requested options created an unsupported depth texture, cannot construct the TextureSurface");
                }
            }
        } else {
            // Just use the Framework methods to schedule update tasks
            for (int i = 0; i < color.length; i++) {
                if (framework.update(color[i]) != Status.READY) {
                    // Something went wrong, so we should fail
                    throw new SurfaceCreationException("Requested options created an invalid color texture, cannot construct the TextureSurface");
                }
            }
            if (depth != null) {
                if (framework.update(depth) != Status.READY){ 
                    // Fail like before
                    throw new SurfaceCreationException("Requested options created an unsupported depth texture, cannot construct the TextureSurface");
                }
            }
        }
    }
    
    private static int ceilPot(int num) {
        int pot = 1;
        while (pot < num)
            pot = pot << 1;
        return pot;
    }
    
    /**
     * A Texture extension that overrides {@link #getOwner()} to return a non-null
     * TextureSurface.
     * 
     * @author Michael Ludwig
     */
    private static class OwnedTexture extends Texture {
        private TextureSurface owner;
        
        public OwnedTexture(TextureSurface owner, Target target, Mipmap[] mipmaps) {
            super(target, mipmaps);
            this.owner = owner;
        }
        
        @Override
        public synchronized TextureSurface getOwner() {
            if (owner != null && owner.isDestroyed())
                owner = null;
            return owner;
        }
    }
}
