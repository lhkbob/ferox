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

import com.ferox.renderer.DataType;
import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.Sampler;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.TextureImpl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import java.nio.IntBuffer;
import java.util.Map.Entry;
import java.util.WeakHashMap;

/**
 * JoglFboTextureSurface is a TextureSurface that uses FBOs to render into textures. Because it uses FBOs it
 * will not have its own JoglContext and is instead attached to a context providing surface each time the
 * surface must be used.
 *
 * @author Michael Ludwig
 */
public class JoglFboTextureSurface extends AbstractTextureSurface {
    private final FBODestructible impl;

    public JoglFboTextureSurface(FrameworkImpl framework, TextureSurfaceOptions options) {
        super(framework, options);
        impl = new FBODestructible(framework);
    }

    @Override
    public SurfaceDestructible getSurfaceDestructible() {
        return impl;
    }

    @Override
    public void flush(OpenGLContext context) {
        // just call glFlush() and generate mipmaps
        JoglContext jctx = (JoglContext) context;
        jctx.getGLContext().getGL().glFlush();
        FBODestructible.FrameBufferObject fbo = impl.fbos.get(jctx);
        if (fbo != null) {
            fbo.generateMipmaps(jctx);
        }
    }

    @Override
    public void setRenderTargets(Sampler.RenderTarget[] colorTargets, Sampler.RenderTarget depthTarget) {
        super.setRenderTargets(colorTargets, depthTarget);
        for (FBODestructible.FrameBufferObject fbo : impl.fbos.values()) {
            fbo.stale = true;
        }
    }

    @Override
    public int getDepthBufferBits() {
        TextureImpl.RenderTargetImpl depth = getDepthBuffer();
        if (depth != null) {
            int dstFormat = Utils
                    .getGLDstFormat(depth.getSampler().getFullFormat(), getFramework().getCapabilities());
            if (dstFormat == GL2GL3.GL_DEPTH24_STENCIL8 || dstFormat == GL2GL3.GL_DEPTH_COMPONENT24) {
                return 24;
            } else if (dstFormat == GL2GL3.GL_DEPTH_COMPONENT16) {
                return 16;
            } else {
                return 32; // GL_DEPTH_COMPONENT32F
            }
        } else {
            return (getDepthRenderBufferFormat() != null ? 24 : 0);
        }
    }

    @Override
    public int getStencilBufferBits() {
        TextureImpl.RenderTargetImpl depth = getDepthBuffer();
        if (depth != null) {
            return (depth.getSampler().getFormat() == Sampler.TexelFormat.DEPTH_STENCIL ? 8 : 0);
        } else {
            return (getDepthRenderBufferFormat() == Sampler.TexelFormat.DEPTH_STENCIL ? 8 : 0);
        }
    }

    @Override
    public void onSurfaceActivate(OpenGLContext context) {
        super.onSurfaceActivate(context);

        JoglContext jctx = (JoglContext) context;
        FBODestructible.FrameBufferObject fbo = impl.fbos.get(jctx);
        if (fbo == null) {
            fbo = impl.newFBO(this, jctx);
            impl.fbos.put(jctx, fbo);
        }

        fbo.bind(this, jctx);
    }

    @Override
    public void onSurfaceDeactivate(OpenGLContext context) {
        JoglContext jctx = (JoglContext) context;
        FBODestructible.FrameBufferObject fbo = impl.fbos.get(jctx);
        if (fbo != null) {
            jctx.bindFbo(0);
        }

        super.onSurfaceDeactivate(context);
    }

    private static class FBODestructible extends SurfaceDestructible {
        // one real fbo per context, vague documentation in object sharing
        // makes it sound as though fbo's aren't shared
        private final WeakHashMap<JoglContext, FrameBufferObject> fbos;

        public FBODestructible(FrameworkImpl framework) {
            super(framework);
            fbos = new WeakHashMap<>();
        }

        public FrameBufferObject newFBO(JoglFboTextureSurface surface, JoglContext context) {
            return new FrameBufferObject(surface, context);
        }

        @Override
        public OpenGLContext getContext() {
            // no context for this type of surface
            return null;
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

        private int glGenFramebuffers(GL2GL3 gl) {
            int[] query = new int[1];
            gl.glGenFramebuffers(1, query, 0);
            return query[0];
        }

        private int glGenRenderbuffers(GL2GL3 gl) {
            int[] query = new int[1];
            gl.glGenRenderbuffers(1, query, 0);
            return query[0];
        }

        private class FrameBufferObject {
            private final int fboId;
            private final int depthRenderBufferId;

            private final IntBuffer drawBuffers;
            private final TextureImpl.RenderTargetImpl[] colorBuffers;
            private TextureImpl.RenderTargetImpl depthBuffer;

            private boolean stale;

            public FrameBufferObject(JoglFboTextureSurface surface, JoglContext context) {
                GL2GL3 gl = context.getGLContext().getGL().getGL2GL3();

                colorBuffers = new TextureImpl.RenderTargetImpl[surface.getFramework().getCapabilities()
                                                                       .getMaxColorBuffers()];
                drawBuffers = BufferUtil.newByteBuffer(DataType.INT, colorBuffers.length).asIntBuffer();
                drawBuffers.put(0, GL2GL3.GL_COLOR_ATTACHMENT0);
                for (int i = 1; i < colorBuffers.length; i++) {
                    drawBuffers.put(i, GL.GL_NONE);
                }

                int width = surface.getWidth();
                int height = surface.getHeight();

                fboId = glGenFramebuffers(gl);
                context.bindFbo(fboId);

                Sampler.TexelFormat depthFormat = surface.getDepthRenderBufferFormat();
                if (depthFormat == null) {
                    // no depth render buffer
                    depthRenderBufferId = 0;
                } else {
                    depthRenderBufferId = glGenRenderbuffers(gl);
                    gl.glBindRenderbuffer(GL2GL3.GL_RENDERBUFFER, depthRenderBufferId);

                    int type = (depthFormat == Sampler.TexelFormat.DEPTH ? GL2GL3.GL_DEPTH_COMPONENT24
                                                                         : GL2GL3.GL_DEPTH24_STENCIL8);
                    int attach = (depthFormat == Sampler.TexelFormat.DEPTH ? GL2GL3.GL_DEPTH_ATTACHMENT
                                                                           : GL2GL3.GL_DEPTH_STENCIL_ATTACHMENT);

                    gl.glRenderbufferStorage(GL2GL3.GL_RENDERBUFFER, type, width, height);
                    gl.glFramebufferRenderbuffer(GL2GL3.GL_FRAMEBUFFER, attach, GL2GL3.GL_RENDERBUFFER,
                                                 depthRenderBufferId);
                }

                stale = true;
            }

            private void generateMipmaps(JoglContext ctx) {
                GL2GL3 gl = ctx.getGLContext().getGL().getGL2GL3();
                for (int i = 0; i < colorBuffers.length; i++) {
                    if (colorBuffers[i] != null) {
                        int numMips = colorBuffers[i].getSampler().getMaxMipmap() -
                                      colorBuffers[i].getSampler().getBaseMipmap();
                        if (numMips > 1) {
                            TextureImpl.TextureHandle t = colorBuffers[i].getSampler().getHandle();
                            ctx.bindTexture(0, t);
                            gl.glGenerateMipmap(Utils.getGLTextureTarget(t.target));
                        }
                    }
                }

                if (depthBuffer != null) {
                    int numMips = depthBuffer.getSampler().getMaxMipmap() -
                                  depthBuffer.getSampler().getBaseMipmap();
                    if (numMips > 1) {
                        TextureImpl.TextureHandle t = depthBuffer.getSampler().getHandle();
                        ctx.bindTexture(0, t);
                        gl.glGenerateMipmap(Utils.getGLTextureTarget(t.target));
                    }
                }
            }

            private void configureDrawBuffers(GL2GL3 gl) {
                // the draw/read buffer state is per FBO so this won't change because of another FBO
                gl.glReadBuffer(GL.GL_NONE);
                boolean change = false;
                for (int i = 0; i < colorBuffers.length; i++) {
                    int attach = GL.GL_NONE;
                    if (colorBuffers[i] != null) {
                        attach = GL2GL3.GL_COLOR_ATTACHMENT0 + i;
                    }
                    if (attach != drawBuffers.get(i)) {
                        change = true;
                        drawBuffers.put(i, attach);
                    }
                }

                if (change) {
                    gl.glDrawBuffers(colorBuffers.length, drawBuffers);
                }
            }

            private void checkFBOStatus(GL2GL3 gl) {
                int complete = gl.glCheckFramebufferStatus(GL2GL3.GL_FRAMEBUFFER);
                if (complete != GL2GL3.GL_FRAMEBUFFER_COMPLETE) {
                    String msg = "FBO failed completion test, unable to render";
                    switch (complete) {
                    case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                        msg = "Fbo attachments aren't complete";
                        break;
                    case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                        msg = "Fbo needs at least one attachment";
                        break;
                    case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                        msg = "Fbo draw buffers improperly enabled";
                        break;
                    case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                        msg = "Fbo read buffer improperly enabled";
                        break;
                    case GL2GL3.GL_FRAMEBUFFER_UNSUPPORTED:
                        msg = "Texture/Renderbuffer combinations aren't supported on the hardware";
                        break;
                    case 0:
                        msg = "glCheckFramebufferStatusEXT() had an error while checking fbo status";
                        break;
                    }
                    throw new FrameworkException(msg);
                }
            }

            public void bind(JoglFboTextureSurface surface, JoglContext context) {
                // bind the fbo if needed
                context.bindFbo(fboId);
                if (stale) {
                    GL2GL3 gl = context.getGLContext().getGL().getGL2GL3();

                    // re-attach all changed images
                    for (int i = 0; i < colorBuffers.length; i++) {
                        TextureImpl.RenderTargetImpl newImage = surface.getColorBuffer(i);
                        if (newImage == null && colorBuffers[i] != null) {
                            attachImage(gl, null, 0, 0, GL2GL3.GL_COLOR_ATTACHMENT0 + i);
                            colorBuffers[i] = null;
                        } else if (newImage != null) {
                            if (colorBuffers[i] == null ||
                                colorBuffers[i].getSampler() != newImage.getSampler() ||
                                colorBuffers[i].image != newImage.image) {
                                attachImage(gl, newImage.getSampler().getHandle(), newImage.image,
                                            newImage.getSampler().getBaseMipmap(),
                                            GL2GL3.GL_COLOR_ATTACHMENT0 + i);
                                colorBuffers[i] = newImage;
                            }
                        }
                    }

                    TextureImpl.RenderTargetImpl newDepth = surface.getDepthBuffer();
                    if (newDepth == null && depthBuffer != null) {
                        attachImage(gl, null, 0, 0,
                                    (depthBuffer.getSampler().getFormat() == Sampler.TexelFormat.DEPTH
                                     ? GL2GL3.GL_DEPTH_ATTACHMENT : GL2GL3.GL_DEPTH_STENCIL_ATTACHMENT));
                        depthBuffer = null;
                    } else if (newDepth != null) {
                        if (depthBuffer == null || depthBuffer.getSampler() != newDepth.getSampler() ||
                            depthBuffer.image != newDepth.image) {
                            if (newDepth.getSampler().getFormat() == Sampler.TexelFormat.DEPTH) {
                                if (depthBuffer != null && depthBuffer.getSampler().getFormat() ==
                                                           Sampler.TexelFormat.DEPTH_STENCIL) {
                                    attachImage(gl, null, 0, 0, GL2GL3.GL_DEPTH_STENCIL_ATTACHMENT);
                                }
                                attachImage(gl, newDepth.getSampler().getHandle(), newDepth.image,
                                            newDepth.getSampler().getBaseMipmap(),
                                            GL2GL3.GL_DEPTH_ATTACHMENT);
                            } else {
                                if (depthBuffer != null &&
                                    depthBuffer.getSampler().getFormat() == Sampler.TexelFormat.DEPTH) {
                                    attachImage(gl, null, 0, 0, GL2GL3.GL_DEPTH_ATTACHMENT);
                                }
                                attachImage(gl, newDepth.getSampler().getHandle(), newDepth.image,
                                            newDepth.getSampler().getBaseMipmap(),
                                            GL2GL3.GL_DEPTH_STENCIL_ATTACHMENT);
                            }
                            depthBuffer = newDepth;
                        }
                    }

                    configureDrawBuffers(gl);
                    checkFBOStatus(gl);
                    stale = false;
                }
            }

            public void destroyFBO(JoglContext context) {
                GL2GL3 gl = context.getGLContext().getGL().getGL2GL3();
                gl.glDeleteFramebuffers(1, new int[] { fboId }, 0);
                if (depthRenderBufferId != 0) {
                    gl.glDeleteRenderbuffers(1, new int[] { depthRenderBufferId }, 0);
                }
            }
        }

        private void attachImage(GL2GL3 gl, TextureImpl.TextureHandle texture, int layer, int level,
                                 int attachment) {
            if (texture == null) {
                // according to documentation, any glFramebufferTexture call with id = 0
                // unbinds all textures at that attachment point (and textarget and level are ignored)
                gl.glFramebufferTexture2D(GL2GL3.GL_FRAMEBUFFER, GL2GL3.GL_DEPTH_ATTACHMENT, 0, 0, 0);
            } else {
                switch (texture.target) {
                case TEX_1D:
                    gl.glFramebufferTexture1D(GL2GL3.GL_FRAMEBUFFER, attachment, GL2GL3.GL_TEXTURE_1D,
                                              texture.texID, level);
                    break;
                case TEX_2D:
                    gl.glFramebufferTexture2D(GL2GL3.GL_FRAMEBUFFER, attachment, GL2GL3.GL_TEXTURE_2D,
                                              texture.texID, level);
                    break;
                case TEX_3D:
                    gl.glFramebufferTexture3D(GL2GL3.GL_FRAMEBUFFER, attachment, GL2GL3.GL_TEXTURE_3D,
                                              texture.texID, level, layer);
                    break;
                case TEX_CUBEMAP:
                    int face = Utils.getGLCubeFace(layer);
                    gl.glFramebufferTexture2D(GL2GL3.GL_FRAMEBUFFER, attachment, face, texture.texID, level);
                    break;
                case TEX_2D_ARRAY:
                case TEX_1D_ARRAY:
                    gl.glFramebufferTextureLayer(GL2GL3.GL_FRAMEBUFFER, attachment, texture.texID, level,
                                                 layer);
                    break;
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
}
