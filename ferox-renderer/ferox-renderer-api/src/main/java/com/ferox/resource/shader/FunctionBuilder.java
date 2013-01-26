package com.ferox.resource.shader;

public interface FunctionBuilder {
    public FunctionBuilder in(Type type, String name);

    public FunctionBuilder inOut(Type type, String name);

    public FunctionBuilder out(Type type, String name);

    public Function invoke(Statement statement);
}
