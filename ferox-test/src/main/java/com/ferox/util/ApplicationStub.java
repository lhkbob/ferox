package com.ferox.util;

import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.KeyPressedCondition;
import com.ferox.input.logic.Trigger;
import com.ferox.math.ColorRGB;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.util.geom.text.CharacterSet;
import com.ferox.util.geom.text.TextRenderer;
import com.ferox.util.geom.text.TextRenderer.Anchor;

public abstract class ApplicationStub {
    private final Framework framework;
    private final OnscreenSurfaceOptions opts;
    
    private boolean showFPS;
    private boolean showProfiling;
    
    public ApplicationStub(Framework framework) {
        this(framework, new OnscreenSurfaceOptions().setWidth(800)
                                                    .setHeight(600)
                                                    .setResizable(false));
        
        showFPS = true;
        showProfiling = true;
    }
    
    public ApplicationStub(Framework framework, OnscreenSurfaceOptions opts) {
        this.framework = framework;
        this.opts = opts;
    }
    
    public void run() {
        OnscreenSurface surface = framework.createSurface(opts);
        InputManager io = new InputManager(surface);
        
        init(surface);
        installInputHandlers(io);
        installPrivateHandlers(io);
        
        Runtime r = Runtime.getRuntime();
        CharacterSet charSet = new CharacterSet(true, false);
        TextRenderer fps = null;
        TextRenderer profile = null;
        
        int numFrames = 0;
        long last = System.currentTimeMillis();
        while(!surface.isDestroyed()) {
            io.process();
            renderFrame(surface);
            numFrames++;
            
            if (fps != null)
                fps.render(surface);
            if (profile != null)
                profile.render(surface);
            framework.flush(surface);
            framework.sync();
            
            long now = System.currentTimeMillis();
            if (now - last > 100) {
                // it's been a 10th of a second
                double dt = (now - last) / 1e3;
                if (showFPS)
                    fps = formatFPS((numFrames / dt), charSet);
                else
                    fps = null;
                
                if (showProfiling)
                    profile = formatProfiling(r.totalMemory() - r.freeMemory(), r.totalMemory(), charSet);
                else
                    profile = null;
                
                last = now;
                numFrames = 0;
            }
        }
    }
    
    private TextRenderer formatFPS(double fps, CharacterSet charSet) {
        return new TextRenderer(charSet, new ColorRGB(1, 1, 1), Anchor.BOTTOM_LEFT, String.format("FPS: %.2f", fps));
    }
    
    private TextRenderer formatProfiling(long heap, long maxHeap, CharacterSet charSet) {
        double heapM = heap / (1024.0 * 1024.0);
        double maxM = maxHeap / (1024.0 * 1024.0);
        return new TextRenderer(charSet, new ColorRGB(1, 1, 1), Anchor.BOTTOM_RIGHT, String.format("MEM: %.2f / %.2f M", heapM, maxM));
    }
    
    protected final Framework getFramework() {
        return framework;
    }
    
    protected abstract void installInputHandlers(InputManager io);
    
    protected abstract void init(OnscreenSurface surface);
    
    protected abstract void renderFrame(OnscreenSurface surface);
    
    private void installPrivateHandlers(InputManager io) {
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                framework.destroy();
            }
        }, new KeyPressedCondition(KeyCode.ESCAPE));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                showFPS = !showFPS;
            }
        }, new KeyPressedCondition(KeyCode.F));
        io.addTrigger(new Trigger() {
            @Override
            public void onTrigger(InputState prev, InputState next) {
                showProfiling = !showProfiling;
            }
        }, new KeyPressedCondition(KeyCode.P));
    }
}
