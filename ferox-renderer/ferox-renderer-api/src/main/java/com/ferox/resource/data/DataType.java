package com.ferox.resource.data;

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
    /**
     * Primitive data is stored in a <code>float[]</code>.
     */
    FLOAT(4),
    /**
     * Primitive data is stored in a <code>int[]</code>. The 32-bit values may be
     * interpreted as signed or unsigned depending on the type of buffer.
     */
    INT(4),
    /**
     * Primitive data is stored in a <code>short[]</code>.   The 16-bit values may be
     * interpreted as signed or unsigned depending on the type of buffer.
     */
    SHORT(2),
    /**
     * Primitive data is stored in a <code>byte[]</code>. The 8-bit values may be
     * interpreted as signed or unsigned depending on the type of buffer.
     */
    BYTE(1);

    private final int byteCount;

    DataType(int byteCount) {
        this.byteCount = byteCount;
    }

    /**
     * @return The number of bytes used by each primitive
     */
    public int getByteCount() {
        return byteCount;
    }
}
