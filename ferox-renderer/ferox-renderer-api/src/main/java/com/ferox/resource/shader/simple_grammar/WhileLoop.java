package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.PrimitiveType;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Statement;
import com.ferox.resource.shader.WhileBuilder;

public class WhileLoop implements Statement {
    private final Expression condition;
    private final Statement[] body;

    public WhileLoop(Expression condition, Statement[] body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public Environment validate(Environment environment) {
        environment = condition.validate(environment);
        if (!condition.getType(environment).equals(PrimitiveType.BOOL)) {
            throw new IllegalStateException("Loop condition expression must evaluate to a boolean");
        }

        // validate loop body
        Environment scoped = environment.newScope(true);
        for (int i = 0; i < body.length; i++) {
            scoped = body[i].validate(scoped);
        }

        return environment;
    }

    @Override
    public void emit(ShaderAccumulator accumulator) {
        accumulator.addLine("while (" + condition.emitExpression(accumulator) + ") {");
        accumulator.pushIndent();
        for (int i = 0; i < body.length; i++) {
            body[i].emit(accumulator);
        }
        accumulator.popIndent();
        accumulator.addLine("}");
    }

    public static class Builder implements WhileBuilder {
        private final Expression condition;

        public Builder(Expression condition) {
            this.condition = condition;
        }

        @Override
        public Statement do_(Statement... statement) {
            return new WhileLoop(condition, statement);
        }
    }
}
