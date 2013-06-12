package com.ferox.resource;

/**
 * <p/>
 * DataType represents the supported set of primitive data types that can be transfered to
 * the GPU. Depending on how the data is to be used, some types might be invalid (e.g. a
 * VBO for indices cannot use FLOAT).
 * <p/>
 * For the integral types, signed and unsigned data can be stored using the same data
 * type. The most significant bit that stores sign information in Java is treated by the
 * GPU like any other bit when an unsigned type is required.
 *
 * @author Michael Ludwig
 */
public enum DataType {
    FLOAT(4, true, true),
    HALF_FLOAT(2, true, true),
    INT(4, false, true),
    UNSIGNED_INT(4, false, false),
    NORMALIZED_INT(4, true, true),
    UNSIGNED_NORMALIZED_INT(4, true, false),
    SHORT(2, false, true),
    UNSIGNED_SHORT(2, false, false),
    NORMALIZED_SHORT(2, true, true),
    UNSIGNED_NORMALIZED_SHORT(2, true, false),
    BYTE(1, false, true),
    UNSIGNED_BYTE(1, false, false),
    NORMALIZED_BYTE(1, true, true),
    UNSIGNED_NORMALIZED_BYTE(1, true, false),
    BIT_FIELD(4, false, false);

    private final int byteCount;
    private final boolean decimal;
    private final boolean signed;

    private DataType(int byteCount, boolean decimal, boolean signed) {
        this.byteCount = byteCount;
        this.decimal = decimal;
        this.signed = signed;
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
