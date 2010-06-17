package com.ferox.renderer.impl.jogl;

import java.nio.Buffer;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.jogl.Utils;
import com.ferox.renderer.impl.resource.AbstractTextureResourceDriver;
import com.ferox.renderer.impl.resource.TextureHandle;
import com.ferox.resource.Texture;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.Texture.WrapMode;

public class JoglTextureDriver extends AbstractTextureResourceDriver {
    private final boolean hasClampEdge;
    private final boolean hasMirrorRepeat;
    private final boolean hasFixedFunction;
    
    private final ThreadLocal<Integer> texBinding;
    private final ThreadLocal<Integer> texTarget;

    public JoglTextureDriver(RenderCapabilities caps) {
        super(caps);
        hasClampEdge = caps.getClampToEdgeSupport();
        hasMirrorRepeat = caps.getMirrorWrapModeSupport();
        hasFixedFunction = caps.getVersion() < 3f; // need true ffp, not just a ffp renderer
        
        texBinding = new ThreadLocal<Integer>();
        texTarget = new ThreadLocal<Integer>();
    }
    
    private GL2GL3 getGL() {
        return JoglContext.getCurrent().getGL();
    }
    
    @Override
    protected String getUnsupportedMessage(Texture tex) {
        String msg = super.getUnsupportedMessage(tex);
        if (msg == null) {
            // make sure that 1D/3D textures aren't compressed
            if (tex.getFormat().isCompressed()) {
                if (tex.getTarget() == Target.T_1D || tex.getTarget() == Target.T_3D)
                    msg = tex.getTarget() + " does not support compressed formats: " + tex.getFormat();
            }
        }
        return msg;
    }

    @Override
    protected void glBindTexture(TextureHandle handle) {
        JoglContext context = JoglContext.getCurrent();
        BoundObjectState state = context.getRecord();
        
        int active = state.getActiveTexture();
        int target = Utils.getGLTextureTarget(handle.target);
        if (target == state.getTextureTarget(active))
            texBinding.set(state.getTexture(active));
        else
            texBinding.set(0);
        texTarget.set(target);
        
        context.getGL().glBindTexture(target, handle.getId());
    }
    
    @Override
    protected void glRestoreTexture() {
        getGL().glBindTexture(texTarget.get(), texBinding.get());
    }

    @Override
    protected TextureHandle glCreateTexture(Target target, TextureFormat format, Class<? extends Buffer> type) {
        int[] id = new int[1];
        getGL().glGenTextures(1, id, 0);
        return new TextureHandle(id[0], target, format, type);
    }

    @Override
    protected void glDeleteTexture(TextureHandle handle) {
        getGL().glDeleteTextures(1, new int[] { handle.getId() }, 0);
    }

    @Override
    protected void glTexImage(TextureHandle h, int layer, int mipmap, int width, int height, int depth, int capacity, Buffer data) {
        // note that 1D and 3D targets don't support or expect compressed textures
        int target = (h.target == Target.T_CUBEMAP ? Utils.getGLCubeFace(layer) : Utils.getGLTextureTarget(h.target));
        int srcFormat = Utils.getGLSrcFormat(h.format);
        int dstFormat = Utils.getGLDstFormat(h.format, h.type);
        int type = (h.format.isPackedFormat() ? Utils.getGLPackedType(h.format) : Utils.getGLType(h.type, true));
        
        data.clear();
        switch(h.target) {
        case T_1D:
            getGL().glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat, type, data);
            break;
        case T_2D:
        case T_CUBEMAP:
            if (srcFormat > 0) {
                // uncompressed
                getGL().glTexImage2D(target, mipmap, dstFormat, width, height, 0, srcFormat, type, data);
            } else if (data != null) {
                // compressed
                getGL().glCompressedTexImage2D(target, mipmap, dstFormat, width, height, 0, capacity, data);
            }
            break;
        case T_3D:
            getGL().glTexImage3D(target, mipmap, dstFormat, width, height, depth, 0, srcFormat, type, data);
            break;
        }
    }

    @Override
    protected void glTexSubImage(TextureHandle h, int layer, int mipmap, int x, int y, int z, int width, int height, int depth, Buffer data) {
        int target = (h.target == Target.T_CUBEMAP ? Utils.getGLCubeFace(layer) : Utils.getGLTextureTarget(h.target));
        int srcFormat = Utils.getGLSrcFormat(h.format);
        int type = (h.format.isPackedFormat() ? Utils.getGLPackedType(h.format) : Utils.getGLType(h.type, true));
        
        data.clear();
        switch(h.target) {
        case T_1D:
            getGL().glTexSubImage1D(target, mipmap, x, width, srcFormat, type, data);
            break;
        case T_2D:
        case T_CUBEMAP:
            getGL().glTexSubImage2D(target, mipmap, x, y, width, height, srcFormat, type, data);
            break;
        case T_3D:
                getGL().glTexSubImage3D(target, mipmap, x, y, z, width, height, depth, srcFormat, type, data);
            break;
        }
    }
    
    private int getWrapMode(WrapMode mode) {
        if (!hasClampEdge && mode == WrapMode.CLAMP)
            return GL2.GL_CLAMP;
        if (!hasMirrorRepeat && mode == WrapMode.MIRROR)
            return GL2GL3.GL_REPEAT;
        return Utils.getGLWrapMode(mode);
    }

    @Override
    protected void glTextureParameters(Texture tex, TextureHandle handle) {
        int target = Utils.getGLTextureTarget(tex.getTarget());
        
        // filter
        if (handle.filter != tex.getFilter()) {
            handle.filter = tex.getFilter();
            int min = Utils.getGLMinFilter(handle.filter);
            int mag = Utils.getGLMagFilter(handle.filter);
            
            getGL().glTexParameteri(target, GL2GL3.GL_TEXTURE_MIN_FILTER, min);
            getGL().glTexParameteri(target, GL2GL3.GL_TEXTURE_MAG_FILTER, mag);
        }
        
        // wrap s/t/r
        if (handle.wrapS != tex.getWrapModeS()) {
            handle.wrapS = tex.getWrapModeS();
            getGL().glTexParameteri(target, GL2GL3.GL_TEXTURE_WRAP_S, getWrapMode(handle.wrapS));
        }
        if (handle.wrapT != tex.getWrapModeT()) {
            handle.wrapT = tex.getWrapModeT();
            getGL().glTexParameteri(target, GL2GL3.GL_TEXTURE_WRAP_T, getWrapMode(handle.wrapT));
        }
        if (handle.wrapR != tex.getWrapModeS()) {
            handle.wrapR = tex.getWrapModeS();
            getGL().glTexParameteri(target, GL2GL3.GL_TEXTURE_WRAP_R, getWrapMode(handle.wrapR));
        }
        
        // depth test
        if (depthSupported && hasFixedFunction) {
            if (handle.depthTest != tex.getDepthComparison()) {
                handle.depthTest = tex.getDepthComparison();
                getGL().glTexParameteri(target, GL2GL3.GL_TEXTURE_COMPARE_FUNC, Utils.getGLPixelTest(handle.depthTest));
            }
            if (handle.enableDepthCompare != tex.isDepthCompareEnabled()) {
                handle.enableDepthCompare = tex.isDepthCompareEnabled();
                getGL().glTexParameteri(target, GL2GL3.GL_TEXTURE_COMPARE_FUNC, (handle.enableDepthCompare ? GL2.GL_COMPARE_R_TO_TEXTURE : GL2GL3.GL_NONE));
            }
        }
        
        // anisotropic filtering
        if (maxAnisoLevel > 0) {
            if (handle.anisoLevel != tex.getAnisotropicFilterLevel()) {
                handle.anisoLevel = tex.getAnisotropicFilterLevel();
                float amount = handle.anisoLevel * maxAnisoLevel + 1f;
                getGL().glTexParameterf(target, GL2GL3.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
            }
        }
        
        // mipmap range
        if (handle.baseMipmap != tex.getBaseMipmapLevel()) {
            handle.baseMipmap = tex.getBaseMipmapLevel();
            getGL().glTexParameteri(target, GL2GL3.GL_TEXTURE_BASE_LEVEL, handle.baseMipmap);
        }
        if (handle.maxMipmap != tex.getMaxMipmapLevel()) {
            handle.maxMipmap = tex.getMaxMipmapLevel();
            getGL().glTexParameteri(target, GL2GL3.GL_TEXTURE_MAX_LEVEL, handle.baseMipmap);
        }
    }

    @Override
    protected void glUnpackRegion(int xOffset, int yOffset, int zOffset, int blockWidth, int blockHeight) {
        // skip pixels
        getGL().glPixelStorei(GL2GL3.GL_UNPACK_SKIP_PIXELS, xOffset);
        // skip rows
        getGL().glPixelStorei(GL2GL3.GL_UNPACK_SKIP_ROWS, yOffset);
        // skip images
        getGL().glPixelStorei(GL2GL3.GL_UNPACK_SKIP_IMAGES, zOffset);

        // width of whole face
        getGL().glPixelStorei(GL2GL3.GL_UNPACK_ROW_LENGTH, blockWidth);
        // height of whole face
        getGL().glPixelStorei(GL2GL3.GL_UNPACK_IMAGE_HEIGHT, blockHeight);

        // configure the alignment
        getGL().glPixelStorei(GL2GL3.GL_UNPACK_ALIGNMENT, 1);
    }
}
