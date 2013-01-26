package com.ferox.resource.shader;

import java.sql.Struct;

public interface FragmentShaderBuilder {
    public FragmentShaderBuilder uniform(Type type, String name);

    public FragmentShaderBuilder constant(Type type, String name);

    public FragmentShaderBuilder struct(Struct type);

    public FragmentShaderBuilder in(Type type, String name);

    public FragmentShaderBuilder out(Type type, String name);

    public FragmentShader main(Statement... body);
}
