package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.*;
import com.ferox.resource.shader.simple_grammar.Parameter.ParameterQualifier;

import java.util.*;

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
    public Type[] getParameterTypes() {
        Type[] paramTypes = new Type[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getType();
        }
        return paramTypes;
    }

    @Override
    public Type getReturnType() {
        return returnType;
    }

    @Override
    public Environment validate(Environment environment) {
        Environment bodyEnv = environment.functionScope(returnType, paramMap);
        for (int i = 0; i < body.length; i++) {
            bodyEnv = body[i].validate(bodyEnv);
        }

        return bodyEnv;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        StringBuilder header = new StringBuilder();
        header.append(returnType.getTypeIdentifier(accumulator, ""));
        header.append(name);
        header.append("(");

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                header.append(", ");
            }
            if (params[i].getQualifier() != ParameterQualifier.NONE) {
                header.append(params[i].getQualifier().name().toLowerCase());
                header.append(" ");
            }
            header.append(params[i].getType()
                                   .getTypeIdentifier(accumulator, params[i].getName()));
        }

        header.append(") {");

        accumulator.addLine(header.toString());
        accumulator.pushIndent();

        for (int i = 0; i < body.length; i++) {
            body[i].emit(accumulator);
        }

        accumulator.popIndent();
        accumulator.addLine("}");
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
            return new FunctionDefinition(name, returnType,
                                          params.toArray(new Parameter[params.size()]),
                                          body);
        }
    }
}
