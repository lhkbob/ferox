package com.ferox.resource;

/**
 * <p>
 * VertexBufferObject is a Resource that represents the concept of a
 * vertex-array in OpenGL. Depending on the {@link StorageMode} of a created
 * VertexBufferObject, the resource's data will live in memory or on the
 * graphics (as an actual VBO).
 * </p>
 * <p>
 * VertexBufferObjects are used to store vertex attributes such as texture
 * coordinates, normals, or vertices and they store the indices accessed when
 * rendering an indexed geometry. The data placed in a vbo is extremely flexible
 * and it is possible to pack multiple attributes into a single resource
 * instance. The type of data, however, will limit how the vbo can be used by
 * the render.
 * </p>
 * <p>
 * If the array contained in the VBO's BufferData is null, it should allocate
 * internal resources as needed but not change any existing data. This functions
 * similarly to the null data handling for {@link Texture}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class VertexBufferObject extends Resource {
    /**
     * StorageMode represents the various ways that a Framework can 'compile' a
     * Geometry resource into something that it can use when rendering them.
     * There are currently three types, each progressing along the spectrum from
     * faster updates to faster rendering performance.
     */
    public static enum StorageMode {
        /**
         * No data is stored on the graphics card. This means that updates are
         * generally very fast (although a copy is necessary). Unfortunately
         * rendering is slower because the data must be resent each render.
         */
        IN_MEMORY,
        /**
         * The Geometry data is stored on the graphics card in specialized
         * memory designed to be updated frequently. This means that, although
         * slower than IN_MEMORY, the updates are faster than RESIDENT_DYNAMIC.
         * Because it's on the graphics card, rendering times should be faster
         * compared to IN_MEMORY.
         */
        GPU_DYNAMIC,
        /**
         * Geometry data is stored on the graphics card in memory designed for
         * fast read access. This allows rendering to be the fastest, but
         * updates are slower.
         */
        GPU_STATIC
    }
    
    private BufferData data;
    private StorageMode storageMode;
    private final BulkChangeQueue<DataRange> changeQueue;

    /**
     * Create a new VertexBufferObject that uses the given BufferData and a
     * StorageMode of GPU_STATIC.
     * 
     * @param data The initial BufferData
     * @throws NullPointerException if data is null
     */
    public VertexBufferObject(BufferData data) {
        this(data, StorageMode.GPU_STATIC);
    }

    /**
     * Create a new VertexBufferObject that uses the given BufferData and the
     * given StorageMode. The StorageMode cannot change after the vbo is
     * created.
     * 
     * @param data The initial BufferData
     * @param storageMode The StorageMode to use
     * @throws NullPointerException if any argument is null
     */
    public VertexBufferObject(BufferData data, StorageMode storageMode) {
        if (storageMode == null)
            throw new NullPointerException("StorageMode cannot be null");
        this.storageMode = storageMode;
        changeQueue = new BulkChangeQueue<DataRange>();
        
        setData(data);
    }
    
    /**
     * @return The underlying BufferData for this VertexBufferObject
     */
    public synchronized BufferData getData() {
        return data;
    }

    /**
     * @return The StorageMode used to store the VertexBufferObject's data after
     *         its been updated with a framework
     */
    public synchronized StorageMode getStorageMode() {
        // This doesn't need to be synchronized since it is final
        return storageMode;
    }

    /**
     * Set the StorageMode for the VertexBufferObject
     * 
     * @param mode The new storage mode
     * @throws NullPointerException if mode is null
     */
    public synchronized void setStorageMode(StorageMode mode) {
        if (mode == null)
            throw new NullPointerException("StorageMode cannot be null");
        storageMode = mode;
    }

    /**
     * Assign the specified BufferData to this VertexBufferObject. Although the
     * storage mode of a vbo will never change, its length and type can. This
     * will also clear the change queue of any pushed DataRanges that referred
     * to the old BufferData, and will queue a dirty range over the new array.
     * 
     * @param data The new BufferData to use
     * @return The new version reported by the vbo's change queue
     * @throws NullPointerException if data is null
     */
    public synchronized int setData(BufferData data) {
        if (data == null)
            throw new NullPointerException("BufferData cannot be null");
        
        this.data = data;
        changeQueue.clear();
        return markDirty(0, data.getLength());
    }

    /**
     * <p>
     * Mark the specified region of the buffer data as dirty so that the next
     * time this VertexBufferObejct is updated (manually or automatically), the
     * range will be updated. The nature of this update depends on the
     * configured StorageMode.
     * </p>
     * <p>
     * The arguments are not validated against the length of the buffer, so
     * anyone querying the change queue must check that condition as necessary.
     * </p>
     * 
     * @param offset The offset into this vbo's BufferData
     * @param len The length of modified data after offset
     * @return The new version reported by the vbo's change queue
     * @throws IllegalArgumentException if offset < 0 or len > 1
     */
    public synchronized int markDirty(int offset, int len) {
        return changeQueue.push(new DataRange(offset, len));
    }

    /**
     * Return the BulkChangeQueue used to track a small set of edits to the
     * vbo's buffer data so that Frameworks can quickly determine if an update
     * must be performed. Reads and modifications of the queue must only perform
     * within synchronized block of this VertexBufferObject.
     * 
     * @return The vbo's change queue
     */
    public BulkChangeQueue<DataRange> getChangeQueue() {
        return changeQueue;
    }
}
