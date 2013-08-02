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

import com.ferox.renderer.Capabilities;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.SharedState;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.ShaderImpl;
import com.ferox.renderer.impl.resources.TextureImpl;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * LwjglContext is an implementation of OpenGLContext that uses the LWJGL OpenGL binding.
 *
 * @author Michael Ludwig
 */
public class LwjglContext implements OpenGLContext {
    private final Drawable context;

    private final List<Runnable> cleanupTasks;

    private final SharedState sharedState;
    private final LwjglFixedFunctionRenderer fixed;
    private final LwjglGlslRenderer glsl;

    private int fbo;

    // cached switches for extensions
    private final boolean useEXTFramebufferObject;

    /**
     * Create a LWJGLContext wrapper around the given Drawable. It is assumed the provided Capabilities
     * accurately reflect the hardware.
     *
     * @param caps    The capabilities of the context
     * @param context The actual Drawable
     *
     * @throws NullPointerException if factory, context, or provider are null
     */
    public LwjglContext(Capabilities caps, Drawable context) {
        if (caps == null || context == null) {
            throw new NullPointerException("Capabilities and context cannot be null");
        }

        this.context = context;

        cleanupTasks = new CopyOnWriteArrayList<>();

        // if functions related to this are called, we assume FBOs are supported somehow
        useEXTFramebufferObject = caps.getMajorVersion() < 3;

        sharedState = new SharedState(caps.getMaxFragmentShaderTextures());

        LwjglRendererDelegate shared = new LwjglRendererDelegate(this, sharedState);
        if (caps.getMajorVersion() < 3) {
            fixed = new LwjglFixedFunctionRenderer(this, shared);
        } else {
            throw new UnsupportedOperationException(
                    "No emulation shader written yet, can't support FFP renderer");
        }
        glsl = new LwjglGlslRenderer(this, shared, caps.getMaxVertexAttributes());
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
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + tex);
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

            if (useEXTFramebufferObject) {
                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fboId);
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
        int error;
        try {
            error = GL11.glGetError();
            context.releaseContext();
        } catch (LWJGLException e) {
            throw new FrameworkException("Unable to release context", e);
        }

        if (error != 0) {
            throw new FrameworkException("OpenGL error flagged, checked on context release: " +
                                         Util.translateGLErrorString(error));
        }
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
        if (vbo.isDestroyed()) {
            vbo = null;
        }

        if (vbo != sharedState.arrayVBO) {
            sharedState.arrayVBO = vbo;
            int bufferID = (vbo == null || vbo.inmemoryBuffer != null ? 0 : vbo.vboID);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferID);
        }
    }

    @Override
    public void bindElementVBO(BufferImpl.BufferHandle vbo) {
        if (vbo.isDestroyed()) {
            vbo = null;
        }

        if (vbo != sharedState.elementVBO) {
            sharedState.elementVBO = vbo;
            int bufferID = (vbo == null || vbo.inmemoryBuffer != null ? 0 : vbo.vboID);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, bufferID);
        }
    }

    @Override
    public void bindShader(ShaderImpl.ShaderHandle shader) {
        if (shader.isDestroyed()) {
            shader = null;
        }

        if (shader != sharedState.shader) {
            sharedState.shader = shader;
            int shaderID = (shader == null ? 0 : shader.programID);
            GL20.glUseProgram(shaderID);
        }
    }

    @Override
    public void bindTexture(int textureUnit, TextureImpl.TextureHandle texture) {
        if (texture.isDestroyed()) {
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
                GL11.glBindTexture(Utils.getGLTextureTarget(prevTex.target), 0);
            }
            if (newTarget != null) {
                GL11.glBindTexture(Utils.getGLTextureTarget(newTarget), textureID);
            }

            sharedState.textures[textureUnit] = texture;
        }
    }

    @Override
    public SharedState getState() {
        return sharedState;
    }
}
