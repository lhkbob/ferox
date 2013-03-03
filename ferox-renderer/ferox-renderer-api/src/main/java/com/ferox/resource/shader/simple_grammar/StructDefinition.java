package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.*;
import com.ferox.resource.shader.simple_grammar.Parameter.ParameterQualifier;

import java.util.*;

public class StructDefinition implements Struct {
    private final String identifier;
    private final Parameter[] parameters;
    private final Map<String, Type> paramMap;

    public StructDefinition(String identifier, Parameter... parameters) {
        this.identifier = identifier;
        this.parameters = parameters;

        Map<String, Type> paramMap = new HashMap<String, Type>();
        for (Parameter p : parameters) {
            paramMap.put(p.getName(), p.getType());
        }
        this.paramMap = Collections.unmodifiableMap(paramMap);
    }

    @Override
    public String getTypeIdentifier(ShaderAccumulator accumulator, String varIdentifier) {
        accumulator.accumulateStruct(this);
        return identifier + " " + varIdentifier;
    }

    @Override
    public Map<String, Type> getFields() {
        return paramMap;
    }

    @Override
    public Environment validate(Environment environment) {
        // no validation to be done
        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        accumulator.addLine("struct " + identifier + "{");
        accumulator.pushIndent();

        for (int i = 0; i < parameters.length; i++) {
            accumulator.addLine(parameters[i].getType().getTypeIdentifier(accumulator,
                                                                          parameters[i]
                                                                                  .getName()) +
                                ";");
        }

        accumulator.popIndent();
        accumulator.addLine("}");
    }

    @Override
    public String[] getOrderedFields() {
        String[] fields = new String[parameters.length];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = parameters[i].getName();
        }
        return fields;
    }

    public static class Builder implements StructBuilder {
        private final String identifier;
        private final List<Parameter> parameters;

        public Builder(String identifier) {
            this.identifier = identifier;
            parameters = new ArrayList<Parameter>();
        }

        @Override
        public StructBuilder add(Type type, String name) {
            parameters.add(new Parameter(ParameterQualifier.NONE, type, name));
            return this;
        }

        @Override
        public Struct build() {
            return new StructDefinition(identifier, parameters
                    .toArray(new Parameter[parameters.size()]));
        }
    }
}
