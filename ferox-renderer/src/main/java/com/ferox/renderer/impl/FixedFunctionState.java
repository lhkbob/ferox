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

import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.FixedFunctionRenderer.CombineFunction;
import com.ferox.renderer.FixedFunctionRenderer.CombineOperand;
import com.ferox.renderer.FixedFunctionRenderer.CombineSource;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.impl.resources.BufferImpl;

import java.util.Arrays;

/**
 * Simple class to expose the fixed-function state of OpenGL. Because of the extent of the available state,
 * normal Java conventions have been sacrificed. Care must be given to coordinate state and maintain the
 * validity of the values in the state instances when OpenGL state actually changes.
 *
 * @author Michael Ludwig
 */
public class FixedFunctionState {
    /**
     * FogMode represents the three different eye fog modes that are available in OpenGL.
     */
    public static enum FogMode {
        LINEAR,
        EXP,
        EXP_SQUARED
    }

    /**
     * When configuring lighting and material colors, OpenGL uses the same functions to control the different
     * types of color. For light colors, the EMISSIVE enum is unused, since it's only available for material
     * colors.
     */
    public static enum ColorPurpose {
        AMBIENT,
        DIFFUSE,
        SPECULAR,
        EMISSIVE
    }

    /**
     * OpenGL provides only one way to update matrices, and to switch between matrix types, you must set the
     * current mode.
     */
    public static enum MatrixMode {
        MODELVIEW,
        PROJECTION,
        TEXTURE
    }

    public static enum VertexTarget {
        VERTICES,
        NORMALS,
        TEXCOORDS,
        COLORS
    }

    public static final int MAX_LIGHTS = 8;
    public static final int MAX_TEXTURES = 4;

    // cached defaults
    private static final Vector4 DEFAULT_MAT_A_COLOR = new Vector4(.2, .2, .2, 1);
    private static final Vector4 DEFAULT_MAT_D_COLOR = new Vector4(.8, .8, .8, 1);

    private static final Vector4 ZERO = new Vector4(0, 0, 0, 0);
    private static final Vector4 BLACK = new Vector4(0, 0, 0, 1);
    private static final Vector4 WHITE = new Vector4(1, 1, 1, 1);

    private static final Vector4 DEFAULT_LIGHT_POS = new Vector4(0, 0, 1, 0);
    private static final Vector3 DEFAULT_SPOT_DIR = new Vector3(0, 0, -1);

    private static final Vector4 DEFAULT_S_PLANE = new Vector4(1, 0, 0, 0);
    private static final Vector4 DEFAULT_T_PLANE = new Vector4(0, 1, 0, 0);
    private static final Vector4 DEFAULT_RQ_PLANE = new Vector4(0, 0, 0, 0);

    /**
     * An inner class that contains per-light state. Although it's accessible to sub-classes, it should be
     * considered read-only because the AbstractFixedFunctionRenderer manages the updates to its variables.
     */
    public static class LightState {
        // post-transform by current modelview matrix
        public final Vector4 position;
        public final Vector3 spotlightDirection;

        public final Vector4 ambient;
        public final Vector4 specular;
        public final Vector4 diffuse;

        public double constAtt;
        public double linAtt;
        public double quadAtt;

        public double spotAngle;
        public double spotExponent;

        public boolean enabled;

        public LightState(boolean firstLight) {
            // default state is to have the identity modelview, so this is correct
            position = new Vector4(DEFAULT_LIGHT_POS);
            spotlightDirection = new Vector3(DEFAULT_SPOT_DIR);

            ambient = new Vector4(BLACK);
            specular = new Vector4(firstLight ? WHITE : BLACK);
            diffuse = new Vector4(firstLight ? WHITE : BLACK);

            constAtt = 1;
            linAtt = 0;
            quadAtt = 0;

            spotAngle = 180;
            spotExponent = 0.0;
            enabled = false;
        }

        public LightState(LightState state) {
            position = new Vector4(state.position);
            spotlightDirection = new Vector3(state.spotlightDirection);
            ambient = new Vector4(state.ambient);
            specular = new Vector4(state.specular);
            diffuse = new Vector4(state.diffuse);
            constAtt = state.constAtt;
            linAtt = state.linAtt;
            quadAtt = state.quadAtt;
            spotAngle = state.spotAngle;
            spotExponent = state.spotExponent;
            enabled = state.enabled;
        }
    }

    /**
     * An inner class that contains per-texture unit state. Although it's accessible to sub-classes, it should
     * be considered read-only because the AbstractFixedFunctionRenderer manages the updates to its
     * variables.
     */
    public static class TextureState {
        public final int unit;

        public TexCoordSource tcS;
        public TexCoordSource tcT;
        public TexCoordSource tcR;
        public TexCoordSource tcQ;

        public final Vector4 objPlaneS;
        public final Vector4 objPlaneT;
        public final Vector4 objPlaneR;
        public final Vector4 objPlaneQ;

        // post-transform by current modelview matrix
        public final Vector4 eyePlaneS;
        public final Vector4 eyePlaneT;
        public final Vector4 eyePlaneR;
        public final Vector4 eyePlaneQ;

        public final Matrix4 textureMatrix;

        public final Vector4 color;

        public CombineFunction rgbFunc;
        public CombineFunction alphaFunc;

        public final CombineOperand[] opRgb;
        public final CombineOperand[] opAlpha;

        public final CombineSource[] srcRgb;
        public final CombineSource[] srcAlpha;

        public TextureState(int unit) {
            this.unit = unit;

            tcS = tcT = tcR = tcQ = TexCoordSource.ATTRIBUTE;

            objPlaneS = new Vector4(DEFAULT_S_PLANE);
            objPlaneT = new Vector4(DEFAULT_T_PLANE);
            objPlaneR = new Vector4(DEFAULT_RQ_PLANE);
            objPlaneQ = new Vector4(DEFAULT_RQ_PLANE);

            eyePlaneS = new Vector4(DEFAULT_S_PLANE);
            eyePlaneT = new Vector4(DEFAULT_T_PLANE);
            eyePlaneR = new Vector4(DEFAULT_RQ_PLANE);
            eyePlaneQ = new Vector4(DEFAULT_RQ_PLANE);

            textureMatrix = new Matrix4().setIdentity();

            color = new Vector4();

            rgbFunc = CombineFunction.MODULATE;
            alphaFunc = CombineFunction.MODULATE;

            opRgb = new CombineOperand[] {
                    CombineOperand.COLOR, CombineOperand.COLOR, CombineOperand.ALPHA
            };
            opAlpha = new CombineOperand[] {
                    CombineOperand.ALPHA, CombineOperand.ALPHA, CombineOperand.ALPHA
            };
            srcRgb = new CombineSource[] {
                    CombineSource.CURR_TEX, CombineSource.PREV_TEX, CombineSource.CONST_COLOR
            };
            srcAlpha = new CombineSource[] {
                    CombineSource.CURR_TEX, CombineSource.PREV_TEX, CombineSource.CONST_COLOR
            };
        }

        public TextureState(TextureState state) {
            unit = state.unit;
            tcS = state.tcS;
            tcT = state.tcT;
            tcR = state.tcR;
            tcQ = state.tcQ;
            objPlaneS = new Vector4(state.objPlaneS);
            objPlaneT = new Vector4(state.objPlaneT);
            objPlaneR = new Vector4(state.objPlaneR);
            objPlaneQ = new Vector4(state.objPlaneQ);
            eyePlaneS = new Vector4(state.eyePlaneS);
            eyePlaneT = new Vector4(state.eyePlaneT);
            eyePlaneR = new Vector4(state.eyePlaneR);
            eyePlaneQ = new Vector4(state.eyePlaneQ);
            textureMatrix = new Matrix4(state.textureMatrix);
            color = new Vector4(state.color);
            rgbFunc = state.rgbFunc;
            alphaFunc = state.alphaFunc;
            opRgb = Arrays.copyOf(state.opRgb, state.opRgb.length);
            opAlpha = Arrays.copyOf(state.opAlpha, state.opAlpha.length);
            srcRgb = Arrays.copyOf(state.srcRgb, state.srcRgb.length);
            srcAlpha = Arrays.copyOf(state.srcAlpha, state.srcAlpha.length);
        }
    }

    public static class VertexState {
        // Used to handle relocking/unlocking
        public final VertexTarget target;
        public final int slot;

        public BufferImpl.BufferHandle vbo;

        public int offset;
        public int stride;
        public int elementSize;

        public VertexState(VertexTarget target, int slot) {
            this.target = target;
            this.slot = slot;

            vbo = null;
            offset = 0;
            stride = 0;
            elementSize = 0;
        }

        public VertexState(VertexState state) {
            target = state.target;
            slot = state.slot;
            vbo = state.vbo;
            offset = state.offset;
            stride = state.stride;
            elementSize = state.elementSize;
        }
    }

    // alpha test
    public Comparison alphaTest;
    public double alphaRefValue;

    // fog
    public final Vector4 fogColor;

    public double fogStart;
    public double fogEnd;
    public double fogDensity;

    public FogMode fogMode;
    public boolean fogEnabled;

    // global lighting
    public final Vector4 globalAmbient;
    public boolean lightingEnabled;

    // lights
    public LightState[] lights; // "final"

    // material
    public final Vector4 matDiffuse;
    public final Vector4 matAmbient;
    public final Vector4 matSpecular;
    public final Vector4 matEmissive;

    public double matShininess;

    // texturing
    public final TextureState[] textures;

    // bindings for vbos and rendering
    public final VertexState vertexBinding;
    public final VertexState normalBinding;
    public final VertexState colorBinding;
    public final VertexState[] texCoordBindings;

    public int activeClientTexture;

    // matrix
    public MatrixMode matrixMode = MatrixMode.MODELVIEW;
    public final Matrix4 modelView;
    public final Matrix4 projection;

    public FixedFunctionState() {
        alphaTest = Comparison.ALWAYS;
        alphaRefValue = 1;

        fogColor = new Vector4(ZERO);
        fogStart = 0;
        fogEnd = 1;
        fogDensity = 1;
        fogMode = FogMode.EXP;
        fogEnabled = false;

        globalAmbient = new Vector4(DEFAULT_MAT_A_COLOR);
        lightingEnabled = false;

        lights = new LightState[MAX_LIGHTS];
        for (int i = 0; i < MAX_LIGHTS; i++) {
            lights[i] = new LightState(i == 0);
            lights[i].specular.set(i == 0 ? WHITE : BLACK);
            lights[i].diffuse.set(i == 0 ? WHITE : BLACK);
        }

        matDiffuse = new Vector4(DEFAULT_MAT_D_COLOR);
        matAmbient = new Vector4(DEFAULT_MAT_A_COLOR);
        matSpecular = new Vector4(BLACK);
        matEmissive = new Vector4(BLACK);

        matShininess = 0;

        textures = new TextureState[MAX_TEXTURES];
        for (int i = 0; i < MAX_TEXTURES; i++) {
            textures[i] = new TextureState(i);
        }
        activeClientTexture = 0;

        vertexBinding = new VertexState(VertexTarget.VERTICES, 0);
        normalBinding = new VertexState(VertexTarget.NORMALS, 0);
        colorBinding = new VertexState(VertexTarget.COLORS, 0);
        texCoordBindings = new VertexState[MAX_TEXTURES];
        for (int i = 0; i < MAX_TEXTURES; i++) {
            texCoordBindings[i] = new VertexState(VertexTarget.TEXCOORDS, i);
        }

        matrixMode = MatrixMode.MODELVIEW;
        modelView = new Matrix4().setIdentity();
        projection = new Matrix4().setIdentity();
    }

    public FixedFunctionState(FixedFunctionState toClone) {
        alphaTest = toClone.alphaTest;
        alphaRefValue = toClone.alphaRefValue;
        fogColor = new Vector4(toClone.fogColor);
        fogStart = toClone.fogStart;
        fogEnd = toClone.fogEnd;
        fogDensity = toClone.fogDensity;
        fogMode = toClone.fogMode;
        fogEnabled = toClone.fogEnabled;
        globalAmbient = new Vector4(toClone.globalAmbient);
        lightingEnabled = toClone.lightingEnabled;

        lights = new LightState[toClone.lights.length];
        for (int i = 0; i < lights.length; i++) {
            lights[i] = new LightState(toClone.lights[i]);
        }

        matDiffuse = new Vector4(toClone.matDiffuse);
        matAmbient = new Vector4(toClone.matAmbient);
        matSpecular = new Vector4(toClone.matSpecular);
        matEmissive = new Vector4(toClone.matEmissive);
        matShininess = toClone.matShininess;

        textures = new TextureState[toClone.textures.length];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = new TextureState(toClone.textures[i]);
        }
        activeClientTexture = toClone.activeClientTexture;

        vertexBinding = new VertexState(toClone.vertexBinding);
        normalBinding = new VertexState(toClone.normalBinding);
        colorBinding = new VertexState(toClone.colorBinding);

        texCoordBindings = new VertexState[toClone.texCoordBindings.length];
        for (int i = 0; i < texCoordBindings.length; i++) {
            texCoordBindings[i] = new VertexState(toClone.texCoordBindings[i]);
        }

        matrixMode = toClone.matrixMode;
        modelView = new Matrix4(toClone.modelView);
        projection = new Matrix4(toClone.projection);
    }
}
