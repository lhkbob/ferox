package com.ferox.resource.shader.grammar;

public class ConditionalSelectExpression implements ConditionalExpression {
    private final BinaryExpression condition;
    private final Expression onTrue;
    private final ConditionalExpression onFalse;

    public ConditionalSelectExpression(BinaryExpression condition, Expression onTrue,
                                       ConditionalExpression onFalse) {
        this.condition = condition;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }
}
