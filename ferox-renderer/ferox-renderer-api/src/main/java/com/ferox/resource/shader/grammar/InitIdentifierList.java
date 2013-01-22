package com.ferox.resource.shader.grammar;

public class InitIdentifierList implements InitDeclaratorList {
    private final InitDeclaratorList priorDeclarations;
    private final String identifier;
    private final AssignmentExpression initializer; // nullable for no initialization

    public InitIdentifierList(InitDeclaratorList priorDeclarations, String identifier,
                              AssignmentExpression initializer) {
        this.priorDeclarations = priorDeclarations;
        this.identifier = identifier;
        this.initializer = initializer;
    }
}
