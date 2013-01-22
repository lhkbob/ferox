package com.ferox.resource.shader.grammar;

public class IdentifierDeclaration implements SingleDeclaration {
    private final FullySpecifiedType type;
    private final String identifier;
    private final AssignmentExpression initializer; // nullable for no initializer

    public IdentifierDeclaration(FullySpecifiedType type, String identifier,
                                 AssignmentExpression initializer) {
        this.type = type;
        this.identifier = identifier;
        this.initializer = initializer;
    }
}
