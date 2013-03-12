/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.resource.geom;

import com.ferox.resource.BulkChangeQueue;
import com.ferox.resource.Resource;
import com.ferox.resource.data.OpenGLData;

/**
 * <p/>
 * BufferObject is a Resource that represents the concept of a buffer object in OpenGL.
 * Depending on the {@link StorageMode} of a created buffer object, the resource's data
 * will live in memory or on the graphics (as an actual VBO).
 * <p/>
 * The buffer object model in OpenGL is very generic and is simply a way to store a block
 * of bytes on the GPU. Subclasses of BufferObject restrict the use cases to circumstances
 * such as vertex attribute data or element indices.
 *
 * @param <T> The specific type of OpenGLData required for the buffer object
 *
 * @author Michael Ludwig
 */
public abstract class BufferObject<T extends OpenGLData<?>> extends Resource {
    /**
     * StorageMode represents the various ways that a Framework can store a VBO resource
     * into something that it can use when rendering them. There are currently three
     * types, each progressing along the spectrum from faster updates to faster rendering
     * performance.
     */
    public static enum StorageMode {
        /**
         * No data is stored on the graphics card. This means that updates are generally
         * very fast (although a copy is necessary). Unfortunately rendering is slower
         * because the data must be resent each render.
         */
        IN_MEMORY,
        /**
         * The VBO data is stored on the graphics card in specialized memory designed to
         * be updated frequently. This means that, although slower than IN_MEMORY, the
         * updates are faster than GPU_STATIC. Because it's on the graphics card,
         * rendering times should be faster compared to IN_MEMORY.
         */
        GPU_DYNAMIC,
        /**
         * VBO data is stored on the graphics card in memory designed for fast read
         * access. This allows rendering to be the fastest, but updates are slower.
         */
        GPU_STATIC
    }

    private T data;
    private StorageMode storageMode;
    private final BulkChangeQueue<DataRange> changeQueue;

    /**
     * Create a new VertexBufferObject that uses the given data and a StorageMode of
     * GPU_STATIC.
     *
     * @param data The initial BufferData
     *
     * @throws NullPointerException if data is null
     */
    public BufferObject(T data) {
        this(data, StorageMode.GPU_STATIC);
    }

    /**
     * Create a new VertexBufferObject that uses the given data and StorageMode. The
     * StorageMode cannot change after the vbo is created.
     *
     * @param data        The initial BufferData
     * @param storageMode The StorageMode to use
     *
     * @throws NullPointerException if any argument is null
     */
    public BufferObject(T data, StorageMode storageMode) {
        if (storageMode == null) {
            throw new NullPointerException("StorageMode cannot be null");
        }
        this.storageMode = storageMode;
        changeQueue = new BulkChangeQueue<DataRange>();

        setData(data);
    }

    /**
     * @return The underlying OpenGLData for this VertexBufferObject
     */
    public synchronized T getData() {
        return data;
    }

    /**
     * @return The StorageMode used to store the VertexBufferObject's data after its been
     *         updated with a framework
     */
    public synchronized StorageMode getStorageMode() {
        return storageMode;
    }

    /**
     * Set the StorageMode for the VertexBufferObject
     *
     * @param mode The new storage mode
     *
     * @throws NullPointerException if mode is null
     */
    public synchronized void setStorageMode(StorageMode mode) {
        if (mode == null) {
            throw new NullPointerException("StorageMode cannot be null");
        }
        storageMode = mode;
    }

    /**
     * Assign the specified data to this VertexBufferObject. Although the storage mode of
     * a vbo will never change, its length and type can as the OpenGL data instance
     * changes.
     * <p/>
     * This will also clear the change queue of any pushed DataRanges that referred to the
     * old data, and will queue a dirty range over the new array.
     *
     * @param data The new data to use
     *
     * @return The new version reported by the vbo's change queue
     *
     * @throws NullPointerException if data is null
     */
    public synchronized int setData(T data) {
        // FIXME make nullable to be consistent with texture resources
        if (data == null) {
            throw new NullPointerException("BufferData cannot be null");
        }

        this.data = data;
        changeQueue.clear();
        return markDirty(0, data.getLength());
    }

    /**
     * <p/>
     * Mark the specified region of the buffer data as dirty so that the next time this
     * BufferObject is updated (manually or automatically), that range will be updated by
     * the Frmaework. The nature of this update depends on the configured StorageMode.
     * <p/>
     * The arguments are not validated against the length of the buffer, so anyone
     * querying the change queue must check that condition as necessary.
     *
     * @param offset The offset into this vbo's BufferData
     * @param len    The length of modified data after offset
     *
     * @return The new version reported by the vbo's change queue
     *
     * @throws IllegalArgumentException if offset < 0 or len > 1
     */
    public synchronized int markDirty(int offset, int len) {
        return changeQueue.push(new DataRange(offset, len));
    }

    /**
     * Return the BulkChangeQueue used to track a small set of edits to the vbo's buffer
     * data so that Frameworks can quickly determine if an update must be performed. Reads
     * and modifications of the queue must only perform within synchronized block of this
     * BufferObject.
     *
     * @return The vbo's change queue
     */
    public BulkChangeQueue<DataRange> getChangeQueue() {
        return changeQueue;
    }
}
