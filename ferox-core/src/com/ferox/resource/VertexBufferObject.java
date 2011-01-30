package com.ferox.resource;

public class VertexBufferObject extends Resource {
    /**
     * CompileType represents the various ways that a Framework can 'compile' a
     * Geometry resource into something that it can use when rendering them.
     * There are currently three types, each progressing along the spectrum from
     * faster updates to faster rendering performance.
     */
    public static enum CompileType {
        /**
         * No data is stored on the graphics card. This means that updates are
         * generally very fast (although a copy is necessary). Unfortunately
         * rendering is slower because the data must be resent each render.
         */
        NONE,
        /**
         * The Geometry data is stored on the graphics card in specialized
         * memory designed to be updated frequently. This means that, although
         * slower than NONE, the updates are faster than RESIDENT_STATIC.
         * Because it's on the graphics card, rendering times are also much
         * faster compared to NONE.
         */
        RESIDENT_DYNAMIC,
        /**
         * Geometry data is stored on the graphics card in memory designed for
         * fast read access. This allows rendering to be the most performant,
         * but updates are slower.
         */
        RESIDENT_STATIC
    }
    
    private BufferData data;
    private CompileType compileType;
    
    public VertexBufferObject(BufferData data) {
        this(data, CompileType.RESIDENT_STATIC);
    }
    
    public VertexBufferObject(BufferData data, CompileType type) {
        setData(data);
        setCompileType(type);
    }
    
    public BufferData getData() {
        return data;
    }
    
    public CompileType getCompileType() {
        return compileType;
    }

    public void setData(BufferData data) {
        if (data == null)
            throw new NullPointerException("BufferData cannot be null");
        this.data = data;
        notifyChange(Resource.FULL_UPDATE);
    }
    
    public void setCompileType(CompileType type) {
        if (type == null)
            throw new NullPointerException("CompileType cannot be null");
        compileType = type;
        notifyChange(Resource.FULL_UPDATE);
    }
    
    public void markDirty(int offset, int len) {
        if (offset < 0)
            throw new IllegalArgumentException("Offset must be at least 0, not: " + offset);
        if (len < 1)
            throw new IllegalArgumentException("Length must be at least 1, not: " + len);
        
        int realLength = len;
        if (offset + len > data.getLength())
            realLength = data.getLength() - offset;
        notifyChange(new DataRange(offset, realLength));
    }
}
