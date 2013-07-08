package com.ferox.renderer.builder;

import com.ferox.renderer.Shader;

/**
 * ShaderBuilder is the resource builder for {@link Shader}.
 *
 * @author Michael Ludwig
 */
public interface ShaderBuilder extends Builder<Shader> {
    /**
     * Set the GLSL source code that will be compiled for the vertex stage of the
     * pipeline. The GLSL version must be consistent with the versions of any fragment or
     * geometry shader that is specified.
     *
     * @param code The vertex shader code
     *
     * @return This builder
     *
     * @throws NullPointerException if code is null
     */
    public ShaderBuilder withVertexShader(String code);

    /**
     * Set the GLSL source code that will be compiled for the fragment stage of the
     * pipeline. The GLSL version must be consistent with the versions of any vertex or
     * geometry shader that is specified.
     *
     * @param code The fragment shader code
     *
     * @return This builder
     *
     * @throws NullPointerException if code is null
     */
    public ShaderBuilder withFragmentShader(String code);

    /**
     * Set the GLSL source code that will be compiled for the geometry stage of the
     * pipeline. The GLSL version must be consistent with the vertex and fragment shaders
     * that are specified.
     * <p/>
     * A null value removes any geometry code, and represents a shader that uses the
     * default geometry pipeline.
     *
     * @param code The geometry code
     *
     * @return This builder
     */
    public ShaderBuilder withGeometryShader(String code);

    /**
     * Bind the fragment shader output variable {@code variableName} to the given color
     * buffer. The color buffer must be at least 0 and less than the maximum number of
     * supported color buffers on the current hardware. If the variable name is not
     * actually declared in the fragment shader, it will trigger a build exception later
     * on.
     * <p/>
     * The variable must be a custom out variable declared in the fragment shader. This is
     * only supported in newer GLSL versions.  If the output of a shader is determined by
     * 'gl_FragColor' or 'gl_FragData[n]' this should not be called. Reserved variable
     * names are not allowed.
     * <p/>
     * Any output variable not explicitly assigned a buffer will be assigned one when the
     * shader is compiled. They can be queried with {@link Shader#getColorBuffer(String)}.
     *
     * @param variableName The custom out variable in the fragment shader
     * @param buffer       The color buffer to bind the variable to
     *
     * @return This builder
     *
     * @throws NullPointerException if variableName is null
     */
    public ShaderBuilder bindColorBuffer(String variableName, int buffer);

    /**
     * Inform the builder of additional user-defined varying output variables in the
     * fragment shader that should have their OpenGL assigned binding queried. This is
     * necessary because OpenGL does not provide a way to query the available output names
     * (unlike uniforms and vertex attributes).
     * <p/>
     * This method can also be used when the variable is assigned a color buffer
     * explicitly within the fragment GLSL code but that index needs to be available from
     * Java as well.  Multiple calls to this method do not overwrite previously requested
     * bindings.
     * <p/>
     * If the any variable name is not declared in the fragment shader, a build exception
     * will be thrown later
     *
     * @param variableNames The user-defined output variables
     *
     * @return This builder
     *
     * @throws NullPointerException if any variable name is null
     */
    public ShaderBuilder requestBinding(String... variableNames);
}
