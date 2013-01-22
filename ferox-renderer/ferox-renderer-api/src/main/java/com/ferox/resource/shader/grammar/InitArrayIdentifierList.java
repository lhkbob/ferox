package com.ferox.resource.shader.grammar;

public class InitArrayIdentifierList implements InitDeclaratorList {
    private final InitDeclaratorList priorDeclarations;
    private final String identifier;
    private final ConstantExpression bracketExpression;

    public InitArrayIdentifierList(InitDeclaratorList priorDeclarations,
                                   String identifier, ConstantExpression bracketExpression) {
        this.priorDeclarations = priorDeclarations;
        this.identifier = identifier;
        this.bracketExpression = bracketExpression;
    }
}
