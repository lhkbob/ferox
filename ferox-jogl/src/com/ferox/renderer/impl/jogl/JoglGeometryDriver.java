package com.ferox.renderer.impl.jogl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.resource.AbstractGeometryResourceDriver;
import com.ferox.renderer.impl.resource.GeometryHandle;
import com.ferox.resource.Geometry.CompileType;

public class JoglGeometryDriver extends AbstractGeometryResourceDriver {
    private final ThreadLocal<Integer> arrayVboBinding;
    private final ThreadLocal<Integer> elementVboBinding;
    
    public JoglGeometryDriver(RenderCapabilities caps) {
        super(caps);
        arrayVboBinding = new ThreadLocal<Integer>();
        elementVboBinding = new ThreadLocal<Integer>();
    }
    
    private GL2GL3 getGL() {
        return JoglContext.getCurrent().getGL();
    }

    @Override
    protected void glAllocateArrayData(CompileType type, int vboSize) {
        int usage = (type == CompileType.RESIDENT_DYNAMIC ? GL2GL3.GL_STREAM_DRAW : GL2GL3.GL_STATIC_DRAW);
        getGL().glBufferData(GL2GL3.GL_ARRAY_BUFFER, vboSize, null, usage);
    }

    @Override
    protected void glAllocateElementData(CompileType type, IntBuffer data) {
        int usage = (type == CompileType.RESIDENT_DYNAMIC ? GL2GL3.GL_STREAM_DRAW : GL2GL3.GL_STATIC_DRAW);
        data.clear();
        getGL().glBufferData(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, data.capacity() * 4, data, usage);
    }

    @Override
    protected void glArrayData(int vboOffset, int dataOffset, int dataLen, FloatBuffer data) {
        data.limit(dataOffset + dataLen).position(dataOffset);
        getGL().glBufferSubData(GL2GL3.GL_ARRAY_BUFFER, vboOffset, dataLen * 4, data);
    }
    
    @Override
    protected void glElementData(int vboOffset, int dataOffset, int dataLen, IntBuffer data) {
        data.limit(dataOffset + dataLen).position(dataOffset);
        getGL().glBufferSubData(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, vboOffset, dataLen * 4, data);
    }

    @Override
    protected void glCreateBuffers(GeometryHandle handle) {
        int[] ids = new int[2];
        getGL().glGenBuffers(2, ids, 0);
        
        handle.arrayVbo = ids[0];
        handle.elementVbo = ids[1];
    }

    @Override
    protected void glDeleteBuffers(GeometryHandle handle) {
        int[] ids = new int[] { handle.arrayVbo, handle.elementVbo };
        getGL().glDeleteBuffers(2, ids, 0);
    }
    
    @Override
    protected void glBindBuffers(GeometryHandle handle) {
        JoglContext context = JoglContext.getCurrent();
        
        BoundObjectState record = context.getRecord();
        arrayVboBinding.set(record.getArrayVbo());
        elementVboBinding.set(record.getElementVbo());
        
        record.bindArrayVbo(context.getGL(), handle.arrayVbo);
        record.bindElementVbo(context.getGL(), handle.elementVbo);
    }

    @Override
    protected void glRestoreBuffers() {
        JoglContext context = JoglContext.getCurrent();
        BoundObjectState record = context.getRecord();
        
        record.bindArrayVbo(context.getGL(), arrayVboBinding.get());
        record.bindElementVbo(context.getGL(), elementVboBinding.get());
    }
}
