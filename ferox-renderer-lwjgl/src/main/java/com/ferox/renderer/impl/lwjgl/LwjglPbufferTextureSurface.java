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

import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurfaceOptions;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.TextureImpl;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;

/**
 * LwjglPbufferTextureSurface is a TextureSurface implementation that relies on pbuffers to render into an
 * offscreen buffer before using the glCopyTexImage command to move the buffer contents into a texture.
 * PBuffers are slower than FBOs so {@link LwjglFboTextureSurface} is favored when FBOs are supported.
 *
 * @author Michael Ludwig
 */
public class LwjglPbufferTextureSurface extends AbstractTextureSurface {
    private final PbufferDestructible impl;

    public LwjglPbufferTextureSurface(FrameworkImpl framework, TextureSurfaceOptions options,
                                      LwjglContext shareWith) {
        super(framework, options);

        // Always create with a basic 32-bit color, 24-bit depth, and 8-bit stencil
        // that way both depth-stencil textures and renderbuffers are supported
        PixelFormat format = new PixelFormat().withBitsPerPixel(24).withAlphaBits(8).withDepthBits(24)
                                              .withStencilBits(8);
        Drawable realShare = (shareWith == null ? null : shareWith.getDrawable());
        try {
            Pbuffer pbuffer = new Pbuffer(getWidth(), getHeight(), format, realShare);
            impl = new PbufferDestructible(framework, pbuffer,
                                           new LwjglContext(framework.getCapabilities(), pbuffer));
        } catch (LWJGLException e) {
            throw new SurfaceCreationException("Unable to create Pbuffer", e);
        }

    }

    @Override
    public int getDepthBufferBits() {
        return 24;
    }

    @Override
    public int getStencilBufferBits() {
        return 8;
    }

    @Override
    public SurfaceDestructible getSurfaceDestructible() {
        return impl;
    }

    @Override
    public void onSurfaceActivate(OpenGLContext context) {
        super.onSurfaceActivate(context);
        ((LwjglContext) context).bindFbo(0);
    }

    @Override
    public void flush(OpenGLContext context) {
        try {
            impl.pbuffer.swapBuffers();
        } catch (LWJGLException e) {
            throw new FrameworkException("Error flushing Pbuffer", e);
        }

        TextureImpl.RenderTargetImpl color = getColorBuffer(0);
        TextureImpl.RenderTargetImpl depth = getDepthBuffer();

        if (color != null) {
            TextureImpl.TextureHandle handle = color.getSampler().getHandle();
            context.bindTexture(0, handle);
            copySubImage(handle, color.image);
        }
        if (depth != null) {
            TextureImpl.TextureHandle handle = depth.getSampler().getHandle();
            context.bindTexture(0, handle);
            copySubImage(handle, depth.image);
        }
    }

    /*
     * Copy the buffer into the given TextureHandle. It assumes the texture was
     * already bound.
     */
    private void copySubImage(TextureImpl.TextureHandle handle, int activeLayerForFrame) {
        int glTarget = Utils.getGLTextureTarget(handle.target);
        switch (glTarget) {
        case GL11.GL_TEXTURE_1D:
            GL11.glCopyTexSubImage1D(glTarget, 0, 0, 0, 0, getWidth());
            break;
        case GL11.GL_TEXTURE_2D:
            GL11.glCopyTexSubImage2D(glTarget, 0, 0, 0, 0, 0, getWidth(), getHeight());
            break;
        case GL13.GL_TEXTURE_CUBE_MAP:
            int face = Utils.getGLCubeFace(activeLayerForFrame);
            GL11.glCopyTexSubImage2D(face, 0, 0, 0, 0, 0, getWidth(), getHeight());
            break;
        case GL12.GL_TEXTURE_3D:
            GL12.glCopyTexSubImage3D(glTarget, 0, 0, 0, activeLayerForFrame, 0, 0, getWidth(), getHeight());
            break;
        case GL30.GL_TEXTURE_1D_ARRAY:
            GL11.glCopyTexSubImage2D(glTarget, 0, 0, activeLayerForFrame, 0, 0, getWidth(), 1);
            break;
        case GL30.GL_TEXTURE_2D_ARRAY:
            GL12.glCopyTexSubImage3D(glTarget, 0, 0, 0, activeLayerForFrame, 0, 0, getWidth(), getHeight());
            break;
        }
    }

    private static class PbufferDestructible extends SurfaceDestructible {
        private final Pbuffer pbuffer;
        private final LwjglContext context;

        public PbufferDestructible(FrameworkImpl framework, Pbuffer pbuffer, LwjglContext context) {
            super(framework);
            this.pbuffer = pbuffer;
            this.context = context;
        }

        @Override
        public OpenGLContext getContext() {
            return context;
        }

        @Override
        protected void destroyImpl() {
            context.destroy();
            pbuffer.destroy();
        }
    }
}
