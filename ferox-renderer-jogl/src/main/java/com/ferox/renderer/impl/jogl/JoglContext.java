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

import com.ferox.renderer.Capabilities;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.ShaderFixedFunctionEmulator;
import com.ferox.renderer.impl.SharedState;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.ShaderImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * JoglContext is an implementation of OpenGLContext that uses the JOGL OpenGL binding.
 *
 * @author Michael Ludwig
 */
public class JoglContext implements OpenGLContext {
    private final GLContext context;

    private final List<Runnable> cleanupTasks;

    private final SharedState sharedState;
    private final FixedFunctionRenderer fixed;
    private final JoglGlslRenderer glsl;

    private int fbo;
    private int vao;

    /**
     * Create a JoglContext wrapper around the given GLContext. It is assumed the provided Capabilities
     * accurately reflect the hardware.
     *
     * @param caps    The capabilities of the context
     * @param context The actual Drawable
     *
     * @throws NullPointerException if factory, context, or provider are null
     */
    public JoglContext(Capabilities caps, GLContext context) {
        if (caps == null || context == null) {
            throw new NullPointerException("Capabilities and context cannot be null");
        }

        this.context = context;

        cleanupTasks = new CopyOnWriteArrayList<>();

        sharedState = new SharedState(caps.getMaxTextureUnits());

        JoglRendererDelegate shared = new JoglRendererDelegate(this, sharedState);
        glsl = new JoglGlslRenderer(this, shared, caps.getMaxVertexAttributes());
        if (caps.getMajorVersion() < 3) {
            vao = 0; // do not use vao's at all
            fixed = new JoglFixedFunctionRenderer(this, shared);
        } else {
            vao = -1; // specify that vao needs to be created first time this context is made current
            fixed = new ShaderFixedFunctionEmulator(glsl);
        }
    }

    /**
     * <p/>
     * Queue the given task to be run the next time this context is bound. Queued tasks can be invoked in any
     * order so they should be independent. These tasks are intended for cleanup of additional resources on
     * the context that don't extend {@link com.ferox.renderer.Resource}.
     * <p/>
     * Tasks may not be executed if the context is destroyed before it is made current after the task has been
     * queued. This behavior should be acceptable for tasks whose sole purpose is to cleanup resources tied to
     * a context (which should be automatically destroyed when hardware context is destroyed).
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
     * Set the active texture. This should be called before any texture operations are needed, since it
     * switches which texture unit is active.
     *
     * @param tex The texture unit, 0 based
     */
    public void setActiveTexture(int tex) {
        // This is safe even with no multitexture support because 0 is the default value, and no
        // other texture unit should be specified by higher-levels of code if there's no support
        if (tex != sharedState.activeTexture) {
            sharedState.activeTexture = tex;
            context.getGL().glActiveTexture(GL.GL_TEXTURE0 + tex);
        }
    }

    /**
     * Bind the given framebuffer object.
     *
     * @param fboId The id of the fbo
     */
    public void bindFbo(int fboId) {
        if (fbo != fboId) {
            fbo = fboId;

            context.getGL().getGL2GL3().glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId);
        }
    }

    /**
     * @return The GLContext wrapped by this JoglContext
     */
    public GLContext getGLContext() {
        return context;
    }

    @Override
    public void destroy() {
        context.destroy();
    }

    @Override
    public void makeCurrent() {
        try {
            context.makeCurrent();
            if (vao < 0) {
                // make a vao for attributes
                int[] id = new int[1];
                context.getGL().getGL3().glGenVertexArrays(1, id, 0);
                vao = id[0];
                context.getGL().getGL3().glBindVertexArray(vao);
            }
        } catch (GLException e) {
            throw new FrameworkException("Unable to make context current", e);
        }

        for (Runnable task : cleanupTasks) {
            task.run();
        }
    }

    @Override
    public void release() {
        try {
            context.release();
        } catch (GLException e) {
            throw new FrameworkException("Unable to release context", e);
        }
    }

    @Override
    public String checkGLErrors() {
        int error = context.getGL().glGetError();
        return Utils.getGLErrorString(error);
    }

    @Override
    public FixedFunctionRenderer getFixedFunctionRenderer() {
        return fixed;
    }

    @Override
    public GlslRenderer getGlslRenderer() {
        return glsl;
    }

    @Override
    public void bindArrayVBO(BufferImpl.BufferHandle vbo) {
        if (vbo != null && vbo.isDestroyed()) {
            vbo = null;
        }

        if (vbo != sharedState.arrayVBO) {
            sharedState.arrayVBO = vbo;
            int bufferID = (vbo == null || vbo.inmemoryBuffer != null ? 0 : vbo.vboID);

            context.getGL().glBindBuffer(GL.GL_ARRAY_BUFFER, bufferID);
        }
    }

    @Override
    public void bindElementVBO(BufferImpl.BufferHandle vbo) {
        if (vbo != null && vbo.isDestroyed()) {
            vbo = null;
        }

        if (vbo != sharedState.elementVBO) {
            sharedState.elementVBO = vbo;
            int bufferID = (vbo == null || vbo.inmemoryBuffer != null ? 0 : vbo.vboID);

            context.getGL().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, bufferID);
        }
    }

    @Override
    public void bindShader(ShaderImpl.ShaderHandle shader) {
        if (shader != null && shader.isDestroyed()) {
            shader = null;
        }

        if (shader != sharedState.shader) {
            sharedState.shader = shader;
            int shaderID = (shader == null ? 0 : shader.programID);
            context.getGL().getGL2GL3().glUseProgram(shaderID);
        }
    }

    @Override
    public void bindTexture(int textureUnit, TextureImpl.TextureHandle texture) {
        if (texture != null && texture.isDestroyed()) {
            texture = null;
        }

        TextureImpl.TextureHandle prevTex = sharedState.textures[textureUnit];

        if (texture != prevTex) {
            setActiveTexture(textureUnit);

            TextureImpl.Target newTarget = null;
            int textureID = 0;
            if (texture != null) {
                newTarget = texture.target;
                textureID = texture.texID;
            }

            if (prevTex != null && prevTex.target != newTarget) {
                // unbind old texture
                context.getGL().glBindTexture(Utils.getGLTextureTarget(prevTex.target), 0);
            }
            if (newTarget != null) {
                context.getGL().glBindTexture(Utils.getGLTextureTarget(newTarget), textureID);
            }

            sharedState.textures[textureUnit] = texture;
        }
    }

    @Override
    public SharedState getState() {
        return sharedState;
    }
}
