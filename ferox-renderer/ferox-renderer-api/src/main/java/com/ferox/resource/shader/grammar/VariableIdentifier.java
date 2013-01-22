package com.ferox.resource.shader.grammar;

public class VariableIdentifier implements PrimaryExpression {
    private final String identifier;

    public VariableIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
