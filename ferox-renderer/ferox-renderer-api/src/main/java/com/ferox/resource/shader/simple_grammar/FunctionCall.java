package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.Function;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Type;

public class FunctionCall extends AbstractExpression {
    private final Function function;
    private final Expression[] parameters;

    public FunctionCall(Function function, Expression... parameters) {
        this.function = function;
        this.parameters = parameters;
    }

    @Override
    public Type getType(Environment env) {
        return function.getReturnType();
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
