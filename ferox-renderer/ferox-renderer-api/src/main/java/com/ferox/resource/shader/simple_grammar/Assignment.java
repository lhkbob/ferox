package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.LValue;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Statement;

public class Assignment implements Statement, RightAssociative {
    private final LValue lvalue;
    private final Expression rvalue;

    public Assignment(LValue lvalue, Expression rvalue) {
        this.lvalue = lvalue;
        this.rvalue = rvalue;
    }

    @Override
    public Environment validate(Environment environment) {
        environment = lvalue.validate(rvalue.validate(environment));
        if (!lvalue.getType(environment).equals(rvalue.getType(environment))) {
            throw new IllegalStateException("Value does not have same type as variable");
        }
        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        // TODO Auto-generated method stub

    }
}
