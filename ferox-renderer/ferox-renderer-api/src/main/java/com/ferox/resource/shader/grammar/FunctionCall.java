package com.ferox.resource.shader.grammar;

public class FunctionCall implements PostfixExpression {
    // this is a reasonable consolidation of the grammar for
    // function calls with and without parameters
    private final FunctionIdentifier identifier;
    private final AssignmentExpression[] parameters;

    public FunctionCall(FunctionIdentifier identifier, AssignmentExpression... parameters) {
        this.identifier = identifier;
        this.parameters = parameters;
    }
}
