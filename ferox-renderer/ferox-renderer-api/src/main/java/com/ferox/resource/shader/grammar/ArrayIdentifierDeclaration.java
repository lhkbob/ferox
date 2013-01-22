package com.ferox.resource.shader.grammar;

public class ArrayIdentifierDeclaration implements SingleDeclaration {
    private final FullySpecifiedType type;
    private final String identifier;
    private final ConstantExpression bracketExpression;

    public ArrayIdentifierDeclaration(FullySpecifiedType type, String identifier,
                                      ConstantExpression bracketExpression) {
        this.type = type;
        this.identifier = identifier;
        this.bracketExpression = bracketExpression;
    }
}
