package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Struct;
import com.ferox.resource.shader.Type;

public class FieldSelection extends AbstractLValue {
    private final Expression variable;
    private final String field;

    public FieldSelection(Expression variable, String field) {
        this.variable = variable;
        this.field = field;
    }

    @Override
    public Type getType(Environment env) {
        Type base = variable.getType(env);
        if (base instanceof Struct) {
            return ((Struct) base).getFields().get(field);
        } else {
            // assume that this is a vector type, must return float/int/bool
            // or the vectorized versions of those based on the length of its
            // swizzle
            // FIXME what about matrix components?
        }
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
