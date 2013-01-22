package com.ferox.resource.shader.grammar;

public class WhileLoopInitializer implements WhileLoopCondition {
    private final FullySpecifiedType type;
    private final String identifier;
    private final AssignmentExpression initializer;

    public WhileLoopInitializer(FullySpecifiedType type, String identifier,
                                AssignmentExpression initializer) {
        this.type = type;
        this.identifier = identifier;
        this.initializer = initializer;
    }
}
