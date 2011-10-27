package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.Context;
import com.ferox.renderer.impl.TextureSurfaceDelegate;
import com.ferox.resource.Texture;

public class PbufferSurfaceDelegate extends TextureSurfaceDelegate {

    public PbufferSurfaceDelegate(Texture[] colorTextures, Texture depthTexture) {
        super(colorTextures, depthTexture);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void flushLayer() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Context getContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void postRender(Action next) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void preRender() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLayer(int layer, int depth) {
        // TODO Auto-generated method stub
        
    }

}
