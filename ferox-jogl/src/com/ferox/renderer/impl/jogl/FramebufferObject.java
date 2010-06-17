package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.jogl.Utils;
import com.ferox.renderer.impl.resource.ResourceHandle;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;

/**
 * FramebufferObject is a low-level wrapper around an OpenGL fbo and can be used
 * to perform render-to-texture effects. These should not be used directly and
 * are intended to be managed by a {@link FboSurfaceDelegate}.
 * 
 * @author Michael Ludwig
 */
public class FramebufferObject {
    private final int fboId;
    private int renderBufferId;

    private final Target target;

    private int boundLayer;
    private int[] colorImageIds;

    public FramebufferObject(JoglFramework framework, Texture[] colors, Texture depth) {
        JoglContext context = JoglContext.getCurrent();
        if (context == null)
            throw new RenderException("FramebufferObject's can only be constructed when there's a current context");
        if (!framework.getCapabilities().getFboSupport())
            throw new RenderException("Current hardware doesn't support the creation of fbos");

        GL2GL3 gl = context.getGL();
        
        boundLayer = 0;
        int width, height;
        if (colors != null) {
            width = colors[0].getWidth();
            height = colors[0].getHeight();
            target = colors[0].getTarget();
        } else {
            width = depth.getWidth();
            height = depth.getHeight();
            target = depth.getTarget();
        }

        int[] id = new int[1];
        gl.glGenFramebuffers(1, id, 0);
        fboId = id[0];
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboId);

        int glTarget = (target == Target.T_CUBEMAP ? Utils.getGLCubeFace(0) : Utils.getGLTextureTarget(target));
        if (depth != null) {
            // attach the depth texture
            ResourceHandle h = framework.getResourceManager().getHandle(depth);
            attachImage(gl, glTarget, h.getId(), 0, GL.GL_DEPTH_ATTACHMENT);

            renderBufferId = 0;
        } else {
            // make and attach the render buffer
            gl.glGenRenderbuffers(1, id, 0);
            renderBufferId = id[0];

            gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, renderBufferId);
            gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL2GL3.GL_DEPTH_COMPONENT, width, height);

            if (gl.glGetError() == GL.GL_OUT_OF_MEMORY) {
                gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);
                destroy();
                throw new RenderException("Error creating a new FBO, not enough memory for the depth RenderBuffer");
            } else
                gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, 
                                         GL.GL_RENDERBUFFER, renderBufferId);
        }

        if (colors != null && colors.length > 0) {
            // attach all of the images
            colorImageIds = new int[colors.length];
            ResourceHandle h;
            for (int i = 0; i < colors.length; i++) {
                h = framework.getResourceManager().getHandle(colors[i]);
                attachImage(gl, glTarget, h.getId(), 0, GL.GL_COLOR_ATTACHMENT0 + i);
                colorImageIds[i] = h.getId();
            }
        } else
            colorImageIds = null;

        // Enable/disable the read/draw buffers to make the fbo "complete"
        gl.glReadBuffer(GL.GL_NONE);
        if (colorImageIds != null) {
            int[] drawBuffers = new int[colorImageIds.length];
            for (int i = 0; i < drawBuffers.length; i++)
                drawBuffers[i] = GL.GL_COLOR_ATTACHMENT0 + i;
            gl.glDrawBuffers(drawBuffers.length, drawBuffers, 0);
        } else
            gl.glDrawBuffer(GL.GL_NONE);

        int complete = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
        if (complete != GL.GL_FRAMEBUFFER_COMPLETE) {
            String msg = "FBO failed completion test, unable to render";
            switch (complete) {
            case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                msg = "Fbo attachments aren't complete";
                break;
            case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                msg = "Fbo needs at least one attachment";
                break;
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                msg = "Fbo draw buffers improperly enabled";
                break;
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                msg = "Fbo read buffer improperly enabled";
                break;
            case GL.GL_FRAMEBUFFER_UNSUPPORTED:
                msg = "Texture/Renderbuffer combinations aren't supported on the hardware";
                break;
            case 0:
                msg = "glCheckFramebufferStatusEXT() had an error while checking fbo status";
                break;
            }
            // clean-up and then throw an exception
            destroy();
            throw new RenderException(msg);
        }

        // restore the old binding
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, context.getRecord().getFbo());
    }

    public void bind(int layer, int depth) {
        JoglContext context = JoglContext.getCurrent();
        GL2GL3 gl = context.getGL();
        
        // bind the fbo if needed
        context.getRecord().bindFbo(gl, fboId);

        // possibly re-attach the images (in the case of cubemaps or 3d textures)
        int toBind = (target == Target.T_CUBEMAP ? layer : depth);
        int glTarget = (target == Target.T_CUBEMAP ? Utils.getGLCubeFace(layer) : Utils.getGLTextureTarget(target));
        if (toBind != boundLayer) {
            if (colorImageIds != null) {
                for (int i = 0; i < colorImageIds.length; i++)
                    attachImage(gl, glTarget, colorImageIds[i], toBind, 
                                GL.GL_COLOR_ATTACHMENT0 + i);
            }
            // we don't have to re-attach depth images -> 1 layer only
            boundLayer = toBind;
        }
    }

    public void release() {
        JoglContext context = JoglContext.getCurrent();
        context.getRecord().bindFbo(context.getGL(), 0);
    }

    public void destroy() {
        GL2GL3 gl = JoglContext.getCurrent().getGL();
        gl.glDeleteFramebuffers(1, new int[] { fboId }, 0);
        if (renderBufferId != 0)
            gl.glDeleteRenderbuffers(1, new int[] { renderBufferId }, 0);
    }

    // Attach the given texture image to the currently bound fbo (on target FRAMEBUFFER)
    private void attachImage(GL2GL3 gl, int target, int id, int layer, int attachment) {
        switch (target) {
        case GL2GL3.GL_TEXTURE_1D:
            gl.glFramebufferTexture1D(GL.GL_FRAMEBUFFER, attachment, target, id, 0);
            break;
        case GL2GL3.GL_TEXTURE_3D:
            gl.glFramebufferTexture3D(GL.GL_FRAMEBUFFER, attachment, target, id, 0, layer);
            break;
        default: // 2d or a cubemap face
            gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, attachment, target, id, 0);
        }
    }
}
