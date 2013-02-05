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
        for (int i = 0; i < parameters.length; i++) {
            environment = parameters[i].validate(environment);
            if (!parameters[i].getType(environment)
                              .equals(function.getParameterTypes()[i])) {
                throw new IllegalStateException("Parameter is of illegal type");
            }
        }

        function.validate(environment.getRoot());

        return environment;
    }

    @Override
    public String emitExpression(ShaderAccumulator accumulator) {
        // parameters do not need to be wrapped in parentheses because they
        // are unambiguous given the outer parentheses and commas
        StringBuilder sb = new StringBuilder();
        sb.append(function.getName());
        sb.append("(");
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }

            sb.append(parameters[i].emitExpression(accumulator));
        }
        sb.append(")");

        // also include the function definition
        accumulator.accumulateFunction(function);
        return sb.toString();
    }

    @Override
    public int getPrecedence() {
        return Precedence.POSTFIX_EXPRESSIONS.ordinal();
    }
}
