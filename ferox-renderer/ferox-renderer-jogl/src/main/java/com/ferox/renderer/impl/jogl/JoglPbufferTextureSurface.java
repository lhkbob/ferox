package com.ferox.renderer.impl.jogl;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererProvider;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Texture;
import com.ferox.resource.TextureFormat;

/**
 * JoglPbufferTextureSurface is a TextureSurface implementation that relies on
 * pbuffers to render into an offscreen buffer before using the glCopyTexImage
 * command to move the buffer contents into a texture. PBuffers are slower than
 * FBOs so {@link JoglFboTextureSurface} is favored when FBOs are supported.
 * 
 * @author Michael Ludwig
 */
public class JoglPbufferTextureSurface extends AbstractTextureSurface {
    private final GLPbuffer pbuffer;
    private final JoglContext context;
    
    private int activeLayerForFrame;
    
    public JoglPbufferTextureSurface(AbstractFramework framework, JoglSurfaceFactory creator, 
                                     TextureSurfaceOptions options, JoglContext shareWith,
                                     RendererProvider provider) {
        super(framework, options);
        
        Texture[] colorBuffers = new Texture[getNumColorBuffers()];
        for (int i = 0; i < colorBuffers.length; i++)
            colorBuffers[i] = getColorBuffer(i);
          
        
        GLContext realShare = (shareWith == null ? null : shareWith.getGLContext());
        GLCapabilities caps = chooseCapabilities(creator.getGLProfile(), colorBuffers, getDepthBuffer());
        AbstractGraphicsDevice device = GLProfile.getDefaultDevice();
        pbuffer = GLDrawableFactory.getFactory(creator.getGLProfile()).createGLPbuffer(device, caps, new DefaultGLCapabilitiesChooser(), 
                                                                                       getWidth(), getHeight(), realShare);
        pbuffer.setAutoSwapBufferMode(false);
        
        context = new JoglContext(creator, pbuffer.getContext(), provider);
        activeLayerForFrame = 0;
    }

    @Override
    public OpenGLContext getContext() {
        return context;
    }

    @Override
    public void flush(OpenGLContext context) {
        pbuffer.swapBuffers();
        Texture color = getNumColorBuffers() > 0 ? getColorBuffer(0) : null; // will be 1 color target at max
        Texture depth = getDepthBuffer();

        GL2GL3 gl = ((JoglContext) context).getGLContext().getGL().getGL2GL3();

        int ct = -1;
        int dt = -1;

        if (color != null) {
            TextureHandle handle = (TextureHandle) getColorHandle(0);
            ct = Utils.getGLTextureTarget(handle.target);

            gl.glBindTexture(ct, handle.texID);
            copySubImage(gl, handle);
        }
        if (depth != null) {
            TextureHandle handle = (TextureHandle) getDepthHandle();
            dt = Utils.getGLTextureTarget(handle.target);
            
            gl.glBindTexture(dt, handle.texID);
            copySubImage(gl, handle);
        }
        
        restoreBindings(gl, ct, dt);
    }
    
    @Override
    public void onSurfaceActivate(OpenGLContext context, int activeLayer) {
        super.onSurfaceActivate(context, activeLayer);
        activeLayerForFrame = activeLayer;
    }

    @Override
    protected void destroyImpl() {
        context.destroy();
    }
    
    /*
     * Copy the buffer into the given TextureHandle. It assumes the texture was
     * already bound.
     */
    private void copySubImage(GL2GL3 gl, TextureHandle handle) {
        int glTarget = Utils.getGLTextureTarget(handle.target);
        switch (glTarget) {
        case GL2GL3.GL_TEXTURE_1D:
            gl.glCopyTexSubImage1D(glTarget, 0, 0, 0, 0, getWidth());
            break;
        case GL2GL3.GL_TEXTURE_2D:
            gl.glCopyTexSubImage2D(glTarget, 0, 0, 0, 0, 0, getWidth(), getHeight());
            break;
        case GL2GL3.GL_TEXTURE_CUBE_MAP:
            int face = Utils.getGLCubeFace(activeLayerForFrame);
            gl.glCopyTexSubImage2D(face, 0, 0, 0, 0, 0, getWidth(), getHeight());
            break;
        case GL2GL3.GL_TEXTURE_3D:
            gl.glCopyTexSubImage3D(glTarget, 0, 0, 0, activeLayerForFrame, 
                                   0, 0, getWidth(), getHeight());
            break;
        }
    }

    private void restoreBindings(GL gl, int colorTarget, int depthTarget) {
        int target = context.getTextureTarget(context.getActiveTexture());
        int tex = context.getTexture(context.getActiveTexture());
        
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
            if (depth.getDataType() == DataType.UNSIGNED_BYTE)
                caps.setDepthBits(16);
            else if (depth.getDataType() == DataType.UNSIGNED_SHORT)
                caps.setDepthBits(24);
            else
                caps.setDepthBits(32);
        } else
            caps.setDepthBits(24);

        caps.setStencilBits(0);
        return caps;
    }
}
