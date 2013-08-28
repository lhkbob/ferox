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
package com.ferox.renderer.impl.jogl;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.DataType;
import com.ferox.renderer.impl.AbstractFixedFunctionRenderer;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FixedFunctionState;
import com.ferox.renderer.impl.FixedFunctionState.ColorPurpose;
import com.ferox.renderer.impl.FixedFunctionState.FogMode;
import com.ferox.renderer.impl.FixedFunctionState.MatrixMode;
import com.ferox.renderer.impl.FixedFunctionState.VertexTarget;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import java.nio.FloatBuffer;

/**
 * JoglFixedFunctionRenderer is a complete implementation of FixedFunctionRenderer that uses a {@link
 * JoglRendererDelegate} for the JOGL OpenGL binding.
 *
 * @author Michael Ludwig
 */
public class JoglFixedFunctionRenderer extends AbstractFixedFunctionRenderer {
    // capabilities
    private boolean initialized;

    // math object transfer objects
    private final FloatBuffer transferBuffer;

    private GL2 gl;

    public JoglFixedFunctionRenderer(JoglContext context, JoglRendererDelegate delegate) {
        super(context, delegate);

        initialized = false;

        transferBuffer = BufferUtil.newByteBuffer(DataType.FLOAT, 16).asFloatBuffer();
    }

    @Override
    public void activate(AbstractSurface surface) {
        gl = ((JoglContext) context).getGLContext().getGL().getGL2();
        if (!initialized) {
            // set initial state not actually tracked
            gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);

            // for the lighting model, we use local viewer and separate specular color, smooth shading, and
            // two sided lighting
            gl.glLightModeli(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, GL.GL_TRUE);
            gl.glLightModeli(GL2.GL_LIGHT_MODEL_COLOR_CONTROL, GL2.GL_SEPARATE_SPECULAR_COLOR);
            gl.glShadeModel(GL2.GL_SMOOTH);

            for (int i = 0; i < FixedFunctionState.MAX_TEXTURES; i++) {
                glActiveTexture(i);
                gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_COMBINE);
            }

            initialized = true;
        }

        super.activate(surface);
    }

    @Override
    protected void glMatrixMode(MatrixMode mode) {
        switch (mode) {
        case MODELVIEW:
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            break;
        case PROJECTION:
            gl.glMatrixMode(GL2.GL_PROJECTION);
            break;
        case TEXTURE:
            gl.glMatrixMode(GL2.GL_TEXTURE);
            break;
        }
    }

    @Override
    protected void glSetMatrix(@Const Matrix4 matrix) {
        matrix.get(transferBuffer, 0, false);
        gl.glLoadMatrixf(transferBuffer);
    }

    @Override
    protected void glAlphaTest(Comparison test, double ref) {
        if (test == Comparison.ALWAYS) {
            glEnable(GL2.GL_ALPHA_TEST, false);
        } else {
            glEnable(GL2.GL_ALPHA_TEST, true);
        }
        gl.glAlphaFunc(Utils.getGLPixelTest(test), (float) ref);
    }

    @Override
    protected void glFogColor(@Const Vector4 color) {
        color.get(transferBuffer, 0);
        gl.glFogfv(GL2.GL_FOG_COLOR, transferBuffer);
    }

    @Override
    protected void glEnableFog(boolean enable) {
        glEnable(GL2.GL_FOG, enable);
    }

    @Override
    protected void glFogDensity(double density) {
        gl.glFogf(GL2.GL_FOG_DENSITY, (float) density);
    }

    @Override
    protected void glFogRange(double start, double end) {
        gl.glFogf(GL2.GL_FOG_START, (float) start);
        gl.glFogf(GL2.GL_FOG_END, (float) end);
    }

    @Override
    protected void glFogMode(FogMode fog) {
        switch (fog) {
        case EXP:
            gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP);
            break;
        case EXP_SQUARED:
            gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP2);
            break;
        case LINEAR:
            gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
            break;
        }
    }

    @Override
    protected void glGlobalLighting(@Const Vector4 ambient) {
        ambient.get(transferBuffer, 0);
        gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, transferBuffer);
    }

    @Override
    protected void glLightColor(int light, FixedFunctionState.ColorPurpose lc, @Const Vector4 color) {
        color.get(transferBuffer, 0);
        int c = getGLLight(lc);
        gl.glLightfv(GL2.GL_LIGHT0 + light, c, transferBuffer);
    }

    @Override
    protected void glEnableLight(int light, boolean enable) {
        glEnable(GL2.GL_LIGHT0 + light, enable);
    }

    @Override
    protected void glLightPosition(int light, @Const Vector4 pos) {
        pos.get(transferBuffer, 0);
        gl.glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_POSITION, transferBuffer);
    }

    @Override
    protected void glLightDirection(int light, @Const Vector3 dir) {
        dir.get(transferBuffer, 0);
        gl.glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_SPOT_DIRECTION, transferBuffer);
    }

    @Override
    protected void glLightAngle(int light, double angle) {
        gl.glLightf(GL2.GL_LIGHT0 + light, GL2.GL_SPOT_CUTOFF, (float) angle);
    }

    @Override
    protected void glLightExponent(int light, double exponent) {
        gl.glLightf(GL2.GL_LIGHT0 + light, GL2.GL_SPOT_EXPONENT, (float) exponent);
    }

    @Override
    protected void glLightAttenuation(int light, double constant, double linear, double quadratic) {
        light += GL2.GL_LIGHT0;
        gl.glLightf(light, GL2.GL_CONSTANT_ATTENUATION, (float) constant);
        gl.glLightf(light, GL2.GL_LINEAR_ATTENUATION, (float) linear);
        gl.glLightf(light, GL2.GL_QUADRATIC_ATTENUATION, (float) quadratic);
    }

    @Override
    protected void glEnableLighting(boolean enable) {
        glEnable(GL2.GL_LIGHTING, enable);
    }

    @Override
    protected void glMaterialColor(ColorPurpose component, @Const Vector4 color) {
        int c = getGLLight(component);
        if (component == FixedFunctionState.ColorPurpose.DIFFUSE) {
            gl.glColor4d(color.x, color.y, color.z, color.w);
        } else {
            color.get(transferBuffer, 0);
            gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, c, transferBuffer);
        }
    }

    @Override
    protected void glMaterialShininess(double shininess) {
        gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, (float) shininess);
    }

    @Override
    protected void glEnableTexture(TextureImpl.Target target, boolean enable) {
        int type = Utils.getGLTextureTarget(target);
        glEnable(type, enable);
    }

    @Override
    protected void glTextureColor(@Const Vector4 color) {
        color.get(transferBuffer, 0);
        gl.glTexEnvfv(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_COLOR, transferBuffer);
    }

    @Override
    protected void glCombineFunction(CombineFunction func, boolean rgb) {
        int c = Utils.getGLCombineFunc(func);
        int target = (rgb ? GL2.GL_COMBINE_RGB : GL2.GL_COMBINE_ALPHA);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, target, c);
    }

    @Override
    protected void glCombineSrc(int operand, CombineSource src, boolean rgb) {
        int o = Utils.getGLCombineSrc(src);
        int target;
        if (rgb) {
            target = GL2.GL_SOURCE0_RGB + operand;
        } else {
            target = GL2.GL_SOURCE0_ALPHA + operand;
        }

        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glCombineOp(int operand, CombineOperand op, boolean rgb) {
        int o = Utils.getGLCombineOp(op);
        int target;
        if (rgb) {
            target = GL2.GL_OPERAND0_RGB + operand;
        } else {
            target = GL2.GL_OPERAND0_ALPHA + operand;
        }

        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glTexGen(TexCoordSource gen) {
        if (gen == TexCoordSource.ATTRIBUTE) {
            // disable tex gen for all coordinates
            glEnable(GL2.GL_TEXTURE_GEN_S, false);
            glEnable(GL2.GL_TEXTURE_GEN_T, false);
            glEnable(GL2.GL_TEXTURE_GEN_R, false);
            glEnable(GL2.GL_TEXTURE_GEN_Q, false);
        } else {
            // enable and configure
            glEnable(GL2.GL_TEXTURE_GEN_S, true);
            glEnable(GL2.GL_TEXTURE_GEN_T, true);
            glEnable(GL2.GL_TEXTURE_GEN_R, true);
            glEnable(GL2.GL_TEXTURE_GEN_Q, true);

            int mode = Utils.getGLTexGen(gen);
            gl.glTexGeni(GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, mode);
            gl.glTexGeni(GL2.GL_T, GL2.GL_TEXTURE_GEN_MODE, mode);
            gl.glTexGeni(GL2.GL_R, GL2.GL_TEXTURE_GEN_MODE, mode);
            gl.glTexGeni(GL2.GL_Q, GL2.GL_TEXTURE_GEN_MODE, mode);
        }
    }

    @Override
    protected void glTexEyePlanes(@Const Matrix4 plane) {
        plane.get(transferBuffer, 0, true); // row-major so every 4 units is a plane

        transferBuffer.limit(4).position(0);
        gl.glTexGenfv(GL2.GL_S, GL2.GL_EYE_PLANE, transferBuffer);
        transferBuffer.limit(8).position(4);
        gl.glTexGenfv(GL2.GL_T, GL2.GL_EYE_PLANE, transferBuffer);
        transferBuffer.limit(12).position(8);
        gl.glTexGenfv(GL2.GL_R, GL2.GL_EYE_PLANE, transferBuffer);
        transferBuffer.limit(16).position(12);
        gl.glTexGenfv(GL2.GL_Q, GL2.GL_EYE_PLANE, transferBuffer);
        transferBuffer.clear();
    }

    @Override
    protected void glTexObjPlanes(@Const Matrix4 plane) {
        plane.get(transferBuffer, 0, true); // row-major so every 4 units is a plane

        transferBuffer.limit(4).position(0);
        gl.glTexGenfv(GL2.GL_S, GL2.GL_OBJECT_PLANE, transferBuffer);
        transferBuffer.limit(8).position(4);
        gl.glTexGenfv(GL2.GL_T, GL2.GL_OBJECT_PLANE, transferBuffer);
        transferBuffer.limit(12).position(8);
        gl.glTexGenfv(GL2.GL_R, GL2.GL_OBJECT_PLANE, transferBuffer);
        transferBuffer.limit(16).position(12);
        gl.glTexGenfv(GL2.GL_Q, GL2.GL_OBJECT_PLANE, transferBuffer);
        transferBuffer.clear();
    }

    @Override
    protected void glActiveTexture(int unit) {
        ((JoglContext) context).setActiveTexture(unit);
    }

    @Override
    protected void glActiveClientTexture(int unit) {
        gl.glClientActiveTexture(GL2.GL_TEXTURE0 + unit);
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
                gl.glNormalPointer(Utils.getGLType(h.type), strideBytes, h.inmemoryBuffer);
                break;
            case TEXCOORDS:
                gl.glTexCoordPointer(elementSize, Utils.getGLType(h.type), strideBytes, h.inmemoryBuffer);
                break;
            case VERTICES:
                gl.glVertexPointer(elementSize, Utils.getGLType(h.type), strideBytes, h.inmemoryBuffer);
                break;
            case COLORS:
                gl.glColorPointer(elementSize, Utils.getGLType(h.type), strideBytes, h.inmemoryBuffer);
                break;
            }
        } else {
            switch (target) {
            case NORMALS:
                gl.glNormalPointer(Utils.getGLType(h.type), strideBytes, vboOffset);
                break;
            case TEXCOORDS:
                gl.glTexCoordPointer(elementSize, Utils.getGLType(h.type), strideBytes, vboOffset);
                break;
            case VERTICES:
                gl.glVertexPointer(elementSize, Utils.getGLType(h.type), strideBytes, vboOffset);
                break;
            case COLORS:
                gl.glVertexPointer(elementSize, Utils.getGLType(h.type), strideBytes, vboOffset);
            }
        }
    }

    @Override
    protected void glEnableAttribute(VertexTarget target, boolean enable) {
        int state = 0;
        switch (target) {
        case NORMALS:
            state = GL2.GL_NORMAL_ARRAY;
            break;
        case TEXCOORDS:
            state = GL2.GL_TEXTURE_COORD_ARRAY;
            break;
        case VERTICES:
            state = GL2.GL_VERTEX_ARRAY;
            break;
        case COLORS:
            state = GL2.GL_COLOR_ARRAY;
            break;
        }

        if (enable) {
            gl.glEnableClientState(state);
        } else {
            gl.glDisableClientState(state);
        }
    }

    private void glEnable(int flag, boolean enable) {
        if (enable) {
            gl.glEnable(flag);
        } else {
            gl.glDisable(flag);
        }
    }

    private int getGLLight(ColorPurpose c) {
        switch (c) {
        case AMBIENT:
            return GL2.GL_AMBIENT;
        case DIFFUSE:
            return GL2.GL_DIFFUSE;
        case EMISSIVE:
            return GL2.GL_EMISSION;
        case SPECULAR:
            return GL2.GL_SPECULAR;
        }
        return -1;
    }
}
