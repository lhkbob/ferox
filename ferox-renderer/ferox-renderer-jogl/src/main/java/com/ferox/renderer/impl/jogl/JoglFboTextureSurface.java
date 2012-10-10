/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.impl.jogl;

import java.util.Map.Entry;
import java.util.WeakHashMap;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.resource.Texture.Target;

/**
 * JoglFboTextureSurface is a TextureSurface that uses FBOs to render into
 * textures. Because it uses FBOs it will not have its own JoglContext and is
 * instead attached to a context providing surface each time the surface must be
 * used.
 * 
 * @author Michael Ludwig
 */
public class JoglFboTextureSurface extends AbstractTextureSurface {
    // one real fbo per context, vague documentation in object sharing
    // makes it sound as though fbo's aren't shared
    private final WeakHashMap<JoglContext, FrameBufferObject> fbos;

    public JoglFboTextureSurface(AbstractFramework framework, JoglSurfaceFactory creator,
                                 TextureSurfaceOptions options) {
        super(framework, options);
        fbos = new WeakHashMap<JoglContext, FrameBufferObject>();
    }

    @Override
    public OpenGLContext getContext() {
        return null;
    }

    @Override
    public void flush(OpenGLContext context) {
        // fbos are rendered into directly so we have no
        // copying to be done (as with pbuffers), or buffer
        // swapping to do.

        // just call glFlush()
        getGL(context).glFlush();
    }

    @Override
    protected void destroyImpl() {
        // Queue up tasks to every context that has an FBO lying around
        // we can't destroy them now because they require the contexts
        // to be current, so we'll just "flag" them for deletion.
        for (Entry<JoglContext, FrameBufferObject> e : fbos.entrySet()) {
            e.getKey().queueCleanupTask(new FBOCleanupTask(e.getValue(), e.getKey()));
        }
    }

    @Override
    public void onSurfaceActivate(OpenGLContext context, int layer) {
        super.onSurfaceActivate(context, layer);

        JoglContext jctx = (JoglContext) context;
        FrameBufferObject fbo = fbos.get(jctx);
        if (fbo == null) {
            fbo = new FrameBufferObject(jctx);
            fbos.put(jctx, fbo);
        }

        fbo.bind(jctx, layer);
    }

    @Override
    public void onSurfaceDeactivate(OpenGLContext context) {
        JoglContext jctx = (JoglContext) context;
        FrameBufferObject fbo = fbos.get(jctx);
        if (fbo != null) {
            fbo.release(jctx);
        }

        super.onSurfaceDeactivate(context);
    }

    private GL2GL3 getGL(OpenGLContext context) {
        return ((JoglContext) context).getGLContext().getGL().getGL2GL3();
    }

    private class FrameBufferObject {
        private final int fboId;
        private final int renderBufferId;

        private final Target target;

        private final int[] colorImageIds;

        private int boundLayer;

        public FrameBufferObject(JoglContext context) {
            if (context == null) {
                throw new FrameworkException("FramebufferObject's can only be constructed when there's a current context");
            }
            if (!context.getRenderCapabilities().getFboSupport()) {
                throw new FrameworkException("Current hardware doesn't support the creation of fbos");
            }

            GL2GL3 gl = getGL(context);

            target = getTarget();
            boundLayer = 0;
            int width = getWidth();
            int height = getHeight();

            int[] id = new int[1];
            gl.glGenFramebuffers(1, id, 0);
            fboId = id[0];
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboId);

            int glTarget = (target == Target.T_CUBEMAP ? Utils.getGLCubeFace(0) : Utils.getGLTextureTarget(target));
            TextureHandle depth = (TextureHandle) getDepthHandle();
            if (depth != null) {
                // attach the depth texture
                attachImage(gl, glTarget, depth.texID, 0, GL.GL_DEPTH_ATTACHMENT);

                renderBufferId = 0;
            } else {
                // make and attach the render buffer
                gl.glGenRenderbuffers(1, id, 0);
                renderBufferId = id[0];

                gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, renderBufferId);
                gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL2ES2.GL_DEPTH_COMPONENT,
                                         width, height);

                if (gl.glGetError() == GL.GL_OUT_OF_MEMORY) {
                    gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);
                    destroy();
                    throw new FrameworkException("Error creating a new FBO, not enough memory for the depth RenderBuffer");
                } else {
                    gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);
                }
                gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT,
                                             GL.GL_RENDERBUFFER, renderBufferId);
            }

            colorImageIds = new int[getNumColorBuffers()];
            for (int i = 0; i < colorImageIds.length; i++) {
                TextureHandle h = (TextureHandle) getColorHandle(i);
                colorImageIds[i] = h.texID;
                attachImage(gl, glTarget, h.texID, 0, GL.GL_COLOR_ATTACHMENT0 + i);
            }

            // Enable/disable the read/draw buffers to make the fbo "complete"
            gl.glReadBuffer(GL.GL_NONE);
            if (colorImageIds != null) {
                int[] drawBuffers = new int[colorImageIds.length];
                for (int i = 0; i < drawBuffers.length; i++) {
                    drawBuffers[i] = GL.GL_COLOR_ATTACHMENT0 + i;
                }
                gl.glDrawBuffers(drawBuffers.length, drawBuffers, 0);
            } else {
                gl.glDrawBuffer(GL.GL_NONE);
            }

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
                throw new FrameworkException(msg);
            }

            // restore the old binding
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, context.getFbo());
        }

        public void bind(JoglContext context, int layer) {
            GL2GL3 gl = getGL(context);

            // bind the fbo if needed
            context.bindFbo(gl, fboId);

            // possibly re-attach the images (in the case of cubemaps or 3d textures)
            int glTarget = (target == Target.T_CUBEMAP ? Utils.getGLCubeFace(layer) : Utils.getGLTextureTarget(target));
            if (layer != boundLayer) {
                if (colorImageIds != null) {
                    for (int i = 0; i < colorImageIds.length; i++) {
                        attachImage(gl, glTarget, colorImageIds[i], layer,
                                    GL.GL_COLOR_ATTACHMENT0 + i);
                    }
                }
                // we don't have to re-attach depth images -> 1 layer only
                boundLayer = layer;
            }
        }

        public void release(JoglContext context) {
            context.bindFbo(getGL(context), 0);
        }

        public void destroyFBO(JoglContext context) {
            GL2GL3 gl = getGL(context);
            gl.glDeleteFramebuffers(1, new int[] {fboId}, 0);
            if (renderBufferId != 0) {
                gl.glDeleteRenderbuffers(1, new int[] {renderBufferId}, 0);
            }
        }

        // Attach the given texture image to the currently bound fbo (on target FRAMEBUFFER)
        private void attachImage(GL2GL3 gl, int target, int id, int layer, int attachment) {
            switch (target) {
            case GL2GL3.GL_TEXTURE_1D:
                gl.glFramebufferTexture1D(GL.GL_FRAMEBUFFER, attachment, target, id, 0);
                break;
            case GL2ES2.GL_TEXTURE_3D:
                gl.glFramebufferTexture3D(GL.GL_FRAMEBUFFER, attachment, target, id, 0,
                                          layer);
                break;
            default: // 2d or a cubemap face
                gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, attachment, target, id, 0);
            }
        }
    }

    private class FBOCleanupTask implements Runnable {
        private final FrameBufferObject fbo;
        private final JoglContext context;

        public FBOCleanupTask(FrameBufferObject fbo, JoglContext context) {
            this.fbo = fbo;
            this.context = context;
        }

        @Override
        public void run() {
            fbo.destroyFBO(context);
        }
    }
}
