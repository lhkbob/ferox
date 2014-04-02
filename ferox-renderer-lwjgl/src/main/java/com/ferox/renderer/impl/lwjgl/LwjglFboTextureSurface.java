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

import com.ferox.renderer.DataType;
import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.Sampler;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.TextureImpl;
import org.lwjgl.opengl.*;

import java.nio.IntBuffer;
import java.util.Map.Entry;
import java.util.WeakHashMap;

/**
 * LwjglFboTextureSurface is a TextureSurface that uses FBOs to render into textures. Because it uses FBOs it
 * will not have its own LwjglContext and is instead attached to a context providing surface each time the
 * surface must be used.
 *
 * @author Michael Ludwig
 */
public class LwjglFboTextureSurface extends AbstractTextureSurface {
    private final FBODestructible impl;

    public LwjglFboTextureSurface(FrameworkImpl framework, TextureSurfaceOptions options) {
        super(framework, options);
        impl = new FBODestructible(framework);
    }

    @Override
    public SurfaceDestructible getSurfaceDestructible() {
        return impl;
    }

    @Override
    public void flush(OpenGLContext context) {
        // just call glFlush()
        GL11.glFlush();

        LwjglContext jctx = (LwjglContext) context;
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
            int dstFormat = Utils.getGLDstFormat(depth.getSampler().getFullFormat(),
                                                 getFramework().getCapabilities());
            if (dstFormat == GL30.GL_DEPTH24_STENCIL8 || dstFormat == GL14.GL_DEPTH_COMPONENT24) {
                return 24;
            } else if (dstFormat == GL14.GL_DEPTH_COMPONENT16) {
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

        LwjglContext jctx = (LwjglContext) context;
        FBODestructible.FrameBufferObject fbo = impl.fbos.get(jctx);
        if (fbo == null) {
            fbo = impl.newFBO(this, jctx);
            impl.fbos.put(jctx, fbo);
        }

        fbo.bind(this, jctx);
    }

    private static class FBODestructible extends SurfaceDestructible {
        // one real fbo per context, vague documentation in object sharing
        // makes it sound as though fbo's aren't shared
        private final WeakHashMap<LwjglContext, FrameBufferObject> fbos;
        private final boolean useEXT;

        public FBODestructible(FrameworkImpl framework) {
            super(framework);
            fbos = new WeakHashMap<>();
            useEXT = framework.getCapabilities().getMajorVersion() < 3;
        }

        public FrameBufferObject newFBO(LwjglFboTextureSurface surface, LwjglContext context) {
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
            for (Entry<LwjglContext, FrameBufferObject> e : fbos.entrySet()) {
                e.getKey().queueCleanupTask(new FBOCleanupTask(e.getValue()));
            }
        }

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

        private void glBindRenderbuffer(int id) {
            if (useEXT) {
                EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, id);
            } else {
                GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, id);
            }
        }

        private class FrameBufferObject {
            private final int fboId;
            private final int depthRenderBufferId;

            private final IntBuffer drawBuffers;
            private final TextureImpl.RenderTargetImpl[] colorBuffers;
            private TextureImpl.RenderTargetImpl depthBuffer;

            private boolean stale;

            public FrameBufferObject(LwjglFboTextureSurface surface, LwjglContext context) {
                colorBuffers = new TextureImpl.RenderTargetImpl[surface.getFramework().getCapabilities()
                                                                       .getMaxColorBuffers()];
                drawBuffers = BufferUtil.newByteBuffer(DataType.INT, colorBuffers.length).asIntBuffer();
                drawBuffers.put(0, GL30.GL_COLOR_ATTACHMENT0);
                for (int i = 1; i < colorBuffers.length; i++) {
                    drawBuffers.put(i, GL11.GL_NONE);
                }

                int width = surface.getWidth();
                int height = surface.getHeight();

                fboId = glGenFramebuffers();
                context.bindFbo(fboId);

                Sampler.TexelFormat depthFormat = surface.getDepthRenderBufferFormat();
                if (depthFormat == null) {
                    // no depth render buffer
                    depthRenderBufferId = 0;
                } else {
                    depthRenderBufferId = glGenRenderbuffers();
                    glBindRenderbuffer(depthRenderBufferId);

                    int type = (depthFormat == Sampler.TexelFormat.DEPTH ? GL14.GL_DEPTH_COMPONENT24
                                                                         : GL30.GL_DEPTH24_STENCIL8);
                    int attach = (depthFormat == Sampler.TexelFormat.DEPTH ? GL30.GL_DEPTH_ATTACHMENT
                                                                           : GL30.GL_DEPTH_STENCIL_ATTACHMENT);

                    if (useEXT) {
                        EXTFramebufferObject
                                .glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, type,
                                                          width, height);
                        EXTFramebufferObject.glFramebufferRenderbufferEXT(GL30.GL_FRAMEBUFFER, attach,
                                                                          GL30.GL_RENDERBUFFER,
                                                                          depthRenderBufferId);
                    } else {
                        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, type, width, height);
                        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, attach, GL30.GL_RENDERBUFFER,
                                                       depthRenderBufferId);
                    }
                }

                stale = true;
            }

            private void generateMipmaps(LwjglContext ctx) {
                for (int i = 0; i < colorBuffers.length; i++) {
                    if (colorBuffers[i] != null) {
                        int numMips = colorBuffers[i].getSampler().getMaxMipmap() -
                                      colorBuffers[i].getSampler().getBaseMipmap();
                        if (numMips > 1) {
                            TextureImpl.TextureHandle t = colorBuffers[i].getSampler().getHandle();
                            ctx.bindTexture(0, t);
                            if (useEXT) {
                                EXTFramebufferObject.glGenerateMipmapEXT(Utils.getGLTextureTarget(t.target));
                            } else {
                                GL30.glGenerateMipmap(Utils.getGLTextureTarget(t.target));
                            }
                        }
                    }
                }

                if (depthBuffer != null) {
                    int numMips = depthBuffer.getSampler().getMaxMipmap() -
                                  depthBuffer.getSampler().getBaseMipmap();
                    if (numMips > 1) {
                        TextureImpl.TextureHandle t = depthBuffer.getSampler().getHandle();
                        ctx.bindTexture(0, t);
                        if (useEXT) {
                            EXTFramebufferObject.glGenerateMipmapEXT(Utils.getGLTextureTarget(t.target));
                        } else {
                            GL30.glGenerateMipmap(Utils.getGLTextureTarget(t.target));
                        }
                    }
                }
            }

            private void configureDrawBuffers() {
                // the draw/read buffer state is per FBO so this won't change because of another FBO
                GL11.glReadBuffer(GL11.GL_NONE);
                boolean change = false;
                for (int i = 0; i < colorBuffers.length; i++) {
                    int attach = GL11.GL_NONE;
                    if (colorBuffers[i] != null) {
                        attach = (useEXT ? EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT + i
                                         : GL30.GL_COLOR_ATTACHMENT0 + i);
                    }
                    if (attach != drawBuffers.get(i)) {
                        change = true;
                        drawBuffers.put(i, attach);
                    }
                }

                if (change) {
                    GL20.glDrawBuffers(drawBuffers);
                }
            }

            private void checkFBOStatus() {
                if (useEXT) {
                    int complete = EXTFramebufferObject
                                           .glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
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
                        throw new FrameworkException(msg);
                    }
                }
            }

            public void bind(LwjglFboTextureSurface surface, LwjglContext context) {
                // bind the fbo if needed
                context.bindFbo(fboId);
                if (stale) {
                    // re-attach all changed images
                    for (int i = 0; i < colorBuffers.length; i++) {
                        TextureImpl.RenderTargetImpl newImage = surface.getColorBuffer(i);
                        if (newImage == null && colorBuffers[i] != null) {
                            attachImage(null, 0, 0, GL30.GL_COLOR_ATTACHMENT0 + i);
                            colorBuffers[i] = null;
                        } else if (newImage != null) {
                            if (colorBuffers[i] == null ||
                                colorBuffers[i].getSampler() != newImage.getSampler() ||
                                colorBuffers[i].image != newImage.image) {
                                attachImage(newImage.getSampler().getHandle(), newImage.image,
                                            newImage.getSampler().getBaseMipmap(),
                                            GL30.GL_COLOR_ATTACHMENT0 + i);
                                colorBuffers[i] = newImage;
                            }
                        }
                    }

                    TextureImpl.RenderTargetImpl newDepth = surface.getDepthBuffer();
                    if (newDepth == null && depthBuffer != null) {
                        attachImage(null, 0, 0,
                                    (depthBuffer.getSampler().getFormat() == Sampler.TexelFormat.DEPTH
                                     ? GL30.GL_DEPTH_ATTACHMENT : GL30.GL_DEPTH_STENCIL_ATTACHMENT)
                                   );
                        depthBuffer = null;
                    } else if (newDepth != null) {
                        if (depthBuffer == null || depthBuffer.getSampler() != newDepth.getSampler() ||
                            depthBuffer.image != newDepth.image) {
                            if (newDepth.getSampler().getFormat() == Sampler.TexelFormat.DEPTH) {
                                if (depthBuffer != null && depthBuffer.getSampler().getFormat() ==
                                                           Sampler.TexelFormat.DEPTH_STENCIL) {
                                    attachImage(null, 0, 0, GL30.GL_DEPTH_STENCIL_ATTACHMENT);
                                }
                                attachImage(newDepth.getSampler().getHandle(), newDepth.image,
                                            newDepth.getSampler().getBaseMipmap(), GL30.GL_DEPTH_ATTACHMENT);
                            } else {
                                if (depthBuffer != null &&
                                    depthBuffer.getSampler().getFormat() == Sampler.TexelFormat.DEPTH) {
                                    attachImage(null, 0, 0, GL30.GL_DEPTH_ATTACHMENT);
                                }
                                attachImage(newDepth.getSampler().getHandle(), newDepth.image,
                                            newDepth.getSampler().getBaseMipmap(),
                                            GL30.GL_DEPTH_STENCIL_ATTACHMENT);
                            }
                            depthBuffer = newDepth;
                        }
                    }

                    configureDrawBuffers();
                    checkFBOStatus();
                    stale = false;
                }
            }

            public void destroyFBO() {
                if (useEXT) {
                    EXTFramebufferObject.glDeleteFramebuffersEXT(fboId);
                    if (depthRenderBufferId != 0) {
                        EXTFramebufferObject.glDeleteRenderbuffersEXT(depthRenderBufferId);
                    }
                } else {
                    GL30.glDeleteFramebuffers(fboId);
                    if (depthRenderBufferId != 0) {
                        GL30.glDeleteRenderbuffers(depthRenderBufferId);
                    }
                }
            }

            private void attachImage(TextureImpl.TextureHandle texture, int layer, int level,
                                     int attachment) {
                if (texture == null) {
                    // according to documentation, any glFramebufferTexture call with id = 0
                    // unbinds all textures at that attachment point (and textarget and level are ignored)
                    if (useEXT) {
                        EXTFramebufferObject
                                .glFramebufferTexture2DEXT(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, 0,
                                                           0, 0);
                    } else {
                        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, 0, 0, 0);
                    }
                } else {
                    if (useEXT) {
                        switch (texture.target) {
                        case TEX_1D:
                            EXTFramebufferObject.glFramebufferTexture1DEXT(GL30.GL_FRAMEBUFFER, attachment,
                                                                           GL11.GL_TEXTURE_1D, texture.texID,
                                                                           level);
                            break;
                        case TEX_2D:
                            EXTFramebufferObject.glFramebufferTexture2DEXT(GL30.GL_FRAMEBUFFER, attachment,
                                                                           GL11.GL_TEXTURE_2D, texture.texID,
                                                                           level);
                            break;
                        case TEX_3D:
                            EXTFramebufferObject.glFramebufferTexture3DEXT(GL30.GL_FRAMEBUFFER, attachment,
                                                                           GL12.GL_TEXTURE_3D, texture.texID,
                                                                           level, layer);
                            break;
                        case TEX_CUBEMAP:
                            int face = Utils.getGLCubeFace(layer);
                            EXTFramebufferObject
                                    .glFramebufferTexture2DEXT(GL30.GL_FRAMEBUFFER, attachment, face,
                                                               texture.texID, level);
                            break;
                        case TEX_2D_ARRAY:
                        case TEX_1D_ARRAY:
                            EXTTextureArray.glFramebufferTextureLayerEXT(GL30.GL_FRAMEBUFFER, attachment,
                                                                         texture.texID, level, layer);
                            break;
                        }
                    } else {
                        switch (texture.target) {
                        case TEX_1D:
                            GL30.glFramebufferTexture1D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_1D,
                                                        texture.texID, level);
                            break;
                        case TEX_2D:
                            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D,
                                                        texture.texID, level);
                            break;
                        case TEX_3D:
                            GL30.glFramebufferTexture3D(GL30.GL_FRAMEBUFFER, attachment, GL12.GL_TEXTURE_3D,
                                                        texture.texID, level, layer);
                            break;
                        case TEX_CUBEMAP:
                            int face = Utils.getGLCubeFace(layer);
                            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, face, texture.texID,
                                                        level);
                            break;
                        case TEX_2D_ARRAY:
                        case TEX_1D_ARRAY:
                            GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, attachment, texture.texID,
                                                           level, layer);
                            break;
                        }
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
}
