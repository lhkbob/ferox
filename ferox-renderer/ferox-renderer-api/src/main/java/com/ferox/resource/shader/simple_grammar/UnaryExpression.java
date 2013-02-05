package com.ferox.resource.shader.simple_grammar;

import java.util.HashMap;
import java.util.Map;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.PrimitiveType;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Type;

public class UnaryExpression extends AbstractExpression {
    public static enum UnaryOperator {
        POSTFIX_INCREMENT("++"),
        POSTFIX_DECREMENT("--"),

        PREFIX_INCREMENT("++"), // ++
        PREFIX_DECREMENT("--"), // --
        NEGATE("-"),
        LOGICAL_NEGATE("!"),
        BIT_INVERT("~"); /* reserved */

        private UnaryOperator(String symbol) {
            this.symbol = symbol;
        }

        private String symbol;
    }

    private final UnaryOperator operator;
    private final Expression expression;

    public UnaryExpression(UnaryOperator operator, Expression expression) {
        this.operator = operator;
        this.expression = expression;
    }

    @Override
    public Type getType(Environment env) {
        return get((PrimitiveType) expression.getType(env), operator);
    }

    @Override
    public Environment validate(Environment environment) {
        environment = expression.validate(environment);

        Type base = expression.getType(environment);
        if (!(base instanceof PrimitiveType)) {
            throw new IllegalStateException("Unary operators only support primitive types");
        }
        if (get((PrimitiveType) base, operator) == null) {
            throw new IllegalStateException("Unary operator not supported on type");
        }
        if (expression.containsDeclaration()) {
            throw new IllegalStateException("Unary operator cannot operate on expressions containing declarations");
        }
        return environment;
    }

    @Override
    public String emitExpression(ShaderAccumulator accumulator) {
        String expr = (expression.getPrecedence() < getPrecedence() ? "(" + expression.emitExpression(accumulator) + ")" : expression.emitExpression(accumulator));
        if (operator == UnaryOperator.POSTFIX_DECREMENT || operator == UnaryOperator.POSTFIX_INCREMENT) {
            return expr + operator.symbol;
        } else {
            return operator.symbol + expr;
        }
    }

    @Override
    public int getPrecedence() {
        switch (operator) {
        case LOGICAL_NEGATE:
        case NEGATE:
        case PREFIX_DECREMENT:
        case PREFIX_INCREMENT:
        case BIT_INVERT:
            return Precedence.UNARY_EXPRESSIONS.ordinal();
        case POSTFIX_DECREMENT:
        case POSTFIX_INCREMENT:
            return Precedence.POSTFIX_EXPRESSIONS.ordinal();
        default:
            throw new UnsupportedOperationException("Unmapped operator, no associated precedence");
        }
    }

    private static final Map<UnaryOperator, Map<PrimitiveType, PrimitiveType>> operatorMap = new HashMap<UnaryOperator, Map<PrimitiveType, PrimitiveType>>();

    private static void add(PrimitiveType type, UnaryOperator op, PrimitiveType resultType) {
        Map<PrimitiveType, PrimitiveType> typeMap = operatorMap.get(op);
        if (typeMap == null) {
            typeMap = new HashMap<PrimitiveType, PrimitiveType>();
            operatorMap.put(op, typeMap);
        }

        typeMap.put(type, resultType);
    }

    private static PrimitiveType get(PrimitiveType type, UnaryOperator op) {
        Map<PrimitiveType, PrimitiveType> typeMap = operatorMap.get(op);
        if (typeMap != null) {
            return typeMap.get(type);
        }

        return null;
    }

    static {
        // float types
        add(PrimitiveType.FLOAT, UnaryOperator.NEGATE, PrimitiveType.FLOAT);
        add(PrimitiveType.FLOAT, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.FLOAT);
        add(PrimitiveType.FLOAT, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.FLOAT);
        add(PrimitiveType.FLOAT, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.FLOAT);
        add(PrimitiveType.FLOAT, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.FLOAT);

        // int types
        add(PrimitiveType.INT, UnaryOperator.NEGATE, PrimitiveType.INT);
        add(PrimitiveType.INT, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.INT);
        add(PrimitiveType.INT, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.INT);
        add(PrimitiveType.INT, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.INT);
        add(PrimitiveType.INT, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.INT);
        add(PrimitiveType.INT, UnaryOperator.BIT_INVERT, PrimitiveType.INT);

        // boolean types
        add(PrimitiveType.BOOL, UnaryOperator.LOGICAL_NEGATE, PrimitiveType.BOOL);

        // vec2 types
        add(PrimitiveType.VEC2, UnaryOperator.NEGATE, PrimitiveType.VEC2);
        add(PrimitiveType.VEC2, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.VEC2);
        add(PrimitiveType.VEC2, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.VEC2);
        add(PrimitiveType.VEC2, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.VEC2);
        add(PrimitiveType.VEC2, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.VEC2);
        add(PrimitiveType.IVEC2, UnaryOperator.NEGATE, PrimitiveType.IVEC2);
        add(PrimitiveType.IVEC2, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.IVEC2);
        add(PrimitiveType.IVEC2, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.IVEC2);
        add(PrimitiveType.IVEC2, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.IVEC2);
        add(PrimitiveType.IVEC2, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.IVEC2);
        add(PrimitiveType.BVEC2, UnaryOperator.NEGATE, PrimitiveType.BVEC2);
        add(PrimitiveType.BVEC2, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.BVEC2);
        add(PrimitiveType.BVEC2, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.BVEC2);
        add(PrimitiveType.BVEC2, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.BVEC2);
        add(PrimitiveType.BVEC2, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.BVEC2);

        // vec3 types
        add(PrimitiveType.VEC3, UnaryOperator.NEGATE, PrimitiveType.VEC3);
        add(PrimitiveType.VEC3, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.VEC3);
        add(PrimitiveType.VEC3, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.VEC3);
        add(PrimitiveType.VEC3, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.VEC3);
        add(PrimitiveType.VEC3, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.VEC3);
        add(PrimitiveType.IVEC3, UnaryOperator.NEGATE, PrimitiveType.IVEC3);
        add(PrimitiveType.IVEC3, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.IVEC3);
        add(PrimitiveType.IVEC3, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.IVEC3);
        add(PrimitiveType.IVEC3, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.IVEC3);
        add(PrimitiveType.IVEC3, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.IVEC3);
        add(PrimitiveType.BVEC3, UnaryOperator.NEGATE, PrimitiveType.BVEC3);
        add(PrimitiveType.BVEC3, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.BVEC3);
        add(PrimitiveType.BVEC3, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.BVEC3);
        add(PrimitiveType.BVEC3, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.BVEC3);
        add(PrimitiveType.BVEC3, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.BVEC3);

        // vec4 types
        add(PrimitiveType.VEC4, UnaryOperator.NEGATE, PrimitiveType.VEC4);
        add(PrimitiveType.VEC4, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.VEC4);
        add(PrimitiveType.VEC4, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.VEC4);
        add(PrimitiveType.VEC4, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.VEC4);
        add(PrimitiveType.VEC4, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.VEC4);
        add(PrimitiveType.IVEC4, UnaryOperator.NEGATE, PrimitiveType.IVEC4);
        add(PrimitiveType.IVEC4, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.IVEC4);
        add(PrimitiveType.IVEC4, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.IVEC4);
        add(PrimitiveType.IVEC4, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.IVEC4);
        add(PrimitiveType.IVEC4, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.IVEC4);
        add(PrimitiveType.BVEC4, UnaryOperator.NEGATE, PrimitiveType.BVEC4);
        add(PrimitiveType.BVEC4, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.BVEC4);
        add(PrimitiveType.BVEC4, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.BVEC4);
        add(PrimitiveType.BVEC4, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.BVEC4);
        add(PrimitiveType.BVEC4, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.BVEC4);

        // mat2 types
        add(PrimitiveType.MAT2, UnaryOperator.NEGATE, PrimitiveType.MAT2);
        add(PrimitiveType.MAT2, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.MAT2);
        add(PrimitiveType.MAT2, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.MAT2);
        add(PrimitiveType.MAT2, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.MAT2);
        add(PrimitiveType.MAT2, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.MAT2);

        // mat3 types
        add(PrimitiveType.MAT3, UnaryOperator.NEGATE, PrimitiveType.MAT3);
        add(PrimitiveType.MAT3, UnaryOperator.POSTFIX_DECREMENT, PrimitiveType.MAT3);
        add(PrimitiveType.MAT3, UnaryOperator.POSTFIX_INCREMENT, PrimitiveType.MAT3);
        add(PrimitiveType.MAT3, UnaryOperator.PREFIX_DECREMENT, PrimitiveType.MAT3);
        add(PrimitiveType.MAT3, UnaryOperator.PREFIX_INCREMENT, PrimitiveType.MAT3);
    }
}
