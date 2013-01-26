package com.ferox.resource.shader.simple_grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.Function;
import com.ferox.resource.shader.FunctionBuilder;
import com.ferox.resource.shader.Statement;
import com.ferox.resource.shader.Type;
import com.ferox.resource.shader.simple_grammar.Parameter.ParameterQualifier;

public class FunctionDefinition implements Function {
    private final String name;
    private final Type returnType;

    private final Parameter[] params;
    private final Map<String, Type> paramMap;

    private final Statement[] body;

    public FunctionDefinition(String name, Type returnType, Parameter[] params,
                              Statement[] body) {
        this.name = name;
        this.returnType = returnType;
        this.params = params;
        this.body = body;

        Map<String, Type> paramMap = new HashMap<String, Type>();
        for (Parameter p : params) {
            paramMap.put(p.getName(), p.getType());
        }
        this.paramMap = Collections.unmodifiableMap(paramMap);
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
        return paramMap;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    public static class Builder implements FunctionBuilder {
        private final List<Parameter> params;
        private final Type returnType;
        private final String name;

        public Builder(Type returnType, String name) {
            this.name = name;
            this.returnType = returnType;
            params = new ArrayList<Parameter>();
        }

        @Override
        public FunctionBuilder in(Type type, String name) {
            params.add(new Parameter(ParameterQualifier.IN, type, name));
            return this;
        }

        @Override
        public FunctionBuilder inOut(Type type, String name) {
            params.add(new Parameter(ParameterQualifier.INOUT, type, name));
            return this;
        }

        @Override
        public FunctionBuilder out(Type type, String name) {
            params.add(new Parameter(ParameterQualifier.OUT, type, name));
            return this;
        }

        @Override
        public Function invoke(Statement... body) {
            return new FunctionDefinition(name,
                                          returnType,
                                          params.toArray(new Parameter[params.size()]),
                                          body);
        }
    }
}
