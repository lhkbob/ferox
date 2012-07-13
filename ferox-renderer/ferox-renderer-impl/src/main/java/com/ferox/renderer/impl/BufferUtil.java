package com.ferox.renderer.impl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.ferox.resource.BufferData;
import com.ferox.resource.BufferData.DataType;

/**
 * BufferUtil is a utility class for creating NIO Buffer objects. All created
 * Buffer objects are direct buffers with the native byte ordering.
 * 
 * @author Michael Ludwig
 */
public class BufferUtil {
    /**
     * Create a new FloatBuffer of the given capacity.
     * 
     * @param size The capacity of the returned buffer
     * @return A new direct FloatBuffer
     */
    public static FloatBuffer newFloatBuffer(int size) {
        return newByteBuffer(size * DataType.FLOAT.getByteCount()).asFloatBuffer();
    }
    
    /**
     * Create a new IntBuffer of the given capacity.
     * 
     * @param size The capacity of the returned buffer
     * @return A new direct IntBuffer
     */
    public static IntBuffer newIntBuffer(int size) {
        return newByteBuffer(size * DataType.UNSIGNED_INT.getByteCount()).asIntBuffer();
    }
    
    /**
     * Create a new ShortBuffer of the given capacity.
     * 
     * @param size The capacity of the returned buffer
     * @return A new direct ShortBuffer
     */
    public static ShortBuffer newShortBuffer(int size) {
        return newByteBuffer(size * DataType.UNSIGNED_SHORT.getByteCount()).asShortBuffer();
    }
    
    /**
     * Create a new ByteBuffer of the given capacity.
     * 
     * @param size The capacity of the returned buffer
     * @return A new direct ByteBuffer
     */
    public static ByteBuffer newByteBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    /**
     * Create a new FloatBuffer that will have the same capacity as the length
     * of the given array, and its contents will be equal the array. The
     * returned buffer will have its position at 0 and limit at the capacity.
     * 
     * @param data The float[] that fills the returned buffer
     * @return A new direct FloatBuffer
     * @throws NullPointerException if data is null
     */
    public static FloatBuffer newFloatBuffer(float[] data) {
        FloatBuffer buffer = newFloatBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }

    /**
     * Create a new IntBuffer that will have the same capacity as the length of
     * the given array, and its contents will be equal the array. The returned
     * buffer will have its position at 0 and limit at the capacity.
     * 
     * @param data The int[] that fills the returned buffer
     * @return A new direct IntBuffer
     * @throws NullPointerException if data is null
     */
    public static IntBuffer newIntBuffer(int[] data) {
        IntBuffer buffer = newIntBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }

    /**
     * Create a new ShortBuffer that will have the same capacity as the length
     * of the given array, and its contents will be equal the array. The
     * returned buffer will have its position at 0 and limit at the capacity.
     * 
     * @param data The short[] that fills the returned buffer
     * @return A new direct ShortBuffer
     * @throws NullPointerException if data is null
     */
    public static ShortBuffer newShortBuffer(short[] data) {
        ShortBuffer buffer = newShortBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }

    /**
     * Create a new ByteBuffer that will have the same capacity as the length of
     * the given array, and its contents will be equal the array. The returned
     * buffer will have its position at 0 and limit at the capacity.
     * 
     * @param data The byte[] that fills the returned buffer
     * @return A new direct ByteBuffer
     * @throws NullPointerException if data is null
     */
    public static ByteBuffer newByteBuffer(byte[] data) {
        ByteBuffer buffer = newByteBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }

    /**
     * Create a new Buffer based on the given {@link DataType} and size. The
     * returned buffer will have a capacity equal to <tt>size</tt> and the
     * returned buffer type will equal the Class returned by
     * {@link #getBufferType(DataType)}.
     * 
     * @param type The DataType controlling the return type of the Buffer
     * @param size The capacity of the buffer
     * @return A new direct buffer, suitable for holding data of the give type
     * @throws NullPointerException if type is null
     */
    public static Buffer newBuffer(DataType type, int size) {
        switch(type) {
        case FLOAT:
            return newFloatBuffer(size);
        case UNSIGNED_BYTE:
            return newByteBuffer(size);
        case UNSIGNED_INT:
            return newIntBuffer(size);
        case UNSIGNED_SHORT:
            return newShortBuffer(size);
        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Create a new Buffer that matches the DataType of the provided BufferData.
     * The created buffer will have a capacity matching the size of the
     * BufferData. If the BufferData has a non-null array, the buffer will have
     * the array copied into it. The returned buffer's position will be 0 and
     * its limit will be at its capacity.
     * 
     * @param data The BufferData to clone into an NIO buffer
     * @return A new direct Buffer
     * @throws NullPointerException if data is null
     */
    public static Buffer newBuffer(BufferData data) {
        switch(data.getDataType()) {
        case FLOAT:
            float[] fd = data.getArray();
            return (fd == null ? newFloatBuffer(data.getLength()) : newFloatBuffer(fd));
        case UNSIGNED_BYTE:
            byte[] bd = data.getArray();
            return (bd == null ? newByteBuffer(data.getLength()) : newByteBuffer(bd));
        case UNSIGNED_SHORT:
            short[] sd = data.getArray();
            return (sd == null ? newShortBuffer(data.getLength()) : newShortBuffer(sd));
        case UNSIGNED_INT:
            int[] id = data.getArray();
            return (id == null ? newIntBuffer(data.getLength()) : newIntBuffer(id));
        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * Return the Class of Buffer that will be created by
     * {@link #newBuffer(BufferData)} and {@link #newBuffer(DataType, int)}
     * based on the given DataType. A DataType of FLOAT creates FloatBuffers; a
     * DataType of UNSIGNED_BYTE creates ByteBuffers; a type of UNSIGNED_INT
     * creates IntBuffers; and a type of UNSIGNED_SHORT creates ShortBuffers.
     * 
     * @param type The DataType
     * @return The class of buffer matching the given DataType
     * @throws NullPointerException if type is null
     */
    public static Class<? extends Buffer> getBufferType(DataType type) {
        switch(type) {
        case FLOAT: return FloatBuffer.class;
        case UNSIGNED_BYTE: return ByteBuffer.class;
        case UNSIGNED_INT: return IntBuffer.class;
        case UNSIGNED_SHORT: return ShortBuffer.class;
        }
        
        return null; // won't happen
    }
}
