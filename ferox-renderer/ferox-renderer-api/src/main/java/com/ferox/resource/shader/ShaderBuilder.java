package com.ferox.resource.shader;

import com.ferox.resource.GlslShader;
import com.ferox.resource.GlslShader.ShaderType;
import com.ferox.resource.GlslShader.Version;
import com.ferox.resource.shader.simple_grammar.*;
import com.ferox.resource.shader.simple_grammar.Jump.JumpType;

import java.util.HashSet;
import java.util.Set;

public final class ShaderBuilder {
    // FIXME define public static final Function definitions for builtin functions

    private ShaderBuilder() {
    }

    public static VertexShaderBuilder newVertexShader() {
        return new VertexShaderImpl.Builder();
    }

    //    public static FragmentShaderBuilder newFragmentShader();
    //
    //    public static FragmentShaderBuilder newFragmentShader(VertexShader vertexStage);

    public static GlslShader build(VertexShader vertexShader,
                                   FragmentShader fragmentShader, Version target) {
        vertexShader.validate(new Environment(false));

        ShaderAccumulatorImpl topLevel = new ShaderAccumulatorImpl();
        topLevel.main = new ShaderAccumulatorImpl();
        topLevel.declare = new ShaderAccumulatorImpl();
        topLevel.struct = new ShaderAccumulatorImpl();
        topLevel.function = new ShaderAccumulatorImpl();

        // wire others together
        topLevel.main.connect(topLevel);
        topLevel.declare.connect(topLevel);
        topLevel.struct.connect(topLevel);
        topLevel.function.connect(topLevel);

        vertexShader.emit(topLevel);

        // convert to single string
        StringBuilder combined = new StringBuilder();
        if (target != Version.V1_10) {
            combined.append("#version " + target);
        }

        combined.append('\n');
        combined.append(topLevel.struct.buffer);
        combined.append('\n');
        combined.append(topLevel.declare.buffer);
        combined.append('\n');
        combined.append(topLevel.function.buffer);
        combined.append('\n');
        combined.append(topLevel.main.buffer);

        // FIXME also combine the toplevel's buffer?

        GlslShader shader = new GlslShader();
        shader.setShader(ShaderType.VERTEX, combined.toString());
        return shader;
    }

    public static Type arrayOf(Type type, int len) {
        return new ArrayType(type, len);
    }

    public static Type arrayOf(Type type) {
        return arrayOf(type, -1);
    }

    public static StructBuilder struct(String structName) {
        return new StructDefinition.Builder(structName);
    }

    public static Expression v(float v) {
        return new Constant(v);
    }

    public static Expression v(int v) {
        return new Constant(v);
    }

    public static Expression v(boolean v) {
        return new Constant(v);
    }

    public static LValue v(String name) {
        return new Variable(name);
    }

    public static FunctionBuilder function(Type type, String name) {
        return new FunctionDefinition.Builder(type, name);
    }

    public static LValue declare(Type type, String name) {
        return new LocalDeclaration(type, name);
    }

    public static IfBuilder if_(Expression condition) {
        return new IfThenElse.Builder(condition);
    }

    public static DoWhileBuilder do_(Statement... body) {
        return new DoWhileLoop.Builder(body);
    }

    public static WhileBuilder while_(Expression condition) {
        return new WhileLoop.Builder(condition);
    }

    public static WhileBuilder for_(Expression init, Expression check,
                                    Expression increment) {
        return new ForLoop.Builder(init, check, increment);
    }

    public static Statement return_(Expression value) {
        return new Jump(value);
    }

    public static Statement return_() {
        return new Jump((Expression) null);
    }

    public static Statement break_() {
        return new Jump(JumpType.BREAK);
    }

    public static Statement continue_() {
        return new Jump(JumpType.CONTINUE);
    }

    public static Statement discard() {
        return new Jump(JumpType.DISCARD);
    }

    private static class ShaderAccumulatorImpl implements ShaderAccumulator {
        private int indentLevel;

        private final StringBuilder buffer;
        private final Set<Function> accumulatedFunctions;
        private final Set<Struct> accumulatedStructs;

        private ShaderAccumulatorImpl main;
        private ShaderAccumulatorImpl declare;
        private ShaderAccumulatorImpl struct;
        private ShaderAccumulatorImpl function;

        public ShaderAccumulatorImpl() {
            buffer = new StringBuilder();
            accumulatedFunctions = new HashSet<Function>();
            accumulatedStructs = new HashSet<Struct>();

            indentLevel = 0;
            main = null;
            declare = null;
            struct = null;
            function = null;
        }

        @Override
        public void addLine(String code) {
            buffer.append('\n');
            for (int i = 0; i < indentLevel; i++) {
                buffer.append("   ");
            }
            buffer.append(code);
        }

        @Override
        public void pushIndent() {
            indentLevel++;
        }

        @Override
        public void popIndent() {
            indentLevel--;
        }

        @Override
        public void accumulateFunction(Function f) {
            if (!function.accumulatedFunctions.contains(f)) {
                function.accumulatedFunctions.add(f);
                f.emit(function);
            }
        }

        @Override
        public void accumulateStruct(Struct s) {
            if (!struct.accumulatedStructs.contains(s)) {
                struct.accumulatedStructs.add(s);
                s.emit(struct);
            }
        }

        @Override
        public ShaderAccumulator getGlobalDeclarationAccumulator() {
            return declare;
        }

        @Override
        public ShaderAccumulator getMainAccumulator() {
            return main;
        }

        public void connect(ShaderAccumulatorImpl parent) {
            main = parent.main;
            declare = parent.declare;
            struct = parent.struct;
            function = parent.function;
        }
    }
}
