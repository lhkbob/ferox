package com.ferox.renderer.impl.jogl;

import java.util.EnumSet;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.AbstractFixedFunctionRenderer;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.renderer.impl.ResourceManager;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * JoglFixedFunctionRenderer is a complete implementation of
 * FixedFunctionRenderer that uses a {@link JoglRendererDelegate} for the JOGL
 * OpenGL binding.
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
    private final float[] matrixBuffer;
    private final float[] vector4Buffer;
    private final float[] vector3Buffer;
    
    // state tracking
    private boolean alphaTestEnabled;
    
    public JoglFixedFunctionRenderer(JoglRendererDelegate delegate) {
        super(delegate);
        
        initialized = false;
        
        matrixBuffer = new float[16];
        vector4Buffer = new float[4];
        vector3Buffer = new float[3];
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
            GL2 gl = getGL();
            gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            
            initialized = true;
        }
    }

    @Override
    protected void glMatrixMode(MatrixMode mode) {
        switch(mode) {
        case MODELVIEW:
            getGL().glMatrixMode(GL2.GL_MODELVIEW);
            break;
        case PROJECTION:
            getGL().glMatrixMode(GL2.GL_PROJECTION);
            break;
        case TEXTURE:
            getGL().glMatrixMode(GL2.GL_TEXTURE);
            break;
        }
    }

    @Override
    protected void glSetMatrix(ReadOnlyMatrix4f matrix) {
        matrix.get(matrixBuffer, 0, false);
        getGL().glLoadMatrixf(matrixBuffer, 0);
    }

    @Override
    protected void glAlphaTest(Comparison test, float ref) {
        if (test == Comparison.ALWAYS) {
            if (alphaTestEnabled) {
                alphaTestEnabled = false;
                glEnable(GL2.GL_ALPHA_TEST, false);
            }
        } else {
            if (!alphaTestEnabled) {
                alphaTestEnabled = true;
                glEnable(GL2.GL_ALPHA_TEST, true);
            }
            
            getGL().glAlphaFunc(Utils.getGLPixelTest(test), ref);
        }
    }

    @Override
    protected void glFogColor(ReadOnlyVector4f color) {
        color.get(vector4Buffer, 0);
        getGL().glFogfv(GL2.GL_FOG_COLOR, vector4Buffer, 0);
    }

    @Override
    protected void glEnableFog(boolean enable) {
        glEnable(GL2.GL_FOG, enable);
    }

    @Override
    protected void glFogDensity(float density) {
        getGL().glFogf(GL2.GL_FOG_DENSITY, density);
    }

    @Override
    protected void glFogRange(float start, float end) {
        getGL().glFogf(GL2.GL_FOG_START, start);
        getGL().glFogf(GL2.GL_FOG_END, end);
    }

    @Override
    protected void glFogMode(FogMode fog) {
        switch(fog) {
        case EXP:
            getGL().glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP);
            break;
        case EXP_SQUARED:
            getGL().glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP2);
            break;
        case LINEAR:
            getGL().glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
            break;
        }
    }

    @Override
    protected void glGlobalLighting(ReadOnlyVector4f ambient) {
        ambient.get(vector4Buffer, 0);
        getGL().glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, vector4Buffer, 0);
    }
    
    

    @Override
    protected void glLightColor(int light, LightColor lc, ReadOnlyVector4f color) {
        color.get(vector4Buffer, 0);
        int c = getGLLight(lc);
        getGL().glLightfv(GL2.GL_LIGHT0 + light, c, vector4Buffer, 0);
    }

    @Override
    protected void glEnableLight(int light, boolean enable) {
        glEnable(GL2.GL_LIGHT0 + light, enable);
    }

    @Override
    protected void glLightPosition(int light, ReadOnlyVector4f pos) {
        pos.get(vector4Buffer, 0);
        getGL().glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_POSITION, vector4Buffer, 0);
    }

    @Override
    protected void glLightDirection(int light, ReadOnlyVector3f dir) {
        dir.get(vector3Buffer, 0);
        getGL().glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_SPOT_DIRECTION, vector3Buffer, 0);
    }

    @Override
    protected void glLightAngle(int light, float angle) {
        getGL().glLightf(GL2.GL_LIGHT0 + light, GL2.GL_SPOT_CUTOFF, angle);
    }

    @Override
    protected void glLightAttenuation(int light, float constant, float linear, float quadratic) {
        light += GL2.GL_LIGHT0;
        GL2 gl = getGL();
        gl.glLightf(light, GL2.GL_CONSTANT_ATTENUATION, constant);
        gl.glLightf(light, GL2.GL_LINEAR_ATTENUATION, linear);
        gl.glLightf(light, GL2.GL_QUADRATIC_ATTENUATION, quadratic);
    }

    @Override
    protected void glEnableLighting(boolean enable) {
        glEnable(GL2.GL_LIGHTING, enable);
    }

    @Override
    protected void glEnableSmoothShading(boolean enable) {
        getGL().glShadeModel(enable ? GL2.GL_SMOOTH : GL2.GL_FLAT);
    }

    @Override
    protected void glEnableTwoSidedLighting(boolean enable) {
        getGL().glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, enable ? GL2.GL_TRUE : GL2.GL_FALSE);
    }

    @Override
    protected void glEnableLineAntiAliasing(boolean enable) {
        glEnable(GL.GL_LINE_SMOOTH, enable);
    }

    @Override
    protected void glLineWidth(float width) {
        getGL().glLineWidth(width);
    }

    @Override
    protected void glMaterialColor(LightColor component, ReadOnlyVector4f color) {
        color.get(vector4Buffer, 0);
        int c = getGLLight(component);
        if (component == LightColor.DIFFUSE)
            getGL().glColor4fv(vector4Buffer, 0);
        else
            getGL().glMaterialfv(GL.GL_FRONT_AND_BACK, c, vector4Buffer, 0);
    }

    @Override
    protected void glMaterialShininess(float shininess) {
        getGL().glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shininess);
    }

    @Override
    protected void glEnablePointAntiAliasing(boolean enable) {
        glEnable(GL2.GL_POINT_SMOOTH, enable);
    }

    @Override
    protected void glPointWidth(float width) {
        getGL().glPointSize(width);
    }

    @Override
    protected void glEnablePolyAntiAliasing(boolean enable) {
        glEnable(GL2.GL_POLYGON_SMOOTH, enable);
    }

    @Override
    protected void glBindTexture(Target target, ResourceHandle image) {
        if (supportedTargets.contains(target)) {
            int glTarget = Utils.getGLTextureTarget(target);
            TextureHandle th = (TextureHandle) image;
            
            GL2 gl = getGL();
            if (th == null)
                ((JoglContext) context).bindTexture(gl, glTarget, 0);
            else
                ((JoglContext) context).bindTexture(gl, glTarget, th.texID);
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
    protected void glTextureColor(ReadOnlyVector4f color) {
        color.get(vector4Buffer, 0);
        getGL().glTexEnvfv(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_COLOR, vector4Buffer, 0);
    }

    @Override
    protected void glCombineFunction(CombineFunction func, boolean rgb) {
        if (supportsCombine) {
            int c = Utils.getGLCombineFunc(func);
            int target = (rgb ? GL2.GL_COMBINE_RGB : GL2.GL_COMBINE_ALPHA);
            getGL().glTexEnvi(GL2.GL_TEXTURE_ENV, target, c);
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
            case 0: target = GL2.GL_SOURCE0_RGB; break;
            case 1: target = GL2.GL_SOURCE1_RGB; break;
            case 2: target = GL2.GL_SOURCE2_RGB; break;
            }
        } else {
            switch(operand) {
            case 0: target = GL2.GL_SOURCE0_ALPHA; break;
            case 1: target = GL2.GL_SOURCE1_ALPHA; break;
            case 2: target = GL2.GL_SOURCE2_ALPHA; break;
            }
        }
        
        getGL().glTexEnvi(GL2.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glCombineOp(int operand, CombineOp op, boolean rgb) {
        if (!supportsCombine)
            return;
        
        int o = Utils.getGLCombineOp(op);
        int target = -1;
        if (rgb) {
            switch(operand) {
            case 0: target = GL2.GL_OPERAND0_RGB; break;
            case 1: target = GL2.GL_OPERAND1_RGB; break;
            case 2: target = GL2.GL_OPERAND2_RGB; break;
            }
        } else {
            switch(operand) {
            case 0: target = GL2.GL_OPERAND0_ALPHA; break;
            case 1: target = GL2.GL_OPERAND1_ALPHA; break;
            case 2: target = GL2.GL_OPERAND2_ALPHA; break;
            }
        }
        
        getGL().glTexEnvi(GL2.GL_TEXTURE_ENV, target, o);
    }

    @Override
    protected void glTexGen(TexCoord coord, TexCoordSource gen) {
        if (gen == TexCoordSource.ATTRIBUTE)
            return; // don't need to do anything, it's already disabled
        if ((gen == TexCoordSource.REFLECTION || gen == TexCoordSource.NORMAL) && !supportedTargets.contains(Target.T_CUBEMAP))
            gen = TexCoordSource.OBJECT;
        
        int mode = Utils.getGLTexGen(gen);
        int tc = Utils.getGLTexCoord(coord, false);
        getGL().glTexGeni(tc, GL2.GL_TEXTURE_GEN_MODE, mode);
    }

    @Override
    protected void glEnableTexGen(TexCoord coord, boolean enable) {
        glEnable(Utils.getGLTexCoord(coord, true), enable);
    }

    @Override
    protected void glTexEyePlane(TexCoord coord, ReadOnlyVector4f plane) {
        plane.get(vector4Buffer, 0);
        int tc = Utils.getGLTexCoord(coord, false);
        getGL().glTexGenfv(tc, GL2.GL_EYE_PLANE, vector4Buffer, 0);
    }

    @Override
    protected void glTexEnvMode(EnvMode mode) {
        if (mode == EnvMode.COMBINE && !supportsCombine)
            mode = EnvMode.MODULATE;
        
        int envMode = Utils.getGLTexEnvMode(mode);
        getGL().glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, envMode);
    }

    @Override
    protected void glTexObjPlane(TexCoord coord, ReadOnlyVector4f plane) {
        plane.get(vector4Buffer, 0);
        int tc = Utils.getGLTexCoord(coord, false);
        getGL().glTexGenfv(tc, GL2.GL_OBJECT_PLANE, vector4Buffer, 0);
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
    protected void glBindArrayVbo(ResourceHandle handle) {
        JoglContext ctx = (JoglContext) context;
        GL2 gl = getGL();
        VertexBufferObjectHandle h = (VertexBufferObjectHandle) handle;
        
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
    protected void glAttributePointer(VertexTarget target, ResourceHandle handle, int offset,
                                      int stride, int elementSize) {
        VertexBufferObjectHandle h = (VertexBufferObjectHandle) handle;
        
        if (h.mode == StorageMode.IN_MEMORY) {
            h.inmemoryBuffer.clear().position(offset);
           
            switch(target) {
            case NORMALS:
                getGL().glNormalPointer(GL2.GL_FLOAT, stride, h.inmemoryBuffer);
                break;
            case TEXCOORDS:
                getGL().glTexCoordPointer(elementSize, GL2.GL_FLOAT, stride, h.inmemoryBuffer);
                break;
            case VERTICES:
                getGL().glVertexPointer(elementSize, GL2.GL_FLOAT, stride, h.inmemoryBuffer);
                break;
            }
        } else {
            int vboOffset = offset * h.dataType.getByteCount();
            
            switch(target) {
            case NORMALS:
                getGL().glNormalPointer(GL2.GL_FLOAT, stride, vboOffset);
                break;
            case TEXCOORDS:
                getGL().glTexCoordPointer(elementSize, GL2.GL_FLOAT, stride, vboOffset);
                break;
            case VERTICES:
                getGL().glVertexPointer(elementSize, GL2.GL_FLOAT, stride, vboOffset);
                break;
            }
        }
    }

    @Override
    protected void glEnableAttribute(VertexTarget target, boolean enable) {
        switch(target) {
        case NORMALS:
            getGL().glEnableClientState(GL2.GL_NORMAL_ARRAY);
            break;
        case TEXCOORDS:
            getGL().glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
            break;
        case VERTICES:
            getGL().glEnableClientState(GL2.GL_VERTEX_ARRAY);
        }
    }
    
    private GL2 getGL() {
        return ((JoglContext) context).getGLContext().getGL().getGL2();
    }
    
    private void glEnable(int flag, boolean enable) {
        if (enable)
            getGL().glEnable(flag);
        else
            getGL().glDisable(flag);
    }
    
    private int getGLLight(LightColor c) {
        switch(c) {
        case AMBIENT: return GL2.GL_AMBIENT;
        case DIFFUSE: return GL2.GL_DIFFUSE;
        case EMISSIVE: return GL2.GL_EMISSION;
        case SPECULAR: return GL2.GL_SPECULAR;
        }
        return -1;
    }
}
