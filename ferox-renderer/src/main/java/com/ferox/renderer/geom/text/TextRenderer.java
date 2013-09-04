/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer.geom.text;

import com.ferox.math.ColorRGB;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.*;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.geom.Geometry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

public class TextRenderer {
    public static enum Anchor {
        /**
         * Each String will be justified to the left edge of the screen, and subsequent strings will flow down
         * the screen from the top left corner.
         */
        TOP_LEFT {
            @Override
            protected double getNextX(double nextWidth, double prevX, double prevWidth) {
                return left(nextWidth, prevX, prevWidth);
            }

            @Override
            protected double getNextY(double nextHeight, double prevY, double prevHeight) {
                return top(nextHeight, prevY, prevHeight);
            }
        },
        /**
         * Each String will be justified to the right edge of the screen. Within a given block of text, the
         * layout will still be left justified if that block contains new lines. Subsequent string blocks will
         * flow down the screen from the top right corner.
         */
        TOP_RIGHT {
            @Override
            protected double getNextX(double nextWidth, double prevX, double prevWidth) {
                return right(nextWidth, prevX, prevWidth);
            }

            @Override
            protected double getNextY(double nextHeight, double prevY, double prevHeight) {
                return top(nextHeight, prevY, prevHeight);
            }
        },
        /**
         * Each String will be justified to the left edge of the screen, and subsequent string blocks will
         * flow up from the bottom left corner.
         */
        BOTTOM_LEFT {
            @Override
            protected double getNextX(double nextWidth, double prevX, double prevWidth) {
                return left(nextWidth, prevX, prevWidth);
            }

            @Override
            protected double getNextY(double nextHeight, double prevY, double prevHeight) {
                return bottom(nextHeight, prevY, prevHeight);
            }
        },
        /**
         * Each String will be justified to the right edge of the screen. Within a given block of text, the
         * layout will still be left justified if that block contains new lines. Subsequent string blocks will
         * flow up the screen from the bottom right corner.
         */
        BOTTOM_RIGHT {
            @Override
            protected double getNextX(double nextWidth, double prevX, double prevWidth) {
                return right(nextWidth, prevX, prevWidth);
            }

            @Override
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

        final Map<Geometry, Matrix4> textLayout = new HashMap<>();
        for (String t : textBlocks) {
            factory.setText(t);

            Geometry block = factory.create(surface.getFramework());
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

        return surface.getFramework().invoke(new Task<Void>() {
            @Override
            public Void run(HardwareAccessLayer access) {
                Context ctx = access.setActiveSurface(surface);
                if (ctx != null) {
                    FixedFunctionRenderer ffp = ctx.getFixedFunctionRenderer();

                    ffp.setProjectionMatrix(projection);

                    ffp.setBlendingEnabled(true);
                    ffp.setBlendMode(BlendFunction.ADD, BlendFactor.SRC_ALPHA,
                                     BlendFactor.ONE_MINUS_SRC_ALPHA);
                    ffp.setDepthTest(Comparison.ALWAYS);
                    ffp.setDepthWriteMask(false);

                    ffp.setMaterial(BLACK, textColor, BLACK, BLACK);

                    ffp.setTexture(0, charSet.getTexture());

                    for (Entry<Geometry, Matrix4> e : textLayout.entrySet()) {
                        ffp.setModelViewMatrix(e.getValue());

                        Geometry g = e.getKey();
                        ffp.setTextureCoordinates(0, g.getTextureCoordinates());
                        ffp.setVertices(g.getVertices());

                        ffp.setIndices(g.getIndices());
                        ffp.render(g.getPolygonType(), g.getIndexOffset(), g.getIndexCount());
                    }
                }
                return null;
            }
        });
    }
}
