package com.ferox.resource.shader.simple_grammar;

public class Parameter {
    private final Type type;
    private final String identifier; // nullable for unlabeled parameter
    private final Expression bracketExpression; // nullable for non-array type, must wrap for > assignmentexpressions

    public Parameter(Type type, String identifier, Expression bracketExpression) {
        this.type = type;
        this.identifier = identifier;
        this.bracketExpression = bracketExpression;
    }
}
