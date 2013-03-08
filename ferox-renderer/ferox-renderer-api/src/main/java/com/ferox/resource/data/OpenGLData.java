package com.ferox.resource.data;

/**
 * <p/>
 * OpenGLData represents a block of primitive memory that can be pushed to the GPU to
 * store texture data, vertex data, or more. Each OpenGLData instance has a fixed length
 * and backing data store.
 * <p/>
 * The backing data store depends on the specific OpenGL implementation. The classes
 * provided use primitive arrays but this interface is also compatible with NIO buffers.
 * Framework implementations may have optimized code paths for using the existing data
 * types.
 * <p/>
 * OpenGLData instances are not thread safe, it is assumed that mutations of an OpenGLData
 * object will be done within the synchronization block of whatever resource owns it.
 *
 * @param <T> The data type that the OpenGLData instance wraps
 *
 * @author Michael Ludwig
 */
public interface OpenGLData<T> {
    /**
     * Get the underlying data wrapped by this OpenGLData instance. This will always
     * return the same array, buffer, etc. for a given OpenGLData object. Its length,
     * however represented, will equal {@link #getLength()}.
     *
     * @return The underlying data for this instance
     */
    public T get();

    /**
     * Get the length of this OpenGLData. This will be a constant value for a given
     * instance. The length is the number of primitives held by the data, and not the
     * number of bytes.
     *
     * @return The number of primitives stored in this data instance
     */
    public int getLength();

    /**
     * Get the data type of this OpenGLData. This will be a constant value for a given
     * instance. The data type determines the underlying Java primitive, but OpenGLData
     * implementations may interpret that data differently (such as signed versus unsigned
     * integers).
     *
     * @return The data type of this OpenGLData
     */
    public DataType getType();

    /**
     * <p/>
     * Get a simple object whose identity mirrors the reference identity of this
     * OpenGLData object. The relationship {@code (data1 == data2) == (data1.getKey() ==
     * data2.getKey())} will always be true. If two keys are the same instance, the
     * producing BufferData objects are the same instance.
     * <p/>
     * This is primarily intended for Framework implementations, so they can store the
     * identify of a OpenGLData used by a resource without preventing the OpenGLData from
     * being garbage collected.
     *
     * @return An identity key for this data instance
     */
    public Object getKey();
}
