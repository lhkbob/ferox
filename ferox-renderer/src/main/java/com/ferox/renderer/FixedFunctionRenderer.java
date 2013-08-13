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
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;

/**
 * <p/>
 * The FixedFunctionRenderer describes a Renderer that exposes the majority of useful state described by the
 * fixed-function pipeline of pre-3.1 OpenGL implementations. Although the OpenGL versions between 2.0 and 3.0
 * had support for GLSL shaders (and even earlier using extensions), this Renderer does not expose that since
 * it's more robust to completely separate fixed and programmable pipelines.
 * <p/>
 * For the purposes of lighting, the renderer supports 8 simultaneous lights. For the purposes of texturing,
 * the renderer supports 4 simultaneous textures. Lighting is computed using the Blinn-Phong model adopted by
 * OpenGL and will use single-sided lighting, separate specular colors, and smooth shading.
 *
 * @author Michael Ludwig
 */
public interface FixedFunctionRenderer extends Renderer {
    /**
     * <p/>
     * The CombineFunction enum describes the advanced texture combining functionality when multiple texture
     * units are used. The final color for a combined texture is computed by evaluating one CombineFunction
     * for the RGB values and one CombineFunction for the alpha values.
     * <p/>
     * Each CombineFunction uses up to three inputs to compute the final component values. Each input gets a
     * source and operand, labeled below: <ul> <li>N = current input, one of {0, 1, 2}</li> <li>opN =
     * configured operand for Nth input</li> <li>srcN = configured source for Nth input</li> </ul> The final
     * input used by the combine function is <code>argN = opN(srcN)</code>. For functions on RGB values, it is
     * the tuple (argNr, argNg, argNb) and math is performed per component. then argN = opN(srcN).
     * <p/>
     * All values are color values clamped to the range [0, 1].
     */
    public static enum CombineFunction {
        /**
         * The REPLACE function is defined as: <code>f(arg0,arg1,arg2) = arg0</code>
         */
        REPLACE,
        /**
         * The MODULATE function is defined as: <code>f(arg0,arg1,arg2) = arg0*arg1</code>
         */
        MODULATE,
        /**
         * The ADD function is defined as: <code>f(arg0,arg1,arg2) = arg0 + arg1</code>
         */
        ADD,
        /**
         * The ADD_SIGNED function is defined as: <code>f(arg0,arg1,arg2) = arg0 + arg1 - 0.5</code>
         */
        ADD_SIGNED,
        /**
         * The INTERPOLATE function is defined as: <code>f(arg0,arg1,arg2) = arg2*arg0 + (1-arg2)*arg1</code>
         */
        INTERPOLATE,
        /**
         * The SUBTRACT function is defined as: <code>f(arg0,arg1,arg2) = arg0 - arg1</code>
         */
        SUBTRACT,
        /**
         * <p/>
         * The DOT3_RGB is a special CombineFunction that's useful when performing bump-mapping. Instead of
         * working component wise like the other functions, DOT3_RGB computes a single value using the red,
         * green, and blue values and then stores that value in each of the RGB components. It is computed as
         * follows:
         * <p/>
         * <pre>
         * 4 x ((r0 - 0.5) * (r1 - 0.5) +
         *      (g0 - 0.5) * (g1 - 0.5) +
         *      (b0 - 0.5) * (b1 - 0.5))
         * </pre>
         * <p/>
         * where arg0 = (r0, g0, b0) and arg1 = (r1, g1, b1).
         * <p/>
         * The DOT3_RGB function cannot be used with the alpha CombineFunction. The alpha value for the final
         * texture is computed as normal using the configured function for that component.
         */
        DOT3_RGB,
        /**
         * DOT3_RGBA is identical to DOT3_RGB except that the computed dot-product value is stored in the
         * alpha component is well. This value overwrites any computed alpha value from the configured alpha
         * CombineFunction (the alpha function is ignored). Like DOT3_RGB, this CombineFunction cannot be used
         * for the alpha function.
         */
        DOT3_RGBA
    }

    /**
     * <p/>
     * Each CombineOperand represents a simple function that acts on component values of some color source,
     * specified by {@link CombineSource}. {@link CombineFunction} evaluates the configured CombineOperands
     * with the correct CombineSources before combining the computed values into a final color.
     * <p/>
     * Multi-texturing uses two CombineFunctions, one for alpha values and one for the RGB values. When
     * specifying the CombineOperandss for the alpha function, it is invalid to use the operands that rely on
     * RGB data.
     * <p/>
     * When describing the operands below, Cs refers to the RGB components and As refers to the alpha
     * component. Operations are done component-wise.
     */
    public static enum CombineOperand {
        /**
         * This operand returns Cs unmodified. It's invalid to use for the alpha CombineOperand.
         */
        COLOR,
        /**
         * This operand returns As unmodified. It can be used with both the alpha and RGB CombineFunctions.
         */
        ALPHA,
        /**
         * This operand returns (1 - Cs). It cannot be used with the alpha CombineOperand.
         */
        ONE_MINUS_COLOR,
        /**
         * This operand returns (1 - As). It can be used with both the alpha and RGB CombineFunctions.
         */
        ONE_MINUS_ALPHA
    }

    /**
     * <p/>
     * The source of color and alpha values for an associated {@link CombineOperand}. Up to three
     * CombineSources and CombineOperands can be used by {@link CombineFunction} to provide more complex
     * texture combination algorithms. <ul> <li>CURR_TEX = color of this texture image</li> <li>PREV_TEX =
     * color of the texture in the texture unit processed just before this one (if this is the first unit,
     * then it's the same as VERTEX_COLOR)</li> <li>CONST_COLOR = environment color of this texture</li>
     * <li>VERTEX_COLOR = color computed based on material color and lighting</li> <li>TEXi = color of the
     * texture image bound to the given unit</li> </ul>
     * <p/>
     * <b>Note:</b> not all TEXi will be supported because hardware may not have that many texture units
     * available. Units beyond 8 are included for advanced graphics cards (or future cards).
     */
    public static enum CombineSource {
        CURR_TEX,
        PREV_TEX,
        CONST_COLOR,
        VERTEX_COLOR,
        TEX0,
        TEX1,
        TEX2,
        TEX3,
        TEX4,
        TEX5,
        TEX6,
        TEX7,
        TEX8,
        TEX9,
        TEX10,
        TEX11,
        TEX12,
        TEX13,
        TEX14,
        TEX15,
        TEX16,
        TEX17,
        TEX18,
        TEX19,
        TEX20,
        TEX21,
        TEX22,
        TEX23,
        TEX24,
        TEX25,
        TEX26,
        TEX27,
        TEX28,
        TEX29,
        TEX30,
        TEX31
    }

    /**
     * TexCoord represents the enum of available texture coordinates used to access a texture. Each texture
     * can be accessed by up to four coordinates: (s, t, r, q). Often however, only the first two or three are
     * used. When all four are specified, the q coordinate acts as a homogeneous coordinate. If r is not
     * provided for when a Texture requires three coordinates, undefined results occur.
     */
    public static enum TexCoord {
        /**
         * The first texture coordinate.
         */
        S,
        /**
         * The second texture coordinate.
         */
        T,
        /**
         * The third texture coordinate.
         */
        R,
        /**
         * The fourth texture coordinate.
         */
        Q
    }

    /**
     * When mapping a Texture onto a rendered Geometry, texture coordinates are used to wrap the image about
     * the shape. There are multiple options for how these coordinates are derived.
     */
    public static enum TexCoordSource {
        /**
         * Texture coordinates are taken from the associated vertex attribute of the Geometry.
         */
        ATTRIBUTE,
        /**
         * Texture coordinates are generated as follows: <ul> <li>(xe, ye, ze, we) is the vertex in eye
         * coordinates</li> <li>(p1, p2, p3, p4) is the configured plane equation</li> <li>M^-1 is the inverse
         * of the modelview matrix when (p1, p2, p3, p4) were set</li> <li>(p1', p2', p3', p4') = (p1, p2, p3,
         * p4) x M^-1</li> </ul> Then the texture coordinate g is: <code> g = p1'*xe + p2'*ye + p3'*ze +
         * p4'*we </code>
         */
        EYE,
        /**
         * Texture coordinates are generated as follows: <ul> <li>(xo, yo, zo, wo) is the vertex in object
         * coordinates</li> <li>(p1, p2, p3, p4) is the configured plane equation</li> </ul> Then the texture
         * coordinate g is: <code> g = p1*xo + p2*yo + p3*zo + p4*wo </code>
         */
        OBJECT,
        /**
         * Texture coordinates are generated to correctly look up a Texture whose pixel data is organized as a
         * spherical reflection map.
         */
        SPHERE,
        /**
         * Texture coordinates are generated and set to the normal vector of each associated vertex.
         */
        NORMAL,
        /**
         * Texture coordinates are generated and set to a computed reflection vector at each vertex. The
         * reflection vector is based on the vertex normal and the current eye point.
         */
        REFLECTION
    }

    /**
     * <p/>
     * Get the current state configuration for this FixedFunctionRenderer. The returned instance can be used
     * in {@link #setCurrentState(ContextState)} with any FixedFunctionRenderer created by the same Framework
     * as this renderer.
     * <p/>
     * Because the fixed-function pipeline maintains a large amount of state, getting and setting the entire
     * state should be used infrequently.
     *
     * @return The current state
     */
    public ContextState<FixedFunctionRenderer> getCurrentState();

    /**
     * <p/>
     * Set the current state of this renderer to equal the given state snapshot. <var>state</var> must have
     * been returned by a prior call to {@link #getCurrentState()} from a FixedFunctionRenderer created by
     * this renderer's Framework or behavior is undefined.
     * <p/>
     * Because the fixed-function pipeline maintains a large amount of state, getting and setting the entire
     * state should be used infrequently.
     *
     * @param state The state snapshot to update this renderer
     *
     * @throws NullPointerException if state is null
     */
    public void setCurrentState(ContextState<FixedFunctionRenderer> state);

    /**
     * <p/>
     * Set whether or not eye-space fogging is enabled. If this is enabled, each rendered pixel's color value
     * is blended with the configured fog color (at the time of the rendering) based on the fog equation. The
     * fog equation can be linear, set with {@link #setFogLinear(double, double)}, or exponential, set with
     * {@link #setFogExponential(double, boolean)}.
     * <p/>
     * The default state starts with fog disabled.
     *
     * @param enable True if fogging is enabled
     */
    public void setFogEnabled(boolean enable);

    /**
     * <p/>
     * Set the color used when fogging is enabled. The color components are clamped to [0, 1] and are ordered
     * red, green, blue and alpha in the vector.
     * <p/>
     * The default fog color is (0, 0, 0, 1).
     *
     * @param color The new fog color
     *
     * @throws NullPointerException if color is null
     */
    public void setFogColor(@Const Vector4 color);

    /**
     * <p/>
     * Set the fog range for linear fog to be between start and end. Anything before start is un-fogged, and
     * anything after end is completely fog.
     * <p/>
     * Calling this method configures the Renderer to subsequently use a linear blending function while
     * fogging is enabled. The fog blend factor is computed as (e - c) / (e - s), where e = end distance, s =
     * start distance and c = camera eye distance for a pixel.
     * <p/>
     * The default fog blending function is exponential with a density of 1.
     *
     * @param start The start range for the linear fog
     * @param end   The end range for the linear fog
     *
     * @throws IllegalArgumentException if start >= end
     */
    public void setFogLinear(double start, double end);

    /**
     * <p/>
     * Set the fog to use an exponential blending factor with the given density function. Fog will be rendered
     * using an exponential equation until {@link #setFogLinear(double, double)} is called, which sets it to
     * use linear fog instead.
     * <p/>
     * The fog blend factor is computed two different ways, depending on squared. If squared is true, then it
     * is e^(-density * c), else it is e^(-(density * c)^2), where c is the camera eye distance.
     * <p/>
     * The default fog blending function is exponential with a density of 1.
     *
     * @param density The new fog density
     * @param squared True if the exponent should be squared
     *
     * @throws IllegalArgumentException if density is less than 0
     */
    public void setFogExponential(double density, boolean squared);

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
     * Configure the alpha test to be used when rendering. If the given test Comparison returns true when a
     * pixel's alpha value is compared to refValue, then the pixel will continue to be processed. Using a
     * Comparison of ALWAYS effectively disables the alpha test.
     * <p/>
     * The starting state uses a test of ALWAYS and a reference value of 1.
     *
     * @param test     The new alpha test Comparison to use
     * @param refValue The reference value that pixel's alphas are compared to
     */
    public void setAlphaTest(Comparison test, double refValue);

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
     * Set whether or not lighting is calculated for rendered geometry. When lighting is enabled, the global
     * ambient light, and all enabled lights have their colors summed together based on how much each
     * contributes to a vertex's color. In the end, the lit pixel is then fed through the texturing pipeline.
     * The amount that each light contributes to a polygon is based on how the light's direction shines onto
     * the polygon's face, and how the configured material colors interact with each light's colors.
     * <p/>
     * For best results, normals should be configured before rendering a geometry.
     * <p/>
     * By default, lighting is disabled.
     *
     * @param enable True if lighting is enabled
     */
    public void setLightingEnabled(boolean enable);

    /**
     * <p/>
     * Set the ambient color that's always added to pixel colors when lighting is enabled. Components in the
     * vector are ordered red, green, blue, alpha and are clamped to be above 0. Values above 1 cause the
     * light to be in a higher range and will be carried through the lighting function until the final color
     * must be clamped.
     * <p/>
     * The default ambient color is (0.2, 0.2, 0.2, 1).
     *
     * @param ambient The new global ambient color
     *
     * @throws NullPointerException if ambient is null
     */
    public void setGlobalAmbientLight(@Const Vector4 ambient);

    /**
     * <p/>
     * Set whether or not the given light is enabled. If it's set to true then it will be used to compute
     * lighting when the overall lighting system has been enabled.
     * <p/>
     * Every light is initially disabled.
     *
     * @param light  The given light, from 0 to the maximum number of lights available
     * @param enable True if it's enabled
     *
     * @throws IndexOutOfBoundsException if light is greater than or equal to 8, or if light is less than 0
     */
    public void setLightEnabled(int light, boolean enable);

    /**
     * <p/>
     * Set the homogenous position for the given light. This position vector can be used to create two
     * different types of light: point/spotlight and infinite directional lights. This is based on the
     * interpretation that a directional light is a point light that's shining from infinitely far away.
     * <p/>
     * When the given pos vector has a w component of 1, the light will act as a point or spotlight located at
     * (x, y, z). When the w component is 0, then the light is infinitely far away in the direction of (x, y,
     * z). In effect the light is a directional light shining along (-x, -y, -z). Other values for w are not
     * allowed.
     * <p/>
     * The given position vector is transformed by the current modelview matrix at the time that this method
     * is called, which converts the position into 'eye' space so that all lighting calculations can be done
     * in eye space when a Geometry is finally rendered.
     * <p/>
     * Every light's default position is (0, 0, 1, 0) so it will act as a directional lighting shining along
     * (0, 0, -1).
     *
     * @param light The given light, from 0 to the maximum number of lights available
     * @param pos   The position vector for this light
     *
     * @throws NullPointerException      if pos is null
     * @throws IllegalArgumentException  if pos.w is not 0 or 1
     * @throws IndexOutOfBoundsException if light is greater than or equal to 8, or if light is less than 0
     */
    public void setLightPosition(int light, @Const Vector4 pos);

    /**
     * <p/>
     * Set the ambient, diffuse and specular colors for the given light. Every enabled light's colors are
     * multiplied with the corresponding material colors and then modulated by the amount of contribution for
     * the light. This scaled color is then added to the final color. Thus a red diffuse material color and a
     * green diffuse light color would make a black contribution since (1, 0, 0, 1) * (0, 1, 0, 1) is (0, 0,
     * 0, 1).
     * <p/>
     * The colors stored in <var>amb</var>, <var>diff</var> and <var>spec</var> have components ordered red,
     * green, blue and alpha. The values are clamped to be above 0. Values higher than 1 cause the light to be
     * outside the standard range and can produce final colors brighter than white that will be clamped to [0,
     * 1].
     * <p/>
     * The default colors of the 0th light are a diffuse and specular of (1, 1, 1, 1) and an ambient of (0, 0,
     * 0, 1). All other lights use (0, 0, 0, 1) for all of the colors.
     *
     * @param light The given light to configure
     * @param amb   The new ambient color of the light
     * @param diff  The new diffuse color of the light
     * @param spec  The new specular color of the light
     *
     * @throws NullPointerException      if amb, diff, or spec are null
     * @throws IndexOutOfBoundsException if light is greater than or equal to 8, or if light is less than 0
     */
    public void setLightColor(int light, @Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec);

    /**
     * <p/>
     * Set the spotlight direction and cutoff angle for the given light. Although this can be set on any
     * light, it only has a visible effect when the light has a position that's not infinite (e.g. it's not a
     * direction light). When a light is set to a point or spotlight, the spotlight direction and cutoff angle
     * control where the light shines. Like the light position, the spotlight direction is transformed by the
     * current modelview matrix when this is called.
     * <p/>
     * The cutoff angle is the degree measure of the half-angle of a cone of light that expands outwards from
     * the light's position in the direction of dir. Anything outside of this cone is not lit. Acceptable
     * values for the angle are in the range [0, 90] and 180. 180 is a special value that causes the light to
     * act as a spherical point light. In this case, the spotlight direction has no effect on the lighting.
     * <p/>
     * Every light starts with a spotlight direction of (0, 0, -1) and a cutoff angle of 180, and an exponent
     * of 0.
     *
     * @param light    The given light to configure
     * @param dir      The direction that spotlights shine
     * @param angle    The cutoff angle for the light from the spotlight
     * @param exponent The sharpness with which the spotlight cutoff occurs, or 0 for a harsh boundary
     *
     * @throws NullPointerException      if dir is null
     * @throws IllegalArgumentException  if angle is not in [0, 90] or equal to 180, or if exponent is not in
     *                                   [0, 128]
     * @throws IndexOutOfBoundsException if light is greater than or equal to 8, or if light is less than 0
     */
    public void setSpotlight(int light, @Const Vector3 dir, double angle, double exponent);

    /**
     * <p/>
     * Set the light attenuation factors used for spotlights and point lights. Light attenuation is an
     * approximation of the light's intensity fading with distance from the light source. To compute this
     * distance requires a finite light's position, which is why attenuation is not used for directional
     * lights.
     * <p/>
     * When determining the final contribution of a given light, it is scaled by a attenuation factor computed
     * from these configured constants and a pixel's distance to the light. If we let c, l, and q represent
     * the constant, linear and quadratic terms and d is the distance to the light, then the attenuation
     * factor is computed as: <code>1 / (c + l * d + q * d^2)</code>
     * <p/>
     * Thus, the default values of c = 1, l = q = 0 cause the attenuation factor to always equal 1 and so the
     * light never fades. Setting the linear and quadratic terms to non-zero values can cause a decrease in
     * performance. Undefined results occur if the attenuation values are given that cause the divisor to
     * equal 0.
     * <p/>
     * The default light attenuation for each light has c = 1, and l = q = 0, which produces no falloff.
     *
     * @param light     The given light to configure
     * @param constant  The constant attenuation term, >= 0
     * @param linear    The linear attenuation factor, >= 0
     * @param quadratic The quadratic attenuation factor, >= 0
     *
     * @throws IllegalArgumentException  if constant, linear or quadratic < 0
     * @throws IndexOutOfBoundsException if light is greater than or equal to 8, or if light is less than 0
     */
    public void setLightAttenuation(int light, double constant, double linear, double quadratic);

    /**
     * <p/>
     * Set the material colors used when rendering primitives. These colors correspond to the amount of each
     * color that's reflected by each type of light component (ambient, diffuse, and specular). Emmissive
     * light is special because it represents the amount of light that the material is generating itself. This
     * color is added to the final lit color without modulation by the light's colors.
     * <p/>
     * When lighting is disabled, the the diffuse color value is used as the solid, unlit color for the
     * rendered shape. This is because the diffuse color generally represents the 'color' of a lit object
     * while the other color values add more subtle shading to it.
     * <p/>
     * The colors stored in <var>amb</var>, <var>diff</var>, <var>spec</var> and <var>emm</var> have
     * components ordered red, green, blue and alpha. The values for <var>amb</var>, <var>diff</var> and
     * <var>spec</var> are clamped to be in [0, 1]. The values in <var>emm</var> are clamped to be above 0 and
     * can be outside the standard range just like light colors.
     * <p/>
     * The default ambient color is (0.2, 0.2, 0.2, 1), the diffuse is (0.8, 0.8, 0.8, 1), and the specular
     * and emissive colors are (0, 0, 0, 1).
     *
     * @param amb  The ambient color of the material
     * @param diff The diffuse color of the material, or solid color when lighting is disabled
     * @param spec The specular color of the material
     * @param emm  The emissive color of the material
     *
     * @throws NullPointerException if any color is null
     */
    public void setMaterial(@Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec, @Const Vector4 emm);

    /**
     * Set just the diffuse material color, leaving the other color properties unchanged. The default diffuse
     * color is (0.8, 0.8, 0.8, 1).
     *
     * @param diff The diffuse color, or solid color when lighting is disabled
     *
     * @throws NullPointerException if diff is null
     * @see #setMaterial(com.ferox.math.Vector4, com.ferox.math.Vector4, com.ferox.math.Vector4,
     *      com.ferox.math.Vector4)
     */
    public void setMaterialDiffuse(@Const Vector4 diff);

    /**
     * Set just the ambient material color, leaving the other color properties unchanged. The default ambient
     * color is (0.2, 0.2, 0.2, 1). This color is ignored when lighting is disabled.
     *
     * @param amb The ambient color of the material
     *
     * @throws NullPointerException if amb is null
     * @see #setMaterial(com.ferox.math.Vector4, com.ferox.math.Vector4, com.ferox.math.Vector4,
     *      com.ferox.math.Vector4)
     */
    public void setMaterialAmbient(@Const Vector4 amb);

    /**
     * Set just the specular material color, leaving the other color properties unchanged. The default
     * specular color is (0.0, 0.0, 0.0, 1). This color is ignored when lighting is disabled.
     *
     * @param spec The specular color of the material
     *
     * @throws NullPointerException if spec is null
     * @see #setMaterial(com.ferox.math.Vector4, com.ferox.math.Vector4, com.ferox.math.Vector4,
     *      com.ferox.math.Vector4)
     */
    public void setMaterialSpecular(@Const Vector4 spec);

    /**
     * Set just the emitted light of the material, leaving the other color properties unchanged. The default
     * emitted color is (0.0, 0.0, 0.0, 1). This color is ignored when lighting is disabled.
     *
     * @param emm The emitted color of the material
     *
     * @throws NullPointerException if emm is null
     * @see #setMaterial(com.ferox.math.Vector4, com.ferox.math.Vector4, com.ferox.math.Vector4,
     *      com.ferox.math.Vector4)
     */
    public void setMaterialEmissive(@Const Vector4 emm);

    /**
     * <p/>
     * Set the material shininess to use when lighting is enabled. This shininess acts as an exponent on the
     * specular intensity, and can be used to increase or dampen the brightness of the specular highlight. The
     * shininess is an exponent in the range [0, 128], where a value of 0 causes the highlight to be
     * non-existent and 128 causes an extremely bright highlight.
     * <p/>
     * The default shininess is 0.
     *
     * @param shininess The material shininess
     *
     * @throws IllegalArgumentException if shininess is not in [0, 128]
     */
    public void setMaterialShininess(double shininess);

    /**
     * <p/>
     * Set the Texture to be bound to the given unit. Specifying a null image unbinds any previous Texture. A
     * non-null Texture will affect the rendering based on the configured texture environment for the unit.
     * <p/>
     * By default every texture unit hase no texture bound.
     *
     * @param tex   The texture unit
     * @param image The Texture to be bound to tex
     *
     * @throws IllegalArgumentException  if tex is less than 0
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     */
    public void setTexture(int tex, Sampler image);

    /**
     * <p/>
     * Set the texture color that's used for this unit. The texture color is the constant color used by {@link
     * CombineSource#CONST_COLOR}.
     * <p/>
     * The color components are ordered red, green, blue, and alpha in the vector. Values are clamped to the
     * range [0, 1].
     * <p/>
     * The default texture color for every unit is (0, 0, 0, 0).
     *
     * @param tex   The texture unit
     * @param color The new constant texture color for the unit
     *
     * @throws NullPointerException      if color is null
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     */
    public void setTextureColor(int tex, @Const Vector4 color);

    /**
     * <p/>
     * Set the texture coordinate source for all four coordinates to <var>gen</var>, for the given texture
     * unit.
     * <p/>
     * The default source for every texture unit and coordinate is ATTRIBUTE.
     *
     * @param tex The texture unit
     * @param gen The TexCoordSource for all four coordinates
     *
     * @throws NullPointerException      if gen is null
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     * @see #setTextureCoordGeneration(int, TexCoord, TexCoordSource)
     */
    public void setTextureCoordGeneration(int tex, TexCoordSource gen);

    /**
     * <p/>
     * Set the texture coordinate source for the specified texture coordinate for the given texture unit.
     * These texture coordinates can be auto-generated or specified as part of the Geometry. See {@link
     * TexCoordSource} for a description of each source.
     * <p/>
     * The default state uses a source of ATTRIBUTE for every unit and all four coordinates.
     *
     * @param tex   The texture unit
     * @param coord The coordinate that's source is to be modified
     * @param gen   The new texture coordinate source
     *
     * @throws NullPointerException      if coord or gen are null
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     */
    public void setTextureCoordGeneration(int tex, TexCoord coord, TexCoordSource gen);

    /**
     * <p/>
     * Set the four values used for {@link TexCoordSource#OBJECT} generation for the given coordinate. These
     * four values represent (p1, p2, p3, p4) as described in TexCoordSource.OBJECT. Each texture coordinate
     * has its own four planar values (for each unit, too). These four values are independent of the four
     * values stored for the eye plane.
     * <p/>
     * For every texture unit, the S coordinate has a value of (1, 0, 0, 0), the T coordinate has a value of
     * (0, 1, 0, 0), and both R and Q have (0, 0, 0, 0).
     *
     * @param tex   The texture unit
     * @param coord The coordinate whose object plane will be set
     * @param plane The object plane that's used for this unit and coordinate
     *
     * @throws NullPointerException      if coord or plane are null
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     */
    public void setTextureObjectPlane(int tex, TexCoord coord, @Const Vector4 plane);

    /**
     * Set the object plane for S coordinate to the first row of the matrix {@code planes}, the plane for T to
     * the second row, the plane for R to the third row, and the plane for Q to the fourth row.
     *
     * @param tex    The texture unit
     * @param planes The matrix holding the 4 plane equations in row-order
     *
     * @throws NullPointerException      if planes is null
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     * @see #setTextureObjectPlane(int, com.ferox.renderer.FixedFunctionRenderer.TexCoord,
     *      com.ferox.math.Vector4)
     */
    public void setTextureObjectPlanes(int tex, @Const Matrix4 planes);

    /**
     * <p/>
     * Set the four values used for the {@link TexCoordSource#EYE} generation for the given coordinate and
     * texture. These four values represent (p1, p2, p3, p4) as described in TexCoordSource.EYE. These four
     * values are multiplied by the inverse of the current modelview matrix when this method is invoked. Like
     * with the object plane, each coordinate for each unit has its own set of four eye plane values. These
     * values are independent from the object plane.
     * <p/>
     * For every texture unit, the S coordinate has a value of (1, 0, 0, 0), the T coordinate has a value of
     * (0, 1, 0, 0), and both R and Q have (0, 0, 0, 0).
     *
     * @param tex   The texture unit
     * @param coord The coordinate whose eye plane will be set
     * @param plane The eye plane to be specified, values are before inverse modelview multiplication
     *
     * @throws NullPointerException      if coord or plane are null
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     */
    public void setTextureEyePlane(int tex, TexCoord coord, @Const Vector4 plane);

    /**
     * Set the eue plane for S coordinate to the first row of the matrix {@code planes}, the plane for T to
     * the second row, the plane for R to the third row, and the plane for Q to the fourth row.
     *
     * @param tex    The texture unit
     * @param planes The matrix holding the 4 plane equations in row-order
     *
     * @throws NullPointerException      if planes is null
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     * @see #setTextureEyePlane(int, com.ferox.renderer.FixedFunctionRenderer.TexCoord,
     *      com.ferox.math.Vector4)
     */
    public void setTextureEyePlanes(int tex, @Const Matrix4 planes);

    /**
     * <p/>
     * Set the transform matrix applied to texture coordinates before they are used to lookup texels in the
     * unit's texture image. For multiplication purposes, all coordinates are considered 4 dimensional
     * vectors. If no R coordinate is provided, 0 is used. If no Q coordinate is provided, 1 is used.
     * <p/>
     * All texture units start with the identity transform matrix.
     *
     * @param tex    The texture unit
     * @param matrix The texture coordinate transform matrix
     *
     * @throws NullPointerException      if matrix is null
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     */
    public void setTextureTransform(int tex, @Const Matrix4 matrix);

    /**
     * <p/>
     * Configure how the texture's RGB values for the unit, <var>text</var> are combined with the colors from
     * the previous stages of the pipeline. The {@link CombineFunction function} uses the three inputs
     * produced by each pair of {@link CombineOperand operand} and {@link CombineSource source} to produce a
     * color for the next stage in the pipeline.
     * <p/>
     * The default RGB combine configuration for every unit uses the MODULATE function, the 0th source is
     * CURR_TEX, the 1st source is PREV_TEX, and the 2nd source is CONST_COLOR. The 0th and 1st operands are
     * COLOR, and the 2nd is ALPHA.
     *
     * @param tex      The texture unit being modified
     * @param function The combine function to use
     * @param src0     The 0th texture source used by the function
     * @param op0      The operand applied to the 0th source
     * @param src1     The 1st texture source used by the function
     * @param op1      The operand applied to the 1st source
     * @param src2     The 2nd texture source used by the function
     * @param op2      The operand applied to the 2nd source
     *
     * @throws NullPointerException      if any argument is null
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     */
    public void setTextureCombineRGB(int tex, CombineFunction function, CombineSource src0,
                                     CombineOperand op0, CombineSource src1, CombineOperand op1,
                                     CombineSource src2, CombineOperand op2);

    /**
     * <p/>
     * Configure how the texture's alpha value for the unit, <var>text</var> is combined with the alphas from
     * the previous stages of the pipeline. The {@link CombineFunction function} uses the three inputs
     * produced by each pair of {@link CombineOperand operand} and {@link CombineSource source} to produce a
     * color for the next stage in the pipeline.
     * <p/>
     * The DOT3_RGB and DOT3_RGBA combine functions cannot be used. The COLOR and ONE_MINUS_COLOR operands
     * cannot be used.
     * <p/>
     * The default RGB combine configuration for every unit uses the MODULATE function, the 0th source is
     * CURR_TEX, the 1st source is PREV_TEX, and the 2nd source is CONST_COLOR. The 0th, 1st and 2nd operands
     * are ALPHA.
     *
     * @param tex      The texture unit being modified
     * @param function The combine function to use
     * @param src0     The 0th texture source used by the function
     * @param op0      The operand applied to the 0th source
     * @param src1     The 1st texture source used by the function
     * @param op1      The operand applied to the 1st source
     * @param src2     The 2nd texture source used by the function
     * @param op2      The operand applied to the 2nd source
     *
     * @throws NullPointerException      if any argument is null
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     */
    public void setTextureCombineAlpha(int tex, CombineFunction function, CombineSource src0,
                                       CombineOperand op0, CombineSource src1, CombineOperand op1,
                                       CombineSource src2, CombineOperand op2);

    /**
     * <p/>
     * Set the projection matrix that transforms eye-space coordinates into clip coordinates. After
     * perspective division (division by the 4th coordinate of a vertex, generally 1), the clip coordinates
     * are converted to normalized device coordinates. These are three-dimensional points that lie within the
     * cube (-1, -1, -1) to (1, 1, 1).
     * <p/>
     * An important effect of this transformation is that it flips the z-axis direction; after the modelview
     * matrix, negative z values extend in front of and away from the view.
     * <p/>
     * The starting projection matrix is the identity matrix.
     *
     * @param projection The new projection matrix
     *
     * @throws NullPointerException if projection is null
     */
    public void setProjectionMatrix(@Const Matrix4 projection);

    /**
     * <p/>
     * Set the combined modelview matrix that transforms object coordinates into the eye coordinate system.
     * This matrix is important because it is not only used during rendering, but its value impacts the final
     * stored values for light position, spotlight direction, and eye plane values for texture coordinate
     * generation.
     * <p/>
     * Care must be given then to call this at appropriate times with respect to the configuration points
     * mentioned above.
     * <p/>
     * Because of this, it is often useful to first set the modelview to a matrix that transforms 'world'
     * coordinates into eye space and specify all of the lights, etc. that are stored in world coordinates in
     * the application. Then for each Geometry to render, compute the product of the camera matrix and the
     * shape's to-world matrix and use this as that Geometry's modelview matrix.
     * <p/>
     * After the modelview transformation, the coordinate space is the 'viewer' located at the origin, looking
     * down the negative z-axis. Thus, more negative z-values are farther way in appearance (assuming a
     * perspective projection).
     * <p/>
     * The starting modelview matrix is the identity matrix.
     *
     * @param modelView The new modelview matrix
     *
     * @throws NullPointerException if modelView is null
     */
    public void setModelViewMatrix(@Const Matrix4 modelView);

    /**
     * <p/>
     * Set the VertexAttribute that is used as the source of vertex positions when {@link #render(PolygonType,
     * int, int)} is invoked. The vertex buffer's data type must store decimal data. The attribute can have an
     * element size of 2, 3 or 4. If the 4th component is not provided, it defaults to 1. If the 3rd component
     * is not provided, it defaults to 0. The vertex elements will be accessed linearly if no indices are
     * bound, or by the indices in the last bound element buffer.
     * <p/>
     * This updates the currently bound vertex position attribute. The bound attribute will remain unchanged
     * after rendering until this method is called again. Rendering will not be performed if no vertex
     * positions are bound. The attribute can be unbound if a null VertexAttribute is provided.
     * <p/>
     * By default, no vertices are bound and must be specified before rendering can occur successfully.
     *
     * @param vertices The VertexAttribute holding the position data and access information
     *
     * @throws IllegalArgumentException if vertices' element size is 1 or does not hold decimal data
     */
    public void setVertices(VertexAttribute vertices);

    /**
     * <p/>
     * Set the VertexAttribute that is used as a source of normal vectors when {@link #render(PolygonType,
     * int, int)} is invoked. The vertex buffer's data type must store decimal data. The attribute must have
     * an element size of 3. The normal elements will be accessed linearly if no indices are bound, or by the
     * indices in the last bound element buffer.
     * <p/>
     * This updates the currently bound normal attribute. The bound attribute will remain unchanged after
     * rendering until this method is called again. A normals attribute is not necessary if lighting is
     * disabled. If normals aren't bound when rendering with lighting, an undefined normal vector is used. The
     * attribute can be unbound if a null VertexAttribute is provided.
     * <p/>
     * By default, no normals are bound and any normal vector used for lighting is undefined.
     *
     * @param normals The VertexAttribute holding the normal vector data and access information
     *
     * @throws IllegalArgumentException if normals' element size is not 3 or does not hold decimal data
     */
    public void setNormals(VertexAttribute normals);

    /**
     * <p/>
     * Set the VertexAttribute that provides per-vertex colors when {@link #render(PolygonType, int, int)} is
     * invoked. The vertex buffer's data type must store decimal data, and the attribute's element size equal
     * to 3 or 4. Values are assumed to be between 0 and 1 to properly represent packed RGB colors. The first
     * primitive in a vertex's element is the red, the second is green, and the third is blue. If the element
     * size is 4, the fourth value is the alpha, otherwise the alpha is set to 1.
     * <p/>
     * This will replace the diffuse color that is specified in {@link #setMaterial(Vector4, Vector4, Vector4,
     * Vector4)} or {@link #setMaterialDiffuse(com.ferox.math.Vector4)}. The ambient, specular, and emissive
     * colors still affect the final rendering, but the diffuse color will be taken from this attribute.
     * <p/>
     * This updates the currently bound color attribute. The bound attribute will remain unchanged after
     * rendering until this method is called again. If a null attribute is bound, per-vertex coloring is
     * disabled and the diffuse color is set to the default.
     * <p/>
     * By default, no color attribute is bound.
     *
     * @param colors The VertexAttribute holding the color vector data
     *
     * @throws IllegalArgumentException if colors element size is not 3 or 4, or if it does not hold decimal
     *                                  data
     */
    public void setColors(VertexAttribute colors);

    /**
     * <p/>
     * Set the VertexAttribute that is used as a source of texture coordinates on the texture unit,
     * <var>tex</var> when {@link #render(PolygonType, int, int)}  is invoked. The attribute element size can
     * be any value between 1 and 4. If the element size of the attribute doesn't meet the expected coordinate
     * size of the bound texture, a default is used for the missing components. The 2nd and 3rd components
     * default to 0 and the 4th defaults to 1. The vertex buffer's data type must hold decimal data.
     * <p/>
     * This updates the currently bound texture coordinate attribute for the given texture unit. The bound
     * attribute will remain unchanged after rendering until this method is called again for the same texture
     * unit. Invoking this on one texture unit does not affect the binding of any other unit. Texture
     * coordinate attributes do not need to be bound to a texture unit if there is no bound texture image. If
     * a texture is bound without texture coordinates, an undefined texture coordinate is used. The attribute
     * can be unbound from the texture unit if a null VertexAttribute is provided.
     * <p/>
     * Every texture unit initially has no attribute bound.
     *
     * @param tex       The texture unit to bind <var>texCoords</var> to
     * @param texCoords The VertexAttribute holding the texture coordinate data and access information
     *
     * @throws IllegalArgumentException  if texCoords' data type is not a decimal type
     * @throws IndexOutOfBoundsException if tex is greater than or equal to 4, or if tex is less than 0
     */
    public void setTextureCoordinates(int tex, VertexAttribute texCoords);
}
