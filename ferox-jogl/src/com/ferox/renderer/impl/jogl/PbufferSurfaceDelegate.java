package com.ferox.renderer.impl.jogl;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

import com.ferox.renderer.impl.Context;
import com.ferox.renderer.impl.TextureSurfaceDelegate;
import com.ferox.renderer.impl.jogl.Utils;
import com.ferox.renderer.impl.resource.TextureHandle;
import com.ferox.resource.Texture;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.Texture.Target;

public class PbufferSurfaceDelegate extends TextureSurfaceDelegate {
    private final JoglFramework framework;
    private final GLPbuffer pbuffer;
    private final JoglContext context;
    
    private final Target target;
    private final int width, height;
    
    private int swapBuffersLayer;

    public PbufferSurfaceDelegate(JoglFramework framework, Texture[] colorTextures, Texture depthTexture,
                                  ReentrantLock surfaceLock) {
        super(colorTextures, depthTexture);
        if (framework == null)
            throw new NullPointerException("Framework cannot be null");
        this.framework = framework;
        
        if (colorTextures != null && colorTextures.length > 0) {
            width = colorTextures[0].getWidth();
            height = colorTextures[0].getHeight();
            target = colorTextures[0].getTarget();
        } else {
            width = depthTexture.getWidth();
            height = depthTexture.getHeight();
            target = depthTexture.getTarget();
        }
        
        JoglContext shareWith = (JoglContext) framework.getResourceManager().getContext();
        GLCapabilities caps = chooseCapabilities(framework.getProfile(), colorTextures, depthTexture);
        pbuffer = GLDrawableFactory.getFactory(framework.getProfile()).createGLPbuffer(caps, new DefaultGLCapabilitiesChooser(), 
                                                                                       width, height, (shareWith == null ? null : shareWith.getGLContext()));
        pbuffer.setAutoSwapBufferMode(false);
        context = new JoglContext(framework, pbuffer.getContext(), surfaceLock);
        swapBuffersLayer = 0;
    }

    @Override
    public void destroy() {
        context.destroy();
        pbuffer.destroy();
    }

    @Override
    public Context getContext() {
        return context;
    }
    
    @Override
    public void flushLayer() {
        pbuffer.swapBuffers();
        Texture color = getColorBuffers().length > 0 ? getColorBuffers()[0] : null; // will be 1 color target at max
        Texture depth = getDepthBuffer();

        GL2GL3 gl = context.getGL();

        int ct = -1;
        int dt = -1;

        if (color != null) {
            TextureHandle handle = (TextureHandle) framework.getResourceManager().getHandle(color);
            ct = Utils.getGLTextureTarget(handle.target);

            gl.glBindTexture(ct, handle.getId());
            copySubImage(gl, handle);
        }
        if (depth != null) {
            TextureHandle handle = (TextureHandle) framework.getResourceManager().getHandle(depth);
            dt = Utils.getGLTextureTarget(handle.target);
            
            gl.glBindTexture(dt, handle.getId());
            copySubImage(gl, handle);
        }
        
        restoreBindings(gl, context.getRecord(), ct, dt);
    }

    @Override
    public void setLayer(int layer, int depth) {
        if (target == Target.T_CUBEMAP)
            swapBuffersLayer = layer;
        else
            swapBuffersLayer = depth;
    }
    
    /*
     * Copy the buffer into the given TextureHandle. It assumes the texture was
     * already bound.
     */
    private void copySubImage(GL2GL3 gl, TextureHandle handle) {
        int glTarget = Utils.getGLTextureTarget(handle.target);
        switch (glTarget) {
        case GL2GL3.GL_TEXTURE_1D:
            gl.glCopyTexSubImage1D(glTarget, 0, 0, 0, 0, width);
            break;
        case GL2GL3.GL_TEXTURE_2D:
            gl.glCopyTexSubImage2D(glTarget, 0, 0, 0, 0, 0, width, height);
            break;
        case GL2GL3.GL_TEXTURE_CUBE_MAP:
            int face = Utils.getGLCubeFace(swapBuffersLayer);
            gl.glCopyTexSubImage2D(face, 0, 0, 0, 0, 0, width, height);
            break;
        case GL2GL3.GL_TEXTURE_3D:
            gl.glCopyTexSubImage3D(glTarget, 0, 0, 0, swapBuffersLayer, 
                                   0, 0, width, height);
            break;
        }
    }

    private void restoreBindings(GL gl, BoundObjectState state, int colorTarget, int depthTarget) {
        int target = state.getTextureTarget(state.getActiveTexture());
        int tex = state.getTexture(state.getActiveTexture());
        
        if (colorTarget > 0) {
            if (target == colorTarget)
                // restore enabled texture
                gl.glBindTexture(colorTarget, tex);
            else
                gl.glBindTexture(colorTarget, 0); // not really the active unit
        }
        if (depthTarget > 0 && colorTarget != depthTarget) {
            if (target == depthTarget)
                // restore enabled texture
                gl.glBindTexture(depthTarget, tex);
            else
                gl.glBindTexture(depthTarget, 0); // not really the active unit
        }
    }

    private static GLCapabilities chooseCapabilities(GLProfile profile, Texture[] colors, Texture depth) {
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setPBuffer(true);
        
        if (colors == null || colors.length == 0) {
            caps.setRedBits(0);
            caps.setGreenBits(0);
            caps.setBlueBits(0);
            caps.setAlphaBits(0);
        } else {
            TextureFormat format = colors[0].getFormat();
            if (format == TextureFormat.ALPHA_FLOAT || format == TextureFormat.LUMINANCE_ALPHA_FLOAT ||
                format == TextureFormat.LUMINANCE_FLOAT || format == TextureFormat.RGB_FLOAT ||
                format == TextureFormat.RGBA_FLOAT)
                caps.setPbufferFloatingPointBuffers(true);
            
            switch(format) {
            // 8, 8, 8, 0
            case LUMINANCE: case RGB: case BGR:
                caps.setRedBits(8); caps.setGreenBits(8); caps.setBlueBits(8); caps.setAlphaBits(0);
                break;
            // 5, 6, 5, 0
            case BGR_565: case RGB_565:
                caps.setRedBits(5); caps.setGreenBits(6); caps.setBlueBits(5); caps.setAlphaBits(0);
                break;
            // 5, 6, 5, 8 - not sure how supported this is
            case BGRA_5551: case BGRA_4444: case ABGR_1555: case ABGR_4444:
            case ARGB_4444: case ARGB_1555: case RGBA_4444: case RGBA_5551:
                caps.setRedBits(5); caps.setGreenBits(6); caps.setBlueBits(5); caps.setAlphaBits(8);
                break;
            // 32, 32, 32, 32
            case ALPHA_FLOAT: case LUMINANCE_ALPHA_FLOAT: 
            case LUMINANCE_FLOAT: case RGBA_FLOAT:
                caps.setRedBits(32); caps.setGreenBits(32); caps.setBlueBits(32); caps.setAlphaBits(32);
                break;
            // 32, 32, 32, 0
            case RGB_FLOAT:
                caps.setRedBits(32); caps.setGreenBits(32); caps.setBlueBits(32); caps.setAlphaBits(0);
                break;
            // 8, 8, 8, 8
            case LUMINANCE_ALPHA: case ALPHA: case BGRA: case BGRA_8888: 
            case RGBA_8888: case RGBA: case ARGB_8888: case ABGR_8888:
            default:
                caps.setRedBits(8); caps.setGreenBits(8); caps.setBlueBits(8); caps.setAlphaBits(8);
                break;
            }
        }
        
        if (depth != null) {
            if (ByteBuffer.class.isAssignableFrom(depth.getDataType()))
                caps.setDepthBits(16);
            else if (ShortBuffer.class.isAssignableFrom(depth.getDataType()))
                caps.setDepthBits(24);
            else
                caps.setDepthBits(32);
        } else
            caps.setDepthBits(24);

        caps.setStencilBits(0);
        return caps;
    }
}
