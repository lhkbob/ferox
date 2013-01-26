package com.ferox.resource.shader;

public interface GlslElement {
    public Environment validate(Environment environment);

    public void emit(ShaderAccumulator accumulator);
}
