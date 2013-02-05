package com.ferox.resource.shader.simple_grammar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.Function;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Type;

public class BuiltInFunction implements Function {
    private final String name;
    private final Type returnType;
    private final Type[] parameterTypes;

    public BuiltInFunction(Type returnType, String name, Type... parameters) {
        this.name = name;
        this.returnType = returnType;
        parameterTypes = Arrays.copyOf(parameters, parameters.length);
    }

    @Override
    public Environment validate(Environment environment) {
        // assume it's valid
        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        // do nothing
    }

    @Override
    public Expression call(Expression... parameters) {
        return new FunctionCall(this, parameters);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Type> getParameters() {
        Map<String, Type> pMap = new HashMap<String, Type>();
        for (int i = 0; i < parameterTypes.length; i++) {
            pMap.put("p" + i, parameterTypes[i]);
        }
        return pMap;
    }

    @Override
    public Type[] getParameterTypes() {
        return Arrays.copyOf(parameterTypes, parameterTypes.length);
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }
}
