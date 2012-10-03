package com.ferox.resource;

/**
 * <p>
 * BufferData represents a block of primitive memory that can be pushed to the
 * GPU to store texture data, vertex data, or more. Each BufferData instance has
 * a fixed length and data type to allow Frameworks to safely allocate internal
 * or GPU storage.
 * <p>
 * BufferData's store their data in Java primitive arrays. Depending on the data
 * type, different array types must be used.
 * <p>
 * BufferData is not thread safe, it is assumed that mutations of a BufferData
 * object will be done within the synchronization block of whatever resource
 * owns it.
 * 
 * @author Michael Ludwig
 */
public class BufferData {
    /**
     * DataType represents the supported set of primitive data types that can be
     * transfered to the GPU. Depending on how the data is to be used, some
     * types might be invalid (e.g. a VBO for indices cannot use FLOAT).
     */
    public static enum DataType {
        /**
         * Each primitive is a 4-byte float, and is stored internally as a Java
         * float array (e.g. <code>float[]</code>).
         */
        FLOAT(4),
        /**
         * <p>
         * Each primitive is an unsigned 4-byte integer and is stored internally
         * as a Java int array (e.g. <code>int[]</code>).
         * <p>
         * Although Java does not support unsigned ints, the GPU will interpret
         * the 32-bit int as unsigned, so values must be assigned appropriately.
         */
        UNSIGNED_INT(4),
        /**
         * <p>
         * Each primitive is an unsigned 2-byte integer and should be stored
         * internally as a Java short array (e.g. <code>short[]</code>).
         * <p>
         * Although Java does not support unsigned shorts, the GPU will
         * interpret the 16-bit short as unsigned, so values must be assigned
         * appropriately.
         */
        UNSIGNED_SHORT(2),
        /**
         * <p>
         * Each primitive is an unsigned byte and should be stored internally as
         * a Java byte array (e.g. <code>byte[]</code>).
         * <p>
         * Although Java does not support unsigned bytes, the GPU will interpret
         * the 8-bit byte as unsigned, so values must be assigned appropriately.
         */
        UNSIGNED_BYTE(1);

        private final int byteCount;

        private DataType(int byteCount) {
            this.byteCount = byteCount;
        }

        /**
         * @return The number of bytes used by each primitive
         */
        public int getByteCount() {
            return byteCount;
        }
    }

    private Object data; // actually a primitive array, of different types
    private final int length;
    private final DataType type;

    // key is an identity specifier that does not prevent the BufferData from
    // being GC'ed, so the Framework can store the keys instead of the actual
    // BufferData when it tracks a resource's state
    private final Object key = new Object();

    /**
     * <p>
     * Create a BufferData that uses the given float[] as its data store. The
     * DataType of the BufferData will be FLOAT, and its length will equal that
     * of the array.
     * <p>
     * Any changes to the primitives in the array will affect the BufferData.
     * Although the array reference can be changed later with
     * {@link #setData(float[])}, the length and type of the BufferData cannot.
     * 
     * @param data The initial float data
     * @throws NullPointerException if data is null
     */
    public BufferData(float[] data) {
        if (data == null) {
            throw new NullPointerException("Array cannot be null");
        }
        this.data = data;
        length = data.length;
        type = DataType.FLOAT;
    }

    /**
     * <p>
     * Create a BufferData that uses the given int[] as its data store. The
     * DataType of the BufferData will be UNSIGNED_INT, and its length will
     * equal that of the array.
     * <p>
     * Any changes to the primitives in the array will affect the BufferData.
     * Although the array reference can be changed later with
     * {@link #setData(int[])}, the length and type of the BufferData cannot.
     * 
     * @param data The initial unsigned int data
     * @throws NullPointerException if data is null
     */
    public BufferData(int[] data) {
        if (data == null) {
            throw new NullPointerException("Array cannot be null");
        }
        this.data = data;
        length = data.length;
        type = DataType.UNSIGNED_INT;
    }

    /**
     * <p>
     * Create a BufferData that uses the given short[] as its data store. The
     * DataType of the BufferData will be UNSIGNED_SHORT, and its length will
     * equal that of the array.
     * <p>
     * Any changes to the primitives in the array will affect the BufferData.
     * Although the array reference can be changed later with
     * {@link #setData(short[])}, the length and type of the BufferData cannot.
     * 
     * @param data The initial unsigned short data
     * @throws NullPointerException if data is null
     */
    public BufferData(short[] data) {
        if (data == null) {
            throw new NullPointerException("Array cannot be null");
        }
        this.data = data;
        length = data.length;
        type = DataType.UNSIGNED_SHORT;
    }

    /**
     * <p>
     * Create a BufferData that uses the given byte[] as its data store. The
     * DataType of the BufferData will be UNSIGNED_BYTE, and its length will
     * equal that of the array.
     * <p>
     * Any changes to the primitives in the array will affect the BufferData.
     * Although the array reference can be changed later with
     * {@link #setData(byte[])}, the length and type of the BufferData cannot.
     * 
     * @param data The initial unsigned byte data
     * @throws NullPointerException if data is null
     */
    public BufferData(byte[] data) {
        if (data == null) {
            throw new NullPointerException("Array cannot be null");
        }
        this.data = data;
        length = data.length;
        type = DataType.UNSIGNED_BYTE;
    }

    /**
     * <p>
     * Create a BufferData with a null internal array, but will be configured to
     * have the given DataType and length. These parameters will control the
     * type of array that can be assigned later when actual data is available.
     * <p>
     * The constructor can be used when the actual buffer data never resides in
     * memory but exists only on the GPU, such as with a framebuffer object.
     * 
     * @param type The data type of this BufferData
     * @param length The length of the buffer data
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if length is less than 1
     */
    public BufferData(DataType type, int length) {
        if (type == null) {
            throw new NullPointerException("DataType cannot be null");
        }
        if (length < 1) {
            throw new IllegalArgumentException("Length must be at least 1, not: " + length);
        }

        this.length = length;
        this.type = type;
        data = null;
    }

    /**
     * <p>
     * Get a simple object whose identity mirrors the reference identity of this
     * BufferData object. The relationship
     * <code>(buffer1 == buffer2) == (buffer1.getKey() == buffer2.getKey())</code>
     * will always be true. Basically, if two keys are the same instance, the
     * producing buffer datas are the same.
     * <p>
     * This is primarily intended for Framework implementations, so they can
     * store the identify of a BufferData used by a resource without preventing
     * the BufferData from being garbage collected.
     * 
     * @return An identity key for this data instance
     */
    public Object getKey() {
        return key;
    }

    /**
     * Get the length or size of this BufferData. All arrays stored by this
     * buffer data will have this length. This allows BufferData to describe an
     * effective block of memory but have the actual array underneath change as
     * needed.
     * 
     * @return The size of this BufferData
     */
    public int getLength() {
        return length;
    }

    /**
     * Get the data type of this BufferData. After creation, the data type
     * cannot be changed. This allows the BufferData to describe the data type
     * stored in its effective memory block but have the actual array underneath
     * change as needed.
     * 
     * @return The data type of this BufferData
     */
    public DataType getDataType() {
        return type;
    }

    /**
     * <p>
     * Get the current data array that this BufferData wraps. If the returned
     * array is not null, its length will equal the return value of
     * {@link #getLength()}.
     * <p>
     * This method uses an unchecked generic cast to make it more convenient to
     * get at the array. Care must be given to use the proper type parameter
     * based on the data type of the buffer data.
     * <ul>
     * <li>If T = float[], then data type must be FLOAT</li>
     * <li>If T = int[], then data type must be UNSIGNED_INT</li>
     * <li>If T = short[], then data type must be UNSIGNED_SHORT</li>
     * <li>If T = byte[], then data type must be UNSIGNED_BYTE</li>
     * </ul>
     * 
     * @return The backing data array, which might be null
     * @throws ClassCastException if the improper array type is expected
     */
    @SuppressWarnings("unchecked")
    public <T> T getArray() {
        return (T) data;
    }

    /**
     * Set the data this BufferData wraps to the given array. The given array
     * must have a length equal to the value returned by {@link #getLength()}.
     * The data type of this BufferData must be FLOAT.
     * 
     * @param data The new float array
     * @throws IllegalStateException if this BufferData's type is not FLOAT
     * @throws IllegalArgumentException if the array length is not equal to the
     *             buffer's length
     */
    public void setData(float[] data) {
        setData(data, (data == null ? length : data.length), DataType.FLOAT);
    }

    /**
     * Set the data this BufferData wraps to the given array. The given array
     * must have a length equal to the value returned by {@link #getLength()}.
     * The data type of this BufferData must be UNSIGNED_INT.
     * 
     * @param data The new int array
     * @throws IllegalStateException if this BufferData's type is not
     *             UNSIGNED_INT
     * @throws IllegalArgumentException if the array length is not equal to the
     *             buffer's length
     */
    public void setData(int[] data) {
        setData(data, (data == null ? length : data.length), DataType.UNSIGNED_INT);
    }

    /**
     * Set the data this BufferData wraps to the given array. The given array
     * must have a length equal to the value returned by {@link #getLength()}.
     * The data type of this BufferData must be UNSIGNED_SHORT.
     * 
     * @param data The new short array
     * @throws IllegalStateException if this BufferData's type is not
     *             UNSIGNED_SHORT
     * @throws IllegalArgumentException if the array length is not equal to the
     *             buffer's length
     */
    public void setData(short[] data) {
        setData(data, (data == null ? length : data.length), DataType.UNSIGNED_SHORT);
    }

    /**
     * Set the data this BufferData wraps to the given array. The given array
     * must have a length equal to the value returned by {@link #getLength()}.
     * The data type of this BufferData must be UNSIGNED_BYTE.
     * 
     * @param data The new byte array
     * @throws IllegalStateException if this BufferData's type is not
     *             UNSIGNED_BYTE
     * @throws IllegalArgumentException if the array length is not equal to the
     *             buffer's length
     */
    public void setData(byte[] data) {
        setData(data, (data == null ? length : data.length), DataType.UNSIGNED_BYTE);
    }

    private void setData(Object array, int arrayLength, DataType expectedType) {
        if (type != expectedType) {
            throw new IllegalStateException("BufferData's type is " + type + ", but " + expectedType + " is required");
        }
        if (arrayLength != length) {
            throw new IllegalArgumentException("Incorrect array length, must be " + length + ", by is " + arrayLength);
        }

        data = array;
    }
}
