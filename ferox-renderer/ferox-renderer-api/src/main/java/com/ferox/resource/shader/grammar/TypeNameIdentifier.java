package com.ferox.resource.shader.grammar;

public class TypeNameIdentifier implements ConstructorIdentifier, TypeSpecifier {
    private final String name;

    public TypeNameIdentifier(String name) {
        this.name = name;
    }
}
