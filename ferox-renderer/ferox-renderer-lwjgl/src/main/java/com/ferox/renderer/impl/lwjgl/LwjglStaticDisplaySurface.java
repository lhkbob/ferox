package com.ferox.renderer.impl.lwjgl;

import org.lwjgl.opengl.Display;

import com.ferox.input.KeyListener;
import com.ferox.input.MouseListener;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.impl.AbstractOnscreenSurface;
import com.ferox.renderer.impl.OpenGLContext;

// FIXME: this will have to be used for a select breed of surfaces
// - easiest so far is to just fullscreen
public class LwjglStaticDisplaySurface extends AbstractOnscreenSurface {

    @Override
    public OnscreenSurfaceOptions getOptions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isVSyncEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setVSyncEnabled(boolean enable) {
        Display.setVSyncEnabled(enable);
    }

    @Override
    public String getTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTitle(String title) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public void setWindowSize(int width, int height) {
        Display.set
    }

    @Override
    public void setLocation(int x, int y) {
        Display.setLocation(x, y);
    }

    @Override
    public boolean isClosable() {

    
    }

    @Override
    public void setClosable(boolean userClosable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getWidth() {
        return Display.getWidth();
    }

    @Override
    public int getHeight() {
        return Display.getHeight();
    }

    @Override
    public void addMouseListener(MouseListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeMouseListener(MouseListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addKeyListener(KeyListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeKeyListener(KeyListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OpenGLContext getContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void flush(OpenGLContext context) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void destroyImpl() {
        Display.destroy();
    }
}
