package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL2GL3;

import com.ferox.renderer.impl.jogl.JoglContext;

/**
 * BoundObjectState represents a per-context record of the OpenGL objects that
 * are bound to it. The correctness of the bound object tracking is dependent on
 * the rest of the JoglFramework using this BoundObjectState to bind objects,
 * too. These should not be create directly, but are instantiated by a
 * {@link JoglContext}.
 * 
 * @author Michael Ludwig
 */
public class BoundObjectState {
    private int activeTexture;
    
    private int[] textures;
    private int[] boundTargets;
    
    private int arrayVbo;
    private int elementVbo;
    
    private int fbo;

    /**
     * Create a new BoundObjectState that is configured to use the given number
     * of textures.
     * 
     * @param numTextures Maximum allowed texture units
     * @throws IllegalArgumentException if numTextures < 0
     */
    public BoundObjectState(int numTextures) {
        if (numTextures < 0)
            throw new IllegalArgumentException("numTextures must be at least 0, not: " + numTextures);
        
        activeTexture = 0;
        textures = new int[numTextures];
        boundTargets = new int[numTextures];
        
        arrayVbo = 0;
        elementVbo = 0;
        
        fbo = 0;
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
     * Bind the given vbo to the ARRAY_BUFFER target.
     * 
     * @param gl The GL to use
     * @param vbo The VBO id to bind
     */
    public void bindArrayVbo(GL2GL3 gl, int vbo) {
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
        if (fbo != fboId) {
            fbo = fboId;
            gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, fboId);
        }
    }
}
