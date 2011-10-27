package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.Framework;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.Context;

public class LwjglOnscreenSurface extends AbstractSurface {

    public LwjglOnscreenSurface(Framework framework) {
        super(framework);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void destroyImpl() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Context getContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void init() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void postRender(Action next) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void preRender() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getHeight() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getWidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean hasColorBuffer() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasDepthBuffer() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasStencilBuffer() {
        // TODO Auto-generated method stub
        return false;
    }

}
