package com.ferox.resource.shader;

public enum PrimitiveType implements Type {
    VOID,
    FLOAT,
    INT,
    BOOL,
    VEC2,
    VEC3,
    VEC4,
    BVEC2,
    BVEC3,
    BVEC4,
    IVEC2,
    IVEC3,
    IVEC4,
    MAT2,
    MAT3,
    MAT4,
    SAMPLER1D,
    SAMPLER2D,
    SAMPLER3D,
    SAMPLERCUBE,
    SAMPLER1DSHADOW,
    SAMPLER2DSHADOW;

    @Override
    public String getTypeIdentifier() {
        return name().toLowerCase();
    }
}
