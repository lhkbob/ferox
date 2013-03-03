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
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererProvider;
import com.ferox.resource.Resource;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * LwjglContext is an implementation of OpenGLContext that uses the LWJGL OpenGL binding.
 *
 * @author Michael Ludwig
 */
public class LwjglContext extends OpenGLContext {
    private final LwjglSurfaceFactory creator;
    private final Drawable context;

    private RenderCapabilities cachedCaps;

    // cleanup
    private List<Runnable> cleanupTasks;

    // bound object state
    private boolean stateInitialized;
    private int activeTexture;

    private int[] textures;
    private int[] boundTargets;

    private int arrayVbo;
    private int elementVbo;

    private int fbo;

    private int glslProgram;

    // cached switches for extensions
    private boolean useARBVertexBufferObject;
    private boolean useEXTFramebufferObject;

    /**
     * Create a LWJGLContext wrapper around the given Drawable. It is assumed that the
     * given LwjglSurfaceFactory is the creator.
     *
     * @param factory  The factory creating, or indirectly creating this context
     * @param context  The actual Drawable
     * @param provider The provider of renderers
     *
     * @throws NullPointerException if factory, context, or provider are null
     */
    public LwjglContext(LwjglSurfaceFactory factory, Drawable context,
                        RendererProvider provider) {
        super(provider);
        if (factory == null || context == null) {
            throw new NullPointerException("Factory and context cannot be null");
        }

        this.context = context;
        creator = factory;
        stateInitialized = false;
        cleanupTasks = new CopyOnWriteArrayList<Runnable>();
    }

    private void initializedMaybe() {
        if (!stateInitialized) {
            RenderCapabilities caps = getRenderCapabilities();

            int ffp = caps.getMaxFixedPipelineTextures();
            int frag = caps.getMaxFragmentShaderTextures();
            int vert = caps.getMaxVertexShaderTextures();

            int maxTextures = Math.max(ffp, Math.max(frag, vert));
            textures = new int[maxTextures];
            boundTargets = new int[maxTextures];

            useARBVertexBufferObject = caps.getVersion() < 1.5f;
            useEXTFramebufferObject = caps.getVersion() < 3.0f;

            stateInitialized = true;
        }
    }

    /**
     * <p/>
     * Queue the given task to be run the next time this context is bound. Queued tasks
     * can be invoked in any order so they should be independent. These tasks are intended
     * for cleanup of additional resources on the context that don't extend {@link
     * Resource}.
     * <p/>
     * Tasks may not be executed if the context is destroyed before it is made current
     * after the task has been queued. This behavior should be acceptable for tasks whose
     * sole purpose is to cleanup resources tied to a context (which should be
     * automatically destroyed when hardware context is destroyed).
     *
     * @param task The cleanup task to queue
     *
     * @throws NullPointerException if task is null
     */
    public void queueCleanupTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }
        cleanupTasks.add(task);
    }

    /**
     * @return The id of the GLSL program object currently in use
     */
    public int getGlslProgram() {
        return glslProgram;
    }

    /**
     * @return The id of the VBO bound to the ARRAY_BUFFER target
     */
    public int getArrayVbo() {
        return arrayVbo;
    }

    /**
     * @return The id of the VBO bound to the ELEMENT_ARRAY_BUFFER target
     */
    public int getElementVbo() {
        return elementVbo;
    }

    /**
     * @return The active texture, index from 0
     */
    public int getActiveTexture() {
        return activeTexture;
    }

    /**
     * @return The id of the currently bound framebuffer object
     */
    public int getFbo() {
        return fbo;
    }

    /**
     * @param tex The 0-based texture unit to lookup
     *
     * @return The id of the currently bound texture image
     */
    public int getTexture(int tex) {
        initializedMaybe();
        return textures[tex];
    }

    /**
     * @param tex The 0-based texture unit to lookup
     *
     * @return The OpenGL texture target enum for the bound texture
     */
    public int getTextureTarget(int tex) {
        initializedMaybe();
        return boundTargets[tex];
    }

    /**
     * Bind the given glsl program so that it will be in use for the next rendering call.
     *
     * @param gl      The GL to use
     * @param program The program id to bind
     */
    public void bindGlslProgram(int program) {
        initializedMaybe();
        if (program != glslProgram) {
            glslProgram = program;
            GL20.glUseProgram(program);
        }
    }

    /**
     * Bind the given vbo to the ARRAY_BUFFER target.
     *
     * @param gl  The GL to use
     * @param vbo The VBO id to bind
     */
    public void bindArrayVbo(int vbo) {
        initializedMaybe();
        if (vbo != arrayVbo) {
            arrayVbo = vbo;

            if (useARBVertexBufferObject) {
                ARBBufferObject
                        .glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, vbo);
            } else {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            }
        }
    }

    /**
     * Bind the given vbo to the ARRAY_BUFFER target.
     *
     * @param gl  The GL to use
     * @param vbo The VBO id to bind
     */
    public void bindElementVbo(int vbo) {
        initializedMaybe();
        if (vbo != elementVbo) {
            elementVbo = vbo;

            if (useARBVertexBufferObject) {
                ARBBufferObject.glBindBufferARB(
                        ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, vbo);
            } else {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vbo);
            }
        }
    }

    /**
     * Set the active texture. This should be called before any texture operations are
     * needed, since it switches which texture unit is active.
     *
     * @param gl  The GL to use
     * @param tex The texture unit, 0 based
     */
    public void setActiveTexture(int tex) {
        initializedMaybe();
        if (activeTexture != tex) {
            activeTexture = tex;
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + tex);
        }
    }

    /**
     * Bind a texture image to the current active texture. <tt>target</tt> must be one of
     * GL_TEXTURE_1D, GL_TEXTURE_2D, GL_TEXTURE_3D, etc.
     *
     * @param gl     The GL to use
     * @param target The valid OpenGL texture target enum for texture image
     * @param texId  The id of the texture image to bind
     */
    public void bindTexture(int target, int texId) {
        initializedMaybe();
        int prevTarget = boundTargets[activeTexture];
        int prevTex = textures[activeTexture];

        if (prevTex != texId) {
            if (prevTex != 0 && prevTarget != target) {
                // unbind old texture
                GL11.glBindTexture(prevTarget, 0);
            }
            GL11.glBindTexture(target, texId);

            boundTargets[activeTexture] = target;
            textures[activeTexture] = texId;
        }
    }

    /**
     * Bind the given framebuffer object.
     *
     * @param gl    The GL to use
     * @param fboId The id of the fbo
     */
    public void bindFbo(int fboId) {
        initializedMaybe();
        if (fbo != fboId) {
            fbo = fboId;

            if (useEXTFramebufferObject) {
                EXTFramebufferObject
                        .glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                                              fboId);
            } else {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
            }
        }
    }

    /**
     * @return The Drawable wrapped by this LWJGLContext
     */
    public Drawable getDrawable() {
        return context;
    }

    @Override
    public RenderCapabilities getRenderCapabilities() {
        if (cachedCaps == null) {
            cachedCaps = new LwjglRenderCapabilities(creator.getCapabilityForceBits());
        }

        return cachedCaps;
    }

    @Override
    public void destroy() {
        context.destroy();
    }

    @Override
    public void makeCurrent() {
        try {
            context.makeCurrent();
        } catch (LWJGLException e) {
            throw new FrameworkException("Unable to make context current", e);
        }

        for (Runnable task : cleanupTasks) {
            task.run();
        }
    }

    @Override
    public void release() {
        try {
            context.releaseContext();
        } catch (LWJGLException e) {
            throw new FrameworkException("Unable to release context", e);
        }
    }
}
