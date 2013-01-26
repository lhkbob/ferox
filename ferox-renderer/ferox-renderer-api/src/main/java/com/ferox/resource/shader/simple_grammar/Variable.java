package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Type;

public class Variable extends AbstractLValue {
    private final String identifier;

    public Variable(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public Type getType(Environment env) {
        return env.getVariable(identifier);
    }

    @Override
    public Environment validate(Environment environment) {
        if (environment.getVariable(identifier) == null) {
            throw new IllegalStateException("No variable with name: " + identifier);
        }
        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        // TODO Auto-generated method stub

    }
}
