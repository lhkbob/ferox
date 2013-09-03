package com.ferox.renderer.impl.jogl;

import com.ferox.input.KeyEvent;
import com.ferox.input.KeyListener;
import com.ferox.input.MouseEvent;
import com.ferox.input.MouseListener;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;

/**
 *
 */
public class JoglInputTest implements KeyListener, MouseListener {
    public static void main(String[] args) throws Exception {
        Framework f = Framework.Factory.create();
        OnscreenSurface s = f.createSurface(new OnscreenSurfaceOptions());
        s.addKeyListener(new JoglInputTest());
        s.addMouseListener(new JoglInputTest());

        try {
            while (!s.isDestroyed()) {
                Thread.sleep(100);
            }
        } finally {
            f.destroy();
        }
    }

    @Override
    public void handleEvent(KeyEvent event) {
        System.out.println(event);
    }

    @Override
    public void handleEvent(MouseEvent event) {
        System.out.println(event);
    }
}
