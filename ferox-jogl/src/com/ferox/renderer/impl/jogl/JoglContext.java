package com.ferox.renderer.impl.jogl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLContext;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererProvider;
import com.ferox.resource.Resource;

/**
 * JoglContext is an implementation of OpenGLContext that uses the JOGL OpenGL
 * binding.
 * 
 * @author Michael Ludwig
 */
public class JoglContext extends OpenGLContext {
    private final JoglSurfaceFactory creator;
    private final GLContext context;
    
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

    /**
     * Create a JoglContext wrapper around the given GLContext. It is assumed
     * that the given JoglSurfaceFactory is the creator.
     * 
     * @param factory The factory creating, or indirectly creating this context
     * @param context The actual GLContext
     * @param provider The provider of renderers
     * @throws NullPointerException if factory, context, or provider are null
     */
    public JoglContext(JoglSurfaceFactory factory, GLContext context, RendererProvider provider) {
        super(provider);
        if (factory == null || context == null)
            throw new NullPointerException("Factory and context cannot be null");
        
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
            stateInitialized = true;
        }
    }

    /**
     * <p>
     * Queue the given task to be run the next time this context is bound.
     * Queued tasks can be invoked in any order so they should be independent.
     * These tasks are intended for cleanup of additional resources on the
     * context that don't extend {@link Resource}.
     * </p>
     * <p>
     * Tasks may not be executed if the context is destroyed before it is made
     * current after the task has been queued. This behavior should be
     * acceptable for tasks whose sole purpose is to cleanup resources tied to a
     * context (which should be automatically destroyed when hardware context is
     * destroyed).
     * </p>
     * 
     * @param task The cleanup task to queue
     * @throws NullPointerException if task is null
     */
    public void queueCleanupTask(Runnable task) {
        if (task == null)
            throw new NullPointerException("Task cannot be null");
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
     * @return The id of the currently bound texture image
     */
    public int getTexture(int tex) {
        return textures[tex];
    }

    /**
     * @param tex The 0-based texture unit to lookup
     * @return The OpenGL texture target enum for the bound texture
     */
    public int getTextureTarget(int tex) {
        return boundTargets[tex];
    }

    /**
     * Bind the given glsl program so that it will be in use for the next
     * rendering call.
     * 
     * @param gl The GL to use
     * @param program The program id to bind
     */
    public void bindGlslProgram(GL2GL3 gl, int program) {
        initializedMaybe();
        if (program != glslProgram) {
            glslProgram = program;
            gl.glUseProgram(program);
        }
    }

    /**
     * Bind the given vbo to the ARRAY_BUFFER target.
     * 
     * @param gl The GL to use
     * @param vbo The VBO id to bind
     */
    public void bindArrayVbo(GL2GL3 gl, int vbo) {
        initializedMaybe();
        if (vbo != arrayVbo) {
            arrayVbo = vbo;
            gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vbo);
        }
    }
    
    /**
     * Bind the given vbo to the ARRAY_BUFFER target.
     * 
     * @param gl The GL to use
     * @param vbo The VBO id to bind
     */
    public void bindElementVbo(GL2GL3 gl, int vbo) {
        initializedMaybe();
        if (vbo != elementVbo) {
            elementVbo = vbo;
            gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, vbo);
        }
    }

    /**
     * Set the active texture. This should be called before any texture
     * operations are needed, since it switches which texture unit is active.
     * 
     * @param gl The GL to use
     * @param tex The texture unit, 0 based
     */
    public void setActiveTexture(GL2GL3 gl, int tex) {
        initializedMaybe();
        if (activeTexture != tex) {
            activeTexture = tex;
            gl.glActiveTexture(GL2GL3.GL_TEXTURE0 + tex);
        }
    }

    /**
     * Bind a texture image to the current active texture. <tt>target</tt> must
     * be one of GL_TEXTURE_1D, GL_TEXTURE_2D, GL_TEXTURE_3D, etc.
     * 
     * @param gl The GL to use
     * @param target The valid OpenGL texture target enum for texture image
     * @param texId The id of the texture image to bind
     */
    public void bindTexture(GL2GL3 gl, int target, int texId) {
        initializedMaybe();
        int prevTarget = boundTargets[activeTexture];
        int prevTex = textures[activeTexture];
        
        if (prevTex != texId) {
            if (prevTex != 0 && prevTarget != target) {
                // unbind old texture
                gl.glBindTexture(prevTarget, 0);
            }
            gl.glBindTexture(target, texId);
            
            boundTargets[activeTexture] = target;
            textures[activeTexture] = texId;
        }
    }

    /**
     * Bind the given framebuffer object.
     * 
     * @param gl The GL to use
     * @param fboId The id of the fbo
     */
    public void bindFbo(GL2GL3 gl, int fboId) {
        initializedMaybe();
        if (fbo != fboId) {
            fbo = fboId;
            gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, fboId);
        }
    }
    
    /**
     * @return The GLContext wrapped by this JoglContext
     */
    public GLContext getGLContext() {
        return context;
    }
    
    @Override
    public RenderCapabilities getRenderCapabilities() {
        if (cachedCaps == null) {
            cachedCaps = new JoglRenderCapabilities(context.getGL(), creator.getGLProfile(), creator.getCapabilityForceBits());
        }
        
        return cachedCaps;
    }

    @Override
    public void destroy() {
        context.destroy();
    }

    @Override
    public void makeCurrent() {
        int result = context.makeCurrent();
        if (result == GLContext.CONTEXT_NOT_CURRENT)
            throw new RenderException("Unable to make context current");
        
        for (Runnable task: cleanupTasks)
            task.run();
    }

    @Override
    public void release() {
        context.release();
    }
}
