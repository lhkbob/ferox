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
package com.ferox.renderer.impl.lwjgl;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.DataType;
import com.ferox.renderer.impl.AbstractFixedFunctionRenderer;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FixedFunctionState.ColorPurpose;
import com.ferox.renderer.impl.FixedFunctionState.FogMode;
import com.ferox.renderer.impl.FixedFunctionState.MatrixMode;
import com.ferox.renderer.impl.FixedFunctionState.VertexTarget;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.TextureImpl;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;

/**
 * LwjglFixedFunctionRenderer is a complete implementation of FixedFunctionRenderer that uses a {@link
 * LwjglRendererDelegate} for the LWJGL OpenGL binding.
 *
 * @author Michael Ludwig
 */
public class LwjglFixedFunctionRenderer extends AbstractFixedFunctionRenderer {
    // capabilities
    private boolean initialized;

    // math object transfer objects
    private final FloatBuffer transferBuffer;

    // state tracking
    private boolean alphaTestEnabled;

    public LwjglFixedFunctionRenderer(LwjglContext context, LwjglRendererDelegate delegate) {
        super(context, delegate);

        initialized = false;

        transferBuffer = BufferUtil.newByteBuffer(DataType.FLOAT, 16).asFloatBuffer();
        alphaTestEnabled = false;
    }

    @Override
    public void activate(AbstractSurface surface) {
        if (!initialized) {
            // set initial state not actually tracked
            GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);

            // for the lighting model, we use local viewer and separate specular color, smooth shading, and
            // two sided lighting
            GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER, GL11.GL_TRUE);
            GL11.glLightModeli(GL12.GL_LIGHT_MODEL_COLOR_CONTROL, GL12.GL_SEPARATE_SPECULAR_COLOR);
            GL11.glShadeModel(GL11.GL_SMOOTH);

            initialized = true;
        }

        super.activate(surface);
    }

    @Override
    protected void glMatrixMode(MatrixMode mode) {
        switch (mode) {
        case MODELVIEW:
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            break;
        case PROJECTION:
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            break;
        case TEXTURE:
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            break;
        }
    }

    @Override
    protected void glSetMatrix(@Const Matrix4 matrix) {
        matrix.get(transferBuffer, 0, false);
        GL11.glLoadMatrix(transferBuffer);
    }

    @Override
    protected void glAlphaTest(Comparison test, double ref) {
        if (test == Comparison.ALWAYS) {
            if (alphaTestEnabled) {
                alphaTestEnabled = false;
                glEnable(GL11.GL_ALPHA_TEST, false);
            }
        } else {
            if (!alphaTestEnabled) {
                alphaTestEnabled = true;
                glEnable(GL11.GL_ALPHA_TEST, true);
            }
        }
        GL11.glAlphaFunc(Utils.getGLPixelTest(test), (float) ref);
    }

    @Override
    protected void glFogColor(@Const Vector4 color) {
        color.get(transferBuffer, 0);
        GL11.glFog(GL11.GL_FOG_COLOR, transferBuffer);
    }

    @Override
    protected void glEnableFog(boolean enable) {
        glEnable(GL11.GL_FOG, enable);
    }

    @Override
    protected void glFogDensity(double density) {
        GL11.glFogf(GL11.GL_FOG_DENSITY, (float) density);
    }

    @Override
    protected void glFogRange(double start, double end) {
        GL11.glFogf(GL11.GL_FOG_START, (float) start);
        GL11.glFogf(GL11.GL_FOG_END, (float) end);
    }

    @Override
    protected void glFogMode(FogMode fog) {
        switch (fog) {
        case EXP:
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
            break;
        case EXP_SQUARED:
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP2);
            break;
        case LINEAR:
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
            break;
        }
    }

    @Override
    protected void glGlobalLighting(@Const Vector4 ambient) {
        ambient.get(transferBuffer, 0);
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, transferBuffer);
    }

    @Override
    protected void glLightColor(int light, ColorPurpose lc, @Const Vector4 color) {
        color.get(transferBuffer, 0);
        int c = getGLLight(lc);
        GL11.glLight(GL11.GL_LIGHT0 + light, c, transferBuffer);
    }

    @Override
    protected void glEnableLight(int light, boolean enable) {
        glEnable(GL11.GL_LIGHT0 + light, enable);
    }

    @Override
    protected void glLightPosition(int light, @Const Vector4 pos) {
        pos.get(transferBuffer, 0);
        GL11.glLight(GL11.GL_LIGHT0 + light, GL11.GL_POSITION, transferBuffer);
    }

    @Override
    protected void glLightDirection(int light, @Const Vector3 dir) {
        dir.get(transferBuffer, 0);
        GL11.glLight(GL11.GL_LIGHT0 + light, GL11.GL_SPOT_DIRECTION, transferBuffer);
    }

    @Override
    protected void glLightAngle(int light, double angle) {
        GL11.glLightf(GL11.GL_LIGHT0 + light, GL11.GL_SPOT_CUTOFF, (float) angle);
    }

    @Override
    protected void glLightExponent(int light, double exponent) {
        GL11.glLightf(GL11.GL_LIGHT0 + light, GL11.GL_SPOT_EXPONENT, (float) exponent);
    }

    @Override
    protected void glLightAttenuation(int light, double constant, double linear, double quadratic) {
        light += GL11.GL_LIGHT0;
        GL11.glLightf(light, GL11.GL_CONSTANT_ATTENUATION, (float) constant);
        GL11.glLightf(light, GL11.GL_LINEAR_ATTENUATION, (float) linear);
        GL11.glLightf(light, GL11.GL_QUADRATIC_ATTENUATION, (float) quadratic);
    }

    @Override
    protected void glEnableLighting(boolean enable) {
        glEnable(GL11.GL_LIGHTING, enable);
    }

    @Override
    protected void glMaterialColor(ColorPurpose component, @Const Vector4 color) {
        int c = getGLLight(component);
        if (component == ColorPurpose.DIFFUSE) {
            GL11.glColor4d(color.x, color.y, color.z, color.w);
        } else {
            color.get(transferBuffer, 0);
            GL11.glMaterial(GL11.GL_FRONT_AND_BACK, c, transferBuffer);
        }
    }

    @Override
    protected void glMaterialShininess(double shininess) {
        GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, (float) shininess);
    }

    @Override
    protected void glEnableTexture(TextureImpl.Target target, boolean enable) {
        int type = Utils.getGLTextureTarget(target);
        glEnable(type, enable);
    }

    @Override
    protected void glTextureColor(@Const Vector4 color) {
        color.get(transferBuffer, 0);
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, transferBuffer);
    }

    @Override
    protected void glCombineFunction(CombineFunction func, boolean rgb) {
        int c = Utils.getGLCombineFunc(func);
        int target = (rgb ? GL13.GL_COMBINE_RGB : GL13.GL_COMBINE_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, target, c);
    }

    @Override
    protected void glCombineSrc(int operand, CombineSource src, boolean rgb) {
        int o = Utils.getGLCombineSrc(src);
        int target = -1;
        if (rgb) {
            switch (operand) {
            case 0:
                target = GL13.GL_SOURCE0_RGB;
                break;
            case 1:
                target = GL13.GL_SOURCE1_RGB;
                break;
            case 2:
                target = GL13.GL_SOURCE2_RGB;
                break;
            }
        } else {
            switch (operand) {
            case 0:
                target = GL13.GL_SOURCE0_ALPHA;
                break;
            case 1:
                target = GL13.GL_SOURCE1_ALPHA;
                break;
            case 2:
                target = GL13.GL_SOURCE2_ALPHA;
                break;
            }
        }

        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glCombineOp(int operand, CombineOperand op, boolean rgb) {
        int o = Utils.getGLCombineOp(op);
        int target = -1;
        if (rgb) {
            switch (operand) {
            case 0:
                target = GL13.GL_OPERAND0_RGB;
                break;
            case 1:
                target = GL13.GL_OPERAND1_RGB;
                break;
            case 2:
                target = GL13.GL_OPERAND2_RGB;
                break;
            }
        } else {
            switch (operand) {
            case 0:
                target = GL13.GL_OPERAND0_ALPHA;
                break;
            case 1:
                target = GL13.GL_OPERAND1_ALPHA;
                break;
            case 2:
                target = GL13.GL_OPERAND2_ALPHA;
                break;
            }
        }

        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glTexGen(TexCoord coord, TexCoordSource gen) {
        if (gen == TexCoordSource.ATTRIBUTE) {
            return; // don't need to do anything, it's already disabled
        }

        int mode = Utils.getGLTexGen(gen);
        int tc = Utils.getGLTexCoord(coord, false);
        GL11.glTexGeni(tc, GL11.GL_TEXTURE_GEN_MODE, mode);
    }

    @Override
    protected void glEnableTexGen(TexCoord coord, boolean enable) {
        glEnable(Utils.getGLTexCoord(coord, true), enable);
    }

    @Override
    protected void glTexEyePlane(TexCoord coord, @Const Vector4 plane) {
        plane.get(transferBuffer, 0);
        int tc = Utils.getGLTexCoord(coord, false);
        GL11.glTexGen(tc, GL11.GL_EYE_PLANE, transferBuffer);
    }

    @Override
    protected void glTexObjPlane(TexCoord coord, @Const Vector4 plane) {
        plane.get(transferBuffer, 0);
        int tc = Utils.getGLTexCoord(coord, false);
        GL11.glTexGen(tc, GL11.GL_OBJECT_PLANE, transferBuffer);
    }

    @Override
    protected void glActiveTexture(int unit) {
        ((LwjglContext) context).setActiveTexture(unit);
    }

    @Override
    protected void glActiveClientTexture(int unit) {
        GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + unit);
    }

    @Override
    protected void glAttributePointer(VertexTarget target, BufferImpl.BufferHandle h, int offset, int stride,
                                      int elementSize) {
        int strideBytes = (elementSize + stride) * h.type.getByteCount();
        int vboOffset = offset * h.type.getByteCount();

        if (h.inmemoryBuffer != null) {
            h.inmemoryBuffer.clear().position(vboOffset);

            switch (target) {
            case NORMALS:
                GL11.glNormalPointer(Utils.getGLType(h.type), strideBytes, h.inmemoryBuffer);
                break;
            case TEXCOORDS:
                GL11.glTexCoordPointer(elementSize, Utils.getGLType(h.type), strideBytes, h.inmemoryBuffer);
                break;
            case VERTICES:
                GL11.glVertexPointer(elementSize, Utils.getGLType(h.type), strideBytes, h.inmemoryBuffer);
                break;
            case COLORS:
                GL11.glColorPointer(elementSize, Utils.getGLType(h.type), strideBytes, h.inmemoryBuffer);
                break;
            }
        } else {
            switch (target) {
            case NORMALS:
                GL11.glNormalPointer(Utils.getGLType(h.type), strideBytes, vboOffset);
                break;
            case TEXCOORDS:
                GL11.glTexCoordPointer(elementSize, Utils.getGLType(h.type), strideBytes, vboOffset);
                break;
            case VERTICES:
                GL11.glVertexPointer(elementSize, Utils.getGLType(h.type), strideBytes, vboOffset);
                break;
            case COLORS:
                GL11.glVertexPointer(elementSize, Utils.getGLType(h.type), strideBytes, vboOffset);
            }
        }
    }

    @Override
    protected void glEnableAttribute(VertexTarget target, boolean enable) {
        int state = 0;
        switch (target) {
        case NORMALS:
            state = GL11.GL_NORMAL_ARRAY;
            break;
        case TEXCOORDS:
            state = GL11.GL_TEXTURE_COORD_ARRAY;
            break;
        case VERTICES:
            state = GL11.GL_VERTEX_ARRAY;
            break;
        case COLORS:
            state = GL11.GL_COLOR_ARRAY;
            break;
        }

        if (enable) {
            GL11.glEnableClientState(state);
        } else {
            GL11.glDisableClientState(state);
        }
    }

    private void glEnable(int flag, boolean enable) {
        if (enable) {
            GL11.glEnable(flag);
        } else {
            GL11.glDisable(flag);
        }
    }

    private int getGLLight(ColorPurpose c) {
        switch (c) {
        case AMBIENT:
            return GL11.GL_AMBIENT;
        case DIFFUSE:
            return GL11.GL_DIFFUSE;
        case EMISSIVE:
            return GL11.GL_EMISSION;
        case SPECULAR:
            return GL11.GL_SPECULAR;
        }
        return -1;
    }
}
