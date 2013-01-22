package com.ferox.resource.shader.grammar;

public class StructDeclarator {
    private final String identifier;
    private final ConstantExpression bracketExpression; // null for non-array type

    public StructDeclarator(String identifier, ConstantExpression bracketExpression) {
        this.identifier = identifier;
        this.bracketExpression = bracketExpression;
    }
}
