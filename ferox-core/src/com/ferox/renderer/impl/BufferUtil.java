package com.ferox.renderer.impl;

import java.lang.ref.SoftReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import com.ferox.resource.BufferData;
import com.ferox.resource.BufferData.DataType;

public class BufferUtil {
    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_INT = 4;
    public static final int SIZEOF_SHORT = 2;

    private static final EnumMap<DataType, BufferCache> caches;
    static {
        caches = new EnumMap<DataType, BufferCache>(DataType.class);
        for (DataType t: DataType.values())
            caches.put(t, new BufferCache(t));
    }

    public static FloatBuffer newFloatBuffer(int size) {
        return newByteBuffer(size * SIZEOF_FLOAT).asFloatBuffer();
    }
    
    public static IntBuffer newIntBuffer(int size) {
        return newByteBuffer(size * SIZEOF_INT).asIntBuffer();
    }
    
    public static ShortBuffer newShortBuffer(int size) {
        return newByteBuffer(size * SIZEOF_SHORT).asShortBuffer();
    }
    
    public static ByteBuffer newByteBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }
    
    public static FloatBuffer newFloatBuffer(float[] data) {
        FloatBuffer buffer = newFloatBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }
    
    public static IntBuffer newIntBuffer(int[] data) {
        IntBuffer buffer = newIntBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }
    
    public static ShortBuffer newShortBuffer(short[] data) {
        ShortBuffer buffer = newShortBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }
    
    public static ByteBuffer newByteBuffer(byte[] data) {
        ByteBuffer buffer = newByteBuffer(data.length);
        buffer.put(data).rewind();
        return buffer;
    }
    
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
    
    public static Buffer newBuffer(BufferData data) {
        switch(data.getDataType()) {
        case FLOAT:
            float[] fd = data.getArray();
            return (fd == null ? newFloatBuffer(data.getLength()) : newFloatBuffer(fd));
        case UNSIGNED_BYTE:
            byte[] bd = data.getArray();
            return (bd == null ? newByteBuffer(data.getLength()) : newByteBuffer(bd));
        case UNSIGNED_INT:
            short[] sd = data.getArray();
            return (sd == null ? newShortBuffer(data.getLength()) : newShortBuffer(sd));
        case UNSIGNED_SHORT:
            int[] id = data.getArray();
            return (id == null ? newIntBuffer(data.getLength()) : newIntBuffer(id));
        default:
            throw new IllegalArgumentException();
        }
    }
    
    public static Buffer leaseBuffer(DataType type, int size) {
        BufferCache cache = caches.get(type);
        return cache.lease(size);
    }
    
    public static Buffer leaseBuffer(BufferData data) {
        BufferCache cache = caches.get(data.getDataType());
        return cache.lease(data);
    }
    
    public static void returnBuffer(Buffer buffer) {
        BufferCache cache;
        if (buffer instanceof ByteBuffer)
            cache = caches.get(DataType.UNSIGNED_BYTE);
        else if (buffer instanceof ShortBuffer)
            cache = caches.get(DataType.UNSIGNED_SHORT);
        else if (buffer instanceof IntBuffer)
            cache = caches.get(DataType.UNSIGNED_INT);
        else if (buffer instanceof FloatBuffer)
            cache = caches.get(DataType.FLOAT);
        else
            throw new IllegalArgumentException();
        
        cache.returnBuffer(buffer);
    }
    
    private static class BufferCache {
        private final DataType dataType;
        private final ConcurrentSkipListMap<Integer, SoftReference<Buffer>> buffers;
        
        public BufferCache(DataType dataType) {
            this.dataType = dataType;
            buffers = new ConcurrentSkipListMap<Integer, SoftReference<Buffer>>();
        }
        
        public Buffer lease(int size) {
            Map<Integer, SoftReference<Buffer>> candidates = buffers.tailMap(size, true);
            Iterator<Entry<Integer, SoftReference<Buffer>>> it = candidates.entrySet().iterator();
            while(it.hasNext()) {
                Buffer b = it.next().getValue().get();
                if (b != null) {
                    // Size the buffer appropriately
                    return b.clear().limit(size);
                } else {
                    // GC'ed so clean up the entry
                    it.remove();
                }
            }
            
            // No buffer found
            return newBuffer(dataType, size);
        }
        
        public Buffer lease(BufferData data) {
            Map<Integer, SoftReference<Buffer>> candidates = buffers.tailMap(data.getLength(), true);
            Iterator<Entry<Integer, SoftReference<Buffer>>> it = candidates.entrySet().iterator();
            while(it.hasNext()) {
                Buffer b = it.next().getValue().get();
                if (b != null) {
                    // Size the buffer appropriately
                    b.clear().limit(data.getLength());
                    switch(dataType) {
                    case FLOAT:
                        float[] fd = data.getArray();
                        return ((FloatBuffer) b).put(fd).rewind();
                    case UNSIGNED_INT:
                        int[] id = data.getArray();
                        return ((IntBuffer) b).put(id).rewind();
                    case UNSIGNED_SHORT:
                        short[] sd = data.getArray();
                        return ((ShortBuffer) b).put(sd).rewind();
                    case UNSIGNED_BYTE:
                        byte[] bd = data.getArray();
                        return ((ByteBuffer) b).put(bd).rewind();
                    }
                } else {
                    // GC'ed so clean up the entry
                    it.remove();
                }
            }
            
            // No buffer found
            return newBuffer(data);
        }
        
        public void returnBuffer(Buffer buffer) {
            int key = buffer.capacity();
            // If there already was a buffer of the exact same capacity, 
            // this will overwrite the old buffer.  Since this is a volatile cache that's fine
            buffers.put(key, new SoftReference<Buffer>(buffer));
        }
    }
}
