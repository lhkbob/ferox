package com.ferox.renderer.impl.jogl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.Context;

public class JoglContext extends Context {
    private final GLContext context;
    
    private final BoundObjectState objState;
    private final List<FramebufferObject> zombieFbos;
    
    public JoglContext(JoglFramework framework, GLContext context, ReentrantLock surfaceLock) {
        super(framework.createRenderer(), surfaceLock);
        if (context == null)
            throw new NullPointerException("GLContext cannot be null");
        this.context = context;
        
        int ffp = framework.getCapabilities().getMaxFixedPipelineTextures();
        int frag = framework.getCapabilities().getMaxFragmentShaderTextures();
        int vert = framework.getCapabilities().getMaxVertexShaderTextures();
        
        int maxTextures = Math.max(ffp, Math.max(frag, vert));
        objState = new BoundObjectState(maxTextures);
        zombieFbos = new CopyOnWriteArrayList<FramebufferObject>();
    }

    /**
     * Destroy the underlying GLContext. This must only when the Framework is
     * exclusively locked because this does not perform it's own locking (to
     * prevent out-of-order deadblocks).
     */
    public void destroy() {
        if (context.isCurrent())
            release();
        context.destroy();
    }
    
    /**
     * @return A GL2GL3 instance associated with this JoglContext
     */
    public GL2GL3 getGL() {
        return context.getGL().getGL2GL3();
    }

    /**
     * @return A GL2 instance for this context. This assumes that GL2 is
     *         supported by the framework's profile
     */
    public GL2 getGL2() {
        return context.getGL().getGL2();
    }

    /**
     * @return A GL3 instance for this context. This assumes that GL3 is
     *         supported by the framework's profile
     */
    public GL3 getGL3() {
        return context.getGL().getGL3();
    }
    
    /**
     * @return The GLContext wrapped by this JoglContext
     */
    public GLContext getGLContext() {
        return context;
    }
    
    /**
     * @return The BoundObjectState used by this JoglContext
     */
    public BoundObjectState getRecord() {
        return objState;
    }
    
    public static JoglContext getCurrent() {
        Context c = Context.getCurrent();
        if (c instanceof JoglContext)
            return (JoglContext) c;
        else
            return null;
    }
    
    @Override
    protected void makeCurrent() {
        int res = context.makeCurrent();
        if (res == GLContext.CONTEXT_NOT_CURRENT)
            throw new RenderException("Unable to make GLContext current");
        
        cleanupZombieFbos();
        super.makeCurrent();
    }
    
    @Override
    protected void release() {
        if (context.isCurrent())
            context.release();
        super.release();
    }
    
    /**
     * Notify the context that a FramebufferObject should be destroyed on this
     * context the next time it's made current. Assumes that the fbo is not
     * null, hasn't been destroyed, and will no longer be used.
     * 
     * @param fbo The fbo to destroy eventually
     */
    void notifyFboZombie(FramebufferObject fbo) {
        zombieFbos.add(fbo);
    }
    
    private void cleanupZombieFbos() {
        int size = zombieFbos.size();
        for (int i = size - 1; i >= 0; i--) {
            zombieFbos.remove(i).destroy();
        }
    }
}
