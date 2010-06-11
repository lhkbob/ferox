package com.ferox.renderer.impl.resource;

import java.nio.Buffer;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.resource.DirtyState;
import com.ferox.resource.ImageRegion;
import com.ferox.resource.Resource;
import com.ferox.resource.Texture;
import com.ferox.resource.TextureDirtyState;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Texture.Target;

/**
 * AbstractTextureResourceDriver is an almost complete ResourceDriver
 * implementation for the {@link Texture} resource type. Implementations are
 * only required to implement the actual OpenGL calls necessary to transfer the
 * texture data, etc. This ResourceDriver uses {@link TextureHandle} as its
 * resource handle.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractTextureResourceDriver implements ResourceDriver {
    // extensions present
    protected final boolean npotSupported;
    protected final boolean unclampledFloatSupported;
    
    // maximum dimensions
    protected final int maxCubeSize;
    protected final int max3dSize;
    protected final int maxTexSize;
    
    protected final float maxAnisoLevel;
    
    public AbstractTextureResourceDriver(RenderCapabilities caps) {
        if (caps == null)
            throw new NullPointerException("Capabilities cannot be null");
        npotSupported = caps.getNpotTextureSupport();
        unclampledFloatSupported = caps.getUnclampedFloatTextureSupport();
        
        maxCubeSize = caps.getMaxTextureCubeMapSize();
        max3dSize = caps.getMaxTexture3DSize();
        maxTexSize = caps.getMaxTextureSize();
        
        maxAnisoLevel = caps.getMaxAnisotropicLevel();
    }
    
    @Override
    public void dispose(ResourceHandle handle) {
        if (handle.getId() > 0)
            glDeleteTexture((TextureHandle) handle);
    }

    /**
     * Destroy the texture pointed to by the given handle, it can be assumed
     * that the handle's id is greater than 0.
     * 
     * @param handle
     */
    protected abstract void glDeleteTexture(TextureHandle handle);

    @Override
    public ResourceHandle init(Resource res) {
        Texture t = (Texture) res;
        
        String unsupported = getUnsupportedMessage(t);
        if (unsupported != null) {
            // cannot make the texture, and it can never become valid
            // again so we don't need to setup an actual TextureHandle
            return new UnsupportedTextureHandle(unsupported);
        }
        
        TextureFormat format = t.getFormat();
        if (!unclampledFloatSupported) {
            // our best bet is to switch to clamped float formatting
            switch(format) {
            case ALPHA_FLOAT:
                format = TextureFormat.ALPHA;
                break;
            case LUMINANCE_ALPHA_FLOAT:
                format = TextureFormat.LUMINANCE_ALPHA;
                break;
            case LUMINANCE_FLOAT:
                format = TextureFormat.LUMINANCE;
                break;
            case RGB_FLOAT:
                format = TextureFormat.RGB;
                break;
            case RGBA_FLOAT:
                format = TextureFormat.RGBA;
                break;
            }
        }
        TextureHandle handle = glCreateTexture(t.getTarget(), format, t.getDataType());
        handle.setStatus(Status.READY);
        handle.setStatusMessage(getStatusMessage(t, handle));
        
        // use update to allocate the textures
        update(t, handle, null, true);
        return handle;
    }

    /**
     * Create a new TextureHandle with a valid texture id that will have the
     * given target, format and data type.
     * 
     * @param target
     * @param format
     * @param type
     * @return
     */
    protected abstract TextureHandle glCreateTexture(Target target, TextureFormat format, Class<? extends Buffer> type);

    @Override
    public Status update(Resource res, ResourceHandle handle, DirtyState<?> dirtyState) {
        if (handle.getStatus() == Status.READY)
            update((Texture) res, (TextureHandle) handle, (TextureDirtyState) dirtyState, false);
        return handle.getStatus();
    }

    /**
     * Return a String specifying the error status message if the given Texture
     * is unsupported. If the texture is supported (e.g. format/type), then
     * return null. This should only return a non-null error String based on the
     * immutable properties of the Texture, such that an unsupported texture
     * cannot be modified to become supported.
     * 
     * @param tex
     * @return
     */
    protected String getUnsupportedMessage(Texture tex) {
        if (!npotSupported) {
            if (tex.getWidth() != ceilPot(tex.getWidth()) || 
                tex.getHeight() != ceilPot(tex.getHeight()) ||
                tex.getDepth() != ceilPot(tex.getDepth()))
                return "Non-power of two textures are not supported";
        }
        
        int maxSize = 0;
        switch(tex.getTarget()) {
        case T_1D:
        case T_2D:
            maxSize = maxTexSize;
            break;
        case T_CUBEMAP:
            maxSize = maxCubeSize;
            break;
        case T_3D:
            maxSize = max3dSize;
            break;
        }
        
        if (tex.getWidth() > maxSize || tex.getHeight() > maxSize || tex.getDepth() > maxSize)
            return "Dimensions excede supported maximum of " + maxSize;
        
        // dimensions are at least valid, subclasses can override to support more validation
        return null;
    }

    /**
     * @return The status message to use for the given Texture, return ""
     *         instead of null when there is no special message
     */
    protected String getStatusMessage(Texture t, TextureHandle h) {
        if (t.getFormat() != h.format)
            return "TextureFormat changed from " + t.getFormat() + " to " + h.format + " to meet hardware support";
        return "";
    }
    
    private void update(Texture tex, TextureHandle handle, TextureDirtyState dirtyState, boolean newTex) {
        glBindTexture(handle);
        
        if (dirtyState == null || dirtyState.getTextureParametersDirty())
            glTextureParameters(tex, handle);
        
        TextureFormat f = tex.getFormat();
        if (newTex || f.isCompressed() || f == TextureFormat.DEPTH)
            doTexImage(tex, handle, newTex);
        else
            doTexSubImage(tex, handle, dirtyState);
        
        glRestoreTexture();
    }

    /**
     * Set all texture parameters so that the handle has up-to-date values that
     * match its Texture. Only those parameters which have changed actually need
     * to be sent to OpenGL.
     * 
     * @param tex
     * @param handle
     */
    protected abstract void glTextureParameters(Texture tex, TextureHandle handle);

    /**
     * Bind the given TextureHandle to the context on the current Thread so that
     * subsequent calls to
     * {@link #glTexImage(TextureHandle, int, int, int, int, int, int, Buffer)},
     * etc. will update the given handle's image data.
     * 
     * @param handle
     */
    protected abstract void glBindTexture(TextureHandle handle);

    /**
     * Restore the texture bindings to their previous state that existed before
     * the last call to {@link #glBindTexture(TextureHandle)} on the calling
     * Thread.
     */
    protected abstract void glRestoreTexture();
    
    private void doTexImage(Texture tex, TextureHandle handle, boolean newTex) {
        int w, h, d;
        
        for (int i = tex.getBaseMipmapLevel(); i < tex.getMaxMipmapLevel(); i++) {
            w = Math.max(1, tex.getWidth() >> i);
            h = Math.max(1, tex.getHeight() >> i);
            d = Math.max(1, tex.getDepth() >> i);
            
            for (int l = 0; l < tex.getNumLayers(); l++) {
                Buffer data = tex.getMipmap(l).getData(i);
                if (data != null) {
                    // proceed with image allocation
                    glUnpackRegion(0, 0, 0, w, h);
                    glTexImage(handle, l, i, w, h, d, data.capacity(), data);
                } else if (newTex) {
                    // allocate empty image
                    glTexImage(handle, l, i, w, h, d, tex.getFormat().getBufferSize(w, h, d), null);
                }
            }
        }
    }
    
    private void doTexSubImage(Texture tex, TextureHandle handle, TextureDirtyState dirty) {
        int w, h, d;
        
        for (int i = tex.getBaseMipmapLevel(); i < tex.getMaxMipmapLevel(); i++) {
            w = Math.max(1, tex.getWidth() >> i);
            h = Math.max(1, tex.getHeight() >> i);
            d = Math.max(1, tex.getDepth() >> i);
            
            for (int l = 0; l < tex.getNumLayers(); l++) {
                Buffer data = tex.getMipmap(l).getData(i);
                if (data != null) {
                    ImageRegion mdr = (dirty == null ? null : dirty.getDirtyMipmap(l, i));
                    if (dirty == null) {
                        // update the whole image region
                        glUnpackRegion(0, 0, 0, w, h);
                        glTexSubImage(handle, l, i, 0, 0, 0, w, h, d, data);
                    } else if (mdr != null) {
                        // use the dirty image region to perform a smaller update
                        glUnpackRegion(mdr.getXOffset(), mdr.getYOffset(), mdr.getZOffset(), w, h);
                        glTexSubImage(handle, l, i, mdr.getXOffset(), mdr.getYOffset(), mdr.getZOffset(),
                                      mdr.getWidth(), mdr.getHeight(), mdr.getDepth(), data);
                    }
                } 
            }
        }
    }

    /**
     * Use glTexImage{1D/2D/3D} as appropriate to allocate a texture image for
     * the given layer and mipmap on the currently bound texture object. If the
     * TextureHandle's glSrcFormat < 0, the texture is compressed, so the
     * appropriate glCompressedTexImage command should be used instead. The data
     * buffer may be null.
     */
    protected abstract void glTexImage(TextureHandle h, int layer, int mipmap, int width, int height, int depth, int capacity, Buffer data);

    /**
     * As
     * {@link #glTexImage(TextureHandle, int, int, int, int, int, int, Buffer)}
     * but this intended for updating a sub-image within the given layer and
     * mipmap. It can be assumed that the the texture will not be compressed,
     * and that its data array will not be null.
     */
    protected abstract void glTexSubImage(TextureHandle h, int layer, int mipmap, int x, int y, int z, int width, int height, int depth, Buffer data);

    /**
     * Set the unpack region for pixel data to use the given parameters.
     * Additionally, the unpack alignment should be set to 1.
     */
    protected abstract void glUnpackRegion(int xOffset, int yOffset, int zOffset, int blockWidth, int blockHeight);

    private static int ceilPot(int num) {
        int pot = 1;
        while (pot < num)
            pot = pot << 1;
        return pot;
    }
    
    private static class UnsupportedTextureHandle extends ResourceHandle {
        public UnsupportedTextureHandle(String errorMsg) {
            super(-1);
            setStatus(Status.ERROR);
            setStatusMessage(errorMsg);
        }
    }
}
