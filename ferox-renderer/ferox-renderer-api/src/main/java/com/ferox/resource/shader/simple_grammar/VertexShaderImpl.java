package com.ferox.resource.shader.simple_grammar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.PrimitiveType;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Statement;
import com.ferox.resource.shader.Type;
import com.ferox.resource.shader.VertexShader;
import com.ferox.resource.shader.VertexShaderBuilder;
import com.ferox.resource.shader.simple_grammar.Parameter.ParameterQualifier;

public class VertexShaderImpl implements VertexShader {
    private final Parameter[] uniforms;
    private final Parameter[] constants;
    private final Parameter[] inputs;
    private final Parameter[] outputs;

    private final Statement[] mainBody;

    public VertexShaderImpl(Parameter[] uniforms, Parameter[] constants,
                            Parameter[] inputs, Parameter[] outputs, Statement[] mainBody) {
        this.uniforms = uniforms;
        this.constants = constants;
        this.inputs = inputs;
        this.outputs = outputs;
        this.mainBody = mainBody;
    }

    @Override
    public Environment validate(Environment environment) {
        // uniforms
        for (int i = 0; i < uniforms.length; i++) {
            if (!environment.declare(uniforms[i].getType(), uniforms[i].getName())) {
                throw new IllegalStateException("Global variable name already defined");
            }
        }
        // constants
        for (int i = 0; i < constants.length; i++) {
            if (!environment.declare(constants[i].getType(), constants[i].getName())) {
                throw new IllegalStateException("Global variable name already defined");
            }
        }
        // inputs
        for (int i = 0; i < inputs.length; i++) {
            if (!environment.declare(inputs[i].getType(), inputs[i].getName())) {
                throw new IllegalStateException("Global variable name already defined");
            }
        }
        // outputs
        for (int i = 0; i < outputs.length; i++) {
            if (!environment.declare(outputs[i].getType(), outputs[i].getName())) {
                throw new IllegalStateException("Global variable name already defined");
            }
        }

        // validate main function body using a child scope
        Environment mainScope = environment.functionScope(PrimitiveType.VOID,
                                                          new HashMap<String, Type>());
        for (int i = 0; i < mainBody.length; i++) {
            mainScope = mainBody[i].validate(mainScope);
        }

        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        ShaderAccumulator globalVars = accumulator.getGlobalDeclarationAccumulator();

        for (int i = 0; i < constants.length; i++) {
            globalVars.addLine("const " + constants[i].getType()
                                                      .getTypeIdentifier(globalVars,
                                                                         constants[i].getName()) + ";");
        }
        globalVars.addLine("");

        for (int i = 0; i < uniforms.length; i++) {
            globalVars.addLine("uniform " + uniforms[i].getType()
                                                       .getTypeIdentifier(globalVars,
                                                                          uniforms[i].getName()) + ";");
        }
        globalVars.addLine("");

        for (int i = 0; i < inputs.length; i++) {
            globalVars.addLine("attribute " + inputs[i].getType()
                                                       .getTypeIdentifier(globalVars,
                                                                          inputs[i].getName()) + ";");
        }
        globalVars.addLine("");

        for (int i = 0; i < outputs.length; i++) {
            globalVars.addLine("varying " + outputs[i].getType()
                                                      .getTypeIdentifier(globalVars,
                                                                         outputs[i].getName()) + ";");
        }
        globalVars.addLine("");

        // main body
        ShaderAccumulator main = accumulator.getMainAccumulator();
        main.addLine("void main() {");
        main.pushIndent();

        for (int i = 0; i < mainBody.length; i++) {
            mainBody[i].emit(main);
        }
        main.popIndent();
        main.addLine("}");
        main.addLine("");
    }

    public static class Builder implements VertexShaderBuilder {
        private final List<Parameter> uniforms;
        private final List<Parameter> constants;
        private final List<Parameter> inputs;
        private final List<Parameter> outputs;

        public Builder() {
            uniforms = new ArrayList<Parameter>();
            constants = new ArrayList<Parameter>();
            inputs = new ArrayList<Parameter>();
            outputs = new ArrayList<Parameter>();
        }

        @Override
        public VertexShaderBuilder uniform(Type type, String name) {
            uniforms.add(new Parameter(ParameterQualifier.NONE, type, name));
            return this;
        }

        @Override
        public VertexShaderBuilder constant(Type type, String name) {
            constants.add(new Parameter(ParameterQualifier.NONE, type, name));
            return this;
        }

        @Override
        public VertexShaderBuilder in(Type type, String name) {
            inputs.add(new Parameter(ParameterQualifier.IN, type, name));
            return this;
        }

        @Override
        public VertexShaderBuilder out(Type type, String name) {
            outputs.add(new Parameter(ParameterQualifier.OUT, type, name));
            return this;
        }

        @Override
        public VertexShader main(Statement... body) {
            return new VertexShaderImpl(uniforms.toArray(new Parameter[uniforms.size()]),
                                        constants.toArray(new Parameter[constants.size()]),
                                        inputs.toArray(new Parameter[inputs.size()]),
                                        outputs.toArray(new Parameter[outputs.size()]),
                                        body);
        }
    }
}
