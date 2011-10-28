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
import com.ferox.renderer.impl.resource.AbstractTextureResourceDriver;
import com.ferox.renderer.impl.resource.TextureHandle;
import com.ferox.resource.Texture;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.Texture.Target;

public class LwjglTextureDriver extends AbstractTextureResourceDriver {

    public LwjglTextureDriver(RenderCapabilities caps) {
        super(caps);
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
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void glRestoreTexture() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected TextureHandle glCreateTexture(Target target, TextureFormat format, Class<? extends Buffer> type) {
        return new TextureHandle(GL11.glGenTextures(), target, format, type);
    }

    @Override
    protected void glDeleteTexture(TextureHandle handle) {
        GL11.glDeleteTextures(handle.getId());
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
            if (IntBuffer.class.isAssignableFrom(h.type))
                GL11.glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat, type, (IntBuffer) data);
            else if (ShortBuffer.class.isAssignableFrom(h.type))
                GL11.glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat, type, (ShortBuffer) data);
            else if (ByteBuffer.class.isAssignableFrom(h.type))
                GL11.glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat, type, (ByteBuffer) data);
            else
                GL11.glTexImage1D(target, mipmap, dstFormat, width, 0, srcFormat, type, (FloatBuffer) data);
            break;
        case T_2D:
        case T_CUBEMAP:
            if (srcFormat > 0) {
                // uncompressed
                if (IntBuffer.class.isAssignableFrom(h.type))
                    GL11.glTexImage2D(target, mipmap, dstFormat, width, height, 0, srcFormat, type, (IntBuffer) data);
                else if (ShortBuffer.class.isAssignableFrom(h.type))
                    GL11.glTexImage2D(target, mipmap, dstFormat, width, height, 0, srcFormat, type, (ShortBuffer) data);
                else if (ByteBuffer.class.isAssignableFrom(h.type))
                    GL11.glTexImage2D(target, mipmap, dstFormat, width, height, 0, srcFormat, type, (ByteBuffer) data);
                else
                    GL11.glTexImage2D(target, mipmap, dstFormat, width, height, 0, srcFormat, type, (FloatBuffer) data);
            } else if (data != null) {
                // compressed
                // we only support DXT compression, so it will be byte data
                GL13.glCompressedTexImage2D(target, mipmap, dstFormat, width, height, 0, (ByteBuffer) data);
            }
            break;
        case T_3D:
            if (IntBuffer.class.isAssignableFrom(h.type))
                GL12.glTexImage3D(target, mipmap, dstFormat, width, height, depth, 0, srcFormat, type, (IntBuffer) data);
            else if (ShortBuffer.class.isAssignableFrom(h.type))
                GL12.glTexImage3D(target, mipmap, dstFormat, width, height, depth, 0, srcFormat, type, (ShortBuffer) data);
            else if (ByteBuffer.class.isAssignableFrom(h.type))
                GL12.glTexImage3D(target, mipmap, dstFormat, width, height, depth, 0, srcFormat, type, (ByteBuffer) data);
            else
                GL12.glTexImage3D(target, mipmap, dstFormat, width, height, depth, 0, srcFormat, type, (FloatBuffer) data);
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
            if (data instanceof IntBuffer)
                GL11.glTexSubImage1D(target, mipmap, x, width, srcFormat, type, (IntBuffer) data);
            else if (data instanceof ShortBuffer)
                GL11.glTexSubImage1D(target, mipmap, x, width, srcFormat, type, (ShortBuffer) data);
            else if (data instanceof ByteBuffer)
                GL11.glTexSubImage1D(target, mipmap, x, width, srcFormat, type, (ByteBuffer) data);
            else
                GL11.glTexSubImage1D(target, mipmap, x, width, srcFormat, type, (FloatBuffer) data);
            break;
        case T_2D:
        case T_CUBEMAP:
            if (data instanceof IntBuffer)
                GL11.glTexSubImage2D(target, mipmap, x, y, width, height, srcFormat, type, (IntBuffer) data);
            else if (data instanceof ShortBuffer)
                GL11.glTexSubImage2D(target, mipmap, x, y, width, height, srcFormat, type, (ShortBuffer) data);
            else if (data instanceof ByteBuffer)
                GL11.glTexSubImage2D(target, mipmap, x, y, width, height, srcFormat, type, (ByteBuffer) data);
            else
                GL11.glTexSubImage2D(target, mipmap, x, y, width, height, srcFormat, type, (FloatBuffer) data);
            break;
        case T_3D:
            if (data instanceof IntBuffer)
                GL12.glTexSubImage3D(target, mipmap, x, y, z, width, height, depth, srcFormat, type, (IntBuffer) data);
            else if (data instanceof ShortBuffer)
                GL12.glTexSubImage3D(target, mipmap, x, y, z, width, height, depth, srcFormat, type, (ShortBuffer) data);
            else if (data instanceof ByteBuffer)
                GL12.glTexSubImage3D(target, mipmap, x, y, z, width, height, depth, srcFormat, type, (ByteBuffer) data);
            else
                GL12.glTexSubImage3D(target, mipmap, x, y, z, width, height, depth, srcFormat, type, (FloatBuffer) data);
            break;
        }
    }

    @Override
    protected void glTextureParameters(Texture tex, TextureHandle handle) {
        int target = Utils.getGLTextureTarget(tex.getTarget());
        
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
            GL11.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_S, Utils.getGLWrapMode(handle.wrapS));
        }
        if (handle.wrapT != tex.getWrapModeT()) {
            handle.wrapT = tex.getWrapModeT();
            GL11.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_T, Utils.getGLWrapMode(handle.wrapT));
        }
        if (handle.wrapR != tex.getWrapModeS()) {
            handle.wrapR = tex.getWrapModeS();
            GL11.glTexParameteri(target, GL12.GL_TEXTURE_WRAP_R, Utils.getGLWrapMode(handle.wrapR));
        }
        
        // depth test
        if (handle.depthTest != tex.getDepthComparison()) {
            handle.depthTest = tex.getDepthComparison();
            GL11.glTexParameteri(target, GL14.GL_TEXTURE_COMPARE_FUNC, Utils.getGLPixelTest(handle.depthTest));
        }
        if (handle.enableDepthCompare != tex.isDepthCompareEnabled()) {
            handle.enableDepthCompare = tex.isDepthCompareEnabled();
            GL11.glTexParameteri(target, GL14.GL_TEXTURE_COMPARE_FUNC, (handle.enableDepthCompare ? GL14.GL_COMPARE_R_TO_TEXTURE : GL11.GL_NONE));
        }
        
        // anisotropic filtering
        if (handle.anisoLevel != tex.getAnisotropicFilterLevel()) {
            handle.anisoLevel = tex.getAnisotropicFilterLevel();
            float amount = handle.anisoLevel * maxAnisoLevel + 1f;
            GL11.glTexParameterf(target, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
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

    @Override
    protected void glUnpackRegion(int xOffset, int yOffset, int zOffset, int blockWidth, int blockHeight) {
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
