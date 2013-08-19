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
package com.ferox.renderer;

import com.ferox.math.Const;
import com.ferox.math.Vector4;

/**
 * <p/>
 * Renderer provides access to the OpenGL functionality that is shared between older versions of OpenGL
 * (version <= 2.1) and the updated, shader based API (version >= 3.0).
 * <p/>
 * Renderers are linked to the {@link Context} that produced them because each Context has a unique set of
 * state that is controlled by the renderer. Use the {@link HardwareAccessLayer#setActiveSurface(Surface)} to
 * get the context of a surface, and then use the context's renderer to render into the activated surface.
 * <p/>
 * There are two sub-implementations of Renderer that provide access to the old fixed-function pipeline in
 * OpenGL, and a shader based Renderer that is compatible with shaders in the old and new versions of OpenGL.
 * <p/>
 * Common behavior shared by all renderers is that exceptions will be thrown when destroyed resources are
 * provided to them.  However, if state is restored with a call such as {@link
 * GlslRenderer#setCurrentState(ContextState)}, resources that were destroyed in the meantime will be silently
 * ignored.
 *
 * @author Michael Ludwig
 * @see FixedFunctionRenderer
 * @see GlslRenderer
 */
public interface Renderer {
    /**
     * <p/>
     * BlendFactor describes a function that outputs scaling factors for each component of a color, where the
     * color is either the source or destination pixel color used by {@link BlendFunction}. The BlendFactor
     * outputs four scalars, each corresponding to red, green, blue, or alpha.
     * <p/>
     * In the following descriptions of the enums, the following variables are used: <ul> <li>(Rs, Gs, Bs, As)
     * = RGBA values for the source/incoming pixel</li> <li>(Rd, Gd, Bd, Ad) = RGBA values for the
     * destination/previous pixel</li> <li>(Rc, Gc, Bc, Ac) = RGBA values of the configured/constant blending
     * color</li> </ul>
     */
    public static enum BlendFactor {
        /**
         * Produces (0, 0, 0, 0).
         */
        ZERO,
        /**
         * Produces (1, 1, 1, 1).
         */
        ONE,
        /**
         * Produces (Rs, Gs, Bs, As).
         */
        SRC_COLOR,
        /**
         * Produces (1 - Rs, 1 - Gs, 1 - Bs, 1 - As).
         */
        ONE_MINUS_SRC_COLOR,
        /**
         * Produces (As, As, As, As).
         */
        SRC_ALPHA,
        /**
         * Produces (1 - As, 1 - As, 1 - As, 1 - As).
         */
        ONE_MINUS_SRC_ALPHA,
        /**
         * Let f = min(As, 1 - Ad), it produces (f, f, f, 1).
         * <p/>
         * Can only be used as a source BlendFactor.
         */
        SRC_ALPHA_SATURATE,
        /**
         * Produces (Rd, Gd, Bd, Ad).
         */
        DST_COLOR,
        /**
         * Produces (1 - Rd, 1 - Gd, 1 - Bd, 1 - Ad).
         */
        ONE_MINUS_DST_COLOR,
        /**
         * Produces (Ad, Ad, Ad, Ad).
         */
        DST_ALPHA,
        /**
         * Produces (1 - Ad, 1 - Ad, 1 - Ad, 1 - Ad).
         */
        ONE_MINUS_DST_ALPHA,
        /**
         * Produces (Rc, Gc, Bc, Ac).
         */
        CONSTANT_COLOR,
        /**
         * Produces (Ac, Ac, Ac, Ac).
         */
        CONSTANT_ALPHA,
        /**
         * Produces (1 - Rc, 1 - Gc, 1 - Bc, 1 - Ac).
         */
        ONE_MINUS_CONSTANT_COLOR,
        /**
         * Produces (1 - Ac, 1 - Ac, 1 - Ac, 1 - Ac).
         */
        ONE_MINUS_CONSTANT_ALPHA
    }

    /**
     * When blending is enabled, incoming pixels are combined with the previously written pixel based on the
     * configured BlendFunction and {@link BlendFactor}'s. In the functions available, the following variables
     * are used: <ul> <li>sC = Incoming pixel color</li> <li>dC = Previous pixel color</li> <li>fC = Computed
     * final color</li> <li>bS = Blending factor for incoming color, based off of the source's configured
     * BlendFactor</li> <li>bD = Blending factor for the previous color, based off of the destinations's
     * configured BlendFactor</li> </ul> All operations are done component-wise across the color.
     */
    public static enum BlendFunction {
        /**
         * ADD blends the two colors together by adding their scaled components together: <code>fC = sC * bS +
         * dC * bD</code>.
         */
        ADD,
        /**
         * SUBTRACT blends the two colors by subtracting the scaled source color from the scaled destination:
         * <code>fC = sC * bS - dC * bD</code>.
         */
        SUBTRACT,
        /**
         * REVERSE_SUBTRACT blends the two colors by subtracting the scaled destination from the scaled
         * source: <code>fC = dC * bD - sC * bS</code>.
         */
        REVERSE_SUBTRACT,
        /**
         * MIN computes the final color, where each component is the smallest value between the source and
         * destination. The blending factors are ignored: <code>fC = min(sC, dC)</code>.
         */
        MIN,
        /**
         * MIN computes the final color, where each component is the largest value between the source and
         * destination. The blending factors are ignored: <code>fC = max(sC, dC)</code>.
         */
        MAX
    }

    /**
     * <p/>
     * Comparison is an enum that represents a function that compares two input values to return a boolean. It
     * is often used for various pixel tests, where if comparison returns true the pixel continues down the
     * pipeline, else the pixel is not rendered.
     * <p/>
     * The two inputs always have the form of a pixel value, and a reference value. The pixel value is then
     * compared to the reference value using the given Comparison function. Sometimes the reference value is
     * defined as a constant, and other times it's the previous pixel value. In the case of NEVER and ALWAYS,
     * the inputs are ignored.
     */
    public static enum Comparison {
        /**
         * Returns true if the pixel value is equal to the reference value.
         */
        EQUAL,
        /**
         * Returns true if the pixel value is greater than the reference value.
         */
        GREATER,
        /**
         * Returns true if the pixel value is less than the reference value.
         */
        LESS,
        /**
         * Returns true if the pixel value is greater than or equal to the reference value.
         */
        GEQUAL,
        /**
         * Returns true if the pixel value is less than or equal to the reference value.
         */
        LEQUAL,
        /**
         * Returns true if the pixel value is not equal to the reference value.
         */
        NOT_EQUAL,
        /**
         * Always returns false, regardless of the inputs.
         */
        NEVER,
        /**
         * Always returns true, regardless of the inputs.
         */
        ALWAYS
    }

    /**
     * DrawStyle is an enum that represents the different ways that a polygon can be rendered. The Renderer
     * interface exposes separate configuration points for front-facing and back-facing polygons. The facing
     * of a polygon is determined by the counter-clockwise ordering of its vertices.
     */
    public static enum DrawStyle {
        /**
         * Polygons are rendered as solid regions.
         */
        SOLID,
        /**
         * Polygons are rendered as line segments around its edges.
         */
        LINE,
        /**
         * Polygons are rendered as points on its vertices.
         */
        POINT,
        /**
         * Polygons are not rendered, which this effectively culls the polygon.
         */
        NONE
    }

    /**
     * <p/>
     * When the stencil test is enabled, and the Surface has a stencil buffer, the stencil test can be
     * performed. When the test is active, there are three stages where incoming pixels can affect the values
     * within the stencil buffer.
     * <p/>
     * The way a pixel affects the buffer is determined by the configured StencilUpdate at each of the
     * following stages: <ol> <li>Failing the stencil test</li> <li>Failing the depth test</li> <li>Passing
     * the depth test</li> </ol> These tests are listed in the order that they are performed, so only one
     * stencil update operation will occur for a given pixel (it fails a test so is no longer processed, or it
     * passes the depth test and is written).
     * <p/>
     * Keep in mind that the precision of a stencil buffer is wholly dependent on the number of bits within a
     * Surface's stencil buffer (which is usually limited to 2 to 8).
     */
    public static enum StencilUpdate {
        /**
         * Performs no edits to the stencil buffer at the current pixel.
         */
        KEEP,
        /**
         * Sets the stencil's value to 0 at the current pixel.
         */
        ZERO,
        /**
         * Replaces the stencil's value with the configured reference value at the current pixel.
         */
        REPLACE,
        /**
         * Add one to the stencil's current value, clamping it to the maximum (dependent on buffer precision),
         * at the current pixel.
         */
        INCREMENT,
        /**
         * Subtract one from the stencil's current value, clamping it to 0, at the current pixel.
         */
        DECREMENT,
        /**
         * Bitwise invert the stencil's value at the current pixel.
         */
        INVERT,
        /**
         * Add one to the stencil's value, wrapping around to 0, at the current pixel.
         */
        INCREMENT_WRAP,
        /**
         * Subtract one from the stencil's value, wrapping around to the max value, at the current pixel.
         */
        DECREMENT_WRAP
    }

    /**
     * Represents how consecutive vertices form polygons. What determines consecutive vertices depends on how
     * the vertices are submitted, and may be the order they exist in an attribute array, or depend on how
     * they are referenced by an index array.
     */
    public static enum PolygonType {
        /**
         * Every vertex is treated as a single point.
         */
        POINTS {
            @Override
            public int getPolygonCount(int count) {
                return count;
            }

            @Override
            public int getPolygonSize() {
                return 1;
            }
        },
        /**
         * Every two vertices form a line, so [i0, i1, i2, i3] creates 2 lines, one from i0 to i1 and another
         * from i2 to i3.
         */
        LINES {
            @Override
            public int getPolygonCount(int count) {
                return count >> 1;
            }

            @Override
            public int getPolygonSize() {
                return 2;
            }
        },
        /**
         * Every three vertices form an individual triangle.
         */
        TRIANGLES {
            @Override
            public int getPolygonCount(int count) {
                return count / 3;
            }

            @Override
            public int getPolygonSize() {
                return 3;
            }
        },
        /**
         * The first three vertices form a triangle, and then every subsequent vertex forms a triangle with
         * the previous two vertices.
         */
        TRIANGLE_STRIP {
            @Override
            public int getPolygonCount(int count) {
                return Math.max(0, count - 2);
            }

            @Override
            public int getPolygonSize() {
                return 3;
            }
        };

        /**
         * Compute the number of polygons, based on the number of indices. This assumes that numIndices > 0.
         * This will return undefined results if numIndices is not positive. This will return the minimum
         * number of fully formed primitives, extra indices will be ignored.
         *
         * @param numIndices The number of indices that build a shape with this PolygonType.
         *
         * @return The polygon count
         */
        public abstract int getPolygonCount(int numIndices);

        /**
         * Return the number of vertices in each polygon. The vertices may be shared by multiple polygons,
         * this is just the number of vertices required for a single primitive. As an example, both TRIANGLES
         * and TRIANGLE_STRIP return 3.
         *
         * @return The number of vertices in a polygon
         */
        public abstract int getPolygonSize();
    }

    /**
     * <p/>
     * Set to true to cause points to be rendered as anti-aliased circles instead of squares. When
     * anti-aliasing is enabled, the final point width may not be exactly the requested point size. This
     * anti-aliasing is independent of any per-pixel anti-aliasing performed by the Surface.
     * <p/>
     * Point anti-aliasing is disabled by default.
     *
     * @param enable True if points should be anti-aliased
     */
    public void setPointAntiAliasingEnabled(boolean enable);

    /**
     * <p/>
     * Set to true to cause lines to be rendered as anti-aliased lines. When enabled, the actual line width
     * may not be exactly the requested line width. This anti-aliasing is independent of the Surface
     * anti-aliasing.
     * <p/>
     * Line anti-aliasing is disabled by default.
     *
     * @param enable True if lines should be anti-aliased
     */
    public void setLineAntiAliasingEnabled(boolean enable);

    /**
     * <p/>
     * Set to true if the edges of rendered polygons should be anti-aliased. It is not recommended to use this
     * when using adjacent polygons because the edges are independently anti-aliased and will appear to pull
     * away from each other. Polygons that have DrawStyles of LINE or POINT use the anti-aliasing
     * configuration for points and lines.
     * <p/>
     * Polygon anti-aliasing is disabled by default.
     *
     * @param enable True if solid polygons should have their edges anti-aliased
     */
    public void setPolygonAntiAliasingEnabled(boolean enable);

    /**
     * <p/>
     * Set the pixel width of rendered points. If the width has a fractional component, it will only appear
     * that width when anti-aliasing is enabled.
     * <p/>
     * The default point width is 1.
     *
     * @param width The new point width
     *
     * @throws IllegalArgumentException if width < 1
     */
    public void setPointSize(double width);

    /**
     * <p/>
     * Set the line width of rendered lines. If the width has a fractional component, it will only appear the
     * correct width when line anti-aliasing is enabled.
     * <p/>
     * The default line width is 1.
     *
     * @param width The new line width
     *
     * @throws IllegalArgumentException if width < 1
     */
    public void setLineSize(double width);

    /**
     * <p/>
     * Set the {@link Renderer.StencilUpdate} operations for the three stages in the pixel pipeline that can
     * update the stencil buffer.
     * <p/>
     * This method configures both the front and back set of StencilOps to use the same set of three. Use
     * {@link #setStencilUpdateFront(Renderer.StencilUpdate, Renderer.StencilUpdate, Renderer.StencilUpdate)}
     * and {@link #setStencilUpdateBack(Renderer.StencilUpdate, Renderer.StencilUpdate,
     * Renderer.StencilUpdate)} to configure the different sets independently.
     * <p/>
     * The starting state of the Renderer is all three stages for front and back facing polygons to use the
     * KEEP update operation.
     *
     * @param stencilFail The StencilUpdate applied when the stencil test fails
     * @param depthFail   The StencilUpdate applied when the depth test fails
     * @param depthPass   The StencilUpdate applied when the depth test passes
     *
     * @throws NullPointerException if any enum is null
     */
    public void setStencilUpdate(StencilUpdate stencilFail, StencilUpdate depthFail, StencilUpdate depthPass);

    /**
     * This method sets the StencilUpdates that are applied with front-facing polygons. See {@link
     * #setStencilUpdate(Renderer.StencilUpdate, Renderer.StencilUpdate, Renderer.StencilUpdate)} for a
     * description for details on when the updates are applied.
     *
     * @param stencilFail The StencilUpdate applied when the stencil test fails for front-facing polygons
     * @param depthFail   The StencilUpdate applied when the depth test fails for front-facing polygons
     * @param depthPass   The StencilUpdate applied when the depth test passes for front-facing polygons
     *
     * @throws NullPointerException if any enum is null
     */
    public void setStencilUpdateFront(StencilUpdate stencilFail, StencilUpdate depthFail,
                                      StencilUpdate depthPass);

    /**
     * This method sets the StencilUpdates that are applied with back-facing polygons. See {@link
     * #setStencilUpdate(Renderer.StencilUpdate, Renderer.StencilUpdate, Renderer.StencilUpdate)} for a
     * description for details on when the updates are applied.
     *
     * @param stencilFail The StencilUpdate applied when the stencil test fails for back-facing polygons
     * @param depthFail   The StencilUpdate applied when the depth test fails for back-facing polygons
     * @param depthPass   The StencilUpdate applied when the depth test passes for back-facing polygons
     *
     * @throws NullPointerException if any enum is null
     */
    public void setStencilUpdateBack(StencilUpdate stencilFail, StencilUpdate depthFail,
                                     StencilUpdate depthPass);

    /**
     * <p/>
     * Set the Comparison and reference values used when performing the stencil test. This test will only be
     * performed when it is enabled with {@link #setStencilTestEnabled(boolean)}. When the test is enabled,
     * the current stencil value is bitwise AND'ed with <var>testMask</var> and then compared to
     * <var>(refValue & testMask)</var>. Thus <var>testMask</var> acts as a bitwise mask to both the current
     * stencil value and the assigned reference value.
     * <p/>
     * There are two different stencil test configurations available: one for front-facing polygons and one
     * for back-facing polygons. Points and lines are considered to be front-facing polygons. Polygons
     * rendered as points or lines, however, will be assigned the front or back test based on the CCW ordering
     * of the polygon. This method configures both the front and the back stencil tests to use the same
     * Comparison, reference value and test mask. The methods {@link #setStencilTestFront(Comparison, int,
     * int)} and {@link #setStencilTestBack(Comparison, int, int)} can be used to control a specific facing.
     * <p/>
     * The starting state of the Renderer uses a Comparison of ALWAYS, a reference value of 0 and a test mask
     * with all bits 1 (~0) for front and back facing polygons.
     *
     * @param test     The Comparison to use for stencil testing on all primitives
     * @param refValue The reference value that the stencil buffer is compared against
     * @param testMask The bit mask AND'ed with the stencil buffer and refValue before performing the
     *                 comparison
     *
     * @throws NullPointerException if test is null
     */
    public void setStencilTest(Comparison test, int refValue, int testMask);

    /**
     * This method sets the stencil test for just front-facing polygons as described in {@link
     * #setStencilTest(Comparison, int, int)}.
     *
     * @param test     The Comparison to use for stencil testing of front-facing polygons
     * @param refValue The reference value that the stencil buffer is compared against
     * @param testMask The bit mask AND'ed with the stencil buffer and refValue before performing the
     *                 comparison
     *
     * @throws NullPointerException if test is null
     */
    public void setStencilTestFront(Comparison test, int refValue, int testMask);

    /**
     * This method sets the stencil test for just back-facing polygons as described in {@link
     * #setStencilTest(Comparison, int, int)}.
     *
     * @param test     The Comparison to use for stencil testing of back-facing polygons
     * @param refValue The reference value that the stencil buffer is compared against
     * @param testMask The bit mask AND'ed with the stencil buffer and refValue before performing the
     *                 comparison
     *
     * @throws NullPointerException if test is null
     */
    public void setStencilTestBack(Comparison test, int refValue, int testMask);

    /**
     * <p/>
     * Set whether or not the stencil test is enabled or disabled. When the stencil test is enabled, the
     * stencil test that's been configured by {@link #setStencilTest(Comparison, int, int)} is performed for
     * each rendered pixel. Depending on the result of that test and the subsequent depth test, the various
     * stencil update operations will be performed and the stencil buffer will be updated.
     * <p/>
     * When rendering polygons, there are two different tests and set of update operations, one for
     * front-facing polygons and one for back-facing polygons. Polygons rendered as points or lines are
     * treated equivalently to filled polygons, however actual points and lines will always use the front set
     * of stencil test state.
     * <p/>
     * When the stencil test is disabled, no stencil buffer related actions occur when rendering (e.g. no
     * stencil update operations are performed, the test always 'passes' and the write masks are effectively
     * ignored since nothing is written).
     * <p/>
     * The stencil test starts out disabled.
     *
     * @param enable True if the stencil test is enabled
     *
     * @see #setStencilUpdate(StencilUpdate, StencilUpdate, StencilUpdate)
     */
    public void setStencilTestEnabled(boolean enable);

    /**
     * Convenience function to set the front and back stencil masks to the same value. See {@link
     * #setStencilWriteMask(int, int)} for more details on the stencil mask.
     *
     * @param mask The stencil mask for front and back polygons
     */
    public void setStencilWriteMask(int mask);

    /**
     * <p/>
     * Set the stencil masks that are applied to stencil values as they're written into the stencil buffer.
     * The front mask is used when writing stencil values from polygons that are front-facing. Similarly, the
     * back mask is used when writing stencil values from back polygons.
     * <p/>
     * When determining the final value that's written into the stencil buffer, only bits that are 1 within
     * the appropriate mask have the corresponding bits from stencil value written. Thus a mask of all 1s
     * writes the entire stencil value, and a mask of all 0s causes no value to be written.
     * <p/>
     * Because stencil buffers often have extremely low numbers of bits (between 2 and 8), only the lowest N
     * bits of each mask are used, where N is the resolution of the stencil buffer.
     * <p/>
     * The starting state is both the front and back masks filled with 1s.
     *
     * @param front The stencil write mask applied to incoming stencil values from front-facing polygons
     * @param back  The stencil write mask applied to incoming stencil values from back-facing polygons
     */
    public void setStencilWriteMask(int front, int back);

    /**
     * <p/>
     * Set the Comparison used when comparing incoming pixels' depth values to the depth value stored in
     * previously at that pixel's location in the depth buffer. The depth test can be used to correctly render
     * intersecting 3D shapes, etc. Use ALWAYS to effectively disable the depth test.
     * <p/>
     * Depth values are stored in a range of 0 to 1. The default depth clear value is 1, so the starting depth
     * test is LESS.
     *
     * @param test The Comparison to use for depth testing
     *
     * @throws NullPointerException if test is null
     */
    public void setDepthTest(Comparison test);

    /**
     * <p/>
     * Set whether or not depth values can be written into the depth buffer once it has been determined that a
     * pixel should be rendered. If <var>mask</var> is true then depth values for rendered pixels will be
     * placed in the depth buffer, otherwise no depth is written.
     * <p/>
     * This depth mask is independent of any mask that's applied to the color components or stencil values
     * that might be written for a pixel.
     * <p/>
     * The depth write mask starts out true, so depth values are written.
     *
     * @param mask True if depth writing is enabled
     */
    public void setDepthWriteMask(boolean mask);

    /**
     * <p/>
     * Set the depth offset configuration values. The depth offset added to each pixel's depth is computed as
     * follows:
     * <p/>
     * <pre>
     * offset = factor * m + units * r.
     * </pre>
     * <p/>
     * <var>offset</var> is the value added to the pixel's window depth; <var>factor</var> and
     * <var>units</var> are the values specified in this method; <var>m</var> is the maximum depth slope of
     * the polygon that contained the rendered pixel; <var>r</var> is the minimum difference between depth
     * values such that they are distinct once stored in the depth buffer.
     * <p/>
     * <var>m</var> is computed as the length of the vector <dz/dx, dz/dy>. <var>r</var> is implementation
     * dependent since it depends on the size and format of the depth buffer in use for a Surface.
     * <p/>
     * The starting state is to have both factor and units equal to 0.
     *
     * @param factor The scale factor applied to a polygon's max depth slope
     * @param units  The scale factor applied to the epsilon value of the depth buffer
     */
    public void setDepthOffsets(double factor, double units);

    /**
     * <p/>
     * Enable or disable depth offsets. When depth offsets are enabled, all pixels have a depth value that is
     * slightly offset from the original computed depth based on the projected vertex coordinate. The specific
     * depth offset applied to each pixel depends on the depth buffer resolution and format, as well as the
     * configured depth offset factor and units.
     * <p/>
     * The depth offset only applies to polygons, although for these purposes polygons rendered with a
     * DrawStyle of POINT or LINE count as polygons.
     * <p/>
     * By default depth offsetting is disabled.
     *
     * @param enable True if depth offsets should be enabled
     *
     * @see #setDepthOffsets(double, double)
     */
    public void setDepthOffsetsEnabled(boolean enable);

    /**
     * Convenience method to set the DrawStyle for both front and back-facing polygons to the same style.
     *
     * @param style The new DrawStyle for both front and back polygons
     *
     * @throws NullPointerException if style is null
     */
    public void setDrawStyle(DrawStyle style);

    /**
     * <p/>
     * Set the DrawStyle to be used for front-facing polygons and back-facing polygons. The <var>front</var>
     * DrawStylespecifies how front-facing polygons are rendered, and <var>back</var> specifies how
     * back-facing polygons are rendered.
     * <p/>
     * The facing of a polygon is determined by the counter-clockwise ordering of its vertices. When a polygon
     * is rendered, if it's vertices are specified in counter-clockwise order then the polygon 'faces' the
     * viewer. If they're presented in clockwise order the polygon faces away from the viewer.
     * <p/>
     * Although it's possible to have both front and back set to {@link DrawStyle#NONE}, it generally will
     * make little sense, since every polygon would then be culled.
     * <p/>
     * The starting draw styles are SOLID for front-facing polygons, and NONE for back-facing polygons.
     *
     * @param front The DrawStyle for front-facing polygons
     * @param back  The DrawStyle for back-facing polygons
     *
     * @throws NullPointerException if front or back are null
     * @see DrawStyle
     */
    public void setDrawStyle(DrawStyle front, DrawStyle back);

    /**
     * <p/>
     * Set the color masks for each of the four available color components. If a color component's
     * corresponding boolean is set to true, that component can be written into when storing a color value
     * into the framebuffer. If it is false, that component will not be written into.
     * <p/>
     * When a color mask of (true, true, true, true) is used, the entire color will be written. A mask of
     * (false, false, false, false) will write no color, even if the pixel passes all tests; however, in this
     * case the pixel's depth would still be written and the stencil buffer would still be updated (depending
     * on those buffer's masks).
     * <p/>
     * When a component is disabled, it does not mean that a 0 is written for that component, but that the
     * previous component at the pixel is preserved.
     * <p/>
     * The starting state uses a mask of (true, true, true, true).
     *
     * @param red   True if red color values can be written
     * @param green True if green color values can be written
     * @param blue  True if blue color values can be written
     * @param alpha True if alpha color values can be written
     */
    public void setColorWriteMask(boolean red, boolean green, boolean blue, boolean alpha);

    /**
     * <p/>
     * Set whether or not blending is enabled. When blending is enabled, incoming pixels will be blended with
     * the destination pixel based on the configured {@link BlendFunction} and {@link BlendFactor
     * BlendFactors}.
     * <p/>
     * The starting state has blending disabled.
     *
     * @param enable True if blending is enabled
     */
    public void setBlendingEnabled(boolean enable);

    /**
     * <p/>
     * Set the 'constant' blend color used by certain {@link BlendFactor BlendFactors}. This accepts four
     * arbitrary float values ordered red, green, blue and alpha. The values are clamped to [0, 1].
     * <p/>
     * The default blend color is (0, 0, 0, 0).
     *
     * @param color The new blend color to use
     *
     * @throws NullPointerException if color is null
     */
    public void setBlendColor(@Const Vector4 color);

    /**
     * Set the {@link BlendFunction} and source and destination {@link BlendFactor BlendFactors} used by both
     * RGB and alpha values.
     *
     * @param function The BlendFunction to use for both RGB and alpha values
     * @param src      The BlendFactor applied to source/incoming pixels
     * @param dst      The BlendFactor applied to destination/prior pixels
     *
     * @throws NullPointerException     if function, src, or dst are null
     * @throws IllegalArgumentException if dst is {@link BlendFactor#SRC_ALPHA_SATURATE}
     * @see #setBlendModeAlpha(BlendFunction, BlendFactor, BlendFactor)
     * @see #setBlendModeRGB(BlendFunction, BlendFactor, BlendFactor)
     */
    public void setBlendMode(BlendFunction function, BlendFactor src, BlendFactor dst);

    /**
     * <p/>
     * Set the BlendFunction and source and destination BlendFactors that will be used to blend the RGB values
     * together. This does not modify the blending functions used for alpha values, which can be blended
     * separately from the RGB components.
     * <p/>
     * The default RGB blending uses the ADD function, a source factor of ONE, and a destination factor of
     * ZERO.
     *
     * @param function The BlendFunction to use for RGB blending
     * @param src      The BlendFactor applied to source RGB values of the incoming pixel
     * @param dst      The BlendFactor applied to the destination RGB values of the prior pixel
     *
     * @throws NullPointerException     if function, src, or dst are null
     * @throws IllegalArgumentException if dst is {@link BlendFactor#SRC_ALPHA_SATURATE}
     * @see #setBlendModeAlpha(BlendFunction, BlendFactor, BlendFactor)
     */
    public void setBlendModeRGB(BlendFunction function, BlendFactor src, BlendFactor dst);

    /**
     * <p/>
     * Set the BlendFunction and source and destination BlendFactors that will be used to blend the alpha
     * values together. This does not modify the blending functions used with RGB components, which are
     * blended separately from alpha values.
     * <p/>
     * The default alpha blending uses the ADD function, a source factor of ONE, and a destination factor of
     * ZERO.
     *
     * @param function The BlendFunction to use for RGB blending
     * @param src      The BlendFactor applied to the source alpha value
     * @param dst      The BlendFactor applied to the destination alpha value
     *
     * @throws NullPointerException     if function, src, or dst are null
     * @throws IllegalArgumentException if dst is {@link BlendFactor#SRC_ALPHA_SATURATE}
     * @see #setBlendModeRGB(BlendFunction, BlendFactor, BlendFactor)
     */
    public void setBlendModeAlpha(BlendFunction function, BlendFactor src, BlendFactor dst);

    /**
     * <p/>
     * Set the index buffer that is used when rendering. If the index buffer is not null, the rendered
     * polygons are described by the index values in <var>indices</var> and the polygon type specified by
     * {@link #render(PolygonType, int, int)}.
     * <p/>
     * The VertexBufferObject might be updated before rendering depending on its update policy. The
     * VertexBufferObject must have an integral data type of INT, SHORT, or BYTE. The integer indices are
     * considered as unsigned values, even though unsigned integers are not supported in Java.
     * <p/>
     * When the index buffer is null, any previous buffer is unbound and rendered polygons are constructed by
     * accessing the vertex attributes consecutively.
     *
     * @param indices An array of integer indices into the configured vertex attributes
     *
     * @throws IllegalArgumentException if indices is not null and has a type of FLOAT
     */
    public void setIndices(ElementBuffer indices);

    /**
     * <p/>
     * Render polygons by iterating through <var>count</var> of the configured indices starting at
     * <var>offset</var>. Polygons are formed based on the polygon type provided. If there is a non-null bound
     * index buffer count index values are read and used to access the vertex attributes. If there is no index
     * buffer, vertex attributes are read linearly starting at offset.
     * <p/>
     * The Renderer interface does not provide a mechanism to configure the active set of vertex attributes
     * needed to form the actual vertices. Specifying vertex attributes depends on the type of Renderer
     * because the fixed-function pipeline and programmable shaders use different mechanisms. However, they
     * can both perform the actual rendering in the same manner. It is possible that specific Renderer types
     * will support more rendering methods, such as rendering multiple instances.
     *
     * @param polyType The type of polygon to render
     * @param offset   The index of the first vertex to render
     * @param count    The number of vertices to render
     *
     * @return The number of polygons rendered
     *
     * @throws IllegalArgumentException  if first or count are negative
     * @throws IndexOutOfBoundsException if there's a bound index buffer and the configured offset and count
     *                                   would access invalid elements
     */
    public int render(PolygonType polyType, int offset, int count);

    /**
     * Manually reset all of the OpenGL-related state in this Renderer to the defaults described in the
     * interface-level documentation. When a Surface is {@link HardwareAccessLayer#setActiveSurface(Surface)
     * activated}, reset is automatically called for its renderers.
     */
    public void reset();

    /**
     * <p/>
     * Clear the framebuffers of the current Surface based on <var>clearColor</var>, <var>clearDepth</var> and
     * <var>clearStencil</var>. The color buffer will be cleared to (0, 0, 0, 0), the depth will be cleared to
     * 1, and the stencil buffer will be cleared to 0.
     *
     * @param clearColor   True if the color buffer is cleared
     * @param clearDepth   True if the depth buffer is cleared
     * @param clearStencil True if the stencil buffer is cleared
     *
     * @see #clear(boolean, boolean, boolean, Vector4, double, int)
     */
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil);

    /**
     * <p/>
     * Clear the framebuffers of the current Surface based on <var>clearColor</var>, <var>clearDepth</var>,
     * and <var>clearStencil</var>. If the color buffer is cleared, the color value written into the buffer is
     * affected by the current color mask state. If the depth buffer is cleared, the depth value written is
     * affected by the current depth mask state. If the stencil buffer is cleared, the stencil value written
     * is affected by the current stencil mask configured for front-facing polygons. Only the current viewport
     * is affected by the clear operation.
     * <p/>
     * Like {@link #setBlendColor(Vector4)}, the color holds arbitrary values ordered red, green, blue and
     * alpha. They are clamped to [0, 1] before clearing the color buffer.
     * <p/>
     * Tasks are responsible for invoking this method as needed to clear the Surface. Depending on what is
     * rendered, it may not be necessary to clear the buffers, in which case performance can be improved.
     *
     * @param clearColor   True if the color buffer is cleared
     * @param clearDepth   True if the depth buffer is cleared
     * @param clearStencil True if the stencil buffer is cleared
     * @param color        The color that the color buffer is cleared to
     * @param depth        The depth value that depth buffer is cleared to, in [0, 1]
     * @param stencil      The stencil value that the stencil buffer is cleared to
     *
     * @throws NullPointerException     if color is null when clearColor is true
     * @throws IllegalArgumentException if depth is not in [0, 1] when clearDepth is true
     */
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil, @Const Vector4 color,
                      double depth, int stencil);

    /**
     * <p/>
     * Set the active region of the current Surface. This effectively changes the location and size of the
     * area being rendered into. The (x,y) coordinates represent the lower-left corner of the viewport,
     * relative to the lower left corner of the current Surface. The width and height can extend beyond the
     * edges of the Surface, but are clamped to implementation maximums.
     * <p/>
     * When a Surface is first activated, the viewport is set to span the entire content of the Surface. When
     * a Surface is {@link #clear(boolean, boolean, boolean, Vector4, double, int) cleared} it uses the
     * current viewport, and only clears the content within it.
     *
     * @param x      The x coordinate of the new viewport
     * @param y      The y coordinate of the new viewport
     * @param width  The width of the new viewport
     * @param height The height of the new viewport
     *
     * @throws IllegalArgumentException if any argument is less than 0
     */
    public void setViewport(int x, int y, int width, int height);
}
