package com.ferox.util.geom.text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import com.ferox.math.ColorRGB;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Surface;
import com.ferox.renderer.Task;
import com.ferox.resource.VertexBufferObject.StorageMode;
import com.ferox.util.geom.Geometry;

public class TextRenderer {
    public static enum Anchor {
        /**
         * Each String will be justified to the left edge of the screen, and
         * subsequent strings will flow down the screen from the top left
         * corner.
         */
        TOP_LEFT { 
            protected double getNextX(double nextWidth, double prevX, double prevWidth) {
                return left(nextWidth, prevX, prevWidth);
            }
            
            protected double getNextY(double nextHeight, double prevY, double prevHeight) {
                return top(nextHeight, prevY, prevHeight);
            }
        }, 
        /**
         * Each String will be justified to the right edge of the screen. Within
         * a given block of text, the layout will still be left justified if
         * that block contains new lines. Subsequent string blocks will flow
         * down the screen from the top right corner.
         */
        TOP_RIGHT {
            protected double getNextX(double nextWidth, double prevX, double prevWidth) {
                return right(nextWidth, prevX, prevWidth);
            }
            
            protected double getNextY(double nextHeight, double prevY, double prevHeight) {
                return top(nextHeight, prevY, prevHeight);
            }
        }, 
        /**
         * Each String will be justified to the left edge of the screen, and
         * subsequent string blocks will flow up from the bottom left corner.
         */
        BOTTOM_LEFT {
            protected double getNextX(double nextWidth, double prevX, double prevWidth) {
                return left(nextWidth, prevX, prevWidth);
            }
            
            protected double getNextY(double nextHeight, double prevY, double prevHeight) {
                return bottom(nextHeight, prevY, prevHeight);
            }
        }, 
        /**
         * Each String will be justified to the right edge of the screen. Within
         * a given block of text, the layout will still be left justified if
         * that block contains new lines. Subsequent string blocks will flow
         * up the screen from the bottom right corner.
         */
        BOTTOM_RIGHT {
            protected double getNextX(double nextWidth, double prevX, double prevWidth) {
                return right(nextWidth, prevX, prevWidth);
            }
            
            protected double getNextY(double nextHeight, double prevY, double prevHeight) {
                return bottom(nextHeight, prevY, prevHeight);
            }
        };
        
        protected abstract double getNextX(double nextWidth, double prevX, double prevWidth);
        protected abstract double getNextY(double nextHeight, double prevY, double prevHeight);
        
        private static double right(double nextWidth, double prevX, double prevWidth) {
            double rightEdge = prevX + prevWidth / 2.0;
            return rightEdge - nextWidth / 2.0;
        }
        
        private static double left(double nextWidth, double prevX, double prevWidth) {
            return nextWidth / 2.0;
        }
        
        private static double top(double nextHeight, double prevY, double prevHeight) {
            return prevY - prevHeight / 2.0 - nextHeight / 2.0;
        }
        
        private static double bottom(double nextHeight, double prevY, double prevHeight) {
            return prevY + prevHeight / 2.0 + nextHeight / 2.0;
        }
    }
    
    private static final Vector4 BLACK = new Vector4(0, 0, 0, 1);
    
    private final List<String> textBlocks;
    private final CharacterSet charSet;
    private final Vector4 textColor;
    
    private final Anchor anchor;
    
    public TextRenderer(String... text) {
        this(new CharacterSet(true, false), text);
    }
    
    public TextRenderer(CharacterSet charSet, String... text) {
        this(charSet, new ColorRGB(1, 1, 1), text);
    }
    
    public TextRenderer(CharacterSet charSet, ColorRGB textColor, String... text) {
        this(charSet, textColor, Anchor.TOP_LEFT, text);
    }
    
    public TextRenderer(CharacterSet charSet, ColorRGB textColor, Anchor anchor, String... text) {
        textBlocks = Arrays.asList(text);
        this.textColor = new Vector4(textColor.red(), textColor.green(), textColor.blue(), 1.0);
        this.charSet = charSet;
        this.anchor = anchor;
    }
    
    public Future<Void> render(final Surface surface) {
        // layout all text blocks given the constraints of the surface
        double prevWidth = 0;
        double prevHeight = 0;
        
        double prevX = (anchor == Anchor.BOTTOM_RIGHT || anchor == Anchor.TOP_RIGHT ? surface.getWidth() : 0);
        double prevY = (anchor == Anchor.TOP_LEFT || anchor == Anchor.TOP_RIGHT ? surface.getHeight() : 0);
        
        Text factory = new Text(charSet);
        factory.setWrapWidth(surface.getWidth());
        
        final Map<Geometry, Matrix4> textLayout = new HashMap<Geometry, Matrix4>();
        for (String t: textBlocks) {
            factory.setText(t);
            
            Geometry block = factory.create(StorageMode.IN_MEMORY);
            Matrix4 pos = new Matrix4().setIdentity();
            pos.m03 = anchor.getNextX(factory.getTextWidth(), prevX, prevWidth);
            pos.m13 = anchor.getNextY(factory.getTextHeight(), prevY, prevHeight);
            
            prevX = pos.m03;
            prevY = pos.m13;
            prevWidth = factory.getTextWidth();
            prevHeight = factory.getTextHeight();
            
            textLayout.put(block, pos);
        }
        
        // compute a projection matrix spanning the entire surface
        Frustum ortho = new Frustum(true, 0, surface.getWidth(), 0, surface.getHeight(), -1, 1);
        final Matrix4 projection = ortho.getProjectionMatrix();
        
        return surface.getFramework().queue(new Task<Void>() {
            @Override
            public Void run(HardwareAccessLayer access) {
                Context ctx = access.setActiveSurface(surface);
                if (ctx != null && ctx.hasFixedFunctionRenderer()) {
                    FixedFunctionRenderer ffp = ctx.getFixedFunctionRenderer();
                    
                    ffp.setProjectionMatrix(projection);
                    
                    ffp.setBlendingEnabled(true);
                    ffp.setBlendMode(BlendFunction.ADD, BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
                    ffp.setDepthTest(Comparison.ALWAYS);
                    ffp.setDepthWriteMask(false);
                    
                    ffp.setMaterial(BLACK, textColor, BLACK, BLACK);
                    
                    ffp.setTexture(0, charSet.getTexture());
                    // FIXME any texture combine changes needed?
                    
                    for (Entry<Geometry, Matrix4> e: textLayout.entrySet()) {
                        ffp.setModelViewMatrix(e.getValue());
                        
                        Geometry g = e.getKey();
                        ffp.setTextureCoordinates(0, g.getTextureCoordinates());
                        ffp.setVertices(g.getVertices());
                        
                        if (g.getIndices() == null)
                            ffp.render(g.getPolygonType(), g.getIndexOffset(), g.getIndexCount());
                        else
                            ffp.render(g.getPolygonType(), g.getIndices(), g.getIndexOffset(), g.getIndexCount());
                    }
                }
                // FIXME support a glsl shader that can do the same rendering
                return null;
            }
        });
    }
}
