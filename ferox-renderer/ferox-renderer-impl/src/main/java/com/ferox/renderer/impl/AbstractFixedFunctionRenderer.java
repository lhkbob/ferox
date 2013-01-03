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
package com.ferox.renderer.impl;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.ContextState;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.FixedFunctionState.FogMode;
import com.ferox.renderer.impl.FixedFunctionState.LightColor;
import com.ferox.renderer.impl.FixedFunctionState.LightState;
import com.ferox.renderer.impl.FixedFunctionState.MatrixMode;
import com.ferox.renderer.impl.FixedFunctionState.TextureState;
import com.ferox.renderer.impl.FixedFunctionState.VertexState;
import com.ferox.renderer.impl.FixedFunctionState.VertexTarget;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;

/**
 * <p>
 * The AbstractFixedFunctionRenderer is an abstract implementation of
 * {@link FixedFunctionRenderer}. It uses a {@link RendererDelegate} to handle
 * implementing the methods exposed by {@link Renderer}. The
 * AbstractFixedFunctionRenderer tracks the current state, and when necessary,
 * delegate to protected abstract methods which have the responsibility of
 * actually making OpenGL calls.
 * </p>
 * <p>
 * It makes a best-effort attempt to preserve the texture and vertex attribute
 * state when resource deadlocks must be resolved. It is possible that a texture
 * must be unbound or will have its data changed based on the actions of another
 * render task.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractFixedFunctionRenderer extends AbstractRenderer implements FixedFunctionRenderer {
    private static final Matrix4 IDENTITY = new Matrix4();
    private static final Vector4 DEFAULT_MAT_D_COLOR = new Vector4(.8, .8, .8, 1);

    protected FixedFunctionState state;
    protected FixedFunctionState defaultState;

    // private handles tracking the actual resource locks of current state
    private VertexBufferObjectHandle verticesHandle;
    private VertexBufferObjectHandle normalsHandle;
    private VertexBufferObjectHandle colorsHandle;
    private VertexBufferObjectHandle[] texCoordsHandles;

    private TextureHandle[] textureHandles;

    // hidden state that is used to minimize opengl calls
    private VertexBufferObject arrayVboBinding;
    private int activeArrayVbos;
    private int activeClientTex;
    private int activeTex;

    private boolean isModelViewDirty;
    private final Matrix4 inverseModelView; // needed for eye-plane texture coordinates
    private boolean isModelInverseDirty;

    /**
     * Create an AbstractFixedFunctionRenderer that will use the given
     * RendererDelegate. If this renderer is used with another GlslRenderer on
     * the same context, they should share RendererDelegate instances.
     * 
     * @param delegate The RendererDelegate that completes the implementations
     *            Renderer behavior
     * @throws NullPointerException if delegate is null
     */
    public AbstractFixedFunctionRenderer(RendererDelegate delegate) {
        super(delegate);
        inverseModelView = new Matrix4();
        isModelInverseDirty = true;
    }

    @Override
    public void activate(AbstractSurface surface, OpenGLContext context,
                         ResourceManager resourceManager) {
        super.activate(surface, context, resourceManager);

        if (state == null) {
            // init state
            RenderCapabilities caps = surface.getFramework().getCapabilities();
            state = new FixedFunctionState(caps.getMaxActiveLights(),
                                           caps.getMaxFixedPipelineTextures(),
                                           caps.getMaxTextureCoordinates());
            defaultState = new FixedFunctionState(delegate.defaultState, state);

            textureHandles = new TextureHandle[caps.getMaxFixedPipelineTextures()];
            texCoordsHandles = new VertexBufferObjectHandle[caps.getMaxTextureCoordinates()];
        }
    }

    @Override
    public void reset() {
        // this also takes care of setting the delegate's default state
        setCurrentState(defaultState);
    }

    @Override
    public ContextState<FixedFunctionRenderer> getCurrentState() {
        return new FixedFunctionState(delegate.getCurrentState(), state);
    }

    @Override
    public void setCurrentState(ContextState<FixedFunctionRenderer> state) {
        FixedFunctionState f = (FixedFunctionState) state;

        setAlphaTest(f.alphaTest, f.alphaRefValue);
        setFogColor(f.fogColor);
        switch (f.fogMode) {
        case EXP:
            setFogExponential(f.fogDensity, false);
            break;
        case EXP_SQUARED:
            setFogExponential(f.fogDensity, true);
            break;
        case LINEAR:
            setFogLinear(f.fogStart, f.fogEnd);
            break;
        default:
            throw new UnsupportedOperationException("Unsupported FogMode value: " + f.fogMode);
        }
        setFogEnabled(f.fogEnabled);

        setGlobalAmbientLight(f.globalAmbient);
        setLightingEnabled(f.lightingEnabled);
        setSmoothedLightingEnabled(f.lightingSmoothed);
        setTwoSidedLightingEnabled(f.lightingTwoSided);

        setMaterial(f.matAmbient, f.matDiffuse, f.matSpecular, f.matEmmissive);
        setMaterialShininess(f.matShininess);

        setLineAntiAliasingEnabled(f.lineAAEnabled);
        setPointAntiAliasingEnabled(f.pointAAEnabled);
        setPolygonAntiAliasingEnabled(f.pointAAEnabled);

        setLineSize(f.lineWidth);
        setPointSize(f.pointWidth);

        setProjectionMatrix(f.projection);

        // set the modelview to the identity matrix, since the subsequent state
        // is modified by the current modelview, but we store the post-transform
        setModelViewMatrix(IDENTITY);
        for (int i = 0; i < f.lights.length; i++) {
            LightState fl = f.lights[i];
            setLightEnabled(i, fl.enabled);
            setLightPosition(i, fl.position);
            setSpotlight(i, fl.spotlightDirection, fl.spotAngle);
            setLightColor(i, fl.ambient, fl.diffuse, fl.specular);
            setLightAttenuation(i, fl.constAtt, fl.linAtt, fl.quadAtt);
        }

        for (int i = 0; i < f.textures.length; i++) {
            TextureState tf = f.textures[i];
            setTexture(i, tf.texture);

            setTextureColor(i, tf.color);

            setTextureCombineRGB(i, tf.rgbFunc, tf.srcRgb[0], tf.opRgb[0], tf.srcRgb[1],
                                 tf.opRgb[1], tf.srcRgb[2], tf.opRgb[2]);
            setTextureCombineAlpha(i, tf.alphaFunc, tf.srcAlpha[0], tf.opAlpha[0],
                                   tf.srcAlpha[1], tf.opAlpha[1], tf.srcAlpha[2],
                                   tf.opAlpha[2]);

            setTextureCoordGeneration(i, TexCoord.S, tf.tcS);
            setTextureCoordGeneration(i, TexCoord.T, tf.tcT);
            setTextureCoordGeneration(i, TexCoord.R, tf.tcR);
            setTextureCoordGeneration(i, TexCoord.Q, tf.tcQ);

            setTextureObjectPlane(i, TexCoord.S, tf.objPlaneS);
            setTextureObjectPlane(i, TexCoord.T, tf.objPlaneT);
            setTextureObjectPlane(i, TexCoord.R, tf.objPlaneR);
            setTextureObjectPlane(i, TexCoord.Q, tf.objPlaneQ);

            setTextureObjectPlane(i, TexCoord.S, tf.eyePlaneS);
            setTextureObjectPlane(i, TexCoord.T, tf.eyePlaneT);
            setTextureObjectPlane(i, TexCoord.R, tf.eyePlaneR);
            setTextureObjectPlane(i, TexCoord.Q, tf.eyePlaneQ);

            setTextureTransform(i, tf.textureMatrix);
        }

        if (f.vertexBinding.vbo == null) {
            setVertices(null);
        } else {
            setVertices(new VertexAttribute(f.vertexBinding.vbo,
                                            f.vertexBinding.elementSize,
                                            f.vertexBinding.offset,
                                            f.vertexBinding.stride));
        }
        if (f.normalBinding.vbo == null) {
            setNormals(null);
        } else {
            setNormals(new VertexAttribute(f.normalBinding.vbo,
                                           f.normalBinding.elementSize,
                                           f.normalBinding.offset,
                                           f.normalBinding.stride));
        }
        if (f.colorBinding.vbo == null) {
            setColors(null);
        } else {
            setColors(new VertexAttribute(f.colorBinding.vbo,
                                          f.colorBinding.elementSize,
                                          f.colorBinding.offset,
                                          f.colorBinding.stride));
        }

        for (int i = 0; i < f.texBindings.length; i++) {
            VertexState fv = f.texBindings[i];
            if (fv.vbo == null) {
                setTextureCoordinates(i, null);
            } else {
                setTextureCoordinates(i, new VertexAttribute(fv.vbo,
                                                             fv.elementSize,
                                                             fv.offset,
                                                             fv.stride));
            }
        }

        // set true modelview matrix
        setModelViewMatrix(f.modelView);

        // set shared state
        delegate.setCurrentState(f.sharedState);
    }

    @Override
    public int render(PolygonType polyType, int first, int count) {
        flushModelView();
        return super.render(polyType, first, count);
    }

    @Override
    public void setModelViewMatrix(@Const Matrix4 matrix) {
        if (matrix == null) {
            throw new NullPointerException("Matrix cannot be null");
        }

        state.modelView.set(matrix);
        isModelViewDirty = true;
        isModelInverseDirty = true;
    }

    /**
     * Invoke OpenGL calls to set the matrix mode
     */
    protected abstract void glMatrixMode(MatrixMode mode);

    /**
     * Invoke OpenGL calls to set the matrix for the current mode
     */
    protected abstract void glSetMatrix(@Const Matrix4 matrix);

    private void setMatrixMode(MatrixMode mode) {
        if (state.matrixMode != mode) {
            state.matrixMode = mode;
            glMatrixMode(mode);
        }
    }

    private void flushModelView() {
        if (isModelViewDirty) {
            setMatrixMode(MatrixMode.MODELVIEW);
            glSetMatrix(state.modelView);
            isModelViewDirty = false;
        }
    }

    @Override
    public void setProjectionMatrix(@Const Matrix4 matrix) {
        if (matrix == null) {
            throw new NullPointerException("Matrix cannot be null");
        }

        state.projection.set(matrix);
        setMatrixMode(MatrixMode.PROJECTION);
        glSetMatrix(matrix);
    }

    @Override
    public void setAlphaTest(Comparison test, double refValue) {
        if (test == null) {
            throw new NullPointerException("Null comparison");
        }
        if (state.alphaTest != test || state.alphaRefValue != refValue) {
            state.alphaTest = test;
            state.alphaRefValue = refValue;
            glAlphaTest(test, refValue);
        }
    }

    /**
     * Invoke OpenGL calls to set the alpha test
     */
    protected abstract void glAlphaTest(Comparison test, double ref);

    @Override
    public void setFogColor(@Const Vector4 color) {
        if (color == null) {
            throw new NullPointerException("Null fog color");
        }
        if (!state.fogColor.equals(color)) {
            state.fogColor.set(color);
            glFogColor(color);
        }
    }

    /**
     * Invoke OpenGL calls to set the fog color. Don't need to clamp since
     * OpenGL does that for us.
     */
    protected abstract void glFogColor(@Const Vector4 color);

    @Override
    public void setFogEnabled(boolean enable) {
        if (state.fogEnabled != enable) {
            state.fogEnabled = enable;
            glEnableFog(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable fog
     */
    protected abstract void glEnableFog(boolean enable);

    @Override
    public void setFogExponential(double density, boolean squared) {
        if (density < 0f) {
            throw new IllegalArgumentException("Density must be >= 0, not: " + density);
        }

        if (state.fogDensity != density) {
            state.fogDensity = density;
            glFogDensity(density);
        }

        if (squared && state.fogMode != FogMode.EXP_SQUARED) {
            state.fogMode = FogMode.EXP_SQUARED;
            glFogMode(FogMode.EXP_SQUARED);
        } else if (state.fogMode != FogMode.EXP) {
            state.fogMode = FogMode.EXP;
            glFogMode(FogMode.EXP);
        }
    }

    /**
     * Invoke OpenGL calls to set the fog density
     */
    protected abstract void glFogDensity(double density);

    @Override
    public void setFogLinear(double start, double end) {
        if (end <= start) {
            throw new IllegalArgumentException("Illegal start/end range: " + start + ", " + end);
        }

        if (state.fogStart != start || state.fogEnd != end) {
            state.fogStart = start;
            state.fogEnd = end;
            glFogRange(start, end);
        }

        if (state.fogMode != FogMode.LINEAR) {
            state.fogMode = FogMode.LINEAR;
            glFogMode(FogMode.LINEAR);
        }
    }

    /**
     * Invoke OpenGL calls to set the linear fog range
     */
    protected abstract void glFogRange(double start, double end);

    /**
     * Invoke OpenGL calls to set the fog equation
     */
    protected abstract void glFogMode(FogMode fog);

    private void clamp(@Const Vector4 input, double min, double max, Vector4 out) {
        out.set(Math.max(min, Math.min(input.x, max)),
                Math.max(min, Math.min(input.y, max)),
                Math.max(min, Math.min(input.z, max)),
                Math.max(min, Math.min(input.w, max)));
    }

    @Override
    public void setGlobalAmbientLight(@Const Vector4 ambient) {
        if (ambient == null) {
            throw new NullPointerException("Null global ambient color");
        }
        if (!state.globalAmbient.equals(ambient)) {
            clamp(ambient, 0, Float.MAX_VALUE, state.globalAmbient);
            glGlobalLighting(state.globalAmbient);
        }
    }

    /**
     * Invoke OpenGL calls to set the global lighting color
     */
    protected abstract void glGlobalLighting(@Const Vector4 ambient);

    @Override
    public void setLightColor(int light, @Const Vector4 amb, @Const Vector4 diff,
                              @Const Vector4 spec) {
        if (amb == null || diff == null || spec == null) {
            throw new NullPointerException("Colors cannot be null");
        }

        LightState l = state.lights[light];
        if (!l.ambient.equals(amb)) {
            clamp(amb, 0, Float.MAX_VALUE, l.ambient);
            glLightColor(light, LightColor.AMBIENT, l.ambient);
        }
        if (!l.diffuse.equals(diff)) {
            clamp(diff, 0, Float.MAX_VALUE, l.diffuse);
            glLightColor(light, LightColor.DIFFUSE, l.diffuse);
        }
        if (!l.specular.equals(spec)) {
            clamp(spec, 0, Float.MAX_VALUE, l.specular);
            glLightColor(light, LightColor.SPECULAR, l.specular);
        }
    }

    /**
     * Invoke OpenGL calls to set the light color for the given light. The color
     * has already been clamped correctly.
     */
    protected abstract void glLightColor(int light, LightColor lc, @Const Vector4 color);

    @Override
    public void setLightEnabled(int light, boolean enable) {
        LightState l = state.lights[light];
        if (l.enabled != enable) {
            l.enabled = enable;
            glEnableLight(light, enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable a specific light
     */
    protected abstract void glEnableLight(int light, boolean enable);

    @Override
    public void setLightPosition(int light, @Const Vector4 pos) {
        if (pos == null) {
            throw new NullPointerException("Light position can't be null");
        }
        if (pos.w != 0.0 && pos.w != 1.0) {
            throw new NullPointerException("pos.w must be 0 or 1, not: " + pos.w);
        }

        // compute the eye-space light position
        state.lights[light].position.mul(state.lights[light].position, state.modelView);
        flushModelView();
        glLightPosition(light, pos);
    }

    /**
     * Invoke OpenGL calls to set a light's position vector
     */
    protected abstract void glLightPosition(int light, @Const Vector4 pos);

    @Override
    public void setSpotlight(int light, @Const Vector3 dir, double angle) {
        if (dir == null) {
            throw new NullPointerException("Spotlight direction can't be null");
        }
        if ((angle < 0f || angle > 90f) && angle != 180f) {
            throw new IllegalArgumentException("Spotlight angle must be in [0, 90] or be 180, not: " + angle);
        }

        LightState l = state.lights[light];
        if (l.spotAngle != angle) {
            l.spotAngle = angle;
            glLightAngle(light, angle);
        }

        // compute eye-space spotlight direction
        l.spotlightDirection.transform(state.modelView, l.spotlightDirection, 0);
        flushModelView();
        glLightDirection(light, dir);
    }

    /**
     * Invoke OpenGL calls to set a light's spotlight direction
     */
    protected abstract void glLightDirection(int light, @Const Vector3 dir);

    /**
     * Invoke OpenGL calls to set a light's spotlight angle
     */
    protected abstract void glLightAngle(int light, double angle);

    @Override
    public void setLightAttenuation(int light, double constant, double linear,
                                    double quadratic) {
        if (constant < 0f) {
            throw new IllegalArgumentException("Constant factor must be positive: " + constant);
        }
        if (linear < 0f) {
            throw new IllegalArgumentException("Linear factor must be positive: " + linear);
        }
        if (quadratic < 0f) {
            throw new IllegalArgumentException("Quadratic factor must be positive: " + quadratic);
        }

        LightState l = state.lights[light];
        if (l.constAtt != constant || l.linAtt != linear || l.quadAtt != quadratic) {
            l.constAtt = constant;
            l.linAtt = linear;
            l.quadAtt = quadratic;
            glLightAttenuation(light, constant, linear, quadratic);
        }
    }

    /**
     * Invoke OpenGL calls to set a light's attenuation factors
     */
    protected abstract void glLightAttenuation(int light, double constant, double linear,
                                               double quadratic);

    @Override
    public void setLightingEnabled(boolean enable) {
        if (state.lightingEnabled != enable) {
            state.lightingEnabled = enable;
            glEnableLighting(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable lighting
     */
    protected abstract void glEnableLighting(boolean enable);

    @Override
    public void setSmoothedLightingEnabled(boolean smoothed) {
        if (state.lightingSmoothed != smoothed) {
            state.lightingSmoothed = smoothed;
            glEnableSmoothShading(smoothed);
        }
    }

    @Override
    public void setTwoSidedLightingEnabled(boolean enable) {
        if (state.lightingTwoSided != enable) {
            state.lightingTwoSided = enable;
            glEnableTwoSidedLighting(enable);
        }
    }

    /**
     * Invoke OpenGL calls to set smooth shading
     */
    protected abstract void glEnableSmoothShading(boolean enable);

    /**
     * Invoke OpenGL calls to set two-sided lighting
     */
    protected abstract void glEnableTwoSidedLighting(boolean enable);

    @Override
    public void setLineAntiAliasingEnabled(boolean enable) {
        if (state.lineAAEnabled != enable) {
            state.lineAAEnabled = enable;
            glEnableLineAntiAliasing(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable line aa
     */
    protected abstract void glEnableLineAntiAliasing(boolean enable);

    @Override
    public void setLineSize(double width) {
        if (width < 1f) {
            throw new IllegalArgumentException("Line width must be at least 1, not: " + width);
        }
        if (state.lineWidth != width) {
            state.lineWidth = width;
            glLineWidth(width);
        }
    }

    /**
     * Invoke OpenGL calls to set line width
     */
    protected abstract void glLineWidth(double width);

    @Override
    public void setMaterial(@Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec,
                            @Const Vector4 emm) {
        if (amb == null || diff == null || spec == null || emm == null) {
            throw new NullPointerException("Material colors can't be null: " + amb + ", " + diff + ", " + spec + ", " + emm);
        }
        if (!state.matAmbient.equals(amb)) {
            clamp(amb, 0, 1, state.matAmbient);
            glMaterialColor(LightColor.AMBIENT, state.matAmbient);
        }

        if (!state.matDiffuse.equals(diff)) {
            clamp(diff, 0, 1, state.matDiffuse);
            glMaterialColor(LightColor.DIFFUSE, state.matDiffuse);
        }

        if (!state.matSpecular.equals(spec)) {
            state.matSpecular.set(spec);
            clamp(spec, 0, 1, state.matSpecular);
            glMaterialColor(LightColor.SPECULAR, state.matSpecular);
        }

        if (!state.matEmmissive.equals(emm)) {
            clamp(emm, 0, Float.MAX_VALUE, state.matEmmissive);
            glMaterialColor(LightColor.EMISSIVE, state.matEmmissive);
        }
    }

    /**
     * Invoke OpenGL calls to set the material color for the LightColor. The
     * color has already been clamped correctly.
     */
    protected abstract void glMaterialColor(LightColor component, @Const Vector4 color);

    @Override
    public void setMaterialShininess(double shininess) {
        if (shininess < 0.0 || shininess > 128.0) {
            throw new IllegalArgumentException("Shininess must be in [0, 128], not: " + shininess);
        }
        if (state.matShininess != shininess) {
            state.matShininess = shininess;
            glMaterialShininess(shininess);
        }
    }

    /**
     * Invoke OpenGL calls to set the material shininess
     */
    protected abstract void glMaterialShininess(double shininess);

    @Override
    public void setPointAntiAliasingEnabled(boolean enable) {
        if (state.pointAAEnabled != enable) {
            state.pointAAEnabled = enable;
            glEnablePointAntiAliasing(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable point aa
     */
    protected abstract void glEnablePointAntiAliasing(boolean enable);

    @Override
    public void setPointSize(double width) {
        if (width < 1.0) {
            throw new IllegalArgumentException("Point width must be at least 1, not: " + width);
        }
        if (state.pointWidth != width) {
            state.pointWidth = width;
            glPointWidth(width);
        }
    }

    /**
     * Invoke OpenGL calls to set point width
     */
    protected abstract void glPointWidth(double width);

    @Override
    public void setPolygonAntiAliasingEnabled(boolean enable) {
        if (state.polyAAEnabled != enable) {
            state.polyAAEnabled = enable;
            glEnablePolyAntiAliasing(enable);
        }
    }

    /**
     * Invoke OpenGL calls to enable polygon aa
     */
    protected abstract void glEnablePolyAntiAliasing(boolean enable);

    @Override
    public void setTexture(int tex, Texture image) {
        TextureState t = state.textures[tex];
        if (t.texture != image) {
            // Release current texture if need-be
            Target oldTarget = null;
            if (t.texture != null) {
                resourceManager.unlock(t.texture);
                oldTarget = textureHandles[tex].target;
                t.texture = null;
                textureHandles[tex] = null;
            }

            // Lock new texture if needed
            TextureHandle newHandle = null;
            Target newTarget = null;
            if (image != null) {
                newHandle = (TextureHandle) resourceManager.lock(context, image);
                if (newHandle != null) {
                    newTarget = newHandle.target;

                    t.texture = image;
                    textureHandles[tex] = newHandle;
                }
            }

            // Update the active texture unit
            setTextureUnit(tex);

            if (oldTarget != null && oldTarget != newTarget) {
                // Unbind old texture and disable old target
                glEnableTexture(oldTarget, false);
                glBindTexture(oldTarget, null);
            }

            if (newHandle != null) {
                // Enable new target (old target was already disabled if needed)
                if (newTarget != oldTarget) {
                    glEnableTexture(newTarget, true);
                }

                // Bind new texture
                glBindTexture(newTarget, newHandle);
            }
        }
    }

    /**
     * Invoke OpenGL calls to bind a Texture to the active texture
     */
    protected abstract void glBindTexture(Target target, TextureHandle image);

    /**
     * Invoke OpenGL to enable the active texture unit
     */
    protected abstract void glEnableTexture(Target target, boolean enable);

    @Override
    public void setTextureColor(int tex, @Const Vector4 color) {
        if (color == null) {
            throw new NullPointerException("Texture color can't be null");
        }

        TextureState t = state.textures[tex];
        if (!t.color.equals(color)) {
            t.color.set(color);
            setTextureUnit(tex);
            glTextureColor(color);
        }
    }

    /**
     * Invoke OpenGL calls to set the texture color for the active texture.
     * OpenGL clamps the values for us.
     */
    protected abstract void glTextureColor(@Const Vector4 color);

    @Override
    public void setTextureCombineRGB(int tex, CombineFunction function,
                                     CombineSource src0, CombineOperand op0,
                                     CombineSource src1, CombineOperand op1,
                                     CombineSource src2, CombineOperand op2) {
        if (function == null || src0 == null || src1 == null || src2 == null || op0 == null || op1 == null || op2 == null) {
            throw new NullPointerException("Arguments cannot be null");
        }

        TextureState t = state.textures[tex];
        setTextureUnit(tex);
        if (t.rgbFunc != function) {
            t.rgbFunc = function;
            glCombineFunction(function, true);
        }

        if (t.srcRgb[0] != src0) {
            t.srcRgb[0] = src0;
            glCombineSrc(0, src0, true);
        }
        if (t.srcRgb[1] != src1) {
            t.srcRgb[1] = src1;
            glCombineSrc(1, src1, true);
        }
        if (t.srcRgb[2] != src2) {
            t.srcRgb[2] = src2;
            glCombineSrc(2, src2, true);
        }

        if (t.opRgb[0] != op0) {
            t.opRgb[0] = op0;
            glCombineOp(0, op0, true);
        }
        if (t.opRgb[1] != op1) {
            t.opRgb[1] = op1;
            glCombineOp(1, op1, true);
        }
        if (t.opRgb[2] != op2) {
            t.opRgb[2] = op2;
            glCombineOp(2, op2, true);
        }
    }

    @Override
    public void setTextureCombineAlpha(int tex, CombineFunction function,
                                       CombineSource src0, CombineOperand op0,
                                       CombineSource src1, CombineOperand op1,
                                       CombineSource src2, CombineOperand op2) {
        if (function == null || src0 == null || src1 == null || src2 == null || op0 == null || op1 == null || op2 == null) {
            throw new NullPointerException("Arguments cannot be null");
        }
        if (function == CombineFunction.DOT3_RGB || function == CombineFunction.DOT3_RGBA) {
            throw new IllegalArgumentException("Alpha CombineFunction can't be DOT3_RGB or DOT3_RGBA");
        }
        checkAlphaCombineOperand(op0);
        checkAlphaCombineOperand(op1);
        checkAlphaCombineOperand(op2);

        TextureState t = state.textures[tex];
        setTextureUnit(tex);
        if (t.alphaFunc != function) {
            t.alphaFunc = function;
            glCombineFunction(function, true);
        }

        if (t.srcAlpha[0] != src0) {
            t.srcAlpha[0] = src0;
            glCombineSrc(0, src0, true);
        }
        if (t.srcAlpha[1] != src1) {
            t.srcAlpha[1] = src1;
            glCombineSrc(1, src1, true);
        }
        if (t.srcAlpha[2] != src2) {
            t.srcRgb[2] = src2;
            glCombineSrc(2, src2, true);
        }

        if (t.opAlpha[0] != op0) {
            t.opAlpha[0] = op0;
            glCombineOp(0, op0, true);
        }
        if (t.opAlpha[1] != op1) {
            t.opAlpha[1] = op1;
            glCombineOp(1, op1, true);
        }
        if (t.opAlpha[2] != op2) {
            t.opAlpha[2] = op2;
            glCombineOp(2, op2, true);
        }
    }

    /**
     * Invoke OpenGL calls to set a combine function, either rgb or alpha
     */
    protected abstract void glCombineFunction(CombineFunction func, boolean rgb);

    private void checkAlphaCombineOperand(CombineOperand op) {
        if (op == CombineOperand.COLOR || op == CombineOperand.ONE_MINUS_COLOR) {
            throw new IllegalArgumentException("Illegal CombineOperand for alpha: " + op);
        }
    }

    /**
     * Invoke OpenGL calls to set the combine source
     */
    protected abstract void glCombineSrc(int operand, CombineSource src, boolean rgb);

    /**
     * Invoke OpenGL calls to set the combine op
     */
    protected abstract void glCombineOp(int operand, CombineOperand op, boolean rgb);

    @Override
    public void setTextureCoordGeneration(int tex, TexCoordSource gen) {
        setTextureCoordGeneration(tex, TexCoord.S, gen);
        setTextureCoordGeneration(tex, TexCoord.T, gen);
        setTextureCoordGeneration(tex, TexCoord.R, gen);
        setTextureCoordGeneration(tex, TexCoord.Q, gen);
    }

    @Override
    public void setTextureCoordGeneration(int tex, TexCoord coord, TexCoordSource gen) {
        if (coord == null) {
            throw new NullPointerException("TexCoord can't be null");
        }
        if (gen == null) {
            throw new NullPointerException("TexCoordSource can't be null");
        }

        TextureState t = state.textures[tex];
        switch (coord) {
        case S:
            if (t.tcS != gen) {
                setTextureUnit(tex);
                if (t.tcS == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, true);
                } else if (gen == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, false);
                }

                t.tcS = gen;
                glTexGen(coord, gen);
            }
            break;
        case R:
            if (t.tcR != gen) {
                setTextureUnit(tex);
                if (t.tcR == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, true);
                } else if (gen == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, false);
                }

                t.tcR = gen;
                glTexGen(coord, gen);
            }
            break;
        case T:
            if (t.tcT != gen) {
                setTextureUnit(tex);
                if (t.tcT == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, true);
                } else if (gen == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, false);
                }

                t.tcT = gen;
                glTexGen(coord, gen);
            }
            break;
        case Q:
            if (t.tcQ != gen) {
                setTextureUnit(tex);
                if (t.tcQ == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, true);
                } else if (gen == TexCoordSource.ATTRIBUTE) {
                    glEnableTexGen(coord, false);
                }

                t.tcQ = gen;
                glTexGen(coord, gen);
            }
            break;
        }
    }

    /**
     * Invoke OpenGL to set the coordinate generation for the active texture
     */
    protected abstract void glTexGen(TexCoord coord, TexCoordSource gen);

    /**
     * Invoke OpenGL operations to enable/disable coord generation
     */
    protected abstract void glEnableTexGen(TexCoord coord, boolean enable);

    @Override
    public void setTextureEyePlane(int tex, TexCoord coord, @Const Vector4 plane) {
        if (plane == null) {
            throw new NullPointerException("Eye plane cannot be null");
        }
        if (coord == null) {
            throw new NullPointerException("TexCoord cannot be null");
        }

        TextureState t = state.textures[tex];

        if (isModelInverseDirty) {
            // update inverse matrix
            inverseModelView.inverse(state.modelView);
            isModelInverseDirty = false;
        }

        switch (coord) {
        case S:
            t.eyePlaneS.mul(t.eyePlaneS, inverseModelView);
            break;
        case T:
            t.eyePlaneT.mul(t.eyePlaneT, inverseModelView);
            break;
        case R:
            t.eyePlaneR.mul(t.eyePlaneR, inverseModelView);
            break;
        case Q:
            t.eyePlaneQ.mul(t.eyePlaneQ, inverseModelView);
            break;
        }

        flushModelView();
        setTextureUnit(tex);
        glTexEyePlane(coord, plane);
    }

    /**
     * Invoke OpenGL to set the eye plane for the given coordinate on the active
     * texture
     */
    protected abstract void glTexEyePlane(TexCoord coord, @Const Vector4 plane);

    @Override
    public void setTextureObjectPlane(int tex, TexCoord coord, @Const Vector4 plane) {
        if (plane == null) {
            throw new NullPointerException("Object plane cannot be null");
        }
        if (coord == null) {
            throw new NullPointerException("TexCoord cannot be null");
        }

        TextureState t = state.textures[tex];
        switch (coord) {
        case S:
            if (!t.objPlaneS.equals(plane)) {
                t.objPlaneS.set(plane);
                setTextureUnit(tex);
                glTexObjPlane(coord, plane);
            }
            break;
        case T:
            if (!t.objPlaneT.equals(plane)) {
                t.objPlaneT.set(plane);
                setTextureUnit(tex);
                glTexObjPlane(coord, plane);
            }
            break;
        case R:
            if (!t.objPlaneR.equals(plane)) {
                t.objPlaneR.set(plane);
                setTextureUnit(tex);
                glTexObjPlane(coord, plane);
            }
            break;
        case Q:
            if (!t.objPlaneQ.equals(plane)) {
                t.objPlaneQ.set(plane);
                setTextureUnit(tex);
                glTexObjPlane(coord, plane);
            }
            break;
        }
    }

    /**
     * Invoke OpenGL to set the object plane for the active texture
     */
    protected abstract void glTexObjPlane(TexCoord coord, @Const Vector4 plane);

    @Override
    public void setTextureTransform(int tex, @Const Matrix4 matrix) {
        if (matrix == null) {
            throw new NullPointerException("Matrix cannot be null");
        }

        state.textures[tex].textureMatrix.set(matrix);

        setTextureUnit(tex);
        setMatrixMode(MatrixMode.TEXTURE);
        glSetMatrix(matrix);
    }

    private void setTextureUnit(int unit) {
        if (unit != activeTex) {
            activeTex = unit;
            glActiveTexture(unit);
        }
    }

    /**
     * Invoke OpenGL calls to set the active texture unit
     */
    protected abstract void glActiveTexture(int unit);

    @Override
    public void setVertices(VertexAttribute vertices) {
        if (vertices != null && vertices.getElementSize() == 1) {
            throw new IllegalArgumentException("Vertices element size cannot be 1");
        }
        verticesHandle = setAttribute(state.vertexBinding, verticesHandle, vertices);
    }

    @Override
    public void setNormals(VertexAttribute normals) {
        if (normals != null && normals.getElementSize() != 3) {
            throw new IllegalArgumentException("Normals element size must be 3");
        }
        normalsHandle = setAttribute(state.normalBinding, normalsHandle, normals);
    }

    @Override
    public void setColors(VertexAttribute colors) {
        if (colors != null && colors.getElementSize() != 3 && colors.getElementSize() != 4) {
            throw new IllegalArgumentException("Colors element size must be 3 or 4");
        }
        colorsHandle = setAttribute(state.colorBinding, colorsHandle, colors);
        if (colorsHandle == null) {
            // per-vertex coloring is disabled, so make sure we have a predictable diffuse color
            glMaterialColor(LightColor.DIFFUSE, DEFAULT_MAT_D_COLOR);
            state.matDiffuse.set(DEFAULT_MAT_D_COLOR);
        }
    }

    @Override
    public void setTextureCoordinates(int tex, VertexAttribute texCoords) {
        texCoordsHandles[tex] = setAttribute(state.texBindings[tex],
                                             texCoordsHandles[tex], texCoords);
    }

    private VertexBufferObjectHandle setAttribute(VertexState state,
                                                  VertexBufferObjectHandle currentHandle,
                                                  VertexAttribute attr) {
        VertexBufferObjectHandle handle = currentHandle;

        if (attr != null) {
            // We are setting a new vertex attribute
            boolean accessDiffers = (state.offset != attr.getOffset() || state.stride != attr.getStride() || state.elementSize != attr.getElementSize());
            if (state.vbo != attr.getVBO() || accessDiffers) {
                // The attributes will be different so must make a change
                VertexBufferObject oldVbo = state.vbo;
                boolean failTypeCheck = false;

                if (state.vbo != null && oldVbo != attr.getVBO()) {
                    // Unlock the old one
                    resourceManager.unlock(oldVbo);
                    state.vbo = null;
                    handle = null;
                }

                if (state.vbo == null) {
                    // Lock the new vbo
                    VertexBufferObjectHandle newHandle = (VertexBufferObjectHandle) resourceManager.lock(context,
                                                                                                         attr.getVBO());
                    if (newHandle != null && newHandle.dataType != DataType.FLOAT) {
                        resourceManager.unlock(attr.getVBO());
                        failTypeCheck = true;

                        state.vbo = null;
                        handle = null;
                    } else {
                        // VBO is ready
                        state.vbo = attr.getVBO();
                        handle = newHandle;
                    }
                }

                // Make sure OpenGL is operating on the correct unit for subsequent commands
                if (state.target == VertexTarget.TEXCOORDS && state.slot != activeClientTex) {
                    // Special case slot handling for texture coordinates (other targets ignore slot)
                    activeClientTex = state.slot;
                    glActiveClientTexture(state.slot);
                }

                if (state.vbo != null) {
                    // At this point, state.vbo is the new VBO (or possibly old VBO)
                    state.elementSize = attr.getElementSize();
                    state.offset = attr.getOffset();
                    state.stride = attr.getStride();

                    bindArrayVbo(attr.getVBO(), handle, oldVbo);

                    if (oldVbo == null) {
                        glEnableAttribute(state.target, true);
                    }
                    glAttributePointer(state.target, handle, state.offset, state.stride,
                                       state.elementSize);
                } else if (oldVbo != null) {
                    // Since there was an old vbo we need to clean some things up
                    // which weren't cleaned up when we unlocked the old vbo
                    glEnableAttribute(state.target, false);
                    unbindArrayVboMaybe(oldVbo);
                }

                if (failTypeCheck) {
                    throw new IllegalArgumentException("VertexAttribute type must be FLOAT");
                }
            }
        } else {
            // The attribute is meant to be unbound
            if (state.vbo != null) {
                // Make sure OpenGL is operating on the correct unit for subsequent commands
                if (state.target == VertexTarget.TEXCOORDS && state.slot != activeClientTex) {
                    // Special case slot handling for texture coordinates (other targets ignore slot)
                    activeClientTex = state.slot;
                    glActiveClientTexture(state.slot);
                }

                // Disable the attribute
                glEnableAttribute(state.target, false);
                // Possibly unbind it from the array vbo
                unbindArrayVboMaybe(state.vbo);

                // Unlock it
                resourceManager.unlock(state.vbo);
                state.vbo = null;
                handle = null;
            }
        }

        return handle;
    }

    private void bindArrayVbo(VertexBufferObject vbo, VertexBufferObjectHandle handle,
                              VertexBufferObject oldVboOnSlot) {
        if (vbo != arrayVboBinding) {
            glBindArrayVbo(handle);
            activeArrayVbos = 0;
            arrayVboBinding = vbo;

            // If we're binding the vbo, then the last vbo on the slot doesn't matter
            // since it wasn't counted in the activeArrayVbos counter
            oldVboOnSlot = null;
        }

        // Only update the count if the new vbo isn't replacing the same vbo in the same slot
        if (oldVboOnSlot != vbo) {
            activeArrayVbos++;
        }
    }

    private void unbindArrayVboMaybe(VertexBufferObject vbo) {
        if (vbo == arrayVboBinding) {
            activeArrayVbos--;
            if (activeArrayVbos == 0) {
                glBindArrayVbo(null);
                arrayVboBinding = null;
            }
        }
    }

    /**
     * Invoke OpenGL commands to change the active texture used by client-side
     * state.
     */
    protected abstract void glActiveClientTexture(int unit);

    /**
     * Bind the given resource handle as the array vbo. If null, unbind the
     * array vbo.
     */
    protected abstract void glBindArrayVbo(VertexBufferObjectHandle handle);

    /**
     * Invoke OpenGL commands to set the given attribute pointer. The resource
     * will have already been bound using glBindArrayVbo. If this is for a
     * texture coordinate, glActiveClientTexture will already have been called.
     */
    protected abstract void glAttributePointer(VertexTarget target,
                                               VertexBufferObjectHandle handle,
                                               int offset, int stride, int elementSize);

    /**
     * Invoke OpenGL commands to enable the given client attribute pointer. If
     * this is for a texture coordinate, glActiveClientTexture will have been
     * called.
     */
    protected abstract void glEnableAttribute(VertexTarget target, boolean enable);
}
