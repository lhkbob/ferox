package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Type;

public class UnaryExpression extends AbstractExpression {
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

    @Override
    public Type getType(Environment env) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Environment validate(Environment environment) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        // TODO Auto-generated method stub

    }
}
