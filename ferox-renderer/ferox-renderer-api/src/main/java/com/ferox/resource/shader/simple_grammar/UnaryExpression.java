package com.ferox.resource.shader.simple_grammar;

public class UnaryExpression implements Expression {
    public static enum UnaryOperator {
        // FIXME other expressions are between the postfix and prefix in terms of precedence
        // is that awkward?
        POSTFIX_INCREMENT,
        POSTFIX_DECREMENT,

        PREFIX_INCREMENT, // ++
        PREFIX_DECREMENT, // --
        // FIXME make these names nicer? after what they do?
        PLUS, // +
        DASH, // -
        BANG, // !
        TILDE /* reserved */
    }

    private final UnaryOperator operator;
    private final Expression expression;

    public UnaryExpression(UnaryOperator operator, Expression expression) {
        this.operator = operator;
        this.expression = expression;
    }
}
