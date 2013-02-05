package com.ferox.resource.shader.simple_grammar;

import java.util.regex.Pattern;

import com.ferox.resource.shader.Environment;
import com.ferox.resource.shader.Expression;
import com.ferox.resource.shader.PrimitiveType;
import com.ferox.resource.shader.ShaderAccumulator;
import com.ferox.resource.shader.Struct;
import com.ferox.resource.shader.Type;

public class FieldSelection extends AbstractLValue {
    private static final Pattern VEC2_ACCESS = Pattern.compile("([xy]([xy])?)|([rg]([rg])?)|([st]([st])?)");
    private static final Pattern VEC3_ACCESS = Pattern.compile("([xyz]([xyz])?([xyz])?)|([rgb]([rgb])?([rgb])?)|([stp]([stp])?([stp])?)");
    private static final Pattern VEC4_ACCESS = Pattern.compile("([xyzw]([xyzw])?([xyzw])?([xyzw])?)|([rgba]([rgba])?([rgba])?([rgba])?)|([stpq]([stpq])?([stpq])?([stpq])?)");

    private final Expression variable;
    private final String field;

    public FieldSelection(Expression variable, String field) {
        this.variable = variable;
        this.field = field;
    }

    @Override
    public Type getType(Environment env) {
        Type base = variable.getType(env);
        if (base instanceof Struct) {
            return ((Struct) base).getFields().get(field);
        } else if (base instanceof PrimitiveType) {
            switch ((PrimitiveType) base) {
            case VEC2:
                if (VEC2_ACCESS.matcher(field).matches()) {
                    return (field.length() == 1 ? PrimitiveType.FLOAT : PrimitiveType.VEC2);
                }
                break;
            case IVEC2:
                if (VEC2_ACCESS.matcher(field).matches()) {
                    return (field.length() == 1 ? PrimitiveType.INT : PrimitiveType.IVEC2);
                }
                break;
            case BVEC2:
                if (VEC2_ACCESS.matcher(field).matches()) {
                    return (field.length() == 1 ? PrimitiveType.BOOL : PrimitiveType.BVEC2);
                }
                break;
            case VEC3:
                if (VEC3_ACCESS.matcher(field).matches()) {
                    return (field.length() == 1 ? PrimitiveType.FLOAT : (field.length() == 2 ? PrimitiveType.VEC2 : PrimitiveType.VEC3));
                }
                break;
            case IVEC3:
                if (VEC3_ACCESS.matcher(field).matches()) {
                    return (field.length() == 1 ? PrimitiveType.INT : (field.length() == 2 ? PrimitiveType.IVEC2 : PrimitiveType.IVEC3));
                }
                break;
            case BVEC3:
                if (VEC3_ACCESS.matcher(field).matches()) {
                    return (field.length() == 1 ? PrimitiveType.BOOL : (field.length() == 2 ? PrimitiveType.BVEC2 : PrimitiveType.BVEC3));
                }
                break;
            case VEC4:
                if (VEC4_ACCESS.matcher(field).matches()) {
                    return (field.length() == 1 ? PrimitiveType.FLOAT : (field.length() == 2 ? PrimitiveType.VEC2 : (field.length() == 3 ? PrimitiveType.VEC3 : PrimitiveType.VEC4)));
                }
                break;
            case IVEC4:
                if (VEC4_ACCESS.matcher(field).matches()) {
                    return (field.length() == 1 ? PrimitiveType.INT : (field.length() == 2 ? PrimitiveType.IVEC2 : (field.length() == 3 ? PrimitiveType.IVEC3 : PrimitiveType.IVEC4)));
                }
                break;
            case BVEC4:
                if (VEC4_ACCESS.matcher(field).matches()) {
                    return (field.length() == 1 ? PrimitiveType.BOOL : (field.length() == 2 ? PrimitiveType.BVEC2 : (field.length() == 3 ? PrimitiveType.BVEC3 : PrimitiveType.BVEC4)));
                }
                break;
            default:
                return null;
            }
        }
        return null;
    }

    @Override
    public Environment validate(Environment environment) {
        environment = variable.validate(environment);
        Type base = variable.getType(environment);

        if (base instanceof Struct) {
            if (((Struct) base).getFields().get(field) == null) {
                throw new IllegalStateException("Field does not exist in struct definition");
            }
        } else if (base instanceof PrimitiveType) {
            switch ((PrimitiveType) base) {
            case VEC2:
            case IVEC2:
            case BVEC2:
                if (!VEC2_ACCESS.matcher(field).matches()) {
                    throw new IllegalStateException("Illegal swizzle for 2-component vector");
                }
                break;
            case VEC3:
            case IVEC3:
            case BVEC3:
                if (!VEC3_ACCESS.matcher(field).matches()) {
                    throw new IllegalStateException("Illegal swizzle for 3-component vector");
                }
                break;
            case VEC4:
            case IVEC4:
            case BVEC4:
                if (!VEC4_ACCESS.matcher(field).matches()) {
                    throw new IllegalStateException("Illegal swizzle for 4-component vector");
                }
                break;
            default:
                throw new IllegalStateException("Primitive type does not support field access");
            }
        }

        return environment;
    }

    @Override
    public String emitExpression(ShaderAccumulator shader) {
        if (variable.getPrecedence() < getPrecedence()) {
            return "(" + variable.emitExpression(shader) + ")." + field;
        } else {
            return variable.emitExpression(shader) + "." + field;
        }
    }

    @Override
    public int getPrecedence() {
        return Precedence.POSTFIX_EXPRESSIONS.ordinal();
    }
}
