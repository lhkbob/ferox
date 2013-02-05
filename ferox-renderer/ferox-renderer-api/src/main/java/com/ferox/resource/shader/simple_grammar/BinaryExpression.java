package com.ferox.resource.shader.simple_grammar;

import java.util.HashMap;
import java.util.Map;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.PrimitiveType;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Type;

public class BinaryExpression extends AbstractExpression {
    public static enum BinaryOperator {
        MULTIPLY("*"),
        DIVIDE("/"),
        ADD("+"),
        SUBTRACT("-"),
        MODULO("%"),
        LEFT_SHIFT("<<"),
        RIGHT_SHIFT(">>"),
        LESS_THAN("<"),
        GREATER_THAN(">"),
        LESS_THAN_OR_EQUAL("<="),
        GREATER_THAN_OR_EQUAL(">="),
        EQUAL("=="),
        NOT_EQUAL("!="),
        LOGICAL_AND("&&"),
        LOGICAL_XOR("^^"),
        LOGICAL_OR("||");
        // FIXME add bitwise AND, XOR, and OR

        private BinaryOperator(String symbol) {
            this.symbol = symbol;
        }

        private String symbol;
    }

    private final Expression left;
    private final BinaryOperator operator;
    private final Expression right;

    public BinaryExpression(Expression left, BinaryOperator operator, Expression right) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public Type getType(Environment env) {
        return get((PrimitiveType) left.getType(env), operator,
                   (PrimitiveType) right.getType(env));
    }

    @Override
    public Environment validate(Environment environment) {
        environment = right.validate(left.validate(environment));

        Type rightType = right.getType(environment);
        Type leftType = left.getType(environment);

        if (!(rightType instanceof PrimitiveType) || !(leftType instanceof PrimitiveType)) {
            throw new IllegalStateException("Binary expressions only operate on primitive types");
        }
        if (get((PrimitiveType) leftType, operator, (PrimitiveType) rightType) == null) {
            throw new IllegalStateException("Binary operator not supported with left and right expressions");
        }
        if (left.containsDeclaration() || right.containsDeclaration()) {
            throw new IllegalStateException("Binary expressions cannot contain declarations");
        }

        return environment;
    }

    @Override
    public String emitExpression(ShaderAccumulator shader) {
        StringBuilder sb = new StringBuilder();
        if (left.getPrecedence() < getPrecedence()) {
            sb.append('(');
            sb.append(left.emitExpression(shader));
            sb.append(')');
        } else {
            sb.append(left.emitExpression(shader));
        }

        sb.append(' ');
        sb.append(operator.symbol);
        sb.append(' ');

        if (right.getPrecedence() < getPrecedence()) {
            sb.append('(');
            sb.append(right.emitExpression(shader));
            sb.append(')');
        } else {
            sb.append(right.emitExpression(shader));
        }

        return sb.toString();
    }

    @Override
    public int getPrecedence() {
        switch (operator) {
        case ADD:
        case SUBTRACT:
            return Precedence.ADDITIVE_EXPRESSIONS.ordinal();
        case MULTIPLY:
        case DIVIDE:
        case MODULO:
            return Precedence.MULTIPLICATIVE_EXPRESSIONS.ordinal();
        case EQUAL:
        case NOT_EQUAL:
            return Precedence.EQUALITY_EXPERSSIONS.ordinal();
        case GREATER_THAN:
        case GREATER_THAN_OR_EQUAL:
        case LESS_THAN:
        case LESS_THAN_OR_EQUAL:
            return Precedence.RELATIONAL_EXPRESSIONS.ordinal();
        case LEFT_SHIFT:
        case RIGHT_SHIFT:
            return Precedence.SHIFT_EXPRESSIONS.ordinal();
        case LOGICAL_AND:
            return Precedence.LOGICAL_AND_EXPRESSIONS.ordinal();
        case LOGICAL_OR:
            return Precedence.LOGICAL_OR_EXPRESSIONS.ordinal();
        case LOGICAL_XOR:
            return Precedence.LOGICAL_XOR_EXPRESSIONS.ordinal();
        default:
            throw new UnsupportedOperationException("Unmapped binary operator, no precedence available");
        }
    }

    private static final Map<BinaryOperator, Map<PrimitiveType, Map<PrimitiveType, PrimitiveType>>> operatorMap = new HashMap<BinaryOperator, Map<PrimitiveType, Map<PrimitiveType, PrimitiveType>>>();

    private static void add(PrimitiveType leftType, BinaryOperator op,
                            PrimitiveType rightType, PrimitiveType resultType) {
        Map<PrimitiveType, Map<PrimitiveType, PrimitiveType>> typeMap = operatorMap.get(op);
        if (typeMap == null) {
            typeMap = new HashMap<PrimitiveType, Map<PrimitiveType, PrimitiveType>>();
            operatorMap.put(op, typeMap);
        }

        Map<PrimitiveType, PrimitiveType> resMap = typeMap.get(leftType);
        if (resMap == null) {
            resMap = new HashMap<PrimitiveType, PrimitiveType>();
            typeMap.put(leftType, resMap);
        }

        resMap.put(rightType, resultType);
    }

    private static PrimitiveType get(PrimitiveType leftType, BinaryOperator op,
                                     PrimitiveType rightType) {
        Map<PrimitiveType, Map<PrimitiveType, PrimitiveType>> typeMap = operatorMap.get(op);
        if (typeMap != null) {
            Map<PrimitiveType, PrimitiveType> resMap = typeMap.get(leftType);
            if (resMap != null) {
                return resMap.get(rightType);
            }

            // for simplicity in operator definition, consider them commutative
            resMap = typeMap.get(rightType);
            if (resMap != null) {
                return resMap.get(leftType);
            }
        }

        return null;
    }

    static {
        // float to float operations
        add(PrimitiveType.FLOAT, BinaryOperator.MULTIPLY, PrimitiveType.FLOAT,
            PrimitiveType.FLOAT);
        add(PrimitiveType.FLOAT, BinaryOperator.DIVIDE, PrimitiveType.FLOAT,
            PrimitiveType.FLOAT);
        add(PrimitiveType.FLOAT, BinaryOperator.ADD, PrimitiveType.FLOAT,
            PrimitiveType.FLOAT);
        add(PrimitiveType.FLOAT, BinaryOperator.SUBTRACT, PrimitiveType.FLOAT,
            PrimitiveType.FLOAT);

        add(PrimitiveType.FLOAT, BinaryOperator.LESS_THAN, PrimitiveType.FLOAT,
            PrimitiveType.BOOL);
        add(PrimitiveType.FLOAT, BinaryOperator.LESS_THAN_OR_EQUAL, PrimitiveType.FLOAT,
            PrimitiveType.BOOL);
        add(PrimitiveType.FLOAT, BinaryOperator.GREATER_THAN, PrimitiveType.FLOAT,
            PrimitiveType.BOOL);
        add(PrimitiveType.FLOAT, BinaryOperator.GREATER_THAN_OR_EQUAL,
            PrimitiveType.FLOAT, PrimitiveType.BOOL);
        add(PrimitiveType.FLOAT, BinaryOperator.EQUAL, PrimitiveType.FLOAT,
            PrimitiveType.BOOL);
        add(PrimitiveType.FLOAT, BinaryOperator.NOT_EQUAL, PrimitiveType.FLOAT,
            PrimitiveType.BOOL);

        // int to int operations
        add(PrimitiveType.INT, BinaryOperator.MULTIPLY, PrimitiveType.INT,
            PrimitiveType.INT);
        add(PrimitiveType.INT, BinaryOperator.DIVIDE, PrimitiveType.INT,
            PrimitiveType.INT);
        add(PrimitiveType.INT, BinaryOperator.ADD, PrimitiveType.INT, PrimitiveType.INT);
        add(PrimitiveType.INT, BinaryOperator.SUBTRACT, PrimitiveType.INT,
            PrimitiveType.INT);

        add(PrimitiveType.INT, BinaryOperator.MODULO, PrimitiveType.INT,
            PrimitiveType.INT);
        add(PrimitiveType.INT, BinaryOperator.LEFT_SHIFT, PrimitiveType.INT,
            PrimitiveType.INT);
        add(PrimitiveType.INT, BinaryOperator.RIGHT_SHIFT, PrimitiveType.INT,
            PrimitiveType.INT);

        add(PrimitiveType.INT, BinaryOperator.LESS_THAN, PrimitiveType.INT,
            PrimitiveType.BOOL);
        add(PrimitiveType.INT, BinaryOperator.LESS_THAN_OR_EQUAL, PrimitiveType.INT,
            PrimitiveType.BOOL);
        add(PrimitiveType.INT, BinaryOperator.GREATER_THAN, PrimitiveType.INT,
            PrimitiveType.BOOL);
        add(PrimitiveType.INT, BinaryOperator.GREATER_THAN_OR_EQUAL, PrimitiveType.INT,
            PrimitiveType.BOOL);
        add(PrimitiveType.INT, BinaryOperator.EQUAL, PrimitiveType.INT,
            PrimitiveType.BOOL);
        add(PrimitiveType.INT, BinaryOperator.NOT_EQUAL, PrimitiveType.INT,
            PrimitiveType.BOOL);

        // boolean operations
        add(PrimitiveType.BOOL, BinaryOperator.LOGICAL_AND, PrimitiveType.BOOL,
            PrimitiveType.BOOL);
        add(PrimitiveType.BOOL, BinaryOperator.LOGICAL_OR, PrimitiveType.BOOL,
            PrimitiveType.BOOL);
        add(PrimitiveType.BOOL, BinaryOperator.LOGICAL_XOR, PrimitiveType.BOOL,
            PrimitiveType.BOOL);
        add(PrimitiveType.BOOL, BinaryOperator.EQUAL, PrimitiveType.BOOL,
            PrimitiveType.BOOL);
        add(PrimitiveType.BOOL, BinaryOperator.NOT_EQUAL, PrimitiveType.BOOL,
            PrimitiveType.BOOL);

        // vec2 operations
        add(PrimitiveType.VEC2, BinaryOperator.MULTIPLY, PrimitiveType.VEC2,
            PrimitiveType.VEC2);
        add(PrimitiveType.VEC2, BinaryOperator.DIVIDE, PrimitiveType.VEC2,
            PrimitiveType.VEC2);
        add(PrimitiveType.VEC2, BinaryOperator.ADD, PrimitiveType.VEC2,
            PrimitiveType.VEC2);
        add(PrimitiveType.VEC2, BinaryOperator.SUBTRACT, PrimitiveType.VEC2,
            PrimitiveType.VEC2);
        add(PrimitiveType.IVEC2, BinaryOperator.MULTIPLY, PrimitiveType.IVEC2,
            PrimitiveType.IVEC2);
        add(PrimitiveType.IVEC2, BinaryOperator.DIVIDE, PrimitiveType.IVEC2,
            PrimitiveType.IVEC2);
        add(PrimitiveType.IVEC2, BinaryOperator.ADD, PrimitiveType.IVEC2,
            PrimitiveType.IVEC2);
        add(PrimitiveType.IVEC2, BinaryOperator.SUBTRACT, PrimitiveType.IVEC2,
            PrimitiveType.IVEC2);
        add(PrimitiveType.BVEC2, BinaryOperator.LOGICAL_AND, PrimitiveType.BVEC2,
            PrimitiveType.BVEC2);
        add(PrimitiveType.BVEC2, BinaryOperator.LOGICAL_OR, PrimitiveType.BVEC2,
            PrimitiveType.BVEC2);
        add(PrimitiveType.BVEC2, BinaryOperator.LOGICAL_XOR, PrimitiveType.BVEC2,
            PrimitiveType.BVEC2);

        add(PrimitiveType.VEC2, BinaryOperator.MULTIPLY, PrimitiveType.FLOAT,
            PrimitiveType.VEC2);
        add(PrimitiveType.VEC2, BinaryOperator.DIVIDE, PrimitiveType.FLOAT,
            PrimitiveType.VEC2);
        add(PrimitiveType.VEC2, BinaryOperator.ADD, PrimitiveType.FLOAT,
            PrimitiveType.VEC2);
        add(PrimitiveType.VEC2, BinaryOperator.SUBTRACT, PrimitiveType.FLOAT,
            PrimitiveType.VEC2);
        add(PrimitiveType.IVEC2, BinaryOperator.MULTIPLY, PrimitiveType.INT,
            PrimitiveType.IVEC2);
        add(PrimitiveType.IVEC2, BinaryOperator.DIVIDE, PrimitiveType.INT,
            PrimitiveType.IVEC2);
        add(PrimitiveType.IVEC2, BinaryOperator.ADD, PrimitiveType.INT,
            PrimitiveType.IVEC2);
        add(PrimitiveType.IVEC2, BinaryOperator.SUBTRACT, PrimitiveType.INT,
            PrimitiveType.IVEC2);
        add(PrimitiveType.BVEC2, BinaryOperator.LOGICAL_AND, PrimitiveType.BOOL,
            PrimitiveType.BVEC2);
        add(PrimitiveType.BVEC2, BinaryOperator.LOGICAL_OR, PrimitiveType.BOOL,
            PrimitiveType.BVEC2);
        add(PrimitiveType.BVEC2, BinaryOperator.LOGICAL_XOR, PrimitiveType.BOOL,
            PrimitiveType.BVEC2);

        add(PrimitiveType.VEC2, BinaryOperator.EQUAL, PrimitiveType.VEC2,
            PrimitiveType.BOOL);
        add(PrimitiveType.VEC2, BinaryOperator.NOT_EQUAL, PrimitiveType.VEC2,
            PrimitiveType.BOOL);
        add(PrimitiveType.IVEC2, BinaryOperator.EQUAL, PrimitiveType.IVEC2,
            PrimitiveType.BOOL);
        add(PrimitiveType.IVEC2, BinaryOperator.NOT_EQUAL, PrimitiveType.IVEC2,
            PrimitiveType.BOOL);
        add(PrimitiveType.BVEC2, BinaryOperator.EQUAL, PrimitiveType.BVEC2,
            PrimitiveType.BOOL);
        add(PrimitiveType.BVEC2, BinaryOperator.NOT_EQUAL, PrimitiveType.BVEC2,
            PrimitiveType.BOOL);

        // vec3 operations
        add(PrimitiveType.VEC3, BinaryOperator.MULTIPLY, PrimitiveType.VEC3,
            PrimitiveType.VEC3);
        add(PrimitiveType.VEC3, BinaryOperator.DIVIDE, PrimitiveType.VEC3,
            PrimitiveType.VEC3);
        add(PrimitiveType.VEC3, BinaryOperator.ADD, PrimitiveType.VEC3,
            PrimitiveType.VEC3);
        add(PrimitiveType.VEC3, BinaryOperator.SUBTRACT, PrimitiveType.VEC3,
            PrimitiveType.VEC3);
        add(PrimitiveType.IVEC3, BinaryOperator.MULTIPLY, PrimitiveType.IVEC3,
            PrimitiveType.IVEC3);
        add(PrimitiveType.IVEC3, BinaryOperator.DIVIDE, PrimitiveType.IVEC3,
            PrimitiveType.IVEC3);
        add(PrimitiveType.IVEC3, BinaryOperator.ADD, PrimitiveType.IVEC3,
            PrimitiveType.IVEC3);
        add(PrimitiveType.IVEC3, BinaryOperator.SUBTRACT, PrimitiveType.IVEC3,
            PrimitiveType.IVEC3);
        add(PrimitiveType.BVEC3, BinaryOperator.LOGICAL_AND, PrimitiveType.BVEC3,
            PrimitiveType.BVEC3);
        add(PrimitiveType.BVEC3, BinaryOperator.LOGICAL_OR, PrimitiveType.BVEC3,
            PrimitiveType.BVEC3);
        add(PrimitiveType.BVEC3, BinaryOperator.LOGICAL_XOR, PrimitiveType.BVEC3,
            PrimitiveType.BVEC3);

        add(PrimitiveType.VEC3, BinaryOperator.MULTIPLY, PrimitiveType.FLOAT,
            PrimitiveType.VEC3);
        add(PrimitiveType.VEC3, BinaryOperator.DIVIDE, PrimitiveType.FLOAT,
            PrimitiveType.VEC3);
        add(PrimitiveType.VEC3, BinaryOperator.ADD, PrimitiveType.FLOAT,
            PrimitiveType.VEC3);
        add(PrimitiveType.VEC3, BinaryOperator.SUBTRACT, PrimitiveType.FLOAT,
            PrimitiveType.VEC3);
        add(PrimitiveType.IVEC3, BinaryOperator.MULTIPLY, PrimitiveType.INT,
            PrimitiveType.IVEC3);
        add(PrimitiveType.IVEC3, BinaryOperator.DIVIDE, PrimitiveType.INT,
            PrimitiveType.IVEC3);
        add(PrimitiveType.IVEC3, BinaryOperator.ADD, PrimitiveType.INT,
            PrimitiveType.IVEC3);
        add(PrimitiveType.IVEC3, BinaryOperator.SUBTRACT, PrimitiveType.INT,
            PrimitiveType.IVEC3);
        add(PrimitiveType.BVEC3, BinaryOperator.LOGICAL_AND, PrimitiveType.BOOL,
            PrimitiveType.BVEC3);
        add(PrimitiveType.BVEC3, BinaryOperator.LOGICAL_OR, PrimitiveType.BOOL,
            PrimitiveType.BVEC3);
        add(PrimitiveType.BVEC3, BinaryOperator.LOGICAL_XOR, PrimitiveType.BOOL,
            PrimitiveType.BVEC3);

        add(PrimitiveType.VEC3, BinaryOperator.EQUAL, PrimitiveType.VEC3,
            PrimitiveType.BOOL);
        add(PrimitiveType.VEC3, BinaryOperator.NOT_EQUAL, PrimitiveType.VEC3,
            PrimitiveType.BOOL);
        add(PrimitiveType.IVEC3, BinaryOperator.EQUAL, PrimitiveType.IVEC3,
            PrimitiveType.BOOL);
        add(PrimitiveType.IVEC3, BinaryOperator.NOT_EQUAL, PrimitiveType.IVEC3,
            PrimitiveType.BOOL);
        add(PrimitiveType.BVEC3, BinaryOperator.EQUAL, PrimitiveType.BVEC3,
            PrimitiveType.BOOL);
        add(PrimitiveType.BVEC3, BinaryOperator.NOT_EQUAL, PrimitiveType.BVEC3,
            PrimitiveType.BOOL);

        // vec4 operations
        add(PrimitiveType.VEC4, BinaryOperator.MULTIPLY, PrimitiveType.VEC4,
            PrimitiveType.VEC4);
        add(PrimitiveType.VEC4, BinaryOperator.DIVIDE, PrimitiveType.VEC4,
            PrimitiveType.VEC4);
        add(PrimitiveType.VEC4, BinaryOperator.ADD, PrimitiveType.VEC4,
            PrimitiveType.VEC4);
        add(PrimitiveType.VEC4, BinaryOperator.SUBTRACT, PrimitiveType.VEC4,
            PrimitiveType.VEC4);
        add(PrimitiveType.IVEC4, BinaryOperator.MULTIPLY, PrimitiveType.IVEC4,
            PrimitiveType.IVEC4);
        add(PrimitiveType.IVEC4, BinaryOperator.DIVIDE, PrimitiveType.IVEC4,
            PrimitiveType.IVEC4);
        add(PrimitiveType.IVEC4, BinaryOperator.ADD, PrimitiveType.IVEC4,
            PrimitiveType.IVEC4);
        add(PrimitiveType.IVEC4, BinaryOperator.SUBTRACT, PrimitiveType.IVEC4,
            PrimitiveType.IVEC4);
        add(PrimitiveType.BVEC4, BinaryOperator.LOGICAL_AND, PrimitiveType.BVEC4,
            PrimitiveType.BVEC4);
        add(PrimitiveType.BVEC4, BinaryOperator.LOGICAL_OR, PrimitiveType.BVEC4,
            PrimitiveType.BVEC4);
        add(PrimitiveType.BVEC4, BinaryOperator.LOGICAL_XOR, PrimitiveType.BVEC4,
            PrimitiveType.BVEC4);

        add(PrimitiveType.VEC4, BinaryOperator.MULTIPLY, PrimitiveType.FLOAT,
            PrimitiveType.VEC4);
        add(PrimitiveType.VEC4, BinaryOperator.DIVIDE, PrimitiveType.FLOAT,
            PrimitiveType.VEC4);
        add(PrimitiveType.VEC4, BinaryOperator.ADD, PrimitiveType.FLOAT,
            PrimitiveType.VEC4);
        add(PrimitiveType.VEC4, BinaryOperator.SUBTRACT, PrimitiveType.FLOAT,
            PrimitiveType.VEC4);
        add(PrimitiveType.IVEC4, BinaryOperator.MULTIPLY, PrimitiveType.INT,
            PrimitiveType.IVEC4);
        add(PrimitiveType.IVEC4, BinaryOperator.DIVIDE, PrimitiveType.INT,
            PrimitiveType.IVEC4);
        add(PrimitiveType.IVEC4, BinaryOperator.ADD, PrimitiveType.INT,
            PrimitiveType.IVEC4);
        add(PrimitiveType.IVEC4, BinaryOperator.SUBTRACT, PrimitiveType.INT,
            PrimitiveType.IVEC4);
        add(PrimitiveType.BVEC4, BinaryOperator.LOGICAL_AND, PrimitiveType.BOOL,
            PrimitiveType.BVEC4);
        add(PrimitiveType.BVEC4, BinaryOperator.LOGICAL_OR, PrimitiveType.BOOL,
            PrimitiveType.BVEC4);
        add(PrimitiveType.BVEC4, BinaryOperator.LOGICAL_XOR, PrimitiveType.BOOL,
            PrimitiveType.BVEC4);

        add(PrimitiveType.VEC4, BinaryOperator.EQUAL, PrimitiveType.VEC4,
            PrimitiveType.BOOL);
        add(PrimitiveType.VEC4, BinaryOperator.NOT_EQUAL, PrimitiveType.VEC4,
            PrimitiveType.BOOL);
        add(PrimitiveType.IVEC4, BinaryOperator.EQUAL, PrimitiveType.IVEC4,
            PrimitiveType.BOOL);
        add(PrimitiveType.IVEC4, BinaryOperator.NOT_EQUAL, PrimitiveType.IVEC4,
            PrimitiveType.BOOL);
        add(PrimitiveType.BVEC4, BinaryOperator.EQUAL, PrimitiveType.BVEC4,
            PrimitiveType.BOOL);
        add(PrimitiveType.BVEC4, BinaryOperator.NOT_EQUAL, PrimitiveType.BVEC4,
            PrimitiveType.BOOL);

        // mat2 operations
        add(PrimitiveType.MAT2, BinaryOperator.MULTIPLY, PrimitiveType.MAT2,
            PrimitiveType.MAT2);
        add(PrimitiveType.MAT2, BinaryOperator.DIVIDE, PrimitiveType.MAT2,
            PrimitiveType.MAT2);
        add(PrimitiveType.MAT2, BinaryOperator.ADD, PrimitiveType.MAT2,
            PrimitiveType.MAT2);
        add(PrimitiveType.MAT2, BinaryOperator.SUBTRACT, PrimitiveType.MAT2,
            PrimitiveType.MAT2);

        add(PrimitiveType.MAT2, BinaryOperator.MULTIPLY, PrimitiveType.FLOAT,
            PrimitiveType.MAT2);
        add(PrimitiveType.MAT2, BinaryOperator.DIVIDE, PrimitiveType.FLOAT,
            PrimitiveType.MAT2);
        add(PrimitiveType.MAT2, BinaryOperator.ADD, PrimitiveType.FLOAT,
            PrimitiveType.MAT2);
        add(PrimitiveType.MAT2, BinaryOperator.SUBTRACT, PrimitiveType.FLOAT,
            PrimitiveType.MAT2);

        add(PrimitiveType.MAT2, BinaryOperator.MULTIPLY, PrimitiveType.VEC2,
            PrimitiveType.VEC2);

        add(PrimitiveType.MAT2, BinaryOperator.EQUAL, PrimitiveType.MAT2,
            PrimitiveType.BOOL);
        add(PrimitiveType.MAT2, BinaryOperator.NOT_EQUAL, PrimitiveType.MAT2,
            PrimitiveType.BOOL);

        // mat3 operations
        add(PrimitiveType.MAT3, BinaryOperator.MULTIPLY, PrimitiveType.MAT3,
            PrimitiveType.MAT3);
        add(PrimitiveType.MAT3, BinaryOperator.DIVIDE, PrimitiveType.MAT3,
            PrimitiveType.MAT3);
        add(PrimitiveType.MAT3, BinaryOperator.ADD, PrimitiveType.MAT3,
            PrimitiveType.MAT3);
        add(PrimitiveType.MAT3, BinaryOperator.SUBTRACT, PrimitiveType.MAT3,
            PrimitiveType.MAT3);

        add(PrimitiveType.MAT3, BinaryOperator.MULTIPLY, PrimitiveType.FLOAT,
            PrimitiveType.MAT3);
        add(PrimitiveType.MAT3, BinaryOperator.DIVIDE, PrimitiveType.FLOAT,
            PrimitiveType.MAT3);
        add(PrimitiveType.MAT3, BinaryOperator.ADD, PrimitiveType.FLOAT,
            PrimitiveType.MAT3);
        add(PrimitiveType.MAT3, BinaryOperator.SUBTRACT, PrimitiveType.FLOAT,
            PrimitiveType.MAT3);

        add(PrimitiveType.MAT3, BinaryOperator.MULTIPLY, PrimitiveType.VEC3,
            PrimitiveType.VEC3);

        add(PrimitiveType.MAT3, BinaryOperator.EQUAL, PrimitiveType.MAT3,
            PrimitiveType.BOOL);
        add(PrimitiveType.MAT3, BinaryOperator.NOT_EQUAL, PrimitiveType.MAT3,
            PrimitiveType.BOOL);

        // mat4 operations
        add(PrimitiveType.MAT4, BinaryOperator.MULTIPLY, PrimitiveType.MAT4,
            PrimitiveType.MAT4);
        add(PrimitiveType.MAT4, BinaryOperator.DIVIDE, PrimitiveType.MAT4,
            PrimitiveType.MAT4);
        add(PrimitiveType.MAT4, BinaryOperator.ADD, PrimitiveType.MAT4,
            PrimitiveType.MAT4);
        add(PrimitiveType.MAT4, BinaryOperator.SUBTRACT, PrimitiveType.MAT4,
            PrimitiveType.MAT4);

        add(PrimitiveType.MAT4, BinaryOperator.MULTIPLY, PrimitiveType.FLOAT,
            PrimitiveType.MAT4);
        add(PrimitiveType.MAT4, BinaryOperator.DIVIDE, PrimitiveType.FLOAT,
            PrimitiveType.MAT4);
        add(PrimitiveType.MAT4, BinaryOperator.ADD, PrimitiveType.FLOAT,
            PrimitiveType.MAT4);
        add(PrimitiveType.MAT4, BinaryOperator.SUBTRACT, PrimitiveType.FLOAT,
            PrimitiveType.MAT4);

        add(PrimitiveType.MAT4, BinaryOperator.MULTIPLY, PrimitiveType.VEC4,
            PrimitiveType.VEC4);

        add(PrimitiveType.MAT4, BinaryOperator.EQUAL, PrimitiveType.MAT4,
            PrimitiveType.BOOL);
        add(PrimitiveType.MAT4, BinaryOperator.NOT_EQUAL, PrimitiveType.MAT4,
            PrimitiveType.BOOL);
    }
}
