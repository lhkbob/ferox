package com.ferox.renderer.builder;

import com.ferox.renderer.Shader;

/**
 *
 */
public interface ShaderBuilder {
    public ShaderBuilder setVertexShader(String code);

    public ShaderBuilder setFragmentShader(String code);

    public ShaderBuilder setGeometryShader(String code);

    public Shader build();
}
