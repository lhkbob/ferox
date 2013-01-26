package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.ArrayType;
import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.PrimitiveType;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Type;

public class ArrayAccess extends AbstractLValue {
    private final Expression array;
    private final Expression index; // int_expression

    public ArrayAccess(Expression array, Expression index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public Type getType(Environment env) {
        return ((ArrayType) array.getType(env)).getComponentType();
    }

    @Override
    public Environment validate(Environment environment) {
        environment = array.validate(index.validate(environment));
        if (!(array.getType(environment) instanceof ArrayType)) {
            throw new IllegalStateException("Expression does not evaluate to an array type");
        } else if (!index.getType(environment).equals(PrimitiveType.INT)) {
            throw new IllegalStateException("Index expression does not evaluate to an integer type");
        }
        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        // TODO Auto-generated method stub

    }
}
