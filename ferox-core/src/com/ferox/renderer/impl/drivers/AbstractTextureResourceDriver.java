package com.ferox.renderer.impl.drivers;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.resource.BufferData;
import com.ferox.resource.Mipmap;
import com.ferox.resource.MipmapRegion;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Texture;
import com.ferox.resource.TextureFormat;

/**
 * AbstractTextureResourceDriver is an almost complete ResourceDriver
 * implementation for the {@link Texture} resource type. Implementations are
 * only required to implement the actual OpenGL calls necessary to transfer the
 * texture data, etc. This ResourceDriver uses {@link TextureHandle} as its
 * resource handle.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractTextureResourceDriver implements ResourceDriver<Texture> {
    @Override
    public Class<Texture> getResourceType() {
        return Texture.class;
    }
    
    @Override
    public void reset(Texture tex, ResourceHandle handle) {
        if (handle instanceof TextureHandle) {
            TextureHandle h = (TextureHandle) handle;
            
            h.baseMipmap = -1; // outside of valid range
            h.maxMipmap = -1; // outside of valid range

            h.filter = null;

            h.wrapS = null;
            h.wrapT = null;
            h.wrapR = null;

            h.depthTest = null;
            h.enableDepthCompare = null;
            h.anisoLevel = -2f; // outside of valid range
            
            h.lastSyncedVersion = -1;
            Arrays.fill(h.lastSyncedKeys, null);
        }
    }
    
    @Override
    public void dispose(OpenGLContext context, ResourceHandle handle) {
        if (handle instanceof TextureHandle)
            glDeleteTexture(context, (TextureHandle) handle);
    }

    @Override
    public ResourceHandle update(OpenGLContext context, Texture tex, ResourceHandle handle) {
        if (handle == null) {
            if (!context.getRenderCapabilities().getSupportedTextureTargets().contains(tex.getTarget())) {
                handle = new ResourceHandle(tex);
                handle.setStatus(Status.UNSUPPORTED);
                handle.setStatusMessage("Texture target, " + tex.getTarget() + ", is not supported");
            } else {
                handle = new TextureHandle(tex);
            }
        }
        
        if (handle instanceof TextureHandle) {
            TextureHandle h = (TextureHandle) handle;
            boolean isBound = false;
            
            // Check for errors in dimensions, format, type, etc.
            if (!validateTexture(context, tex, h))
                return h;
            
            // Check for (and possibly update) texture parameters
            if (haveParametersChanged(tex, h)) {
                glBindTexture(context, h);
                isBound = true;

                glTextureParameters(context, tex, h);
            }
            
            if (tex.getChangeQueue().isVersionStale(h.lastSyncedVersion)) {
                if (isBound) {
                    glBindTexture(context, h);
                    isBound = true;
                }
                
                boolean isMissingChanges = tex.getChangeQueue().hasLostChanges(h.lastSyncedVersion);
                
                // organize all changes by layer and mipmap level
                @SuppressWarnings("unchecked")
                Map<Integer, List<MipmapRegion>>[] changes = new Map[h.lastSyncedKeys.length];
                for (MipmapRegion edit: tex.getChangeQueue().getChangesSince(h.lastSyncedVersion)) {
                    int layer = edit.getLayer();
                    int mipmap = edit.getMipmapLevel();
                    
                    if (layer >= changes.length)
                        continue; // user created a bad mipmap region
                    
                    if (changes[layer] == null)
                        changes[layer] = new HashMap<Integer, List<MipmapRegion>>();
                    if (changes[layer].get(mipmap) == null)
                        changes[layer].put(mipmap, new ArrayList<MipmapRegion>());
                    changes[layer].get(mipmap).add(edit);
                }
                
                // update type/format/isMipmapped based on potentially new sets of mipmaps
                TextureFormat format = tex.getFormat();
                if (!context.getRenderCapabilities().getUnclampedFloatTextureSupport()) {
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
                
                h.format = format;
                h.type = tex.getDataType();
                h.isMipmapped = tex.getLayer(0).isMipmapped();
                
                for (int i = 0; i < h.lastSyncedKeys.length; i++) {
                    Mipmap mipmap = tex.getLayer(i);
                    int numMipmaps = mipmap.getNumMipmaps();
                    if (h.lastSyncedKeys[i] == null)
                        h.lastSyncedKeys[i] = new Object[numMipmaps];
                    else if (numMipmaps != h.lastSyncedKeys[i].length)
                        h.lastSyncedKeys[i] = Arrays.copyOf(h.lastSyncedKeys[i], numMipmaps);
                    
                    for (int j = 0; j < numMipmaps; j++) {
                        BufferData data = mipmap.getData(j);
                        if (isMissingChanges || data.getKey() != h.lastSyncedKeys[i][j]) {
                            // make sure a tex image gets allocated and that we push
                            // all of the changes
                            doTexImage(context, h, mipmap, i, j);
                        } else if (h.format == TextureFormat.DEPTH || h.format.isCompressed()) {
                            // depth/compressed textures don't seem to behave well with glTexSubImage
                            if (changes[i] != null && changes[i].containsKey(j))
                                doTexImage(context, h, mipmap, i, j);
                        } else {
                            // use glTexSubImage to process all mipmap regions
                            List<MipmapRegion> toFlush = (changes[i] != null ? changes[i].get(j) : null);
                            if (toFlush != null) {
                                for (MipmapRegion rg: toFlush)
                                    doTexSubImage(context, h, mipmap, rg);
                            }
                        }
                        
                        h.lastSyncedKeys[i][j] = data.getKey();
                    }
                }
                
                h.lastSyncedVersion = tex.getChangeQueue().getVersion();
            }
            
            if (isBound)
                glRestoreTexture(context);
            
            if (h.format != tex.getFormat())
                h.setStatusMessage("TextureFormat changed from " + tex.getFormat() + " to " + h.format + " to meet hardware support");
            else
                h.setStatusMessage("");
            h.setStatus(Status.READY);
        }
        
        return handle;
    }

    /**
     * Return true if the texture parameters that are modified by
     * glTextureParameters have been changed.
     * 
     * @param tex
     * @param handle
     * @return
     */
    protected boolean haveParametersChanged(Texture tex, TextureHandle handle) {
        if (handle.baseMipmap != tex.getBaseMipmapLevel() || handle.maxMipmap != tex.getMaxMipmapLevel())
            return true;
        if (handle.filter != tex.getFilter())
            return true;
        if (handle.wrapR != tex.getWrapModeR() || handle.wrapS != tex.getWrapModeS() || handle.wrapT != tex.getWrapModeT())
            return true;
        if (handle.depthTest != tex.getDepthComparison())
            return true;
        if (handle.enableDepthCompare == null || handle.enableDepthCompare != tex.isDepthCompareEnabled())
            return true;
        if (handle.anisoLevel != tex.getAnisotropicFilterLevel())
            return true;
        
        return false;
    }

    /**
     * Validate the current state of the texture. Return true if the update
     * should continue. If false is returned, the handle must have an
     * appropriate error message assigned and its status set to ERROR. The
     * texture has not been bound yet.
     * 
     * @param context
     * @param tex
     * @param handle
     * @return True if the texture can be updated
     */
    protected boolean validateTexture(OpenGLContext context, Texture tex, TextureHandle handle) {
        String errorMsg = null;
        // error handling for textures that are owned by texture surfaces
        //  1. They cannot change size from what is reported by the surface
        //  2. They cannot change texture format or data type
        //  3. They cannot be mipmapped -> this might go away
        if (tex.getOwner() != null) {
            TextureSurface owner = tex.getOwner();
            if (tex.getWidth() != owner.getWidth() || tex.getHeight() != owner.getHeight()
                || tex.getDepth() != owner.getDepth())
                errorMsg = "Cannot change dimensions of a Texture owned by a TextureSurface";
            else if (handle.format != null && tex.getFormat() != handle.format)
                errorMsg = "Cannot change TextureFormat of a Texture owned by a TextureSurface";
            else if (handle.type != null && tex.getDataType() != handle.type)
                errorMsg = "Cannot change DataType of a Texture owned by a TextureSurface";
            else if (tex.getLayer(0).isMipmapped())
                errorMsg = "Cannot use multiple mipmap levels with a Texture owned by a TextureSurface";
        }
        
        RenderCapabilities caps = context.getRenderCapabilities();
        if (errorMsg == null && !caps.getDepthTextureSupport() && tex.getFormat() == TextureFormat.DEPTH)
            errorMsg = "Depth textures are not supported";
        
        if (errorMsg == null && !caps.getNpotTextureSupport()) {
            if (tex.getWidth() != ceilPot(tex.getWidth()) || 
                tex.getHeight() != ceilPot(tex.getHeight()) ||
                tex.getDepth() != ceilPot(tex.getDepth()))
                errorMsg = "Non-power of two textures are not supported";
        }
        
        int maxSize = 0;
        switch(tex.getTarget()) {
        case T_1D:
        case T_2D:
            maxSize = caps.getMaxTextureSize();
            break;
        case T_CUBEMAP:
            maxSize = caps.getMaxTextureCubeMapSize();
            break;
        case T_3D:
            maxSize = caps.getMaxTexture3DSize();
            break;
        }
        
        if (errorMsg == null && (tex.getWidth() > maxSize || 
                                 tex.getHeight() > maxSize || 
                                 tex.getDepth() > maxSize))
            errorMsg = "Dimensions excede supported maximum of " + maxSize;
        
        if (errorMsg != null) {
            handle.setStatus(Status.ERROR);
            handle.setStatusMessage(errorMsg);
            return false;
        }
        
        return true;
    }

    /**
     * Set all texture parameters so that the handle has up-to-date values that
     * match its Texture. Only those parameters which have changed actually need
     * to be sent to OpenGL.
     * 
     * @param context
     * @param tex
     * @param handle
     */
    protected abstract void glTextureParameters(OpenGLContext context, Texture tex, TextureHandle handle);

    /**
     * Bind the given TextureHandle to the context on the current Thread so that
     * subsequent calls to
     * {@link #glTexImage(TextureHandle, int, int, int, int, int, int, Buffer)},
     * etc. will update the given handle's image data.
     * 
     * @param context
     * @param handle
     */
    protected abstract void glBindTexture(OpenGLContext context, TextureHandle handle);

    /**
     * Restore the texture bindings to their previous state that existed before
     * the last call to {@link #glBindTexture(TextureHandle)} on the calling
     * Thread.
     */
    protected abstract void glRestoreTexture(OpenGLContext context);
    
    /**
     * Destroy the texture pointed to by the given handle, it can be assumed
     * that the handle's id is greater than 0.
     * 
     * @param handle
     */
    protected abstract void glDeleteTexture(OpenGLContext context, TextureHandle handle);
    
    /**
     * Use glTexImage{1D/2D/3D} as appropriate to allocate a texture image for
     * the given layer and mipmap on the currently bound texture object. If the
     * TextureHandle's glSrcFormat < 0, the texture is compressed, so the
     * appropriate glCompressedTexImage command should be used instead. The data
     * buffer may be null.
     */
    protected abstract void glTexImage(OpenGLContext context, TextureHandle h, int layer, int mipmap, int width, int height, int depth, int capacity, Buffer data);

    /**
     * As
     * {@link #glTexImage(TextureHandle, int, int, int, int, int, int, Buffer)}
     * but this intended for updating a sub-image within the given layer and
     * mipmap. It can be assumed that the the texture will not be compressed,
     * and that its data array will not be null.
     */
    protected abstract void glTexSubImage(OpenGLContext context, TextureHandle h, int layer, int mipmap, int x, int y, int z, int width, int height, int depth, Buffer data);

    /**
     * Set the unpack region for pixel data to use the given parameters.
     * Additionally, the unpack alignment should be set to 1.
     */
    protected abstract void glUnpackRegion(OpenGLContext context, int xOffset, int yOffset, int zOffset, int blockWidth, int blockHeight);
    
    private void doTexImage(OpenGLContext context, TextureHandle handle, Mipmap mipmap, int layer, int level) {
        int w = mipmap.getWidth(level);
        int h = mipmap.getHeight(level);
        int d = mipmap.getDepth(level);
        
        BufferData data = mipmap.getData(level);
        if (data.getArray() != null) {
            glUnpackRegion(context, 0, 0, 0, w, h);
            Buffer nioData = BufferUtil.newBuffer(data);
            glTexImage(context, handle, layer, level, w, h, d, data.getLength(), nioData);
        } else if (handle.lastSyncedKeys[layer][level] == null) {
            // First time for this texture handle, so alloc the image
            glTexImage(context, handle, layer, level, w, h, d, handle.format.getBufferSize(w, h, d), null);
        }
    }
    
    private void doTexSubImage(OpenGLContext context, TextureHandle handle, Mipmap mipmap, MipmapRegion dirty) {
        BufferData data = mipmap.getData(dirty.getMipmapLevel());
        if (data.getArray() == null)
            return; // no update to perform
        
        // this is only called if we know the layer and level are valid, but
        // we still have to check the dimensions and clamp them.
        int maxWidth = mipmap.getWidth(dirty.getMipmapLevel());
        int maxHeight = mipmap.getHeight(dirty.getMipmapLevel());
        int maxDepth = mipmap.getDepth(dirty.getMipmapLevel());
        
        int x = Math.min(dirty.getXOffset(), maxWidth - 1);
        int y = Math.min(dirty.getYOffset(), maxHeight - 1);
        int z = Math.min(dirty.getZOffset(), maxDepth - 1);
        int w = Math.min(dirty.getWidth(), maxWidth - x);
        int h = Math.min(dirty.getHeight(), maxHeight - y);
        int d = Math.min(dirty.getDepth(), maxDepth - z);
        
        glUnpackRegion(context, x, y, z, w, h);
        Buffer nioData = BufferUtil.newBuffer(data);
        // FIXME: would be awesome here to figure out how send/transfer a buffer that only
        // contained the desired data, right now this newBuffer() could cost as much
        // as actually sending a subimage to the gfx card
        //  - it would modify the unpack region to change the x/y offset to 0
        //  - have to fill the buffer manually from the array
        glTexSubImage(context, handle, dirty.getLayer(), dirty.getMipmapLevel(), x, y, z, w, h, d, nioData);
    }

    private static int ceilPot(int num) {
        int pot = 1;
        while (pot < num)
            pot = pot << 1;
        return pot;
    }
}