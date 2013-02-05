package com.ferox.resource.shader;


public interface VertexShaderBuilder {
    public VertexShaderBuilder uniform(Type type, String name);

    public VertexShaderBuilder constant(Type type, String name);

    public VertexShaderBuilder in(Type type, String name);

    public VertexShaderBuilder out(Type type, String name);

    public VertexShader main(Statement... body);
}
