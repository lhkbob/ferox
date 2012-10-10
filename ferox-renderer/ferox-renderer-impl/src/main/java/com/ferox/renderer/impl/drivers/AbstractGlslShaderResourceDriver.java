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
package com.ferox.renderer.impl.drivers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.UpdateResourceException;
import com.ferox.resource.GlslShader;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.Resource;

/**
 * Abstract implementation of a ResourceDriver for GlslShaders. This implements
 * all necessary logic to detect changes to the GlslShaders and provides
 * abstract methods that more closely mirror the OpenGL API and implements the
 * driver methods in terms of those. This driver uses {@link GlslShaderHandle}
 * as its resource handle.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractGlslShaderResourceDriver implements ResourceDriver {
    @Override
    public Object init(Resource resource) {
        return new GlslShaderHandle();
    }

    @Override
    public String update(OpenGLContext context, Resource resource, Object handle) throws UpdateResourceException {
        GlslShader shader = (GlslShader) resource;
        GlslShaderHandle h = (GlslShaderHandle) handle;
        EnumSet<ShaderType> supported = context.getRenderCapabilities()
                                               .getSupportedShaderTypes();

        if (supported.isEmpty() || context.getRenderCapabilities().getGlslVersion() == null) {
            throw new UpdateResourceException("GLSL is not supported on current hardware");
        }

        if (h.programID <= 0) {
            // Create a new shader program id
            h.programID = glCreateProgram(context);
        }

        // Loop over all shaders and see if they've been changed
        List<String> problems = null;
        boolean needsRelink = false;

        ShaderType[] values = ShaderType.values();
        for (int i = 0; i < values.length; i++) {
            ShaderType type = values[i];

            String oldShaderSource = h.shaderSource.get(type);
            String newShaderSource = shader.getShader(type);

            if (areSourcesDifferent(oldShaderSource, newShaderSource)) {
                if (supported.contains(type)) {
                    Integer shaderId = h.shaders.get(type);
                    if (newShaderSource == null && shaderId != null) {
                        // Remove the old shader
                        glDetachShader(context, h.programID, shaderId);
                        glDeleteShader(context, shaderId);
                    }

                    if (newShaderSource != null) {
                        // Compile and attach new version
                        if (shaderId == null) {
                            shaderId = glCreateShader(context, type);
                            glAttachShader(context, h.programID, shaderId);
                            h.shaders.put(type, shaderId);
                        }

                        String errorLog = glCompileShader(context, shaderId,
                                                          newShaderSource);
                        if (errorLog != null) {
                            if (problems == null) {
                                problems = new ArrayList<String>();
                            }
                            problems.add("Error compiling " + type + " shader: " + errorLog);
                        } else {
                            // No error so update the shader source map so future updates
                            // don't do extra work
                            h.shaderSource.put(type, newShaderSource);
                        }
                    } else {
                        // No more shader for this type, clean type from the map
                        h.shaders.remove(type);
                        h.shaderSource.remove(type);
                    }

                    // Record that we've changed the program
                    needsRelink = true;
                } else {
                    if (problems == null) {
                        problems = new ArrayList<String>();
                    }
                    problems.add("Hardware does not support shader type: " + type);
                    needsRelink = true; // set to true to fall into the error handling code
                }
            }
        }

        // Update the program if we changed the shaders at all
        if (needsRelink) {
            h.attributes.clear();
            h.uniforms.clear();

            // Check for errors with compiling of the shaders
            if (problems != null) {
                throw new UpdateResourceException(problems.toString());
            }

            // Link the program
            String errorLog = glLinkProgram(context, h.programID);
            if (errorLog != null) {
                // Program linking failed
                throw new UpdateResourceException(errorLog);
            }

            // No failures, so update the uniforms and attrs
            updateUniforms(context, h);
            updateAttributes(context, h);
        }

        // Made it this far, the handle has been configured correctly
        return "";
    }

    @Override
    public void reset(Object handle) {
        if (handle instanceof GlslShaderHandle) {
            GlslShaderHandle h = (GlslShaderHandle) handle;
            h.shaderSource.clear();
        }
    }

    @Override
    public Class<GlslShader> getResourceType() {
        return GlslShader.class;
    }

    @Override
    public void dispose(OpenGLContext context, Object handle) {
        if (handle instanceof GlslShaderHandle) {
            GlslShaderHandle h = (GlslShaderHandle) handle;

            // Detach and delete all shader objects
            for (Integer shader : h.shaders.values()) {
                glDetachShader(context, shader.intValue(), h.programID);
                glDeleteShader(context, shader.intValue());
            }

            // Delete program
            glDeleteProgram(context, h.programID);
        }
    }

    private boolean areSourcesDifferent(String oldSrc, String newSrc) {
        if (oldSrc == null) {
            return newSrc != null;
        } else if (newSrc == null) {
            return oldSrc != null;
        } else {
            return !oldSrc.equals(newSrc);
        }
    }

    /**
     * Generate a new GLSL program id.
     * 
     * @param context
     * @return
     */
    protected abstract int glCreateProgram(OpenGLContext context);

    /**
     * Generate a new GLSL shader id for the given shader type
     * 
     * @param context
     * @param type
     * @return
     */
    protected abstract int glCreateShader(OpenGLContext context, ShaderType type);

    /**
     * Compile the shader of the given id, with the given source code. If there
     * were compile errors, return a non-null error message. If it was compiled
     * successfully, return null.
     * 
     * @param context
     * @param shaderId
     * @param code
     * @return
     */
    protected abstract String glCompileShader(OpenGLContext context, int shaderId,
                                              String code);

    /**
     * Delete a shader with the given shader id.
     * 
     * @param context
     * @param id
     */
    protected abstract void glDeleteShader(OpenGLContext context, int id);

    /**
     * Delete a program with the given program id.
     * 
     * @param context
     * @param id
     */
    protected abstract void glDeleteProgram(OpenGLContext context, int id);

    /**
     * Attach the given shader id to the program id.
     * 
     * @param context
     * @param programId
     * @param shaderId
     */
    protected abstract void glAttachShader(OpenGLContext context, int programId,
                                           int shaderId);

    /**
     * Detach the given shader id from the program.
     * 
     * @param context
     * @param programId
     * @param shaderId
     */
    protected abstract void glDetachShader(OpenGLContext context, int programId,
                                           int shaderId);

    /**
     * Finish linking the given program. This is called after all attached
     * shaders have been compiled.
     * 
     * @param context
     * @param programId
     * @return
     */
    protected abstract String glLinkProgram(OpenGLContext context, int programId);

    /**
     * Query OpenGL to update the attribute map within the given handle. The map
     * will already have been cleared. This is only called if the program has
     * been successfully linked.
     * 
     * @param context
     * @param handle
     */
    protected abstract void updateAttributes(OpenGLContext context,
                                             GlslShaderHandle handle);

    /**
     * Query OpenGL to update the uniform map within the given handle. The map
     * will already have been cleared. This is only called if the program has
     * been successfully linked.
     * 
     * @param context
     * @param handle
     */
    protected abstract void updateUniforms(OpenGLContext context, GlslShaderHandle handle);
}
