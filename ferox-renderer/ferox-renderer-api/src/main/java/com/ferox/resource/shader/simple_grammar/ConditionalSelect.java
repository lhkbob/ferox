package com.ferox.resource.shader.simple_grammar;

public class ConditionalSelect implements Expression, RightAssociative {
    private final Expression condition;
    private final Expression onTrue;
    private final Expression onFalse;

    public ConditionalSelect(Expression condition, Expression onTrue,
                                       Expression onFalse) {
        this.condition = condition;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }
}
