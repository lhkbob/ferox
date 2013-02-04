package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.DoWhileBuilder;
import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.PrimitiveType;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Statement;

public class DoWhileLoop implements Statement {
    private final Statement[] body;
    private final Expression condition;

    public DoWhileLoop(Statement[] body, Expression condition) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public Environment validate(Environment environment) {
        environment = condition.validate(environment);
        if (!condition.getType(environment).equals(PrimitiveType.BOOL)) {
            throw new IllegalStateException("Loop condition must evaluate to a boolean");
        }
        if (condition.containsDeclaration()) {
            throw new IllegalStateException("Do-while loops do not support declarations in condition expression");
        }

        // validate loop body
        Environment scoped = environment.newScope();
        for (int i = 0; i < body.length; i++) {
            scoped = body[i].validate(scoped);
        }

        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        accumulator.addLine("do {");
        accumulator.pushIndent();
        for (int i = 0; i < body.length; i++) {
            body[i].emit(accumulator);
        }
        accumulator.popIndent();
        accumulator.addLine("} while (" + condition.emitExpression() + ");");
    }

    public static class Builder implements DoWhileBuilder {
        private final Statement[] body;

        public Builder(Statement[] body) {
            this.body = body;
        }

        @Override
        public Statement while_(Expression condition) {
            return new DoWhileLoop(body, condition);
        }
    }
}
