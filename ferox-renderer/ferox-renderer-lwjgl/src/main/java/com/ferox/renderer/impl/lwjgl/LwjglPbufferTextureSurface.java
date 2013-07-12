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
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.AbstractTextureSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererProvider;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.renderer.texture.Texture;
import com.ferox.renderer.texture.TextureFormat;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;

/**
 * LwjglPbufferTextureSurface is a TextureSurface implementation that relies on pbuffers
 * to render into an offscreen buffer before using the glCopyTexImage command to move the
 * buffer contents into a texture. PBuffers are slower than FBOs so {@link
 * LwjglFboTextureSurface} is favored when FBOs are supported.
 *
 * @author Michael Ludwig
 */
public class LwjglPbufferTextureSurface extends AbstractTextureSurface {
    private final Pbuffer pbuffer;
    private final LwjglContext context;

    private int activeLayerForFrame;

    public LwjglPbufferTextureSurface(FrameworkImpl framework,
                                      LwjglSurfaceFactory creator,
                                      TextureSurfaceOptions options,
                                      LwjglContext shareWith, RendererProvider provider) {
        super(framework, options);

        Texture[] colorBuffers = new Texture[getNumColorBuffers()];
        for (int i = 0; i < colorBuffers.length; i++) {
            colorBuffers[i] = getColorBuffer(i);
        }

        PixelFormat format = choosePixelFormat(colorBuffers, getDepthBuffer());
        Drawable realShare = (shareWith == null ? null : shareWith.getDrawable());
        try {
            pbuffer = new Pbuffer(getWidth(), getHeight(), format, realShare);
        } catch (LWJGLException e) {
            throw new SurfaceCreationException("Unable to create Pbuffer", e);
        }
        context = new LwjglContext(creator, pbuffer, provider);

        activeLayerForFrame = 0;
    }

    @Override
    public OpenGLContext getContext() {
        return context;
    }

    @Override
    public void flush(OpenGLContext context) {
        try {
            pbuffer.swapBuffers();
        } catch (LWJGLException e) {
            throw new FrameworkException("Error flushing Pbuffer", e);
        }

        Texture color = getNumColorBuffers() > 0 ? getColorBuffer(0)
                                                 : null; // will be 1 color target at max
        Texture depth = getDepthBuffer();

        int ct = -1;
        int dt = -1;

        if (color != null) {
            TextureHandle handle = (TextureHandle) getColorHandle(0);
            ct = Utils.getGLTextureTarget(handle.target);

            GL11.glBindTexture(ct, handle.texID);
            copySubImage(handle);
        }
        if (depth != null) {
            TextureHandle handle = (TextureHandle) getDepthHandle();
            dt = Utils.getGLTextureTarget(handle.target);

            GL11.glBindTexture(dt, handle.texID);
            copySubImage(handle);
        }

        restoreBindings(ct, dt);
    }

    @Override
    public void onSurfaceActivate(OpenGLContext context, int activeLayer) {
        super.onSurfaceActivate(context, activeLayer);
        activeLayerForFrame = activeLayer;
    }

    @Override
    protected void destroyImpl() {
        context.destroy();
        pbuffer.destroy();
    }

    /*
     * Copy the buffer into the given TextureHandle. It assumes the texture was
     * already bound.
     */
    private void copySubImage(TextureHandle handle) {
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
            GL12.glCopyTexSubImage3D(glTarget, 0, 0, 0, activeLayerForFrame, 0, 0,
                                     getWidth(), getHeight());
            break;
        }
    }

    private void restoreBindings(int colorTarget, int depthTarget) {
        int target = context.getTextureTarget(context.getActiveTexture());
        int tex = context.getTexture(context.getActiveTexture());

        if (colorTarget > 0) {
            if (target == colorTarget) {
                // restore enabled texture
                GL11.glBindTexture(colorTarget, tex);
            } else {
                GL11.glBindTexture(colorTarget, 0); // not really the active unit
            }
        }
        if (depthTarget > 0 && colorTarget != depthTarget) {
            if (target == depthTarget) {
                // restore enabled texture
                GL11.glBindTexture(depthTarget, tex);
            } else {
                GL11.glBindTexture(depthTarget, 0); // not really the active unit
            }
        }
    }

    private static PixelFormat choosePixelFormat(Texture[] colors, Texture depth) {
        PixelFormat pf = new PixelFormat();

        if (colors == null || colors.length == 0) {
            pf = pf.withBitsPerPixel(0);
        } else {
            TextureFormat format = colors[0].getFormat();
            if (format == TextureFormat.R_FLOAT || format == TextureFormat.RG_FLOAT ||
                format == TextureFormat.RGB_FLOAT || format == TextureFormat.RGBA_FLOAT) {
                pf = pf.withFloatingPoint(true);
            }

            switch (format) {
            // 8, 8, 8, 0
            case R:
            case RGB:
            case BGR:
                pf = pf.withBitsPerPixel(24);
                break;
            // 5, 6, 5, 0
            case BGR_565:
            case RGB_565:
                pf = pf.withBitsPerPixel(16);
                break;
            // 5, 6, 5, 8 - not sure how supported this is
            case BGRA_4444:
            case ABGR_4444:
            case RGBA_4444:
            case ARGB_4444:
                pf = pf.withBitsPerPixel(12).withAlphaBits(4);
                break;
            case BGRA_5551:
            case ARGB_1555:
            case RGBA_5551:
            case ABGR_1555:
                pf = pf.withBitsPerPixel(15).withAlphaBits(1);
                break;
            case RG_FLOAT:
            case R_FLOAT:
            case RGBA_FLOAT:
                pf = pf.withBitsPerPixel(32).withAlphaBits(32);
                break;
            // 32, 32, 32, 0
            case RGB_FLOAT:
                pf = pf.withBitsPerPixel(24);
                break;
            // 8, 8, 8, 8
            case RG:
            case BGRA:
            case BGRA_8888:
            case RGBA_8888:
            case RGBA:
            case ARGB_8888:
            case ABGR_8888:
            default:
                pf = pf.withBitsPerPixel(24).withAlphaBits(8);
                break;
            }
        }

        if (depth != null) {
            //            if (depth.getDataType() == DataType.UNSIGNED_BYTE) {
            //                pf = pf.withDepthBits(16);
            //            } else if (depth.getDataType() == DataType.UNSIGNED_SHORT) {
            //                pf = pf.withDepthBits(24);
            //            } else {
            //                pf = pf.withDepthBits(32);
            //            }
            // FIXME On my Mac, LWJGL seems unable to select appropriate depth
            pf = pf.withDepthBits(24);
        } else {
            pf = pf.withDepthBits(24);
        }

        pf = pf.withStencilBits(0);
        return pf;
    }
}
