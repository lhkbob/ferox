package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.*;

public class Jump implements Statement {
    public static enum JumpType {
        BREAK,
        CONTINUE,
        DISCARD,
        RETURN
    }

    private final JumpType type;
    private final Expression returnExpression; // only non-null for return

    public Jump(JumpType type) {
        this.type = type;
        returnExpression = null;
    }

    public Jump(Expression returnExpression) {
        type = JumpType.RETURN;
        this.returnExpression = returnExpression;
    }

    @Override
    public Environment validate(Environment environment) {
        switch (type) {
        case BREAK:
        case CONTINUE:
            if (!environment.inLoop()) {
                throw new IllegalStateException(
                        "Continue and break can only be used in loops");
            }
            break;
        case DISCARD:
            if (!environment.inFragmentShader()) {
                throw new IllegalStateException(
                        "Discard can only be used in a fragment shader");
            }
            break;
        case RETURN:
            if (returnExpression != null) {
                if (!returnExpression.getType(environment)
                                     .equals(environment.getRequiredReturnType())) {
                    throw new IllegalStateException(
                            "Returned expression does not match required return type for function");
                }
            } else {
                if (!environment.getRequiredReturnType().equals(PrimitiveType.VOID)) {
                    throw new IllegalStateException("Return statement must return void");
                }
            }
            break;
        }

        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        if (returnExpression != null) {
            // type is RETURN and not a void return
            accumulator.addLine(
                    "return " + returnExpression.emitExpression(accumulator) + ";");
        } else {
            accumulator.addLine(type.name().toLowerCase() + ";");
        }
    }
}
