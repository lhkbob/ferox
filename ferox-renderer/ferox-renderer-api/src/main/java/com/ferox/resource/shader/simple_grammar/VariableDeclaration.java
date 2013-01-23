package com.ferox.resource.shader.simple_grammar;

public class VariableDeclaration implements Declaration {
    // FIXME type qualifier (does this get consumed by parameter qualifier?)
    private final Type type;
    private final String identifier;
    private final Expression initializer; // nullable for no initializer

    public VariableDeclaration(Type type, String identifier, Expression initializer) {
        this.type = type;
        this.identifier = identifier;
        this.initializer = initializer;
    }
}
