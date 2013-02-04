package com.ferox.resource.shader.simple_grammar;

import com.ferox.resource.shader.ArrayType;
import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.PrimitiveType;
import com.ferox.resource.shader.Type;

public class ArrayAccess extends AbstractLValue {
    private final Expression array;
    private final Expression index; // int_expression

    public ArrayAccess(Expression array, Expression index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public Type getType(Environment env) {
        Type type = array.getType(env);
        if (type.equals(PrimitiveType.BVEC2) || type.equals(PrimitiveType.BVEC3) || type.equals(PrimitiveType.BVEC4)) {
            return PrimitiveType.BOOL;
        } else if (type.equals(PrimitiveType.IVEC2) || type.equals(PrimitiveType.IVEC3) || type.equals(PrimitiveType.IVEC4)) {
            return PrimitiveType.INT;
        } else if (type.equals(PrimitiveType.VEC2) || type.equals(PrimitiveType.VEC3) || type.equals(PrimitiveType.VEC4)) {
            return PrimitiveType.FLOAT;
        } else if (type.equals(PrimitiveType.MAT2)) {
            return PrimitiveType.VEC2;
        } else if (type.equals(PrimitiveType.MAT3)) {
            return PrimitiveType.VEC3;
        } else if (type.equals(PrimitiveType.MAT4)) {
            return PrimitiveType.VEC4;
        }
        // assume it's an array
        return ((ArrayType) array.getType(env)).getComponentType();
    }

    @Override
    public Environment validate(Environment environment) {
        environment = array.validate(index.validate(environment));
        Type type = array.getType(environment);

        if (type instanceof PrimitiveType) {
            switch ((PrimitiveType) type) {
            case BVEC2:
            case BVEC3:
            case BVEC4:
            case IVEC2:
            case IVEC3:
            case IVEC4:
            case VEC2:
            case VEC3:
            case VEC4:
            case MAT2:
            case MAT3:
            case MAT4:
                // array-accessible primitive types
                break;
            default:
                throw new IllegalStateException("Primitive type does not support array access");
            }
        } else if (!(array.getType(environment) instanceof ArrayType)) {
            throw new IllegalStateException("Expression does not evaluate to an array type");
        }

        if (!index.getType(environment).equals(PrimitiveType.INT)) {
            throw new IllegalStateException("Index expression does not evaluate to an integer type");
        }
        if (array.containsDeclaration() || index.containsDeclaration()) {
            throw new IllegalStateException("Array and index expressions cannot contain variable declarations");
        }
        return environment;
    }

    @Override
    public String emitExpression() {
        if (array.getPrecedence() < getPrecedence()) {
            return "(" + array.emitExpression() + ")[" + index.emitExpression() + "]";
        } else {
            return array.emitExpression() + "[" + index.emitExpression() + "]";
        }
    }

    @Override
    public int getPrecedence() {
        return Precedence.POSTFIX_EXPRESSIONS.ordinal();
    }
}
