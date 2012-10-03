package com.ferox.renderer.impl.jogl;

import java.nio.Buffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.drivers.AbstractTextureResourceDriver;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.Texture.WrapMode;

/**
 * JoglTextureResourceDriver is a concrete ResourceDriver that handles Texture
 * objects and uses the JOGL OpenGL binding.
 * 
 * @author Michael Ludwig
 */
// FIXME: I seem to have lost the rules for supported depth targets and compressed targets,
// where should these go? In Texture, Mipmap, AbstractTextureResourceDriver or JoglTextureResourceDriver?
public class JoglTextureResourceDriver extends AbstractTextureResourceDriver {
    private final ThreadLocal<Integer> texBinding;
    private final ThreadLocal<Integer> texTarget;

    public JoglTextureResourceDriver() {
        texBinding = new ThreadLocal<Integer>();
        texTarget = new ThreadLocal<Integer>();
    }

    @Override
    protected void glTextureParameters(OpenGLContext context, Texture tex,
                                       TextureHandle handle) {
        RenderCapabilities caps = context.getRenderCapabilities();
        GL2GL3 gl = getGL(context);
        int target = Utils.getGLTextureTarget(handle.target);

        // filter
        if (handle.filter != tex.getFilter()) {
            handle.filter = tex.getFilter();
            int min = Utils.getGLMinFilter(handle.filter);
            int mag = Utils.getGLMagFilter(handle.filter);

            gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, min);
            gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, mag);
        }

        // wrap s/t/r
        if (handle.wrapS != tex.getWrapModeS()) {
            handle.wrapS = tex.getWrapModeS();
            gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S,
                               getWrapMode(handle.wrapS, caps));
        }
        if (handle.wrapT != tex.getWrapModeT()) {
            handle.wrapT = tex.getWrapModeT();
            gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T,
                               getWrapMode(handle.wrapT, caps));
        }
        if (handle.wrapR != tex.getWrapModeS()) {
            handle.wrapR = tex.getWrapModeS();
            gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_R,
                               getWrapMode(handle.wrapR, caps));
        }

        // depth test
        if (caps.getDepthTextureSupport() && caps.getVersion() < 3f) {
            if (handle.depthTest != tex.getDepthComparison()) {
                handle.depthTest = tex.getDepthComparison();
                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_COMPARE_FUNC,
                                   Utils.getGLPixelTest(handle.depthTest));
            }
            if (handle.enableDepthCompare == null || handle.enableDepthCompare != tex.isDepthCompareEnabled()) {
                handle.enableDepthCompare = tex.isDepthCompareEnabled();
                gl.glTexParameteri(target,
                                   GL2ES2.GL_TEXTURE_COMPARE_MODE,
                                   (handle.enableDepthCompare ? GL2.GL_COMPARE_R_TO_TEXTURE : GL.GL_NONE));
            }
        }

        // anisotropic filtering
        if (caps.getMaxAnisotropicLevel() > 0) {
            if (handle.anisoLevel != tex.getAnisotropicFilterLevel()) {
                handle.anisoLevel = tex.getAnisotropicFilterLevel();
                float amount = handle.anisoLevel * caps.getMaxAnisotropicLevel() + 1f;
                gl.glTexParameterf(target, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
            }
        }

        // mipmap range
        if (handle.baseMipmap != tex.getBaseMipmapLevel()) {
            handle.baseMipmap = tex.getBaseMipmapLevel();
            gl.glTexParameteri(target, GL2GL3.GL_TEXTURE_BASE_LEVEL, handle.baseMipmap);
        }
        if (handle.maxMipmap != tex.getMaxMipmapLevel()) {
            handle.maxMipmap = tex.getMaxMipmapLevel();
            gl.glTexParameteri(target, GL2GL3.GL_TEXTURE_MAX_LEVEL, handle.baseMipmap);
        }
    }

    private int getWrapMode(WrapMode mode, RenderCapabilities caps) {
        if (!caps.getClampToEdgeSupport() && mode == WrapMode.CLAMP) {
            return GL2.GL_CLAMP;
        }
        if (!caps.getMirrorWrapModeSupport() && mode == WrapMode.MIRROR) {
            return GL.GL_REPEAT;
        }
        return Utils.getGLWrapMode(mode);
    }

    @Override
    protected void glBindTexture(OpenGLContext context, TextureHandle handle) {
        JoglContext c = (JoglContext) context;

        int activeTex = c.getActiveTexture();
        int target = Utils.getGLTextureTarget(handle.target);
        if (target == c.getTextureTarget(activeTex)) {
            texBinding.set(c.getTexture(activeTex));
        } else {
            texBinding.set(0);
        }
        texTarget.set(target);

        getGL(context).glBindTexture(target, handle.texID);
    }

    @Override
    protected void glRestoreTexture(OpenGLContext context) {
        getGL(context).glBindTexture(texTarget.get(), texBinding.get());
    }

    @Override
    protected void glDeleteTexture(OpenGLContext context, TextureHandle handle) {
        getGL(context).glDeleteTextures(1, new int[] {handle.texID}, 0);
    }

    @Override
    protected void glTexImage(OpenGLContext context, TextureHandle h, int layer,
                              int mipmap, int width, int height, int depth, int capacity,
                              Buffer data) {
        // note that 1D and 3D targets don't support or expect compressed textures
        int target = (h.target == Target.T_CUBEMAP ? Utils.getGLCubeFace(layer) : Utils.getGLTextureTarget(h.target));
        int srcFormat = Utils.getGLSrcFormat(h.format);
        int dstFormat = Utils.getGLDstFormat(h.format, h.type);
        int type = (h.format.isPackedFormat() ? Utils.getGLPackedType(h.format) : Utils.getGLType(h.type));

        switch (h.target) {
        case T_1D:
            getGL(context).glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat,
                                        type, data);
            break;
        case T_2D:
        case T_CUBEMAP:
            if (srcFormat > 0) {
                // uncompressed
                getGL(context).glTexImage2D(target, mipmap, dstFormat, width, height, 0,
                                            srcFormat, type, data);
            } else if (data != null) {
                // compressed
                getGL(context).glCompressedTexImage2D(target, mipmap, dstFormat, width,
                                                      height, 0, capacity, data);
            }
            break;
        case T_3D:
            getGL(context).glTexImage3D(target, mipmap, dstFormat, width, height, depth,
                                        0, srcFormat, type, data);
            break;
        }
    }

    @Override
    protected void glTexSubImage(OpenGLContext context, TextureHandle h, int layer,
                                 int mipmap, int x, int y, int z, int width, int height,
                                 int depth, Buffer data) {
        int target = (h.target == Target.T_CUBEMAP ? Utils.getGLCubeFace(layer) : Utils.getGLTextureTarget(h.target));
        int srcFormat = Utils.getGLSrcFormat(h.format);
        int type = (h.format.isPackedFormat() ? Utils.getGLPackedType(h.format) : Utils.getGLType(h.type));

        switch (h.target) {
        case T_1D:
            getGL(context).glTexSubImage1D(target, mipmap, x, width, srcFormat, type,
                                           data);
            break;
        case T_2D:
        case T_CUBEMAP:
            getGL(context).glTexSubImage2D(target, mipmap, x, y, width, height,
                                           srcFormat, type, data);
            break;
        case T_3D:
            getGL(context).glTexSubImage3D(target, mipmap, x, y, z, width, height, depth,
                                           srcFormat, type, data);
            break;
        }
    }

    @Override
    protected void glUnpackRegion(OpenGLContext context, int xOffset, int yOffset,
                                  int zOffset, int blockWidth, int blockHeight) {
        GL2GL3 gl = getGL(context);

        // skip pixels
        gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_PIXELS, xOffset);
        // skip rows
        gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_ROWS, yOffset);
        // skip images
        gl.glPixelStorei(GL2GL3.GL_UNPACK_SKIP_IMAGES, zOffset);

        // width of whole face
        gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH, blockWidth);
        // height of whole face
        gl.glPixelStorei(GL2GL3.GL_UNPACK_IMAGE_HEIGHT, blockHeight);

        // configure the alignment
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
    }

    private GL2GL3 getGL(OpenGLContext context) {
        return ((JoglContext) context).getGLContext().getGL().getGL2GL3();
    }
}
