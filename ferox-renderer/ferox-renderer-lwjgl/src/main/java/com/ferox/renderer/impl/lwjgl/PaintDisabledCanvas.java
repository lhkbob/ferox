package com.ferox.renderer.impl.lwjgl;

import java.awt.Canvas;
import java.awt.Graphics;

/**
 * <p>
 * PaintDisabledCanvas is a Canvas that completely negates its paint() method
 * and provides a NullGraphics to prevent AWT from attempting any funny
 * business.
 * 
 * @author Michael Ludwig
 */
public class PaintDisabledCanvas extends Canvas {
    private static final long serialVersionUID = 1L;

    public PaintDisabledCanvas() {
        super();
    }

    @Override
    public void paint(Graphics g) {
        // do nothing, DO NOT call super.paint() since that invokes display
    }

    @Override
    public Graphics getGraphics() {
        // Must return a bogus graphics object because things are painted once,
        // even if they have ignoreRepaint() set to true, and getGraphics() can
        // block for some reason with JOGL's GLCanvas impl.
        return new NullGraphics();
    }
}
