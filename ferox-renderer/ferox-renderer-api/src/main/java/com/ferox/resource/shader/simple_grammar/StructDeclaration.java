package com.ferox.resource.shader.simple_grammar;

public class StructDeclaration implements Declaration, Type {
    private final String identifier;
    private final Parameter[] parameters;

    public StructDeclaration(String identifier, Parameter... parameters) {
        this.identifier = identifier;
        this.parameters = parameters;
    }
}
