package com.ferox.resource.builder;

import com.ferox.resource.Shader;

/**
 *
 */
public interface ShaderBuilder {
    public ShaderBuilder setVertexShader(String code);

    public ShaderBuilder setFragmentShader(String code);

    public ShaderBuilder setGeometryShader(String code);

    public Shader build();
}
