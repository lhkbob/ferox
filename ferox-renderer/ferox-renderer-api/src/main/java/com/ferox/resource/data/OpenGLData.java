package com.ferox.resource.data;

/**
 * <p/>
 * BufferData represents a block of primitive memory that can be pushed to the GPU to
 * store texture data, vertex data, or more. Each BufferData instance has a fixed length.
 * <p/>
 * BufferData's store their data in Java primitive arrays. Depending on the data type,
 * different array types must be used. The concrete subclasses of BufferData will expose
 * methods to interact with the arrays.
 * <p/>
 * BufferData is not thread safe, it is assumed that mutations of a BufferData object will
 * be done within the synchronization block of whatever resource owns it.
 *
 * @author Michael Ludwig
 */
// FIXME fix references to BufferData
public interface OpenGLData<T> {
    public T get();

    /**
     * Get the length of this BufferData. All arrays stored by this buffer data will have
     * this length. This allows BufferData to describe an effective block of memory but
     * have the actual array instance change as needed.
     *
     * @return The size of this BufferData
     */
    public int length();

    /**
     * Get the data type of this BufferData. After creation, the data type cannot be
     * changed. This allows the BufferData to describe the data type stored in its
     * effective memory block but have the actual array instance change as needed.
     *
     * @return The data type of this BufferData
     */
    public DataType type();

    /**
     * <p/>
     * Get a simple object whose identity mirrors the reference identity of this
     * BufferData object. The relationship <code>(buffer1 == buffer2) == (buffer1.getKey()
     * == buffer2.getKey())</code> will always be true. If two keys are the same instance,
     * the producing BufferData objects are the same instance.
     * <p/>
     * This is primarily intended for Framework implementations, so they can store the
     * identify of a BufferData used by a resource without preventing the BufferData from
     * being garbage collected.
     *
     * @return An identity key for this data instance
     */
    public Object key();
}
