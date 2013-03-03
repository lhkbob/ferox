package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.*;

public class Assignment extends AbstractExpression {
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
        if (rvalue.containsDeclaration()) {
            throw new IllegalStateException(
                    "Value expression cannot contain a declaration");
        }
        return environment;
    }

    @Override
    public Type getType(Environment env) {
        return lvalue.getType(env);
    }

    @Override
    public String emitExpression(ShaderAccumulator shader) {
        // we do not need to contain either expression in parentheses because
        // the assignment expression has the lowest precedence
        return lvalue.emitExpression(shader) + " = " + rvalue.emitExpression(shader);
    }

    @Override
    public boolean containsDeclaration() {
        return lvalue.containsDeclaration();
    }

    @Override
    public int getPrecedence() {
        return Precedence.ASSIGNMENT_EXPRESSIONS.ordinal();
    }
}
