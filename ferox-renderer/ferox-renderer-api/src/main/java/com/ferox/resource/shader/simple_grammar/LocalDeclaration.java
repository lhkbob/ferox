package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Type;

public class LocalDeclaration extends AbstractLValue {
    private final Type type;
    private final String identifier;

    public LocalDeclaration(Type type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    @Override
    public Type getType(Environment env) {
        return type;
    }

    @Override
    public Environment validate(Environment environment) {
        if (!environment.declare(type, identifier)) {
            throw new IllegalStateException("Variable already declared at this scope");
        }
        return environment;
    }

    @Override
    public String emitExpression(ShaderAccumulator accumulator) {
        return type.getTypeIdentifier(accumulator, identifier);
    }

    @Override
    public int getPrecedence() {
        return Precedence.ASSIGNMENT_EXPRESSIONS.ordinal();
    }
}
