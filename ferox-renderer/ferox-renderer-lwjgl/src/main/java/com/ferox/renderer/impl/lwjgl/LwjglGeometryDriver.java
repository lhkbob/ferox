package com.ferox.renderer.impl.lwjgl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.GL15;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.resource.AbstractGeometryResourceDriver;
import com.ferox.renderer.impl.resource.GeometryHandle;
import com.ferox.util.geom.Geometry.CompileType;

public class LwjglGeometryDriver extends AbstractGeometryResourceDriver {
    private final boolean useArbCompatibility;
    
    // FIXME: add support for the ARB functions as well
    public LwjglGeometryDriver(RenderCapabilities caps) {
        super(caps);
    }

    @Override
    protected void glAllocateArrayData(CompileType type, int vboSize) {
        int usage = (type == CompileType.RESIDENT_DYNAMIC ? GL15.GL_STREAM_DRAW : GL15.GL_STATIC_DRAW);
        if (useArbCompatibility)
            ARBBufferObject.glBufferDataARB(GL15.GL_ARRAY_BUFFER, vboSize, usage);
        else
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vboSize, usage);
    }

    @Override
    protected void glAllocateElementData(CompileType type, IntBuffer data) {
        int usage = (type == CompileType.RESIDENT_DYNAMIC ? GL15.GL_STREAM_DRAW : GL15.GL_STATIC_DRAW);
        data.clear();
        if (useArbCompatibility)
            ARBBufferObject.glBufferDataARB(GL15.GL_ELEMENT_ARRAY_BUFFER, data, usage);
        else
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, usage);
        
    }

    @Override
    protected void glArrayData(int vboOffset, int dataOffset, int dataLen, FloatBuffer data) {
        data.limit(dataOffset + dataLen).position(dataOffset);
        if (useArbCompatibility)
            ARBBufferObject.glBufferSubDataARB(GL15.GL_ARRAY_BUFFER, vboOffset, data);
        else
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, vboOffset, data);
    }
    
    @Override
    protected void glElementData(int vboOffset, int dataOffset, int dataLen, IntBuffer data) {
        data.limit(dataOffset + dataLen).position(dataOffset);
        if (useArbCompatibility)
            ARBBufferObject.glBufferSubDataARB(GL15.GL_ELEMENT_ARRAY_BUFFER, vboOffset, data);
        else
            GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, vboOffset, data);
    }

    @Override
    protected void glCreateBuffers(GeometryHandle handle) {
        IntBuffer ids = BufferUtils.createIntBuffer(2);
        if (useArbCompatibility)
            ARBBufferObject.glGenBuffersARB(ids);
        else
            GL15.glGenBuffers(ids);
        
        handle.arrayVbo = ids.get(0);
        handle.elementVbo = ids.get(1);
    }

    @Override
    protected void glDeleteBuffers(GeometryHandle handle) {
        IntBuffer ids = BufferUtils.createIntBuffer(2);
        ids.put(0, handle.arrayVbo);
        ids.put(1, handle.elementVbo);
        
        if (useArbCompatibility)
            ARBBufferObject.glDeleteBuffersARB(ids);
        else
            GL15.glDeleteBuffers(ids);
    }
    
    @Override
    protected void glBindBuffers(GeometryHandle handle) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void glRestoreBuffers() {
        // TODO Auto-generated method stub
        
    }
}
