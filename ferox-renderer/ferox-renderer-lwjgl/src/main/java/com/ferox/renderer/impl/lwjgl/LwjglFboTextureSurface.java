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
package com.ferox.renderer.impl.lwjgl;

import java.nio.IntBuffer;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.resource.Texture.Target;

/**
 * LwjglFboTextureSurface is a TextureSurface that uses FBOs to render into
 * textures. Because it uses FBOs it will not have its own LwjglContext and is
 * instead attached to a context providing surface each time the surface must be
 * used.
 * 
 * @author Michael Ludwig
 */
public class LwjglFboTextureSurface extends AbstractTextureSurface {
    // one real fbo per context, vague documentation in object sharing
    // makes it sound as though fbo's aren't shared
    private final WeakHashMap<LwjglContext, FrameBufferObject> fbos;

    private final boolean useEXT;

    public LwjglFboTextureSurface(AbstractFramework framework,
                                  LwjglSurfaceFactory creator,
                                  TextureSurfaceOptions options) {
        super(framework, options);
        fbos = new WeakHashMap<LwjglContext, FrameBufferObject>();
        useEXT = framework.getCapabilities().getVersion() < 3.0;
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
        GL11.glFlush();
    }

    @Override
    protected void destroyImpl() {
        // Queue up tasks to every context that has an FBO lying around
        // we can't destroy them now because they require the contexts
        // to be current, so we'll just "flag" them for deletion.
        for (Entry<LwjglContext, FrameBufferObject> e : fbos.entrySet()) {
            e.getKey().queueCleanupTask(new FBOCleanupTask(e.getValue()));
        }
    }

    @Override
    public void onSurfaceActivate(OpenGLContext context, int layer) {
        super.onSurfaceActivate(context, layer);

        LwjglContext jctx = (LwjglContext) context;
        FrameBufferObject fbo = fbos.get(jctx);
        if (fbo == null) {
            fbo = new FrameBufferObject(jctx);
            fbos.put(jctx, fbo);
        }

        fbo.bind(jctx, layer);
    }

    @Override
    public void onSurfaceDeactivate(OpenGLContext context) {
        LwjglContext jctx = (LwjglContext) context;
        FrameBufferObject fbo = fbos.get(jctx);
        if (fbo != null) {
            fbo.release(jctx);
        }

        super.onSurfaceDeactivate(context);
    }

    private class FrameBufferObject {
        private final int fboId;
        private final int renderBufferId;

        private final Target target;

        private final int[] colorImageIds;

        private int boundLayer;

        private int glGenFramebuffers() {
            if (useEXT) {
                return EXTFramebufferObject.glGenFramebuffersEXT();
            } else {
                return GL30.glGenFramebuffers();
            }
        }

        private int glGenRenderbuffers() {
            if (useEXT) {
                return EXTFramebufferObject.glGenRenderbuffersEXT();
            } else {
                return GL30.glGenRenderbuffers();
            }
        }

        private void glBindFramebuffer(int id) {
            if (useEXT) {
                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                                                          id);
            } else {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id);
            }
        }

        private void glBindRenderbuffer(int id) {
            if (useEXT) {
                EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                                                           id);
            } else {
                GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, id);
            }
        }

        public FrameBufferObject(LwjglContext context) {
            if (context == null) {
                throw new FrameworkException("FramebufferObject's can only be constructed when there's a current context");
            }
            if (!context.getRenderCapabilities().getFboSupport()) {
                throw new FrameworkException("Current hardware doesn't support the creation of fbos");
            }

            target = getTarget();
            boundLayer = 0;
            int width = getWidth();
            int height = getHeight();

            fboId = glGenFramebuffers();
            glBindFramebuffer(fboId);

            int glTarget = (target == Target.T_CUBEMAP ? Utils.getGLCubeFace(0) : Utils.getGLTextureTarget(target));
            TextureHandle depth = (TextureHandle) getDepthHandle();
            if (depth != null) {
                // attach the depth texture
                attachImage(glTarget, depth.texID, 0, -1); // -1 translates to GL_DEPTH_ATTACHMENT(_EXT)

                renderBufferId = 0;
            } else {
                // make and attach the render buffer
                renderBufferId = glGenRenderbuffers();

                glBindRenderbuffer(renderBufferId);
                if (useEXT) {
                    EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                                                                  GL11.GL_DEPTH_COMPONENT,
                                                                  width, height);
                } else {
                    GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER,
                                               GL11.GL_DEPTH_COMPONENT, width, height);
                }

                if (GL11.glGetError() == GL11.GL_OUT_OF_MEMORY) {
                    glBindRenderbuffer(0);
                    destroy();
                    throw new FrameworkException("Error creating a new FBO, not enough memory for the depth RenderBuffer");
                } else {
                    glBindRenderbuffer(0);
                }

                if (useEXT) {
                    EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                                                                      EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                                                                      EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                                                                      renderBufferId);
                } else {
                    GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER,
                                                   GL30.GL_DEPTH_ATTACHMENT,
                                                   GL30.GL_RENDERBUFFER, renderBufferId);
                }
            }

            colorImageIds = new int[getNumColorBuffers()];
            for (int i = 0; i < colorImageIds.length; i++) {
                TextureHandle h = (TextureHandle) getColorHandle(i);
                colorImageIds[i] = h.texID;
                attachImage(glTarget, h.texID, 0, i);
            }

            // Enable/disable the read/draw buffers to make the fbo "complete"
            GL11.glReadBuffer(GL11.GL_NONE);
            if (colorImageIds.length > 0) {
                IntBuffer drawBuffers = BufferUtil.newIntBuffer(colorImageIds.length);
                for (int i = 0; i < colorImageIds.length; i++) {
                    if (useEXT) {
                        drawBuffers.put(i,
                                        EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT + i);
                    } else {
                        drawBuffers.put(i, GL30.GL_COLOR_ATTACHMENT0 + i);
                    }
                }

                GL20.glDrawBuffers(drawBuffers);
            } else {
                GL11.glDrawBuffer(GL11.GL_NONE);
            }

            // I'm getting a little sick of this duplication
            if (useEXT) {
                int complete = EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
                if (complete != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT) {
                    String msg = "FBO failed completion test, unable to render";
                    switch (complete) {
                    case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
                        msg = "Fbo attachments aren't complete";
                        break;
                    case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
                        msg = "Fbo needs at least one attachment";
                        break;
                    case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
                        msg = "Fbo draw buffers improperly enabled";
                        break;
                    case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
                        msg = "Fbo read buffer improperly enabled";
                        break;
                    case EXTFramebufferObject.GL_FRAMEBUFFER_UNSUPPORTED_EXT:
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
            } else {
                int complete = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
                if (complete != GL30.GL_FRAMEBUFFER_COMPLETE) {
                    String msg = "FBO failed completion test, unable to render";
                    switch (complete) {
                    case GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                        msg = "Fbo attachments aren't complete";
                        break;
                    case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                        msg = "Fbo needs at least one attachment";
                        break;
                    case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                        msg = "Fbo draw buffers improperly enabled";
                        break;
                    case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                        msg = "Fbo read buffer improperly enabled";
                        break;
                    case GL30.GL_FRAMEBUFFER_UNSUPPORTED:
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
            }

            // restore the old binding
            glBindFramebuffer(context.getFbo());
        }

        public void bind(LwjglContext context, int layer) {
            // bind the fbo if needed
            context.bindFbo(fboId);

            // possibly re-attach the images (in the case of cubemaps or 3d textures)
            int glTarget = (target == Target.T_CUBEMAP ? Utils.getGLCubeFace(layer) : Utils.getGLTextureTarget(target));
            if (layer != boundLayer) {
                if (colorImageIds != null) {
                    for (int i = 0; i < colorImageIds.length; i++) {
                        attachImage(glTarget, colorImageIds[i], layer, i);
                    }
                }
                // we don't have to re-attach depth images -> 1 layer only
                boundLayer = layer;
            }
        }

        public void release(LwjglContext context) {
            context.bindFbo(0);
        }

        public void destroyFBO() {
            if (useEXT) {
                EXTFramebufferObject.glDeleteFramebuffersEXT(fboId);
                if (renderBufferId != 0) {
                    EXTFramebufferObject.glDeleteRenderbuffersEXT(renderBufferId);
                }
            } else {
                GL30.glDeleteFramebuffers(fboId);
                if (renderBufferId != 0) {
                    GL30.glDeleteRenderbuffers(renderBufferId);
                }
            }
        }

        // Attach the given texture image to the currently bound fbo (on target FRAMEBUFFER)
        // Use 0-i for positive COLOR_ATTACHMENT, and -1 for DEPTH_ATTACHMENT
        private void attachImage(int target, int id, int layer, int attachment) {
            if (useEXT) {
                if (attachment == -1) {
                    attachment = EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT;
                } else {
                    attachment = EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT + attachment;
                }

                switch (target) {
                case GL11.GL_TEXTURE_1D:
                    EXTFramebufferObject.glFramebufferTexture1DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                                                                   attachment, target,
                                                                   id, 0);
                    break;
                case GL12.GL_TEXTURE_3D:
                    EXTFramebufferObject.glFramebufferTexture3DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                                                                   attachment, target,
                                                                   id, 0, layer);
                    break;
                default:
                    EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                                                                   attachment, target,
                                                                   id, 0);
                }
            } else {
                if (attachment == -1) {
                    attachment = GL30.GL_DEPTH_ATTACHMENT;
                } else {
                    attachment = GL30.GL_COLOR_ATTACHMENT0 + attachment;
                }

                switch (target) {
                case GL11.GL_TEXTURE_1D:
                    GL30.glFramebufferTexture1D(GL30.GL_FRAMEBUFFER, attachment, target,
                                                id, 0);
                    break;
                case GL12.GL_TEXTURE_3D:
                    GL30.glFramebufferTexture3D(GL30.GL_FRAMEBUFFER, attachment, target,
                                                id, 0, layer);
                    break;
                default:
                    GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, target,
                                                id, 0);
                }
            }
        }
    }

    private class FBOCleanupTask implements Runnable {
        private final FrameBufferObject fbo;

        public FBOCleanupTask(FrameBufferObject fbo) {
            this.fbo = fbo;
        }

        @Override
        public void run() {
            fbo.destroyFBO();
        }
    }
}
