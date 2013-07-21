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

/**
 * <p/>
 * The GlslRenderer describes a Renderer that exposes the programmable pipeline of the GPU that uses GLSL
 * shaders. This API is compatible with the shader versions before OpenGL 3 and attempts to be future friendly
 * for the most important aspects of OpenGL 3+.
 *
 * @author Michael Ludwig
 */
public interface GlslRenderer extends Renderer {
    /**
     * <p/>
     * Get the current state configuration for this GlslRenderer. The returned instance can be used in {@link
     * #setCurrentState(ContextState)} with any GlslRenderer created by the same Framework as this renderer.
     * The returned snapshot must preserve all of the current uniform and attribute values or bindings.
     * <p/>
     * Because the shader pipeline maintains a large amount of state, getting and setting the entire state
     * should be used infrequently.
     *
     * @return The current state
     */
    public ContextState<GlslRenderer> getCurrentState();

    /**
     * <p/>
     * Set the current state of this renderer to equal the given state snapshot. <var>state</var> must have
     * been returned by a prior call to {@link #getCurrentState()} from a GlslRenderer created by this
     * renderer's Framework or behavior is undefined.
     * <p/>
     * Because the shader pipeline maintains a large amount of state, getting and setting the entire state
     * should be used infrequently.
     *
     * @param state The state snapshot to update this renderer
     *
     * @throws NullPointerException if state is null
     */
    public void setCurrentState(ContextState<GlslRenderer> state);

    /**
     * Set the shader that will process and affect all future calls to {@link
     * #render(com.ferox.renderer.Renderer.PolygonType, int, int)} until a new shader is assigned.
     * <p/>
     * Each shader has its own set of attributes and uniforms, so after activating the shader it is important
     * to bind all attributes and assign uniform values. Binding a new shader unbinds all vertex arrays that
     * were previously attached to the old attributes.
     * <p/>
     * Shaders remember the last state of their uniforms but not their attributes. Thus, setting uniform
     * values and then binding another shader, and then restoring the original shader will preserve the
     * uniform state of each shader.
     *
     * @param shader The shader to activate
     *
     * @throws IllegalArgumentException if shader was not created by the same framework
     */
    public void setShader(Shader shader);

    /**
     * Bind the given attribute so that it reads values from the VertexAttribute {@code attr}. The values read
     * will be iterated over directly or using the indices assigned via {@link #setIndices(ElementBuffer)}.
     * For multi-column attributes, this binds to the first column of the attribute.
     * <p/>
     * A null {@code attr} value unbinds any previous vertex array from the variable.
     *
     * @param var  The vertex attribute variable
     * @param attr The attribute that specifies data for the variable
     *
     * @throws IllegalArgumentException if var is not defined by the current shader, or if the element size of
     *                                  attr is different than the row count of the variable's type
     * @throws NullPointerException     if var is null
     */
    public void bindAttribute(Shader.Attribute var, VertexAttribute attr);

    /**
     * Bind the given attribute, just like {@link #bindAttribute(com.ferox.renderer.Shader.Attribute,
     * VertexAttribute)}, except that it allows overriding of the column. This should only be used with
     * multi-column attributes such as the MAT2, MAT3, and MAT4 types. The 0th column is supported for any
     * attribute type.
     * <p/>
     * If the element size of the VertexAttribute differs from the component count of the attribute type,
     * extra components will be ignored and any component not specified will receive a default value (such as
     * z = 0, or w = 1).
     *
     * @param var    The vertex attribute variable
     * @param column The specific column of the attribute to assign values to
     * @param attr   The attribute that provides the data for the variable
     *
     * @throws IllegalArgumentException  if var is not defined by the current shader, or if the element size
     *                                   of attr is different than the row count of the variable type
     * @throws NullPointerException      if var is null
     * @throws IndexOutOfBoundsException if column is less than 0 or greater than or equal to the column count
     *                                   of the variable type
     */
    public void bindAttribute(Shader.Attribute var, int column, VertexAttribute attr);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be FLOAT.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param val The value to assign
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not
     *                                  FLOAT
     */
    public void bindAttribute(Shader.Attribute var, double val);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be VEC2.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param v1  The first component of the vec2
     * @param v2  The second component of the vec2
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not
     *                                  VEC2
     */
    public void bindAttribute(Shader.Attribute var, double v1, double v2);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be VEC3.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param v   The value to assign
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not
     *                                  VEC3
     */
    public void bindAttribute(Shader.Attribute var, @Const Vector3 v);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be VEC4 or MAT2. If the type is MAT2, the x and y values hold the first row and the z and w
     * values hold the second row of the matrix.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param v   The value to assign
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not
     *                                  VEC4 or MAT2
     */
    public void bindAttribute(Shader.Attribute var, @Const Vector4 v);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be MAT2 or VEC4. If the type is MAT2, the arguments are listed in row major order. If the
     * type is VEC4, the arguments are X, Y, Z, and W components respectively.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param m00 The value for the first row and column, or the X coordinate
     * @param m01 The value for the first row, second column, or the Y coordinate
     * @param m10 The value for the second row, first column or the Z coordinate
     * @param m11 The value for the second row, second column or the W coordinate
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not
     *                                  MAT2 or VEC4
     */
    public void bindAttribute(Shader.Attribute var, double m00, double m01, double m10, double m11);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be MAT3.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param v   The value to assign
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not
     *                                  FLOAT
     */
    public void bindAttribute(Shader.Attribute var, @Const Matrix3 v);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be MAT4.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param v   The value to assign
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not
     *                                  MAT4
     */
    public void bindAttribute(Shader.Attribute var, @Const Matrix4 v);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be INT or UINT. Depending on the actual type the integer will either be interpreted as a
     * signed or unsigned value.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param val The value to assign
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not INT
     *                                  or UINT
     */
    public void bindAttribute(Shader.Attribute var, int val);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be IVEC2 or UVEC2. Depending on the actual type the integer will either be interpreted as a
     * signed or unsigned value.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param v1  The x component of the vector
     * @param v2  The y component of the vector
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not
     *                                  IVEC2 or UVEC2
     */
    public void bindAttribute(Shader.Attribute var, int v1, int v2);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be IVEC3 or UVEC3. Depending on the actual type the integer will either be interpreted as a
     * signed or unsigned value.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param v1  The x component of the vector
     * @param v2  The y component of the vector
     * @param v3  The z component of the vector
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not
     *                                  IVEC3 or UVEC3
     */
    public void bindAttribute(Shader.Attribute var, int v1, int v2, int v3);

    /**
     * Bind the attribute to the constant value. The same value will be used for every vertex. This can be
     * useful for simple but rapid state change, and will be faster than updating a uniform. The attribute's
     * type must be IVEC4 or UVE42. Depending on the actual type the integer will either be interpreted as a
     * signed or unsigned value.
     * <p/>
     * This automatically unbind any vertex array that had previously been bound to the attribute.
     *
     * @param var The attribute variable
     * @param v1  The x component of the vector
     * @param v2  The y component of the vector
     * @param v3  The z component of the vector
     * @param v4  The w component of the vector
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not defined in the current shader, or if its type is not
     *                                  IVEC2 or UVEC2
     */
    public void bindAttribute(Shader.Attribute var, int v1, int v2, int v3, int v4);

    /**
     * Set the value of the given uniform to {@code val}. Once assigned the state of a shader's uniform value
     * will not change until it is modified by another call to setUniform(). Assigning a new shader replaces
     * the available uniforms but it does not destroy the previously assigned values and they will be restored
     * when the old shader is reactivated.
     * <p/>
     * This version of setUniform() requires the variable type to be FLOAT.
     *
     * @param var The uniform variable
     * @param val The new value for the uniform
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not FLOAT
     */
    public void setUniform(Shader.Uniform var, double val);

    /**
     * Set the uniform value when the uniform's type is VEC2.
     *
     * @param var The uniform variable
     * @param v1  The first component of the vector
     * @param v2  The second component of the vector
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not VEC2
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, double v1, double v2);

    /**
     * Set the uniform value when the uniform's type is VEC3.
     *
     * @param var The uniform variable
     * @param v   The vector
     *
     * @throws NullPointerException     if var or v is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not VEC3
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, @Const Vector3 v);

    /**
     * Set the uniform value when the uniform's type is VEC4 or MAT2. The layout of the vector when
     * interpreted as a 2x2 matrix is identical to {@link #bindAttribute(com.ferox.renderer.Shader.Attribute,
     * com.ferox.math.Vector4)}.
     *
     * @param var The uniform variable
     * @param v   The vector
     *
     * @throws NullPointerException     if var or v is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not VEC4 or
     *                                  MAT2
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, @Const Vector4 v);

    /**
     * Set the uniform value when the uniform's type is MAT2 or VEC4. When the type is MAT2, the arguments are
     * in row major order. When it is VEC4, m00 is the x, m01 is the y, m10 is the z, and m11 is the w.
     *
     * @param var The uniform variable
     * @param m00 The first row, first column, or x coordinate
     * @param m01 The first row, second column, or y coordinate
     * @param m10 The second row, first column, or z coordinate
     * @param m11 The second row, second column, or the w coordinate
     *
     * @throws NullPointerException     if var is null
     * @throws IllegalArgumentException if var is from another shader, or if its type is not MAT2 or VEC4
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     * @see #bindAttribute(com.ferox.renderer.Shader.Attribute, double, double, double, double)
     */
    public void setUniform(Shader.Uniform var, double m00, double m01, double m10, double m11);

    /**
     * Set the uniform value when the uniform's type is MAT3.
     *
     * @param var The uniform variable
     * @param val The matrix
     *
     * @throws NullPointerException     if var  or val is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not MAT3
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, @Const Matrix3 val);

    /**
     * Set the uniform value when the uniform's type is MAT4.
     *
     * @param var The uniform variable
     * @param val The matrix
     *
     * @throws NullPointerException     if var  or val is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not MAT4
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, @Const Matrix4 val);

    /**
     * Set the uniform value when the uniform's type is INT or UINT. Depending on the type, the value is
     * interpreted as a signed or unsigned integer by OpenGL.
     *
     * @param var The uniform variable
     * @param val The integer value
     *
     * @throws NullPointerException     if var  or val is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not INT or
     *                                  UINT
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, int val);

    /**
     * Set the uniform value when the uniform's type is IVEC2 or UVEC2. Depending on the type, the value is
     * interpreted as a signed or unsigned integer by OpenGL.
     *
     * @param var The uniform variable
     * @param v1  The first coordinate
     * @param v2  The second coordinate
     *
     * @throws NullPointerException     if var  or val is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not IVEC2 or
     *                                  UVEC2
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, int v1, int v2);

    /**
     * Set the uniform value when the uniform's type is IVEC3 or UVEC3. Depending on the type, the value is
     * interpreted as a signed or unsigned integer by OpenGL.
     *
     * @param var The uniform variable
     * @param v1  The first coordinate
     * @param v2  The second coordinate
     * @param v3  The third coordinate
     *
     * @throws NullPointerException     if var  or val is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not IVEC3 or
     *                                  UVEC3
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, int v1, int v2, int v3);

    /**
     * Set the uniform value when the uniform's type is IVEC4 or UVEC4. Depending on the type, the value is
     * interpreted as a signed or unsigned integer by OpenGL.
     *
     * @param var The uniform variable
     * @param v1  The first coordinate
     * @param v2  The second coordinate
     * @param v3  The third coordinate
     * @param v4  The fourth coordinate
     *
     * @throws NullPointerException     if var  or val is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not IVEC4 or
     *                                  UVEC4
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, int v1, int v2, int v3, int v4);

    /**
     * Set the uniform value when the uniform's type is BOOL.
     *
     * @param var The uniform variable
     * @param val The boolean value
     *
     * @throws NullPointerException     if var  or val is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not BOOL
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, boolean val);

    /**
     * Set the uniform value when the uniform's type is BVEC2.
     *
     * @param var The uniform variable
     * @param v1  The first coordinate
     * @param v2  The second coordinate
     *
     * @throws NullPointerException     if var  or val is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not BVEC2
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, boolean v1, boolean v2);

    /**
     * Set the uniform value when the uniform's type is BVEC3.
     *
     * @param var The uniform variable
     * @param v1  The first coordinate
     * @param v2  The second coordinate
     * @param v3  The third coordinate
     *
     * @throws NullPointerException     if var  or val is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not BVEC3
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, boolean v1, boolean v2, boolean v3);

    /**
     * Set the uniform value when the uniform's type is BVEC4.
     *
     * @param var The uniform variable
     * @param v1  The first coordinate
     * @param v2  The second coordinate
     * @param v3  The third coordinate
     * @param v4  The fourth coordinate
     *
     * @throws NullPointerException     if var  or val is null
     * @throws IllegalArgumentException if var is not from the current shader, or if its type is not BVEC4
     * @see #setUniform(com.ferox.renderer.Shader.Uniform, double)
     */
    public void setUniform(Shader.Uniform var, boolean v1, boolean v2, boolean v3, boolean v4);

    /**
     * Set the uniform to access the given sampler. This can only be used with uniforms that are one of the
     * sampler varieties, such as SAMPLER_2D or ISAMPLER_CUBE. The particular uniform type must be consistent
     * with the Sampler subclass as well as its data type.  The signed and unsigned sampler variables can only
     * be used in conjunction with samplers that were created with signed and unsigned integer data (that
     * wasn't normalized).
     * <p/>
     * Similarly, the shadow sampler types can only be used with depth maps that have the depth comparison
     * enabled. Otherwise one of the basic samplers should be used.
     * <p/>
     * If the sampler is null, the variable's binding is reset to have no image attached and will report
     * default values within the shader execution.
     *
     * @param var     The uniform variable
     * @param texture The sampler to bind to
     *
     * @throws IllegalArgumentException if var is not from the current shader, or if it is not consistent with
     *                                  the particular sampler
     * @throws NullPointerException     if var is null
     */
    public void setUniform(Shader.Uniform var, Sampler texture);

    /**
     * Set the uniform value, just like {@link #setUniform(com.ferox.renderer.Shader.Uniform,
     * com.ferox.math.Vector3)} except the x, y, and z values are taken from the clamped red, green, and blue
     * values of the color. The w value will be set to 1 if the uniform type is VEC4.
     *
     * @param var   The uniform variable
     * @param color The color to assign values from
     *
     * @throws IllegalArgumentException if var is from another shader, or if its type is not VEC3 or VEC4
     * @throws NullPointerException     if var is null
     */
    public void setUniform(Shader.Uniform var, @Const ColorRGB color);

    /**
     * Set the uniform value, just like {@link #setUniform(com.ferox.renderer.Shader.Uniform,
     * com.ferox.math.Vector3)} except the x, y, and z values are taken from the unclamped/HDR red, green, and
     * blue values of the color. The w value will be set to 1 if the uniform type is VEC4.
     *
     * @param var   The uniform variable
     * @param color The color to assign values from
     *
     * @throws IllegalArgumentException if var is from another shader, or if its type is not VEC3 or VEC4
     * @throws NullPointerException     if var is null
     */
    public void setUniform(Shader.Uniform var, @Const ColorRGB color, boolean isHDR);
}
