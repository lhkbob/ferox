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
import com.ferox.renderer.Capabilities;
import com.ferox.renderer.geom.VertexBufferObject.StorageMode;
import com.ferox.renderer.impl.AbstractFixedFunctionRenderer;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FixedFunctionState.FogMode;
import com.ferox.renderer.impl.FixedFunctionState.LightColor;
import com.ferox.renderer.impl.FixedFunctionState.MatrixMode;
import com.ferox.renderer.impl.FixedFunctionState.VertexTarget;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.renderer.texture.Texture.Target;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.fixedfunc.GLPointerFunc;
import java.nio.FloatBuffer;
import java.util.EnumSet;

/**
 * JoglFixedFunctionRenderer is a complete implementation of FixedFunctionRenderer that uses a {@link
 * JoglRendererDelegate} for the JOGL OpenGL binding.
 *
 * @author Michael Ludwig
 */
public class JoglFixedFunctionRenderer extends AbstractFixedFunctionRenderer {
    // capabilities
    private boolean supportsMultitexture;
    private boolean supportsCombine;
    private EnumSet<Target> supportedTargets;

    private boolean initialized;

    // math object transfer objects
    private final FloatBuffer transferBuffer;

    // state tracking
    private boolean alphaTestEnabled;

    public JoglFixedFunctionRenderer(JoglRendererDelegate delegate) {
        super(delegate);

        initialized = false;

        transferBuffer = BufferUtil.newFloatBuffer(16);
        alphaTestEnabled = false;
    }

    @Override
    public void activate(AbstractSurface surface, OpenGLContext context, ResourceManager manager) {
        super.activate(surface, context, manager);

        if (!initialized) {
            // detect caps
            Capabilities caps = surface.getFramework().getCapabilities();
            supportsMultitexture = caps.getMaxFixedPipelineTextures() > 1;
            supportsCombine = caps.getCombineEnvModeSupport();
            supportedTargets = caps.getSupportedTextureTargets();

            // set initial state
            GL2 gl = getGL();
            gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_DIFFUSE);
            gl.glEnable(GLLightingFunc.GL_COLOR_MATERIAL);

            initialized = true;
        }
    }

    @Override
    protected void glMatrixMode(MatrixMode mode) {
        switch (mode) {
        case MODELVIEW:
            getGL().glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            break;
        case PROJECTION:
            getGL().glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            break;
        case TEXTURE:
            getGL().glMatrixMode(GL.GL_TEXTURE);
            break;
        }
    }

    @Override
    protected void glSetMatrix(@Const Matrix4 matrix) {
        matrix.get(transferBuffer, 0, false);
        transferBuffer.rewind();
        getGL().glLoadMatrixf(transferBuffer);
    }

    @Override
    protected void glAlphaTest(Comparison test, double ref) {
        if (test == Comparison.ALWAYS) {
            if (alphaTestEnabled) {
                alphaTestEnabled = false;
                glEnable(GL2ES1.GL_ALPHA_TEST, false);
            }
        } else {
            if (!alphaTestEnabled) {
                alphaTestEnabled = true;
                glEnable(GL2ES1.GL_ALPHA_TEST, true);
            }

            getGL().glAlphaFunc(Utils.getGLPixelTest(test), (float) ref);
        }
    }

    @Override
    protected void glFogColor(@Const Vector4 color) {
        color.get(transferBuffer, 0);
        transferBuffer.rewind();
        getGL().glFogfv(GL2ES1.GL_FOG_COLOR, transferBuffer);
    }

    @Override
    protected void glEnableFog(boolean enable) {
        glEnable(GL2ES1.GL_FOG, enable);
    }

    @Override
    protected void glFogDensity(double density) {
        getGL().glFogf(GL2ES1.GL_FOG_DENSITY, (float) density);
    }

    @Override
    protected void glFogRange(double start, double end) {
        getGL().glFogf(GL2ES1.GL_FOG_START, (float) start);
        getGL().glFogf(GL2ES1.GL_FOG_END, (float) end);
    }

    @Override
    protected void glFogMode(FogMode fog) {
        switch (fog) {
        case EXP:
            getGL().glFogi(GL2ES1.GL_FOG_MODE, GL2ES1.GL_EXP);
            break;
        case EXP_SQUARED:
            getGL().glFogi(GL2ES1.GL_FOG_MODE, GL2ES1.GL_EXP2);
            break;
        case LINEAR:
            getGL().glFogi(GL2ES1.GL_FOG_MODE, GL.GL_LINEAR);
            break;
        }
    }

    @Override
    protected void glGlobalLighting(@Const Vector4 ambient) {
        ambient.get(transferBuffer, 0);
        transferBuffer.rewind();
        getGL().glLightModelfv(GL2ES1.GL_LIGHT_MODEL_AMBIENT, transferBuffer);
    }

    @Override
    protected void glLightColor(int light, LightColor lc, @Const Vector4 color) {
        color.get(transferBuffer, 0);
        transferBuffer.rewind();

        int c = getGLLight(lc);
        getGL().glLightfv(GLLightingFunc.GL_LIGHT0 + light, c, transferBuffer);
    }

    @Override
    protected void glEnableLight(int light, boolean enable) {
        glEnable(GLLightingFunc.GL_LIGHT0 + light, enable);
    }

    @Override
    protected void glLightPosition(int light, @Const Vector4 pos) {
        pos.get(transferBuffer, 0);
        getGL().glLightfv(GLLightingFunc.GL_LIGHT0 + light, GLLightingFunc.GL_POSITION, transferBuffer);
    }

    @Override
    protected void glLightDirection(int light, @Const Vector3 dir) {
        dir.get(transferBuffer, 0);
        transferBuffer.rewind();

        getGL().glLightfv(GLLightingFunc.GL_LIGHT0 + light, GLLightingFunc.GL_SPOT_DIRECTION, transferBuffer);
    }

    @Override
    protected void glLightAngle(int light, double angle) {
        getGL().glLightf(GLLightingFunc.GL_LIGHT0 + light, GLLightingFunc.GL_SPOT_CUTOFF, (float) angle);
    }

    @Override
    protected void glLightAttenuation(int light, double constant, double linear, double quadratic) {
        light += GLLightingFunc.GL_LIGHT0;
        GL2 gl = getGL();
        gl.glLightf(light, GLLightingFunc.GL_CONSTANT_ATTENUATION, (float) constant);
        gl.glLightf(light, GLLightingFunc.GL_LINEAR_ATTENUATION, (float) linear);
        gl.glLightf(light, GLLightingFunc.GL_QUADRATIC_ATTENUATION, (float) quadratic);
    }

    @Override
    protected void glEnableLighting(boolean enable) {
        glEnable(GLLightingFunc.GL_LIGHTING, enable);
    }

    @Override
    protected void glEnableSmoothShading(boolean enable) {
        getGL().glShadeModel(enable ? GLLightingFunc.GL_SMOOTH : GLLightingFunc.GL_FLAT);
    }

    @Override
    protected void glEnableTwoSidedLighting(boolean enable) {
        getGL().glLightModeli(GL2ES1.GL_LIGHT_MODEL_TWO_SIDE, enable ? GL.GL_TRUE : GL.GL_FALSE);
    }

    @Override
    protected void glEnableLineAntiAliasing(boolean enable) {
        glEnable(GL.GL_LINE_SMOOTH, enable);
    }

    @Override
    protected void glLineWidth(double width) {
        getGL().glLineWidth((float) width);
    }

    @Override
    protected void glMaterialColor(LightColor component, @Const Vector4 color) {
        color.get(transferBuffer, 0);
        transferBuffer.rewind();

        int c = getGLLight(component);
        if (component == LightColor.DIFFUSE) {
            getGL().glColor4fv(transferBuffer);
        } else {
            getGL().glMaterialfv(GL.GL_FRONT_AND_BACK, c, transferBuffer);
        }
    }

    @Override
    protected void glMaterialShininess(double shininess) {
        getGL().glMaterialf(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_SHININESS, (float) shininess);
    }

    @Override
    protected void glEnablePointAntiAliasing(boolean enable) {
        glEnable(GL2ES1.GL_POINT_SMOOTH, enable);
    }

    @Override
    protected void glPointWidth(double width) {
        getGL().glPointSize((float) width);
    }

    @Override
    protected void glEnablePolyAntiAliasing(boolean enable) {
        glEnable(GL2GL3.GL_POLYGON_SMOOTH, enable);
    }

    @Override
    protected void glBindTexture(Target target, TextureHandle image) {
        if (supportedTargets.contains(target)) {
            int glTarget = Utils.getGLTextureTarget(target);

            GL2 gl = getGL();
            if (image == null) {
                ((JoglContext) context).bindTexture(gl, glTarget, 0);
            } else {
                ((JoglContext) context).bindTexture(gl, glTarget, image.texID);
            }
        }
    }

    @Override
    protected void glEnableTexture(Target target, boolean enable) {
        if (supportedTargets.contains(target)) {
            int type = Utils.getGLTextureTarget(target);
            glEnable(type, enable);
        }
    }

    @Override
    protected void glTextureColor(@Const Vector4 color) {
        color.get(transferBuffer, 0);
        transferBuffer.rewind();

        getGL().glTexEnvfv(GL2ES1.GL_TEXTURE_ENV, GL2ES1.GL_TEXTURE_ENV_COLOR, transferBuffer);
    }

    @Override
    protected void glCombineFunction(CombineFunction func, boolean rgb) {
        if (supportsCombine) {
            int c = Utils.getGLCombineFunc(func);
            int target = (rgb ? GL2ES1.GL_COMBINE_RGB : GL2ES1.GL_COMBINE_ALPHA);
            getGL().glTexEnvi(GL2ES1.GL_TEXTURE_ENV, target, c);
        }
    }

    @Override
    protected void glCombineSrc(int operand, CombineSource src, boolean rgb) {
        if (!supportsCombine) {
            return;
        }

        int o = Utils.getGLCombineSrc(src);
        int target = -1;
        if (rgb) {
            switch (operand) {
            case 0:
                target = GL2.GL_SOURCE0_RGB;
                break;
            case 1:
                target = GL2.GL_SOURCE1_RGB;
                break;
            case 2:
                target = GL2.GL_SOURCE2_RGB;
                break;
            }
        } else {
            switch (operand) {
            case 0:
                target = GL2.GL_SOURCE0_ALPHA;
                break;
            case 1:
                target = GL2.GL_SOURCE1_ALPHA;
                break;
            case 2:
                target = GL2.GL_SOURCE2_ALPHA;
                break;
            }
        }

        getGL().glTexEnvi(GL2ES1.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glCombineOp(int operand, CombineOperand op, boolean rgb) {
        if (!supportsCombine) {
            return;
        }

        int o = Utils.getGLCombineOp(op);
        int target = -1;
        if (rgb) {
            switch (operand) {
            case 0:
                target = GL2ES1.GL_OPERAND0_RGB;
                break;
            case 1:
                target = GL2ES1.GL_OPERAND1_RGB;
                break;
            case 2:
                target = GL2ES1.GL_OPERAND2_RGB;
                break;
            }
        } else {
            switch (operand) {
            case 0:
                target = GL2ES1.GL_OPERAND0_ALPHA;
                break;
            case 1:
                target = GL2ES1.GL_OPERAND1_ALPHA;
                break;
            case 2:
                target = GL2ES1.GL_OPERAND2_ALPHA;
                break;
            }
        }

        getGL().glTexEnvi(GL2ES1.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glTexGen(TexCoord coord, TexCoordSource gen) {
        if (gen == TexCoordSource.ATTRIBUTE) {
            return; // don't need to do anything, it's already disabled
        }
        if ((gen == TexCoordSource.REFLECTION || gen == TexCoordSource.NORMAL) &&
            !supportedTargets.contains(Target.T_CUBEMAP)) {
            gen = TexCoordSource.OBJECT;
        }

        int mode = Utils.getGLTexGen(gen);
        int tc = Utils.getGLTexCoord(coord, false);
        getGL().glTexGeni(tc, GL2ES1.GL_TEXTURE_GEN_MODE, mode);
    }

    @Override
    protected void glEnableTexGen(TexCoord coord, boolean enable) {
        glEnable(Utils.getGLTexCoord(coord, true), enable);
    }

    @Override
    protected void glTexEyePlane(TexCoord coord, @Const Vector4 plane) {
        plane.get(transferBuffer, 0);
        transferBuffer.rewind();

        int tc = Utils.getGLTexCoord(coord, false);
        getGL().glTexGenfv(tc, GL2.GL_EYE_PLANE, transferBuffer);
    }

    @Override
    protected void glTexObjPlane(TexCoord coord, @Const Vector4 plane) {
        plane.get(transferBuffer, 0);
        transferBuffer.rewind();

        int tc = Utils.getGLTexCoord(coord, false);
        getGL().glTexGenfv(tc, GL2.GL_OBJECT_PLANE, transferBuffer);
    }

    @Override
    protected void glActiveTexture(int unit) {
        if (supportsMultitexture) {
            ((JoglContext) context).setActiveTexture(getGL(), unit);
        }
    }

    @Override
    protected void glActiveClientTexture(int unit) {
        if (supportsMultitexture) {
            getGL().glClientActiveTexture(GL.GL_TEXTURE0 + unit);
        }
    }

    @Override
    protected void glBindArrayVbo(VertexBufferObjectHandle h) {
        JoglContext ctx = (JoglContext) context;
        GL2 gl = getGL();

        if (h != null) {
            if (h.mode != StorageMode.IN_MEMORY) {
                // Must bind the VBO
                ctx.bindArrayVbo(gl, h.vboID);
            } else {
                // Must unbind any old VBO, will grab the in-memory buffer during render call
                ctx.bindArrayVbo(gl, 0);
            }
        } else {
            // Must unbind the vbo
            ctx.bindArrayVbo(gl, 0);
        }
    }

    @Override
    protected void glAttributePointer(VertexTarget target, VertexBufferObjectHandle h, int offset, int stride,
                                      int elementSize) {
        int strideBytes = (stride + elementSize) * h.dataType.getByteCount();

        if (h.mode == StorageMode.IN_MEMORY) {
            h.inmemoryBuffer.clear().position(offset);

            switch (target) {
            case NORMALS:
                getGL().glNormalPointer(GL.GL_FLOAT, strideBytes, h.inmemoryBuffer);
                break;
            case TEXCOORDS:
                getGL().glTexCoordPointer(elementSize, GL.GL_FLOAT, strideBytes, h.inmemoryBuffer);
                break;
            case VERTICES:
                getGL().glVertexPointer(elementSize, GL.GL_FLOAT, strideBytes, h.inmemoryBuffer);
                break;
            case COLORS:
                getGL().glColorPointer(elementSize, GL.GL_FLOAT, strideBytes, h.inmemoryBuffer);
                break;
            }
        } else {
            int vboOffset = offset * h.dataType.getByteCount();

            switch (target) {
            case NORMALS:
                getGL().glNormalPointer(GL.GL_FLOAT, strideBytes, vboOffset);
                break;
            case TEXCOORDS:
                getGL().glTexCoordPointer(elementSize, GL.GL_FLOAT, strideBytes, vboOffset);
                break;
            case VERTICES:
                getGL().glVertexPointer(elementSize, GL.GL_FLOAT, strideBytes, vboOffset);
                break;
            case COLORS:
                getGL().glColorPointer(elementSize, GL.GL_FLOAT, strideBytes, vboOffset);
            }
        }
    }

    @Override
    protected void glEnableAttribute(VertexTarget target, boolean enable) {
        int state = 0;
        switch (target) {
        case NORMALS:
            state = GLPointerFunc.GL_NORMAL_ARRAY;
            break;
        case TEXCOORDS:
            state = GLPointerFunc.GL_TEXTURE_COORD_ARRAY;
            break;
        case VERTICES:
            state = GLPointerFunc.GL_VERTEX_ARRAY;
            break;
        case COLORS:
            state = GLPointerFunc.GL_COLOR_ARRAY;
            break;
        }

        if (enable) {
            getGL().glEnableClientState(state);
        } else {
            getGL().glDisableClientState(state);
        }
    }

    private GL2 getGL() {
        return ((JoglContext) context).getGLContext().getGL().getGL2();
    }

    private void glEnable(int flag, boolean enable) {
        if (enable) {
            getGL().glEnable(flag);
        } else {
            getGL().glDisable(flag);
        }
    }

    private int getGLLight(LightColor c) {
        switch (c) {
        case AMBIENT:
            return GLLightingFunc.GL_AMBIENT;
        case DIFFUSE:
            return GLLightingFunc.GL_DIFFUSE;
        case EMISSIVE:
            return GLLightingFunc.GL_EMISSION;
        case SPECULAR:
            return GLLightingFunc.GL_SPECULAR;
        }
        return -1;
    }
}
