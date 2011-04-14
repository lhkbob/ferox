package com.ferox.renderer.impl2;

import com.ferox.input.EventQueue;
import com.ferox.input.KeyListener;
import com.ferox.input.MouseListener;
import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;

public class AbstractOnscreenSurface extends AbstractSurface implements OnscreenSurface {

    public AbstractOnscreenSurface(AbstractFramework framework) {
        super(framework);
        // TODO Auto-generated constructor stub
    }

    @Override
    public int getWidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getHeight() {
        // TODO Auto-generated method stub
        return 0;
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
    public EventQueue getQueue() {
        // TODO Auto-generated method stub
        return null;
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
    public OnscreenSurfaceOptions getOptions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isFullscreen() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setFullscreen(boolean fullscreen) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setDisplayMode(DisplayMode mode) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public DisplayMode getDisplayMode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DisplayMode[] getAvailableDisplayModes() {
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
        // TODO Auto-generated method stub
        
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
    public Object getWindowImpl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUndecorated() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isResizable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getX() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getY() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setWindowSize(int width, int height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setLocation(int x, int y) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OpenGLContextAdapter getContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onSurfaceActivate(OpenGLContextAdapter context, int layer) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onSurfaceDeactivate(OpenGLContextAdapter context) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void destroyImpl() {
        // TODO Auto-generated method stub
        
    }

}
