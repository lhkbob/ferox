package com.ferox.resource.shader;

public class ArrayType implements Type {
    private final Type componentType;
    private final int length;

    public ArrayType(Type componentType, int length) {
        this.componentType = componentType;
        this.length = length;
    }

    public Type getComponentType() {
        return componentType;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String getTypeIdentifier(ShaderAccumulator accumulator, String varIdentifier) {
        if (length < 0) {
            return componentType.getTypeIdentifier(accumulator, varIdentifier) + "[]";
        } else {
            return componentType.getTypeIdentifier(accumulator, varIdentifier) + "[" + length + "]";
        }
    }
}
