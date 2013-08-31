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
import com.ferox.renderer.*;
import com.ferox.renderer.impl.FixedFunctionState.*;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 * <p/>
 * The AbstractFixedFunctionRenderer is an abstract implementation of {@link FixedFunctionRenderer}. It uses a
 * {@link RendererDelegate} to handle implementing the methods exposed by {@link Renderer}. The
 * AbstractFixedFunctionRenderer tracks the current state, and when necessary, delegate to protected abstract
 * methods which have the responsibility of actually making OpenGL calls.
 * <p/>
 * It makes a best-effort attempt to preserve the texture and vertex attribute state when resource deadlocks
 * must be resolved. It is possible that a texture must be unbound or will have its data changed based on the
 * actions of another render task.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractFixedFunctionRenderer extends AbstractRenderer
        implements FixedFunctionRenderer {
    private static final Matrix4 IDENTITY = new Matrix4().setIdentity();
    private static final Vector4 DEFAULT_MAT_D_COLOR = new Vector4(.8, .8, .8, 1);

    protected final FixedFunctionState state;
    protected final FixedFunctionState defaultState;

    private boolean isModelViewDirty;
    private final Matrix4 inverseModelView; // needed for eye-plane texture coordinates
    private boolean isModelInverseDirty;

    /**
     * Create an AbstractFixedFunctionRenderer that will use the given RendererDelegate.
     *
     * @param context  The context using this renderer
     * @param delegate The RendererDelegate that completes the implementations Renderer behavior
     *
     * @throws NullPointerException if delegate is null
     */
    public AbstractFixedFunctionRenderer(OpenGLContext context, RendererDelegate delegate) {
        super(context, delegate);
        inverseModelView = new Matrix4();
        isModelInverseDirty = true;

        state = new FixedFunctionState();
        defaultState = new FixedFunctionState();
    }

    @Override
    public void reset() {
        // this also takes care of setting the delegate's default state
        setCurrentState(defaultState, delegate.defaultState);
    }

    @Override
    public ContextState<FixedFunctionRenderer> getCurrentState() {
        return new FFPState(new FixedFunctionState(state), delegate.getCurrentState());
    }

    @Override
    public void setCurrentState(ContextState<FixedFunctionRenderer> state) {
        FFPState s = (FFPState) state;
        setCurrentState(s.ffpState, s.sharedState);
    }

    private void setCurrentState(FixedFunctionState f, SharedState shared) {
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

        setMaterial(f.matAmbient, f.matDiffuse, f.matSpecular, f.matEmissive);
        setMaterialShininess(f.matShininess);

        setProjectionMatrix(f.projection);

        // set the modelview to the identity matrix, since the subsequent state
        // is modified by the current modelview, but we store them post-transform
        setModelViewMatrix(IDENTITY);
        flushModelView();
        for (int i = 0; i < f.lights.length; i++) {
            LightState fl = f.lights[i];
            setLightEnabled(i, fl.enabled);
            setLightPosition(i, fl.position);
            setSpotlight(i, fl.spotlightDirection, fl.spotAngle, fl.spotExponent);
            setLightColor(i, fl.ambient, fl.diffuse, fl.specular);
            setLightAttenuation(i, fl.constAtt, fl.linAtt, fl.quadAtt);
        }

        for (int i = 0; i < f.textures.length; i++) {
            TextureState tf = f.textures[i];
            // we only set the enabled state here, the delegate state setting will bind the appropriate handle
            enableTexture(i, shared.textures[i]);

            setTextureColor(i, tf.color);

            setTextureCombineRGB(i, tf.rgbFunc, tf.srcRgb[0], tf.opRgb[0], tf.srcRgb[1], tf.opRgb[1],
                                 tf.srcRgb[2], tf.opRgb[2]);
            setTextureCombineAlpha(i, tf.alphaFunc, tf.srcAlpha[0], tf.opAlpha[0], tf.srcAlpha[1],
                                   tf.opAlpha[1], tf.srcAlpha[2], tf.opAlpha[2]);

            setTextureCoordinateSource(i, tf.source);
            setTextureObjectPlanes(i, tf.objPlanes);
            setTextureEyePlanes(i, tf.eyePlanes);

            setTextureTransform(i, tf.textureMatrix);
        }

        // set true modelview matrix
        setModelViewMatrix(f.modelView);

        // note these bypass the destroyed check performed by the public interface
        if (f.vertexBinding.vbo == null) {
            setAttribute(state.vertexBinding, null, 0, 0, 0);
        } else {
            setAttribute(state.vertexBinding, f.vertexBinding.vbo, f.vertexBinding.offset,
                         f.vertexBinding.stride, f.vertexBinding.elementSize);
        }
        if (f.normalBinding.vbo == null) {
            setAttribute(state.normalBinding, null, 0, 0, 0);
        } else {
            setAttribute(state.normalBinding, f.normalBinding.vbo, f.normalBinding.offset,
                         f.normalBinding.stride, f.normalBinding.elementSize);
        }
        if (f.colorBinding.vbo == null) {
            setAttribute(state.colorBinding, null, 0, 0, 0);
        } else {
            setAttribute(state.colorBinding, f.colorBinding.vbo, f.colorBinding.offset, f.colorBinding.stride,
                         f.colorBinding.elementSize);
        }

        for (int i = 0; i < f.texCoordBindings.length; i++) {
            VertexState fv = f.texCoordBindings[i];
            if (fv.vbo == null) {
                setAttribute(state.texCoordBindings[i], null, 0, 0, 0);
            } else {
                setAttribute(state.texCoordBindings[i], fv.vbo, fv.offset, fv.stride, fv.elementSize);
            }
        }

        // set shared state
        delegate.setCurrentState(shared);

        // clean up additional resource state that might have been destroyed
        if (state.vertexBinding.vbo != null && state.vertexBinding.vbo.isDestroyed()) {
            setVertices(null);
        }
        if (state.normalBinding.vbo != null && state.normalBinding.vbo.isDestroyed()) {
            setNormals(null);
        }
        if (state.colorBinding.vbo != null && state.colorBinding.vbo.isDestroyed()) {
            setColors(null);
        }
        for (int i = 0; i < state.texCoordBindings.length; i++) {
            if (state.texCoordBindings[i].vbo != null && state.texCoordBindings[i].vbo.isDestroyed()) {
                setTextureCoordinates(i, null);
            }
        }
        for (int i = 0; i < state.textures.length; i++) {
            if (shared.textures[i] != null && delegate.state.textures[i] == null) {
                glEnableTexture(shared.textures[i].target, false);
            }
        }
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
     * Invoke OpenGL calls to set the fog color. Don't need to clamp since OpenGL does that for us.
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
        out.set(Math.max(min, Math.min(input.x, max)), Math.max(min, Math.min(input.y, max)),
                Math.max(min, Math.min(input.z, max)), Math.max(min, Math.min(input.w, max)));
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
    public void setLightColor(int light, @Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec) {
        if (amb == null || diff == null || spec == null) {
            throw new NullPointerException("Colors cannot be null");
        }

        LightState l = state.lights[light];
        if (!l.ambient.equals(amb)) {
            clamp(amb, 0, Float.MAX_VALUE, l.ambient);
            glLightColor(light, ColorPurpose.AMBIENT, l.ambient);
        }
        if (!l.diffuse.equals(diff)) {
            clamp(diff, 0, Float.MAX_VALUE, l.diffuse);
            glLightColor(light, ColorPurpose.DIFFUSE, l.diffuse);
        }
        if (!l.specular.equals(spec)) {
            clamp(spec, 0, Float.MAX_VALUE, l.specular);
            glLightColor(light, ColorPurpose.SPECULAR, l.specular);
        }
    }

    /**
     * Invoke OpenGL calls to set the light color for the given light. The color has already been clamped
     * correctly.
     */
    protected abstract void glLightColor(int light, ColorPurpose lc, @Const Vector4 color);

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
        state.lights[light].position.mul(state.modelView, pos);
        flushModelView();
        glLightPosition(light, pos);
    }

    /**
     * Invoke OpenGL calls to set a light's position vector
     */
    protected abstract void glLightPosition(int light, @Const Vector4 pos);

    @Override
    public void setSpotlight(int light, @Const Vector3 dir, double angle, double exponent) {
        if (dir == null) {
            throw new NullPointerException("Spotlight direction can't be null");
        }
        if ((angle < 0.0 || angle > 90.0) && angle != 180.0) {
            throw new IllegalArgumentException("Spotlight angle must be in [0, 90] or be 180, not: " + angle);
        }
        if (exponent < 0.0 || exponent > 128.0) {
            throw new IllegalArgumentException("Spotlight exponent must be in [0, 128], not: " + exponent);
        }

        LightState l = state.lights[light];
        if (l.spotAngle != angle) {
            l.spotAngle = angle;
            glLightAngle(light, angle);
        }

        if (l.spotExponent != exponent) {
            l.spotExponent = exponent;
            glLightExponent(light, exponent);
        }

        // compute eye-space spotlight direction
        l.spotlightDirection.transform(state.modelView, dir, 0);
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

    /**
     * Invoke OpenGL calls to set a light's spotlight exponent
     */
    protected abstract void glLightExponent(int light, double exponent);

    @Override
    public void setLightAttenuation(int light, double constant, double linear, double quadratic) {
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
    protected abstract void glLightAttenuation(int light, double constant, double linear, double quadratic);

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
    public void setMaterial(@Const Vector4 amb, @Const Vector4 diff, @Const Vector4 spec,
                            @Const Vector4 emm) {
        setMaterialAmbient(amb);
        setMaterialDiffuse(diff);
        setMaterialSpecular(spec);
        setMaterialEmissive(emm);
    }

    @Override
    public void setMaterialDiffuse(@Const Vector4 diff) {
        if (diff == null) {
            throw new NullPointerException("Color cannot be null");
        }
        if (!state.matDiffuse.equals(diff)) {
            clamp(diff, 0, 1, state.matDiffuse);
            glMaterialColor(ColorPurpose.DIFFUSE, state.matDiffuse);
        }
    }

    @Override
    public void setMaterialAmbient(@Const Vector4 amb) {
        if (amb == null) {
            throw new NullPointerException("Color cannot be null");
        }
        if (!state.matAmbient.equals(amb)) {
            clamp(amb, 0, 1, state.matAmbient);
            glMaterialColor(ColorPurpose.AMBIENT, state.matAmbient);
        }
    }

    @Override
    public void setMaterialSpecular(@Const Vector4 spec) {
        if (spec == null) {
            throw new NullPointerException("Color cannot be null");
        }
        if (!state.matSpecular.equals(spec)) {
            clamp(spec, 0, 1, state.matSpecular);
            glMaterialColor(ColorPurpose.SPECULAR, state.matSpecular);
        }
    }

    @Override
    public void setMaterialEmissive(@Const Vector4 emm) {
        if (emm == null) {
            throw new NullPointerException("Color cannot be null");
        }
        if (!state.matEmissive.equals(emm)) {
            clamp(emm, 0, 1, state.matEmissive);
            glMaterialColor(ColorPurpose.EMISSIVE, state.matEmissive);
        }
    }

    /**
     * Invoke OpenGL calls to set the material color for the ColorPurpose. The color has already been clamped
     * correctly.
     */
    protected abstract void glMaterialColor(ColorPurpose component, @Const Vector4 color);

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
    public void setTexture(int tex, Sampler image) {
        if (tex < 0 || tex >= FixedFunctionState.MAX_TEXTURES) {
            throw new IndexOutOfBoundsException("Bad texture unit: " + tex);
        }

        if (image == null) {
            enableTexture(tex, null);
            context.bindTexture(tex, null);
        } else {
            if (image.isDestroyed()) {
                throw new ResourceException("Cannot use a destroyed resource");
            }
            if (!(image instanceof Texture1D) && !(image instanceof Texture2D) &&
                !(image instanceof TextureCubeMap) && !(image instanceof DepthMap2D)) {
                throw new UnsupportedOperationException(
                        image.getClass() + " cannot be used in a FixedFunctionRenderer");
            }

            TextureImpl.TextureHandle newImage = ((TextureImpl) image).getHandle();
            enableTexture(tex, newImage);
            context.bindTexture(tex, newImage);
        }
    }

    private void enableTexture(int tex, TextureImpl.TextureHandle newImage) {
        TextureImpl.TextureHandle oldImage = delegate.state.textures[tex];
        if (oldImage != newImage) {
            // Update the active texture unit
            setTextureUnit(tex);

            if (oldImage != null && (newImage == null || oldImage.target != newImage.target)) {
                glEnableTexture(oldImage.target, false);
            }
            if (newImage != null && (oldImage == null || oldImage.target != newImage.target)) {
                glEnableTexture(newImage.target, true);
            }
        }
    }

    /**
     * Invoke OpenGL to enable the active texture unit
     */
    protected abstract void glEnableTexture(TextureImpl.Target target, boolean enable);

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
     * Invoke OpenGL calls to set the texture color for the active texture. OpenGL clamps the values for us.
     */
    protected abstract void glTextureColor(@Const Vector4 color);

    @Override
    public void setTextureCombineRGB(int tex, CombineFunction function, CombineSource src0,
                                     CombineOperand op0, CombineSource src1, CombineOperand op1,
                                     CombineSource src2, CombineOperand op2) {
        if (function == null || src0 == null || src1 == null || src2 == null ||
            op0 == null || op1 == null || op2 == null) {
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
    public void setTextureCombineAlpha(int tex, CombineFunction function, CombineSource src0,
                                       CombineOperand op0, CombineSource src1, CombineOperand op1,
                                       CombineSource src2, CombineOperand op2) {
        if (function == null || src0 == null || src1 == null || src2 == null ||
            op0 == null || op1 == null || op2 == null) {
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
            glCombineFunction(function, false);
        }

        if (t.srcAlpha[0] != src0) {
            t.srcAlpha[0] = src0;
            glCombineSrc(0, src0, false);
        }
        if (t.srcAlpha[1] != src1) {
            t.srcAlpha[1] = src1;
            glCombineSrc(1, src1, false);
        }
        if (t.srcAlpha[2] != src2) {
            t.srcRgb[2] = src2;
            glCombineSrc(2, src2, false);
        }

        if (t.opAlpha[0] != op0) {
            t.opAlpha[0] = op0;
            glCombineOp(0, op0, false);
        }
        if (t.opAlpha[1] != op1) {
            t.opAlpha[1] = op1;
            glCombineOp(1, op1, false);
        }
        if (t.opAlpha[2] != op2) {
            t.opAlpha[2] = op2;
            glCombineOp(2, op2, false);
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
    public void setTextureCoordinateSource(int tex, TexCoordSource gen) {
        if (gen == null) {
            throw new NullPointerException("TexCoordSource can't be null");
        }

        TextureState t = state.textures[tex];
        if (gen != t.source) {
            t.source = gen;
            setTextureUnit(tex);
            glTexGen(gen);
        }
    }

    /**
     * Invoke OpenGL to set the coordinate generation for the active texture, disable tex gen if source is
     * ATTRIBUTE.
     */
    protected abstract void glTexGen(TexCoordSource gen);

    @Override
    public void setTextureEyePlanes(int tex, @Const Matrix4 planes) {
        if (planes == null) {
            throw new NullPointerException("Object planes cannot be null");
        }

        TextureState t = state.textures[tex];
        if (isModelInverseDirty) {
            // update inverse matrix
            inverseModelView.inverse(state.modelView);
            isModelInverseDirty = false;
        }

        // store post transform
        t.eyePlanes.mul(planes, inverseModelView);
        flushModelView();
        setTextureUnit(tex);
        glTexEyePlanes(planes);
    }

    /**
     * Invoke OpenGL to set the eye plane for the given coordinate on the active texture
     */
    protected abstract void glTexEyePlanes(@Const Matrix4 planes);

    @Override
    public void setTextureObjectPlanes(int tex, @Const Matrix4 planes) {
        if (planes == null) {
            throw new NullPointerException("Object planes cannot be null");
        }

        TextureState t = state.textures[tex];
        if (!planes.equals(t.objPlanes)) {
            t.objPlanes.set(planes);
            glTexObjPlanes(planes);
        }
    }

    /**
     * Invoke OpenGL to set the object plane for the active texture
     */
    protected abstract void glTexObjPlanes(@Const Matrix4 planes);

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
        if (unit != delegate.state.activeTexture) {
            glActiveTexture(unit);
        }
    }

    /**
     * Invoke OpenGL calls to set the active texture unit, and is responsible for updating the state, or
     * delegating to the proper context method that preserves state
     */
    protected abstract void glActiveTexture(int unit);

    @Override
    public void setVertices(VertexAttribute vertices) {
        if (vertices == null) {
            setAttribute(state.vertexBinding, null, 0, 0, 0);
        } else {
            if (vertices.getVBO().isDestroyed()) {
                throw new ResourceException("Cannot use a destroyed resource");
            }
            if (vertices.getElementSize() == 1) {
                throw new IllegalArgumentException("Vertices element size cannot be 1");
            }
            if (vertices.getVBO().getDataType().isNormalized()) {
                throw new IllegalArgumentException("Vertices do not accept normalized types");
            }
            if (vertices.getVBO().getDataType().getJavaPrimitive().equals(byte.class)) {
                throw new IllegalArgumentException("Vertices cannot be specified with byte values");
            }

            setAttribute(state.vertexBinding, ((BufferImpl) vertices.getVBO()).getHandle(),
                         vertices.getOffset(), vertices.getStride(), vertices.getElementSize());
        }
    }

    @Override
    public void setNormals(VertexAttribute normals) {
        if (normals == null) {
            setAttribute(state.normalBinding, null, 0, 0, 0);
        } else {
            if (normals.getVBO().isDestroyed()) {
                throw new ResourceException("Cannot use a destroyed resource");
            }
            if (normals.getElementSize() != 3) {
                throw new IllegalArgumentException("Normals element size must be 3");
            }
            if (!normals.getVBO().getDataType().isDecimalNumber()) {
                throw new IllegalArgumentException("VBO must have a decimal data type");
            }
            // if it's a decimal number it's either floating point or normalized so it's valid

            setAttribute(state.normalBinding, ((BufferImpl) normals.getVBO()).getHandle(),
                         normals.getOffset(), normals.getStride(), normals.getElementSize());
        }
    }

    @Override
    public void setColors(VertexAttribute colors) {
        if (colors == null) {
            setAttribute(state.colorBinding, null, 0, 0, 0);
            // per-vertex coloring is disabled, so make sure we have a predictable diffuse color
            glMaterialColor(ColorPurpose.DIFFUSE, DEFAULT_MAT_D_COLOR);
            state.matDiffuse.set(DEFAULT_MAT_D_COLOR);
        } else {
            if (colors.getVBO().isDestroyed()) {
                throw new ResourceException("Cannot use a destroyed resource");
            }
            if (colors.getElementSize() != 3 && colors.getElementSize() != 4) {
                throw new IllegalArgumentException("Colors element size must be 3 or 4");
            }
            if (!colors.getVBO().getDataType().isDecimalNumber()) {
                throw new IllegalArgumentException("VBO must have a decimal data type");
            }
            // if it's a decimal number it's either floating point or normalized so it's valid

            setAttribute(state.colorBinding, ((BufferImpl) colors.getVBO()).getHandle(), colors.getOffset(),
                         colors.getStride(), colors.getElementSize());
        }
    }

    @Override
    public void setTextureCoordinates(int tex, VertexAttribute texCoords) {
        if (texCoords == null) {
            setAttribute(state.texCoordBindings[tex], null, 0, 0, 0);
        } else {
            if (texCoords.getVBO().isDestroyed()) {
                throw new ResourceException("Cannot use a destroyed resource");
            }
            if (texCoords.getVBO().getDataType().isNormalized()) {
                throw new IllegalArgumentException("Texture coordinates do not accept normalized types");
            }
            if (texCoords.getVBO().getDataType().getJavaPrimitive().equals(byte.class)) {
                throw new IllegalArgumentException(
                        "Texture coordinates cannot be specified with byte values");
            }
            setAttribute(state.texCoordBindings[tex], ((BufferImpl) texCoords.getVBO()).getHandle(),
                         texCoords.getOffset(), texCoords.getStride(), texCoords.getElementSize());
        }
    }

    private void setAttribute(VertexState vertex, BufferImpl.BufferHandle vbo, int offset, int stride,
                              int elementSize) {
        if (vbo != null) {
            // We are setting a new vertex attribute
            boolean accessDiffers = (vertex.offset != offset ||
                                     vertex.stride != stride ||
                                     vertex.elementSize != elementSize);
            if (vertex.vbo != vbo || accessDiffers) {
                // The attributes will be different so must make a change

                // Make sure OpenGL is operating on the correct unit for subsequent commands
                if (vertex.target == VertexTarget.TEXCOORDS && vertex.slot != state.activeClientTexture) {
                    // Special case slot handling for texture coordinates (other targets ignore slot)
                    state.activeClientTexture = vertex.slot;
                    glActiveClientTexture(vertex.slot);
                }

                if (vertex.vbo == null) {
                    // enable the attribute
                    glEnableAttribute(vertex.target, true);
                }

                vertex.vbo = vbo;
                vertex.elementSize = elementSize;
                vertex.offset = offset;
                vertex.stride = stride;

                if (vbo != delegate.state.arrayVBO) {
                    context.bindArrayVBO(vbo);
                }
                glAttributePointer(vertex.target, vbo, offset, stride, elementSize);
            }
        } else {
            // The attribute is meant to be unbound
            if (vertex.vbo != null) {
                // Make sure OpenGL is operating on the correct unit for subsequent commands
                if (vertex.target == VertexTarget.TEXCOORDS && vertex.slot != state.activeClientTexture) {
                    // Special case slot handling for texture coordinates (other targets ignore slot)
                    state.activeClientTexture = vertex.slot;
                    glActiveClientTexture(vertex.slot);
                }

                // Disable the attribute
                glEnableAttribute(vertex.target, false);
                vertex.vbo = null;
            }
        }
    }

    /**
     * Invoke OpenGL commands to change the active texture used by client-side state.
     */
    protected abstract void glActiveClientTexture(int unit);

    /**
     * Invoke OpenGL commands to set the given attribute pointer. The resource will have already been bound
     * using glBindArrayVbo. If this is for a texture coordinate, glActiveClientTexture will already have been
     * called.
     */
    protected abstract void glAttributePointer(VertexTarget target, BufferImpl.BufferHandle handle,
                                               int offset, int stride, int elementSize);

    /**
     * Invoke OpenGL commands to enable the given client attribute pointer. If this is for a texture
     * coordinate, glActiveClientTexture will have been called.
     */
    protected abstract void glEnableAttribute(VertexTarget target, boolean enable);

    private static class FFPState implements ContextState<FixedFunctionRenderer> {
        private final SharedState sharedState;
        private final FixedFunctionState ffpState;

        public FFPState(FixedFunctionState ffp, SharedState shared) {
            ffpState = ffp;
            sharedState = shared;
        }
    }
}
