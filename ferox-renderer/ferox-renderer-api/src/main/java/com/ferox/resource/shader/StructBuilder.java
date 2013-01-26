package com.ferox.resource.shader;

public interface StructBuilder {
    public StructBuilder add(Type type, String name);

    public Struct build();
}
