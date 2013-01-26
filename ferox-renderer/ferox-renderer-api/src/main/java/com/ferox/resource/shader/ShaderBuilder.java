package com.ferox.resource.shader;

import com.ferox.resource.GlslShader;
import com.ferox.resource.shader.simple_grammar.Constant;
import com.ferox.resource.shader.simple_grammar.DoWhileLoop;
import com.ferox.resource.shader.simple_grammar.ForLoop;
import com.ferox.resource.shader.simple_grammar.FunctionDefinition;
import com.ferox.resource.shader.simple_grammar.IfThenElse;
import com.ferox.resource.shader.simple_grammar.Jump;
import com.ferox.resource.shader.simple_grammar.Jump.JumpType;
import com.ferox.resource.shader.simple_grammar.Variable;
import com.ferox.resource.shader.simple_grammar.WhileLoop;

public final class ShaderBuilder {
    // FIXME define public static final Function definitions for builtin functions

    private ShaderBuilder() {}

    public static VertexShaderBuilder newVertexShader();

    public static FragmentShaderBuilder newFragmentShader();

    public static FragmentShaderBuilder newFragmentShader(VertexShader vertexStage);

    public static GlslShader build(VertexShader vertexShader,
                                   FragmentShader fragmentShader);

    public static Type arrayOf(Type type, int len) {

    }

    public static Type arrayOf(Type type) {
        return arrayOf(type, -1);
    }

    public static StructBuilder struct(String structName) {

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
}
