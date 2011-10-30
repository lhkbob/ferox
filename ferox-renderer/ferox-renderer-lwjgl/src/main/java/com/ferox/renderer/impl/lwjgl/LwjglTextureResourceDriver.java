package com.ferox.renderer.impl.lwjgl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.drivers.AbstractTextureResourceDriver;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.Texture.WrapMode;

/**
 * LwjglTextureResourceDriver is a concrete ResourceDriver that handles Texture
 * objects and uses the JOGL OpenGL binding.
 * 
 * @author Michael Ludwig
 */
// FIXME: I seem to have lost the rules for supported depth targets and compressed targets,
// where should these go? In Texture, Mipmap, AbstractTextureResourceDriver or LwjglTextureResourceDriver?
public class LwjglTextureResourceDriver extends AbstractTextureResourceDriver {
    private final ThreadLocal<Integer> texBinding;
    private final ThreadLocal<Integer> texTarget;
    
    public LwjglTextureResourceDriver() {
        texBinding = new ThreadLocal<Integer>();
        texTarget = new ThreadLocal<Integer>();
    }
    
    @Override
    protected void glTextureParameters(OpenGLContext context, Texture tex, TextureHandle handle) {
        RenderCapabilities caps = context.getRenderCapabilities();
        int target = Utils.getGLTextureTarget(handle.target);
        
        // filter
        if (handle.filter != tex.getFilter()) {
            handle.filter = tex.getFilter();
            int min = Utils.getGLMinFilter(handle.filter);
            int mag = Utils.getGLMagFilter(handle.filter);
            
            GL11.glTexParameteri(target, GL11.GL_TEXTURE_MIN_FILTER, min);
            GL11.glTexParameteri(target, GL11.GL_TEXTURE_MAG_FILTER, mag);
        }
        
        // wrap s/t/r
        if (handle.wrapS != tex.getWrapModeS()) {
            handle.wrapS = tex.getWrapModeS();
            GL11.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_S, getWrapMode(handle.wrapS, caps));
        }
        if (handle.wrapT != tex.getWrapModeT()) {
            handle.wrapT = tex.getWrapModeT();
            GL11.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_T, getWrapMode(handle.wrapT, caps));
        }
        if (handle.wrapR != tex.getWrapModeS()) {
            handle.wrapR = tex.getWrapModeS();
            GL11.glTexParameteri(target, GL12.GL_TEXTURE_WRAP_R, getWrapMode(handle.wrapR, caps));
        }
        
        // depth test
        if (caps.getDepthTextureSupport() && caps.getVersion() < 3f) {
            if (handle.depthTest != tex.getDepthComparison()) {
                handle.depthTest = tex.getDepthComparison();
                GL11.glTexParameteri(target, GL14.GL_TEXTURE_COMPARE_FUNC, Utils.getGLPixelTest(handle.depthTest));
            }
            if (handle.enableDepthCompare == null || handle.enableDepthCompare != tex.isDepthCompareEnabled()) {
                handle.enableDepthCompare = tex.isDepthCompareEnabled();
                GL11.glTexParameteri(target, GL14.GL_TEXTURE_COMPARE_MODE, (handle.enableDepthCompare ? GL14.GL_COMPARE_R_TO_TEXTURE 
                                                                                                      : GL11.GL_NONE));
            }
        }
        
        // anisotropic filtering
        if (caps.getMaxAnisotropicLevel() > 0) {
            if (handle.anisoLevel != tex.getAnisotropicFilterLevel()) {
                handle.anisoLevel = tex.getAnisotropicFilterLevel();
                float amount = handle.anisoLevel * caps.getMaxAnisotropicLevel() + 1f;
                GL11.glTexParameterf(target, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
            }
        }
        
        // mipmap range
        if (handle.baseMipmap != tex.getBaseMipmapLevel()) {
            handle.baseMipmap = tex.getBaseMipmapLevel();
            GL11.glTexParameteri(target, GL12.GL_TEXTURE_BASE_LEVEL, handle.baseMipmap);
        }
        if (handle.maxMipmap != tex.getMaxMipmapLevel()) {
            handle.maxMipmap = tex.getMaxMipmapLevel();
            GL11.glTexParameteri(target, GL12.GL_TEXTURE_MAX_LEVEL, handle.baseMipmap);
        }
    }
    
    private int getWrapMode(WrapMode mode, RenderCapabilities caps) {
        if (!caps.getClampToEdgeSupport() && mode == WrapMode.CLAMP)
            return GL11.GL_CLAMP;
        if (!caps.getMirrorWrapModeSupport() && mode == WrapMode.MIRROR)
            return GL11.GL_REPEAT;
        return Utils.getGLWrapMode(mode);
    }

    @Override
    protected void glBindTexture(OpenGLContext context, TextureHandle handle) {
        LwjglContext c = (LwjglContext) context;
        
        int activeTex = c.getActiveTexture();
        int target = Utils.getGLTextureTarget(handle.target);
        if (target == c.getTextureTarget(activeTex))
            texBinding.set(c.getTexture(activeTex));
        else
            texBinding.set(0);
        texTarget.set(target);
        
        GL11.glBindTexture(target, handle.texID);
    }

    @Override
    protected void glRestoreTexture(OpenGLContext context) {
        GL11.glBindTexture(texTarget.get(), texBinding.get());
    }

    @Override
    protected void glDeleteTexture(OpenGLContext context, TextureHandle handle) {
        GL11.glDeleteTextures(handle.texID);
    }

    @Override
    protected void glTexImage(OpenGLContext context, TextureHandle h, int layer, int mipmap,
                              int width, int height, int depth, int capacity, Buffer data) {
        // note that 1D and 3D targets don't support or expect compressed textures
        int target = (h.target == Target.T_CUBEMAP ? Utils.getGLCubeFace(layer) : Utils.getGLTextureTarget(h.target));
        int srcFormat = Utils.getGLSrcFormat(h.format);
        int dstFormat = Utils.getGLDstFormat(h.format, h.type);
        int type = (h.format.isPackedFormat() ? Utils.getGLPackedType(h.format) : Utils.getGLType(h.type));
        
        switch(h.target) {
        case T_1D:
            switch(h.type) {
            case FLOAT:
                GL11.glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat, type, (FloatBuffer) data);
                break;
            case UNSIGNED_BYTE:
                GL11.glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat, type, (ByteBuffer) data);
                break;
            case UNSIGNED_INT:
                GL11.glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat, type, (IntBuffer) data);
                break;
            case UNSIGNED_SHORT:
                GL11.glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat, type, (ShortBuffer) data);
                break;
            }
            break;
        case T_2D:
        case T_CUBEMAP:
            if (srcFormat > 0) {
                // uncompressed
                switch(h.type) {
                case FLOAT:
                    GL11.glTexImage2D(target, mipmap, dstFormat, width, height, 0, srcFormat, type, (FloatBuffer) data);
                    break;
                case UNSIGNED_BYTE:
                    GL11.glTexImage2D(target, mipmap, dstFormat, width, height, 0, srcFormat, type, (ByteBuffer) data);
                    break;
                case UNSIGNED_INT:
                    GL11.glTexImage2D(target, mipmap, dstFormat, width, height, 0, srcFormat, type, (IntBuffer) data);
                    break;
                case UNSIGNED_SHORT:
                    GL11.glTexImage2D(target, mipmap, dstFormat, width, height, 0, srcFormat, type, (ShortBuffer) data);
                    break;
                }
            } else if (data != null) {
                // compressed - always a ByteBuffer
                GL13.glCompressedTexImage2D(target, mipmap, dstFormat, width, height, 0, (ByteBuffer) data);
            }
            break;
        case T_3D:
            switch(h.type) {
            case FLOAT:
                GL12.glTexImage3D(target, mipmap, dstFormat, width, height, depth, 0, srcFormat, type, (FloatBuffer) data);
                break;
            case UNSIGNED_BYTE:
                GL12.glTexImage3D(target, mipmap, dstFormat, width, height, depth, 0, srcFormat, type, (ByteBuffer) data);
                break;
            case UNSIGNED_INT:
                GL12.glTexImage3D(target, mipmap, dstFormat, width, height, depth, 0, srcFormat, type, (IntBuffer) data);
                break;
            case UNSIGNED_SHORT:
                GL12.glTexImage3D(target, mipmap, dstFormat, width, height, depth, 0, srcFormat, type, (ShortBuffer) data);
                break;
            }
            break;
        }
    }

    @Override
    protected void glTexSubImage(OpenGLContext context, TextureHandle h, int layer, int mipmap, int x, int y,
                                 int z, int width, int height, int depth, Buffer data) {
        int target = (h.target == Target.T_CUBEMAP ? Utils.getGLCubeFace(layer) : Utils.getGLTextureTarget(h.target));
        int srcFormat = Utils.getGLSrcFormat(h.format);
        int type = (h.format.isPackedFormat() ? Utils.getGLPackedType(h.format) : Utils.getGLType(h.type));
        
        switch(h.target) {
        case T_1D:
            switch(h.type) {
            case FLOAT:
                GL11.glTexSubImage1D(target, mipmap, x, width, srcFormat, type, (FloatBuffer) data);
                break;
            case UNSIGNED_BYTE:
                GL11.glTexSubImage1D(target, mipmap, x, width, srcFormat, type, (ByteBuffer) data);
                break;
            case UNSIGNED_INT:
                GL11.glTexSubImage1D(target, mipmap, x, width, srcFormat, type, (IntBuffer) data);
                break;
            case UNSIGNED_SHORT:
                GL11.glTexSubImage1D(target, mipmap, x, width, srcFormat, type, (ShortBuffer) data);
                break;
            }
            break;
        case T_2D:
        case T_CUBEMAP:
            switch(h.type) {
            case FLOAT:
                GL11.glTexSubImage2D(target, mipmap, x, y, width, height, srcFormat, type, (FloatBuffer) data);
                break;
            case UNSIGNED_BYTE:
                GL11.glTexSubImage2D(target, mipmap, x, y, width, height, srcFormat, type, (ByteBuffer) data);
                break;
            case UNSIGNED_INT:
                GL11.glTexSubImage2D(target, mipmap, x, y, width, height, srcFormat, type, (IntBuffer) data);
                break;
            case UNSIGNED_SHORT:
                GL11.glTexSubImage2D(target, mipmap, x, y, width, height, srcFormat, type, (ShortBuffer) data);
                break;
            }
            break;
        case T_3D:
            switch(h.type) {
            case FLOAT:
                GL12.glTexSubImage3D(target, mipmap, x, y, z, width, height, depth, srcFormat, type, (FloatBuffer) data);
                break;
            case UNSIGNED_BYTE:
                GL12.glTexSubImage3D(target, mipmap, x, y, z, width, height, depth, srcFormat, type, (ByteBuffer) data);
                break;
            case UNSIGNED_INT:
                GL12.glTexSubImage3D(target, mipmap, x, y, z, width, height, depth, srcFormat, type, (IntBuffer) data);
                break;
            case UNSIGNED_SHORT:
                GL12.glTexSubImage3D(target, mipmap, x, y, z, width, height, depth, srcFormat, type, (ShortBuffer) data);
                break;
            }
            break;
        }
    }

    @Override
    protected void glUnpackRegion(OpenGLContext context, int xOffset, int yOffset, int zOffset,
                                  int blockWidth, int blockHeight) {
        // skip pixels
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, xOffset);
        // skip rows
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, yOffset);
        // skip images
        GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, zOffset);

        // width of whole face
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, blockWidth);
        // height of whole face
        GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, blockHeight);

        // configure the alignment
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
    }
}
