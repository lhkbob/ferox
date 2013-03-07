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
package com.ferox.renderer;

import com.ferox.math.*;
import com.ferox.resource.GlslShader;
import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;

import java.util.Map;

public interface GlslRenderer extends Renderer {
    /**
     * <p/>
     * Get the current state configuration for this GlslRenderer. The returned instance
     * can be used in {@link #setCurrentState(ContextState)} with any GlslRenderer created
     * by the same Framework as this renderer. The returned snapshot must preserve all of
     * the current uniform and attribute values or bindings.
     * <p/>
     * Because the shader pipeline maintains a large amount of state, getting and setting
     * the entire state should be used infrequently.
     *
     * @return The current state
     */
    public ContextState<GlslRenderer> getCurrentState();

    /**
     * <p/>
     * Set the current state of this renderer to equal the given state snapshot.
     * <var>state</var> must have been returned by a prior call to {@link
     * #getCurrentState()} from a GlslRenderer created by this renderer's Framework or
     * behavior is undefined.
     * <p/>
     * Because the shader pipeline maintains a large amount of state, getting and setting
     * the entire state should be used infrequently.
     *
     * @param state The state snapshot to update this renderer
     *
     * @throws NullPointerException if state is null
     */
    public void setCurrentState(ContextState<GlslRenderer> state);

    // FIXME: for advanced shaders, this is the fragment variable to GL_COLOR_ATTACHMENT0+target
    //    and is configured with glBindFragDataLocation
    // for older shaders, they have to write to glFragData[target], so maybe switch
    //  order of arguments, and say null reverts to default output (e.g. glFragData())
    public void bindRenderTarget(String fragmentVariable, int target);

    public void setShader(GlslShader shader);

    public Map<String, AttributeType> getAttributes();

    public Map<String, GlslUniform> getUniforms();

    public void bindAttribute(String glslAttrName, VertexAttribute attr);

    public void bindAttribute(String glslAttrName, int column, VertexAttribute attr);

    public void bindAttribute(String glslAttrName, float val);

    public void bindAttribute(String glslAttrName, float v1, float v2);

    public void bindAttribute(String glslAttrName, @Const Vector3 v);

    public void bindAttribute(String glslAttrName, @Const Vector4 v);

    public void bindAttribute(String glslAttrName, @Const Matrix3 v);

    public void bindAttribute(String glslAttrName, @Const Matrix4 v);

    // FIXME should these be changed to doubles?
    public void setUniform(String name, float val);

    public void setUniform(String name, float v1, float v2);

    public void setUniform(String name, float v1, float v2, float v3);

    public void setUniform(String name, float v1, float v2, float v3, float v4);

    public void setUniform(String name, @Const Vector3 v);

    public void setUniform(String name, @Const Vector4 v);

    public void setUniform(String name, @Const Matrix3 val);

    public void setUniform(String name, @Const Matrix4 val);

    // FIXME should I get rid of the array versions?
    // I am inclined to say yes, especially now that state snapshots require me
    // to track everything and that's not feasible for entire arrays.
    // - Somehow I must incorporate UniformBuffers as resource types too
    public void setUniform(String name, float[] vals);

    public void setUniform(String name, int val);

    public void setUniform(String name, int v1, int v2);

    public void setUniform(String name, int v1, int v2, int v3);

    public void setUniform(String name, int v1, int v2, int v3, int v4);

    public void setUniform(String name, int[] vals);

    public void setUniform(String name, boolean val);

    public void setUniform(String name, boolean[] vals);

    public void setUniform(String name, Texture texture);

    public void setUniform(String name, @Const ColorRGB color);

    public void setUniform(String name, @Const ColorRGB color, boolean isHDR);
}
