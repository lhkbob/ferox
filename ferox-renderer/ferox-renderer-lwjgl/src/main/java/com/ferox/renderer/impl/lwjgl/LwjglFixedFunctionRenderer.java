package com.ferox.renderer.impl.lwjgl;

import java.nio.FloatBuffer;
import java.util.EnumSet;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.AbstractFixedFunctionRenderer;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.renderer.impl.ResourceManager;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * LwjglFixedFunctionRenderer is a complete implementation of
 * FixedFunctionRenderer that uses a {@link LwjglRendererDelegate} for the LWJGL
 * OpenGL binding.
 * 
 * @author Michael Ludwig
 */
public class LwjglFixedFunctionRenderer extends AbstractFixedFunctionRenderer {
 // capabilities
    private boolean supportsMultitexture;
    private boolean supportsCombine;
    private EnumSet<Target> supportedTargets;
    
    private boolean initialized;
    
    // math object transfer objects
    private final FloatBuffer matrixBuffer;
    private final FloatBuffer vector4Buffer;
    private final FloatBuffer vector3Buffer;
    
    // state tracking
    private boolean alphaTestEnabled;
    
    public LwjglFixedFunctionRenderer(LwjglRendererDelegate delegate) {
        super(delegate);
        
        initialized = false;
        
        matrixBuffer = BufferUtil.newFloatBuffer(16);
        vector4Buffer = BufferUtil.newFloatBuffer(4);
        vector3Buffer = BufferUtil.newFloatBuffer(3);
        alphaTestEnabled = false;
    }
    
    @Override
    public void activate(AbstractSurface surface, OpenGLContext context, ResourceManager manager) {
        super.activate(surface, context, manager);
        
        if (!initialized) {
            // detect caps
            RenderCapabilities caps = surface.getFramework().getCapabilities();
            supportsMultitexture = caps.getMaxFixedPipelineTextures() > 1;
            supportsCombine = caps.getCombineEnvModeSupport();
            supportedTargets = caps.getSupportedTextureTargets();
            
            // set initial state
            GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            
            initialized = true;
        }
    }

    @Override
    protected void glMatrixMode(MatrixMode mode) {
        switch(mode) {
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
        matrix.get(matrixBuffer, 0, false);
        GL11.glLoadMatrix(matrixBuffer);
    }

    @Override
    protected void glAlphaTest(Comparison test, float ref) {
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
            
            GL11.glAlphaFunc(Utils.getGLPixelTest(test), ref);
        }
    }

    @Override
    protected void glFogColor(@Const Vector4 color) {
        color.get(vector4Buffer, 0);
        GL11.glFog(GL11.GL_FOG_COLOR, vector4Buffer);
    }

    @Override
    protected void glEnableFog(boolean enable) {
        glEnable(GL11.GL_FOG, enable);
    }

    @Override
    protected void glFogDensity(float density) {
        GL11.glFogf(GL11.GL_FOG_DENSITY, density);
    }

    @Override
    protected void glFogRange(float start, float end) {
        GL11.glFogf(GL11.GL_FOG_START, start);
        GL11.glFogf(GL11.GL_FOG_END, end);
    }

    @Override
    protected void glFogMode(FogMode fog) {
        switch(fog) {
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
        ambient.get(vector4Buffer, 0);
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, vector4Buffer);
    }
    
    

    @Override
    protected void glLightColor(int light, LightColor lc, @Const Vector4 color) {
        color.get(vector4Buffer, 0);
        int c = getGLLight(lc);
        GL11.glLight(GL11.GL_LIGHT0 + light, c, vector4Buffer);
    }

    @Override
    protected void glEnableLight(int light, boolean enable) {
        glEnable(GL11.GL_LIGHT0 + light, enable);
    }

    @Override
    protected void glLightPosition(int light, @Const Vector4 pos) {
        pos.get(vector4Buffer, 0);
        GL11.glLight(GL11.GL_LIGHT0 + light, GL11.GL_POSITION, vector4Buffer);
    }

    @Override
    protected void glLightDirection(int light, @Const Vector3 dir) {
        dir.get(vector3Buffer, 0);
        GL11.glLight(GL11.GL_LIGHT0 + light, GL11.GL_SPOT_DIRECTION, vector3Buffer);
    }

    @Override
    protected void glLightAngle(int light, float angle) {
        GL11.glLightf(GL11.GL_LIGHT0 + light, GL11.GL_SPOT_CUTOFF, angle);
    }

    @Override
    protected void glLightAttenuation(int light, float constant, float linear, float quadratic) {
        light += GL11.GL_LIGHT0;
        GL11.glLightf(light, GL11.GL_CONSTANT_ATTENUATION, constant);
        GL11.glLightf(light, GL11.GL_LINEAR_ATTENUATION, linear);
        GL11.glLightf(light, GL11.GL_QUADRATIC_ATTENUATION, quadratic);
    }

    @Override
    protected void glEnableLighting(boolean enable) {
        glEnable(GL11.GL_LIGHTING, enable);
    }

    @Override
    protected void glEnableSmoothShading(boolean enable) {
        GL11.glShadeModel(enable ? GL11.GL_SMOOTH : GL11.GL_FLAT);
    }

    @Override
    protected void glEnableTwoSidedLighting(boolean enable) {
        GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, enable ? GL11.GL_TRUE : GL11.GL_FALSE);
    }

    @Override
    protected void glEnableLineAntiAliasing(boolean enable) {
        glEnable(GL11.GL_LINE_SMOOTH, enable);
    }

    @Override
    protected void glLineWidth(float width) {
        GL11.glLineWidth(width);
    }

    @Override
    protected void glMaterialColor(LightColor component, @Const Vector4 color) {
        int c = getGLLight(component);
        if (component == LightColor.DIFFUSE) {
            GL11.glColor4d(color.x, color.y, color.z, color.w);
        } else {
            color.get(vector4Buffer, 0);
            GL11.glMaterial(GL11.GL_FRONT_AND_BACK, c, vector4Buffer);
        }
    }

    @Override
    protected void glMaterialShininess(float shininess) {
        GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, shininess);
    }

    @Override
    protected void glEnablePointAntiAliasing(boolean enable) {
        glEnable(GL11.GL_POINT_SMOOTH, enable);
    }

    @Override
    protected void glPointWidth(float width) {
        GL11.glPointSize(width);
    }

    @Override
    protected void glEnablePolyAntiAliasing(boolean enable) {
        glEnable(GL11.GL_POLYGON_SMOOTH, enable);
    }

    @Override
    protected void glBindTexture(Target target, ResourceHandle image) {
        if (supportedTargets.contains(target)) {
            int glTarget = Utils.getGLTextureTarget(target);
            TextureHandle th = (TextureHandle) image;
            
            if (th == null)
                ((LwjglContext) context).bindTexture(glTarget, 0);
            else
                ((LwjglContext) context).bindTexture(glTarget, th.texID);
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
        color.get(vector4Buffer, 0);
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, vector4Buffer);
    }

    @Override
    protected void glCombineFunction(CombineFunction func, boolean rgb) {
        if (supportsCombine) {
            int c = Utils.getGLCombineFunc(func);
            int target = (rgb ? GL13.GL_COMBINE_RGB : GL13.GL_COMBINE_ALPHA);
            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, target, c);
        }
    }

    @Override
    protected void glCombineSrc(int operand, CombineSource src, boolean rgb) {
        if (!supportsCombine)
            return;
        
        int o = Utils.getGLCombineSrc(src);
        int target = -1;
        if (rgb) {
            switch(operand) {
            case 0: target = GL13.GL_SOURCE0_RGB; break;
            case 1: target = GL13.GL_SOURCE1_RGB; break;
            case 2: target = GL13.GL_SOURCE2_RGB; break;
            }
        } else {
            switch(operand) {
            case 0: target = GL13.GL_SOURCE0_ALPHA; break;
            case 1: target = GL13.GL_SOURCE1_ALPHA; break;
            case 2: target = GL13.GL_SOURCE2_ALPHA; break;
            }
        }
        
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glCombineOp(int operand, CombineOp op, boolean rgb) {
        if (!supportsCombine)
            return;
        
        int o = Utils.getGLCombineOp(op);
        int target = -1;
        if (rgb) {
            switch(operand) {
            case 0: target = GL13.GL_OPERAND0_RGB; break;
            case 1: target = GL13.GL_OPERAND1_RGB; break;
            case 2: target = GL13.GL_OPERAND2_RGB; break;
            }
        } else {
            switch(operand) {
            case 0: target = GL13.GL_OPERAND0_ALPHA; break;
            case 1: target = GL13.GL_OPERAND1_ALPHA; break;
            case 2: target = GL13.GL_OPERAND2_ALPHA; break;
            }
        }
        
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glTexGen(TexCoord coord, TexCoordSource gen) {
        if (gen == TexCoordSource.ATTRIBUTE)
            return; // don't need to do anything, it's already disabled
        if ((gen == TexCoordSource.REFLECTION || gen == TexCoordSource.NORMAL) && !supportedTargets.contains(Target.T_CUBEMAP))
            gen = TexCoordSource.OBJECT;
        
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
        plane.get(vector4Buffer, 0);
        int tc = Utils.getGLTexCoord(coord, false);
        GL11.glTexGen(tc, GL11.GL_EYE_PLANE, vector4Buffer);
    }

    @Override
    protected void glTexEnvMode(EnvMode mode) {
        if (mode == EnvMode.COMBINE && !supportsCombine)
            mode = EnvMode.MODULATE;
        
        int envMode = Utils.getGLTexEnvMode(mode);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, envMode);
    }

    @Override
    protected void glTexObjPlane(TexCoord coord, @Const Vector4 plane) {
        plane.get(vector4Buffer, 0);
        int tc = Utils.getGLTexCoord(coord, false);
        GL11.glTexGen(tc, GL11.GL_OBJECT_PLANE, vector4Buffer);
    }

    @Override
    protected void glActiveTexture(int unit) {
        if (supportsMultitexture) {
            ((LwjglContext) context).setActiveTexture(unit);
        }
    }

    @Override
    protected void glActiveClientTexture(int unit) {
        if (supportsMultitexture) {
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + unit);
        }
    }

    @Override
    protected void glBindArrayVbo(ResourceHandle handle) {
        LwjglContext ctx = (LwjglContext) context;
        VertexBufferObjectHandle h = (VertexBufferObjectHandle) handle;
        
        if (h != null) {
            if (h.mode != StorageMode.IN_MEMORY) {
                // Must bind the VBO
                ctx.bindArrayVbo(h.vboID);
            } else {
                // Must unbind any old VBO, will grab the in-memory buffer during render call
                ctx.bindArrayVbo(0);
            }
        } else {
            // Must unbind the vbo
            ctx.bindArrayVbo(0);
        }
    }

    @Override
    protected void glAttributePointer(VertexTarget target, ResourceHandle handle, int offset,
                                      int stride, int elementSize) {
        VertexBufferObjectHandle h = (VertexBufferObjectHandle) handle;
        int strideBytes = (elementSize + stride) * h.dataType.getByteCount();
        
        if (h.mode == StorageMode.IN_MEMORY) {
            h.inmemoryBuffer.clear().position(offset);
           
            switch(target) {
            case NORMALS:
                GL11.glNormalPointer(strideBytes, (FloatBuffer) h.inmemoryBuffer);
                break;
            case TEXCOORDS:
                GL11.glTexCoordPointer(elementSize, strideBytes, (FloatBuffer) h.inmemoryBuffer);
                break;
            case VERTICES:
                GL11.glVertexPointer(elementSize, strideBytes, (FloatBuffer) h.inmemoryBuffer);
                break;
            }
        } else {
            int vboOffset = offset * h.dataType.getByteCount();
            
            switch(target) {
            case NORMALS:
                GL11.glNormalPointer(GL11.GL_FLOAT, strideBytes, vboOffset);
                break;
            case TEXCOORDS:
                GL11.glTexCoordPointer(elementSize, GL11.GL_FLOAT, strideBytes, vboOffset);
                break;
            case VERTICES:
                GL11.glVertexPointer(elementSize, GL11.GL_FLOAT, strideBytes, vboOffset);
                break;
            }
        }
    }

    @Override
    protected void glEnableAttribute(VertexTarget target, boolean enable) {
        int state = 0;
        switch(target) {
        case NORMALS:
            state = GL11.GL_NORMAL_ARRAY;
            break;
        case TEXCOORDS:
            state = GL11.GL_TEXTURE_COORD_ARRAY;
            break;
        case VERTICES:
            state = GL11.GL_VERTEX_ARRAY;
            break;
        }
        
        if (enable)
            GL11.glEnableClientState(state);
        else
            GL11.glDisableClientState(state);
    }
    
    private void glEnable(int flag, boolean enable) {
        if (enable)
            GL11.glEnable(flag);
        else
            GL11.glDisable(flag);
    }
    
    private int getGLLight(LightColor c) {
        switch(c) {
        case AMBIENT: return GL11.GL_AMBIENT;
        case DIFFUSE: return GL11.GL_DIFFUSE;
        case EMISSIVE: return GL11.GL_EMISSION;
        case SPECULAR: return GL11.GL_SPECULAR;
        }
        return -1;
    }
}