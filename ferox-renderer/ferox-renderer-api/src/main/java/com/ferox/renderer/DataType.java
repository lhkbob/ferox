package com.ferox.renderer;

/**
 * DataType represents the supported set of primitive data types that can be transferred to the GPU. The type
 * system leveraged by GPUs is more extensive than what is available in Java. To account for this, common data
 * interpretations are specified in this enum to map from Java's primitives to the majority of types needed.
 * If the GPU requires a more complex type, the {@link DataType#INT_BIT_FIELD} value represents arbitrary
 * 32-bit patterns.
 *
 * @author Michael Ludwig
 */
public enum DataType {
    /**
     * The 4 bytes are treated as the IEEE floating point standard, e.g. the same as Java.
     */
    FLOAT(4, float.class, true, true, false),
    /**
     * The 2 bytes are treated as the OpenGL 16-bit half-float with 1 bit for sign, 5 bits for exponent, and
     * 10 bits for a mantissa.
     */
    HALF_FLOAT(2, short.class, true, true, false),
    /**
     * The 4 bytes are treated as the standard two's-complement signed 32-bit integer, e.g. the same as Java.
     */
    INT(4, int.class, false, true, false),
    /**
     * The 4 bytes are treated as a one's-complement 32-bit unsigned integer. The sign-bit in the Java
     * representation is treated as the most significant bit instead of a sign.
     */
    UNSIGNED_INT(4, int.class, false, false, false),
    /**
     * The 4 bytes are evaluated identically to INT, and then normalized to the decimal range [-1.0, 1.0].
     */
    NORMALIZED_INT(4, int.class, true, true, true),
    /**
     * The 4 bytes are evaluated identically to UNSIGNED_INT, and then normalized to the decimal range [0.0,
     * 1.0].
     */
    UNSIGNED_NORMALIZED_INT(4, int.class, true, false, true),
    /**
     * The 2 bytes are evaluated as the standard two's-complement signed 16-bit integer, e.g. the same as
     * Java.
     */
    SHORT(2, short.class, false, true, false),
    /**
     * The 2 bytes are evaluated as a one's complement 16-bit unsigned integer. The sign-bit in the Java
     * representation is treated as the most significant bit.
     */
    UNSIGNED_SHORT(2, short.class, false, false, false),
    /**
     * The 2 bytes are evaluated identically to SHORT, and then normalized to the decimal range [-1.0, 1.0].
     */
    NORMALIZED_SHORT(2, short.class, true, true, true),
    /**
     * The 2 bytes are evaluated identically to UNSIGNED_SHORT, and then normalized to the decimal range [0.0,
     * 1.0].
     */
    UNSIGNED_NORMALIZED_SHORT(2, short.class, true, false, true),
    /**
     * The single byte is evaluated as the standard two's-complement signed 8-bit integer, e.g. the same as
     * Java.
     */
    BYTE(1, byte.class, false, true, false),
    /**
     * The single byte is evaluated as a one's complement 8-bit unsigned integer. The sign-bit in the Java
     * representation is treated as the most significant bit.
     */
    UNSIGNED_BYTE(1, byte.class, false, false, false),
    /**
     * The single byte is evaluated identically to BYTE, and then normalized to the decimal range [-1.0,
     * 1.0].
     */
    NORMALIZED_BYTE(1, byte.class, true, true, true),
    /**
     * The single byte is evaluated identically to UNSIGNED_BYTE, and then normalized to the decimal range
     * [0.0, 1.0].
     */
    UNSIGNED_NORMALIZED_BYTE(1, byte.class, true, false, true),
    /**
     * The 4 bytes are interpreted in a context-specific manner. The 32-bit Java int is merely a vehicle to
     * deliver those bits to OpenGL. This is most common with packed texture formats such as DEPTH_STENCIL
     * where 24 bits are the depth, and 8 are the stencil; another example is the ARGB texture format with 8
     * bits per component packed into a single integer.
     */
    INT_BIT_FIELD(4, int.class, false, false, false);

    private final int byteCount;
    private final boolean decimal;
    private final boolean signed;
    private final boolean normalized;
    private final Class<?> primitive;

    private DataType(int byteCount, Class<?> primitive, boolean decimal, boolean signed, boolean normalized) {
        this.byteCount = byteCount;
        this.decimal = decimal;
        this.signed = signed;
        this.primitive = primitive;
        this.normalized = normalized;
    }

    /**
     * Get the Java primitive type that holds the data. This will be the class representing {@code float},
     * {@code byte}, {@code short}, {@code int}. The actual interpretation of the bits in the Java primitive
     * may be different depending on how the type is defined.
     *
     * @return The java primitive for this more complex data type
     */
    public Class<?> getJavaPrimitive() {
        return primitive;
    }

    /**
     * @return True if the data type can represent both positive and negative numbers, false implies positive
     *         only
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
     * @return True if the decimal number is a normalized integer. This is always false if {@link
     *         #isDecimalNumber()} returns false.
     */
    public boolean isNormalized() {
        return normalized;
    }

    /**
     * @return The number of bytes used by each primitive
     */
    public int getByteCount() {
        return byteCount;
    }
}
