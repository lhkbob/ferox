package com.ferox.renderer.impl.jogl;

import java.util.EnumSet;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import com.ferox.math.Color4f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.AbstractFixedFunctionRenderer;
import com.ferox.renderer.impl.resource.GeometryHandle;
import com.ferox.renderer.impl.resource.ResourceHandle;
import com.ferox.renderer.impl.resource.VertexArray;
import com.ferox.resource.Geometry;
import com.ferox.resource.Texture;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.resource.Texture.Target;

public class JoglFixedFunctionRenderer extends AbstractFixedFunctionRenderer {
    // capabilities
    private final boolean supportsMultitexture;
    private final boolean supportsCombine;
    private final EnumSet<Target> supportedTargets;
    
    // context cache to support faster GL lookups
    private JoglContext context;
    
    private final float[] colorBuffer;
    private final float[] matrixBuffer;
    private final float[] vector4Buffer;
    private final float[] vector3Buffer;
    
    // state tracking
    private boolean alphaTestEnabled;
    private VertexArray boundVertices;
    private VertexArray boundNormals;
    private final VertexArray[] boundTexCoords;
    
    private GeometryHandle lastGeometry;

    /**
     * Create a new JoglFixedFunctionRendererer that is paired with the given
     * JoglContext, and is to be used within the given JoglFramework.
     * 
     * @param framework The JoglFramework that created the JoglContext
     */
    public JoglFixedFunctionRenderer(JoglFramework framework) {
        super(new JoglRendererDelegate(framework.getCapabilities()), framework);
        
        RenderCapabilities caps = framework.getCapabilities();
        supportsMultitexture = caps.getMaxFixedPipelineTextures() > 1;
        supportsCombine = caps.getCombineEnvModeSupport();
        supportedTargets = caps.getSupportedTextureTargets();
        
        colorBuffer = new float[4];
        matrixBuffer = new float[16];
        vector4Buffer = new float[4];
        vector3Buffer = new float[3];
        alphaTestEnabled = false;
        
        boundVertices = null;
        boundNormals = null;
        boundTexCoords = new VertexArray[texBindings.length];
    }
    
    private GL2 getGL() {
        // we cache the context the first time we need it,
        // a renderer will not be passed around amongst contexts
        if (context == null)
            context = JoglContext.getCurrent();
        return context.getGL2();
    }
    
    private void glEnable(int flag, boolean enable) {
        if (enable)
            getGL().glEnable(flag);
        else
            getGL().glDisable(flag);
    }

    @Override
    protected void glActiveTexture(int unit) {
        if (supportsMultitexture) {
            GL2 gl = getGL();
            context.getRecord().setActiveTexture(gl, unit);
        } // else unit will always be 0 anyway
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
    protected void glEnableFog(boolean enable) {
        glEnable(GL2.GL_FOG, enable);
    }

    @Override
    protected void glEnableLight(int light, boolean enable) {
        glEnable(GL2.GL_LIGHT0 + light, enable);
    }

    @Override
    protected void glEnableLighting(boolean enable) {
        glEnable(GL2.GL_LIGHTING, enable);
    }

    @Override
    protected void glEnableLineAntiAliasing(boolean enable) {
        glEnable(GL2.GL_LINE_SMOOTH, enable);
    }

    @Override
    protected void glEnablePointAntiAliasing(boolean enable) {
        glEnable(GL2.GL_POINT_SMOOTH, enable);
    }

    @Override
    protected void glEnablePolyAntiAliasing(boolean enable) {
        glEnable(GL2.GL_POLYGON_SMOOTH, enable);
    }

    @Override
    protected void glEnableSmoothShading(boolean enable) {
        getGL().glShadeModel(enable ? GL2.GL_SMOOTH : GL2.GL_FLAT);
    }

    @Override
    protected void glEnableTexture(Target target, boolean enable) {
        if (supportedTargets.contains(target)) {
            int type = Utils.getGLTextureTarget(target);
            glEnable(type, enable);
        }
    }

    @Override
    protected void glEnableTwoSidedLighting(boolean enable) {
        getGL().glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, (enable ? GL.GL_TRUE : GL.GL_FALSE));
    }

    @Override
    protected void glFogColor(Color4f color) {
        Utils.get(color, colorBuffer);
        getGL().glFogfv(GL2.GL_FOG_COLOR, colorBuffer, 0);
    }

    @Override
    protected void glFogDensity(float density) {
        getGL().glFogf(GL2.GL_FOG_DENSITY, density);
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
    protected void glFogRange(float start, float end) {
        getGL().glFogf(GL2.GL_FOG_START, start);
        getGL().glFogf(GL2.GL_FOG_END, end);
    }

    @Override
    protected void glGlobalLighting(Color4f ambient) {
        Utils.get(ambient, colorBuffer);
        getGL().glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, colorBuffer, 0);
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
    protected void glLightColor(int light, LightColor lc, Color4f color) {
        Utils.get(color, colorBuffer);
        int c = getGLLight(lc);
        getGL().glLightfv(GL2.GL_LIGHT0 + light, c, colorBuffer, 0);
    }

    @Override
    protected void glLightDirection(int light, ReadOnlyVector3f dir) {
        dir.get(vector3Buffer, 0);
        getGL().glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_SPOT_DIRECTION, vector3Buffer, 0);
    }

    @Override
    protected void glLightPosition(int light, ReadOnlyVector4f pos) {
        pos.get(vector4Buffer, 0);
        getGL().glLightfv(GL2.GL_LIGHT0 + light, GL2.GL_POSITION, vector4Buffer, 0);
    }

    @Override
    protected void glLineWidth(float width) {
        getGL().glLineWidth(width);
    }

    @Override
    protected void glMaterialColor(LightColor component, Color4f color) {
        Utils.get(color, colorBuffer);
        int c = getGLLight(component);
        if (component == LightColor.DIFFUSE)
            getGL().glColor4fv(colorBuffer, 0);
        else
            getGL().glMaterialfv(GL.GL_FRONT_AND_BACK, c, colorBuffer, 0);
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

    @Override
    protected void glMaterialShininess(float shininess) {
        getGL().glMaterialf(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shininess);
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
    protected void glPointWidth(float width) {
        getGL().glPointSize(width);
    }

    @Override
    protected void glSetMatrix(ReadOnlyMatrix4f matrix) {
        matrix.get(matrixBuffer, 0, false);
        getGL().glLoadMatrixf(matrixBuffer, 0);
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
    protected void glTexEnvMode(EnvMode mode) {
        if (mode == EnvMode.COMBINE && !supportsCombine)
            mode = EnvMode.MODULATE;
        
        int envMode = Utils.getGLTexEnvMode(mode);
        getGL().glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, envMode);
    }

    @Override
    protected void glTexEyePlane(TexCoord coord, ReadOnlyVector4f plane) {
        plane.get(vector4Buffer, 0);
        int tc = Utils.getGLTexCoord(coord, false);
        getGL().glTexGenfv(tc, GL2.GL_EYE_PLANE, vector4Buffer, 0);
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
    protected void glTexObjPlane(TexCoord coord, Vector4f plane) {
        plane.get(vector4Buffer, 0);
        int tc = Utils.getGLTexCoord(coord, false);
        getGL().glTexGenfv(tc, GL2.GL_OBJECT_PLANE, vector4Buffer, 0);
    }

    @Override
    protected void glTextureColor(Color4f color) {
        Utils.get(color, colorBuffer);
        getGL().glTexEnvfv(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_COLOR, colorBuffer, 0);
    }
    
    @Override
    protected void glBindTexture(Target target, Texture img) {
        if (supportedTargets.contains(target)) {
            int glTarget = Utils.getGLTextureTarget(target);
            ResourceHandle handle = (img == null ? null : resourceManager.getHandle(img));

            // the BoundObjectState takes care of the same id for us
            GL2 gl = getGL();
            if (handle == null) {
                context.getRecord().bindTexture(gl, glTarget, 0);
            } else {
                context.getRecord().bindTexture(gl, glTarget, handle.getId());
            }
        }
    }
    
    @Override
    public void reset() {
        GL2 gl = getGL();
        BoundObjectState state = context.getRecord();
        // ensure there is no glsl program in use
        state.bindGlslProgram(gl, 0);
        
        super.reset();
        
        // unbind vbos
        state.bindArrayVbo(gl, 0);
        state.bindElementVbo(gl, 0);
        
        // disable all vertex pointers
        if (boundVertices != null) {
            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            boundVertices = null;
        }
        if (boundNormals != null) {
            gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
            boundNormals = null;
        }
        for (int i = 0; i < texBindings.length; i++) {
            if (boundTexCoords[i] != null) {
                if (supportsMultitexture)
                    gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
                boundTexCoords[i] = null;
            }
        }
        
        // reset geom tracker
        lastGeometry = null;
        
        // clear context cache
        context = null;
    }
    
    /*
     * Must reset the geometry handle any time the binding changes so
     * that the vbo's get correctly updated during the next rener,
     * even if it's the same geometry instance.
     */
    
    @Override
    public void setVertexBinding(String name) {
        super.setVertexBinding(name);
        lastGeometry = null;
    }
    
    @Override
    public void setNormalBinding(String name) {
        super.setNormalBinding(name);
        lastGeometry = null;
    }
    
    @Override
    public void setTextureCoordinateBinding(int tex, String name) {
        super.setTextureCoordinateBinding(tex, name);
        lastGeometry = null;
    }

    @Override
    public int render(Geometry geom) {
        ResourceHandle handle = resourceManager.getHandle(geom);
        if (handle != null) {
            super.render(geom);
            return renderImpl((GeometryHandle) handle);
        } else
            return 0;
    }
    
    private int renderImpl(GeometryHandle handle) {
        GL2 gl = getGL();
        BoundObjectState state = context.getRecord();
        
        VertexArray vertices = getVertexArray(handle, vertexBinding);
        if (vertices == null || vertices.elementSize == 1)
            return 0; // can't use this va as vertices

        boolean useVbos = handle.compile != CompileType.NONE;
        int vertexCount = (useVbos ? vertices.vboLen / (4 * vertices.elementSize) : vertices.buffer.capacity() / vertices.elementSize);
        
        // BoundObjectState takes care of the same id for us
        if (lastGeometry != handle) {
            if (!useVbos) {
                state.bindArrayVbo(gl, 0);
                state.bindElementVbo(gl, 0);
            } else {
                state.bindArrayVbo(gl, handle.arrayVbo);
                state.bindElementVbo(gl, handle.elementVbo);
            }

            if (boundVertices != vertices) {
                if (boundVertices == null)
                    gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
                glVertexPointer(gl, vertices, useVbos);
                boundVertices = vertices;
            }

            VertexArray normals = getVertexArray(handle, normalBinding);
            if (lightingEnabled && normals != null && normals.elementSize == 3) {
                if (boundNormals != normals) {
                    // update pointer
                    if (boundNormals == null)
                        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
                    glNormalPointer(gl, normals, useVbos);
                    boundNormals = normals;
                }
            } else {
                // don't send normals
                if (boundNormals != null) {
                    gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
                    boundNormals = null;
                }
            }

            VertexArray tcs;
            for (int i = 0; i < texBindings.length; i++) {
                tcs = getVertexArray(handle, texBindings[i]);
                if (textures[i].enabled && state.getTexture(i) != 0 && tcs != null) {
                    if (boundTexCoords[i] != tcs) {
                        // update pointer
                        if (supportsMultitexture)
                            gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                        if (boundTexCoords[i] == null)
                            gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
                        glTexCoordPointer(gl, tcs, useVbos);
                        boundTexCoords[i] = tcs;
                    }
                } else {
                    // disable texcoords
                    if (boundTexCoords[i] != null) {
                        if (supportsMultitexture)
                            gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                        gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
                        boundTexCoords[i] = null;
                    }
                }
            }
        }
        
        int glPolyType = Utils.getGLPolygonConnectivity(handle.polyType);
        if (useVbos)
            gl.glDrawRangeElements(glPolyType, handle.minIndex, handle.maxIndex, 
                                   handle.indexCount, GL2.GL_UNSIGNED_INT, 0);
        else
            gl.glDrawRangeElements(glPolyType, handle.minIndex, handle.maxIndex, 
                                   handle.indexCount, GL2.GL_UNSIGNED_INT, handle.indices.rewind());
        
        lastGeometry = handle;
        context.getFrameStatistics().add(1, vertexCount, handle.polyCount);
        return handle.polyCount;
    }
    
    private void glVertexPointer(GL2 gl, VertexArray vertices, boolean vbo) {
        if (vbo)
            gl.glVertexPointer(vertices.elementSize, GL.GL_FLOAT, 0, vertices.offset);
        else
            gl.glVertexPointer(vertices.elementSize, GL.GL_FLOAT, 0, vertices.buffer.rewind());
    }
    
    private void glNormalPointer(GL2 gl, VertexArray normals, boolean vbo) {
        if (vbo)
            gl.glNormalPointer(GL.GL_FLOAT, 0, normals.offset);
        else
            gl.glNormalPointer(GL.GL_FLOAT, 0, normals.buffer.rewind());
    }
    
    private void glTexCoordPointer(GL2 gl, VertexArray tcs, boolean vbo) {
        if (vbo)
            gl.glTexCoordPointer(tcs.elementSize, GL.GL_FLOAT, 0, tcs.offset);
        else
            gl.glTexCoordPointer(tcs.elementSize, GL.GL_FLOAT, 0, tcs.buffer.rewind());
    }
    
    private VertexArray getVertexArray(GeometryHandle handle, String name) {
        if (name == null)
            return null;
        
        VertexArray arr;
        int len = handle.compiledPointers.size();
        for (int i = 0; i < len; i++) {
            arr = handle.compiledPointers.get(i);
            if (arr.name.equals(name))
                return arr;
        }
        // couldn't find a match
        return null;
    }

    @Override
    protected void init() {
        GL2 gl = getGL();
        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
    }
}
