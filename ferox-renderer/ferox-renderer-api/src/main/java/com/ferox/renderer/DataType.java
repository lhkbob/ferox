package com.ferox.renderer;

/**
 * DataType represents the supported set of primitive data types that can be transfered to
 * the GPU. The type system leveraged by GPUs is more extensive than what is available in
 * Java. To account for this, common data interpretations are specified in this enum to
 * map from Java's primitives to the majority of types needed. If the GPU requires a more
 * complex type, the {@link DataType#INT_BIT_FIELD} value represents arbitrary 32-bit
 * patterns.
 *
 * @author Michael Ludwig
 */
public enum DataType {
    FLOAT(4, float.class, true, true),
    HALF_FLOAT(2, short.class, true, true),
    INT(4, int.class, false, true),
    UNSIGNED_INT(4, int.class, false, false),
    NORMALIZED_INT(4, int.class, true, true),
    UNSIGNED_NORMALIZED_INT(4, int.class, true, false),
    SHORT(2, short.class, false, true),
    UNSIGNED_SHORT(2, short.class, false, false),
    NORMALIZED_SHORT(2, short.class, true, true),
    UNSIGNED_NORMALIZED_SHORT(2, short.class, true, false),
    BYTE(1, byte.class, false, true),
    UNSIGNED_BYTE(1, byte.class, false, false),
    NORMALIZED_BYTE(1, byte.class, true, true),
    UNSIGNED_NORMALIZED_BYTE(1, byte.class, true, false),
    INT_BIT_FIELD(4, int.class, false, false);

    private final int byteCount;
    private final boolean decimal;
    private final boolean signed;
    private final Class<?> primitive;

    private DataType(int byteCount, Class<?> primitive, boolean decimal, boolean signed) {
        this.byteCount = byteCount;
        this.decimal = decimal;
        this.signed = signed;
        this.primitive = primitive;
    }

    /**
     * Get the Java primitive type that holds the data. This will be the class
     * representing {@code float}, {@code byte}, {@code short}, {@code int}. The actual
     * interpretation of the bits in the Java primitive may be different depending on how
     * the type is defined.
     *
     * @return The java primitive for this more complex data type
     */
    public Class<?> getJavaPrimitive() {
        return primitive;
    }

    /**
     * @return True if the data type can represent both positive and negative numbers,
     *         false implies positive only
     */
    public boolean isSigned() {
        return signed;
    }

    /**
     * @return True if the data can represent decimal, non-integer numbers
     */
    public boolean isDecimalNumber() {
        return decimal;
    }

    /**
     * @return The number of bytes used by each primitive
     */
    public int getByteCount() {
        return byteCount;
    }
}
